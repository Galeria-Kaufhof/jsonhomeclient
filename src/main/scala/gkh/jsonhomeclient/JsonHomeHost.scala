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

import java.net.URI

/**
 * This class represents a system that provides a REST API and a json home
 * document to describe its published resources.
 *
 * @author <a href="mailto:martin.grotzke@inoio.de">Martin Grotzke</a>
 *
 * @param uri the URI to the host.
 * @param jsonHomeUri the URI to the json home document
 * @param rels the link relations provided by this host.
 */
case class JsonHomeHost(uri: URI, jsonHomeUri: URI, rels: Seq[LinkRelationType])

object JsonHomeHost {

  val jsonHomePath = "/.well-known/home"

  def apply(hostURL: String, rels: Seq[LinkRelationType]): JsonHomeHost = {
    val uri = new URI(hostURL)
    JsonHomeHost(uri, URI.create(uri + jsonHomePath), rels)
  }

}