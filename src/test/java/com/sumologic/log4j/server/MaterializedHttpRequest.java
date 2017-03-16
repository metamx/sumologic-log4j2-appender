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
package com.sumologic.log4j.server;

import com.sun.net.httpserver.Headers;

/**
 * Author: Jose Muniz (jose@sumologic.com)
 * Date: 4/4/13
 * Time: 3:10 PM
 */
public class MaterializedHttpRequest
{
  private String method;
  private String body;
  private Headers headers;

  @Override
  public String toString()
  {
    return "HTTP Request: {" +
           "\nmethod='" + method + "'" +
           "\nheaders='" + headers + "'" +
           "\nbody='" + body + "' " +
           "\n}";
  }

  public String getMethod()
  {
    return method;
  }

  public void setMethod(String method)
  {
    this.method = method;
  }

  public String getBody()
  {
    return body;
  }

  public void setBody(String body)
  {
    this.body = body;
  }

  public Headers getHeaders()
  {
    return headers;
  }

  public void setHeaders(Headers headers)
  {
    this.headers = headers;
  }
}
