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

import com.sumologic.log4j.http.SumoBufferFlushingTask;
import com.sumologic.log4j.http.SumoHttpSender;
import com.sumologic.log4j.queue.BufferWithEviction;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Jose Muniz (jose@sumologic.com)
 */
public class SumoBufferFlusher
{
  private final ScheduledExecutorService executor;
  private final long maxFlushTimeoutMs;
  private final Logger logger;
  private final SumoBufferFlushingTask flushingTask;
  private final long flushingAccuracy;


  public SumoBufferFlusher(
      long flushingAccuracy,
      long messagesPerRequest,
      long maxFlushInterval,
      long maxFlushTimeoutMs,
      SumoHttpSender sender,
      BufferWithEviction<String> buffer,
      Logger logger
  )
  {
    this.flushingAccuracy = flushingAccuracy;
    this.maxFlushTimeoutMs = maxFlushTimeoutMs;
    this.logger = logger;

    flushingTask = new SumoBufferFlushingTask(
        buffer,
        sender,
        maxFlushInterval,
        messagesPerRequest
    );

    executor = Executors.newSingleThreadScheduledExecutor(r -> {
      final Thread thread = new Thread(r);
      thread.setName("SumoBufferFlusherThread");
      thread.setDaemon(true);
      return thread;
    });
  }

  public void start()
  {
    executor.scheduleWithFixedDelay(flushingTask, 0, flushingAccuracy, TimeUnit.MILLISECONDS);
  }

  public void stop() throws InterruptedException
  {
    executor.shutdown();

    // Keep the current task running until it's done sending

    if (maxFlushTimeoutMs >= 0) {
      if (executor.awaitTermination(maxFlushTimeoutMs, TimeUnit.MILLISECONDS)) {
        logger.warn("Timed out waiting for buffer flusher to finish.");
      }
    }
    executor.shutdownNow();
  }
}
