/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gkh.jsonhomeclient

import scala.language.postfixOps

import play.api.libs.json.JsValue
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.ws._
import play.api.Play.current

/**
 * The client to load a json-home document and extract relevant information.
 * A client is responsible for a specific json-home host.
 *
 * @author <a href="mailto:martin.grotzke@inoio.de">Martin Grotzke</a>
 */
class JsonHomeClient(val host: JsonHomeHost) {

  private[jsonhomeclient] def jsonHome(): Future[JsValue] = {
    WS.url(host.jsonHomeUri.toString)
      .withHeaders("Accept" -> "application/json-home").get().map(_.json)
  }

}

