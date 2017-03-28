/**
 *  _____ _____ _____ _____    __    _____ _____ _____ _____
 * |   __|  |  |     |     |  |  |  |     |   __|     |     |
 * |__   |  |  | | | |  |  |  |  |__|  |  |  |  |-   -|   --|
 * |_____|_____|_|_|_|_____|  |_____|_____|_____|_____|_____|
 *
 * UNICORNS AT WARP SPEED SINCE 2010
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sumologic.log4j.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.entity.GzipCompressingEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

import java.io.IOException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Jose Muniz (jose@sumologic.com)
 */

public class SumoHttpSender
{
  private static final Logger logger = StatusLogger.getLogger();

  private final long retryInterval;
  private final String url;
  private final CloseableHttpClient httpClient;
  private final String name;
  private final String host;
  private final String category;

  public SumoHttpSender(
      long retryInterval,
      String url,
      CloseableHttpClient httpClient,
      String name,
      String host,
      String category
  )
  {
    this.retryInterval = retryInterval;
    this.url = url;
    this.httpClient = httpClient;
    this.name = name;
    this.host = host;
    this.category = category;
  }

  public boolean isInitialized()
  {
    return httpClient != null;
  }

  public void send(byte[] body)
  {
    keepTrying(body);
  }

  private void keepTrying(byte[] body)
  {
    int nTry = 1;
    while (!Thread.currentThread().isInterrupted()) {
      try {
        trySend(body);
        return;
      }
      catch (Exception e) {
        // Exponential backoff with jitter
        final double fuzzyMultiplier = Math.min(Math.max(1 + 0.2 * ThreadLocalRandom.current().nextGaussian(), 0), 2);
        final long sleepMillis = (long) (Math.min(retryInterval * 100, retryInterval * Math.pow(2, nTry - 1))
                                         * fuzzyMultiplier);
        nTry += 1;
        try {
          Thread.sleep(sleepMillis);
        }
        catch (InterruptedException e1) {
          break;
        }
      }
    }
    logger.warn("Not sent");
  }

  private void trySend(byte[] body) throws IOException
  {
    final HttpPost post = new HttpPost(url);
    try {
      if (name != null) {
        post.setHeader("X-Sumo-Name", name);
      }
      if (host != null) {
        post.setHeader("X-Sumo-Host", host);
      }
      if (category != null) {
        post.setHeader("X-Sumo-Category", category);
      }
      post.setEntity(new GzipCompressingEntity(new ByteArrayEntity(body)));
      final HttpResponse response = httpClient.execute(post);
      final int statusCode = response.getStatusLine().getStatusCode();
      if (statusCode != 200) {
        logger.warn("Received HTTP error from Sumo Service: {}", statusCode);
        // Not success. Only retry if status is unavailable.
        if (statusCode == 503) {
          throw new IOException("Server unavailable");
        }
      }
      //need to consume the body if you want to re-use the connection.
      logger.debug("Successfully sent log request to Sumo Logic");
      EntityUtils.consume(response.getEntity());
    }
    catch (IOException e) {
      logger.warn("Could not send log to Sumo Logic", e);
      try {
        post.abort();
      }
      catch (Exception e1) {
        e.addSuppressed(e1);
      }
      throw e;
    }
  }
}
