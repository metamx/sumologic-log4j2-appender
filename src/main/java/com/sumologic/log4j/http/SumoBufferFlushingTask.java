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
package com.sumologic.log4j.http;

import com.sumologic.log4j.aggregation.BufferFlushingTask;
import com.sumologic.log4j.queue.BufferWithEviction;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import java.util.List;

/**
 * @author Jose Muniz (jose@sumologic.com)
 */
public class SumoBufferFlushingTask extends BufferFlushingTask<String, String>
{
  private static final Logger logger = StatusLogger.getLogger();
  private final SumoHttpSender sender;
  private final long maxFlushInterval;
  private final long messagesPerRequest;

  public SumoBufferFlushingTask(BufferWithEviction<String> queue, SumoHttpSender sender, long maxFlushInterval, long messagesPerRequest)
  {
    super(queue);
    this.sender = sender;
    this.maxFlushInterval = maxFlushInterval;
    this.messagesPerRequest = messagesPerRequest;
  }

  @Override
  protected long getMaxFlushInterval()
  {
    return maxFlushInterval;
  }

  @Override
  protected long getMessagesPerRequest()
  {
    return messagesPerRequest;
  }

  @Override
  protected String aggregate(List<String> messages)
  {
    final StringBuilder builder = new StringBuilder(messages.size() * 10);
    for (String message : messages) {
      builder.append(message);
    }
    return builder.toString();
  }

  @Override
  protected void sendOut(String body)
  {
    if (sender != null && sender.isInitialized()) {
      logger.debug("Sending out data");
      sender.send(body);
    } else {
      logger.error("HTTPSender is not initialized");
    }
  }
}
