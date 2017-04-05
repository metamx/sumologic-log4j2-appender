/**
 * _____ _____ _____ _____    __    _____ _____ _____ _____
 * |   __|  |  |     |     |  |  |  |     |   __|     |     |
 * |__   |  |  | | | |  |  |  |  |__|  |  |  |  |-   -|   --|
 * |_____|_____|_|_|_|_____|  |_____|_____|_____|_____|_____|
 * <p>
 * UNICORNS AT WARP SPEED SINCE 2010
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sumologic.log4j.aggregation;

import com.sumologic.log4j.http.SumoBufferFlusherThread;
import com.sumologic.log4j.http.SumoHttpSender;
import com.sumologic.log4j.queue.BufferWithEviction;
import org.apache.logging.log4j.Logger;

/**
 * @author Jose Muniz (jose@sumologic.com)
 */
public class SumoBufferFlusher
{
  private final long maxFlushTimeoutMs;
  private final Logger logger;
  private final SumoBufferFlusherThread flushingThread;
  private final long flushingAccuracy;
  private final boolean flushOnError;
  private final SumoBufferFlusherThread.ErrorLogObserver errorLogObserver;

  public SumoBufferFlusher(
      long flushingAccuracy,
      long messagesPerRequest,
      long maxFlushInterval,
      long maxFlushTimeoutMs,
      boolean flushOnError,
      SumoHttpSender sender,
      BufferWithEviction<byte[]> buffer,
      Logger logger
  )
  {
    this.flushingAccuracy = flushingAccuracy;
    this.maxFlushTimeoutMs = maxFlushTimeoutMs;
    this.flushOnError = flushOnError;
    this.logger = logger;

    this.errorLogObserver = new SumoBufferFlusherThread.ErrorLogObserver()
    {
      private volatile boolean hasError;
      @Override
      public void recordError(boolean hasError)
      {
        this.hasError = hasError && flushOnError;
      }

      @Override
      public boolean hasError()
      {
        return this.hasError;
      }
    };

    flushingThread = new SumoBufferFlusherThread(
        buffer,
        sender,
        flushingAccuracy,
        maxFlushInterval,
        messagesPerRequest,
        this.errorLogObserver
    );
  }

  public void start()
  {
    flushingThread.start();
  }

  public void stop() throws InterruptedException
  {
    flushingThread.setTerminating();
    flushingThread.interrupt();

    // Keep the current task running until it's done sending

    flushingThread.join(maxFlushTimeoutMs + flushingAccuracy + 1);
    if (flushingThread.isAlive()) {
      logger.warn("Timed out waiting for buffer flusher to finish.");
    }
  }

  public void flushOnError()
  {
    if(this.flushOnError && !this.errorLogObserver.hasError()) {
      this.errorLogObserver.recordError(true);
      flushingThread.interrupt();
    }
  }
}
