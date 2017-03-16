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

import com.sumologic.log4j.queue.BufferWithEviction;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Task to perform a single flushing check
 *
 * @author Jose Muniz (jose@sumologic.com)
 */
public abstract class BufferFlushingTask<In, Out> implements Runnable
{
  private static final Logger logger = StatusLogger.getLogger();

  private final BufferWithEviction<In> messageQueue;

  private long timeOfLastFlush = System.currentTimeMillis();

  private boolean needsFlushing()
  {
    final long currentTime = System.currentTimeMillis();
    final long dateOfNextFlush = timeOfLastFlush + getMaxFlushInterval();

    return (currentTime >= dateOfNextFlush) || (messageQueue.size() >= getMessagesPerRequest());
  }

  private void flushAndSend()
  {
    // Racy, but extra messages can be picked up next time around.
    final int size = messageQueue.size();
    List<In> messages = new ArrayList<In>(size);
    messageQueue.drainTo(messages, size);

    if (messages.size() > 0) {
      if(logger.isDebugEnabled()) {
        logger.debug(String.format(
            "%s - Flushing and sending out %d messages (%d messages left)",
            new java.util.Date(),
            messages.size(),
            messageQueue.size()
        ));
      }
      final Out body = aggregate(messages);
      sendOut(body);
      timeOfLastFlush = System.currentTimeMillis();
    }
  }


    /* Subclasses should define from here */

  abstract protected long getMaxFlushInterval();

  abstract protected long getMessagesPerRequest();

  protected BufferFlushingTask(BufferWithEviction<In> messageQueue)
  {
    this.messageQueue = messageQueue;
  }

  // Given the list of messages, aggregate them into a single Out object
  abstract protected Out aggregate(List<In> messages);

  // Send aggregated message out. Block until we've successfully sent it.
  abstract protected void sendOut(Out body);



    /* Public interface */

  public void run()
  {
    if (needsFlushing()) {
      try {
        flushAndSend();
      }
      catch (Exception e) {
        logger.warn("Exception while attempting to flush and send", e);
      }
    }
  }

}
