/**
  * Licensed to the Apache Software Foundation (ASF) under one or more
  * contributor license agreements.  See the NOTICE file distributed with
  * this work for additional information regarding copyright ownership.
  * The ASF licenses this file to You under the Apache License, Version 2.0
  * (the "License"); you may not use this file except in compliance with
  * the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing, software
  * distributed under the License is distributed on an "AS IS" BASIS,
  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  * See the License for the specific language governing permissions and
  * limitations under the License.
  */
package de.kaufhof.jsonhomeclient

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.MediaType.WithFixedCharset
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.settings.{ClientConnectionSettings, ConnectionPoolSettings, ParserSettings}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.Materializer
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport
import play.api.libs.json._

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.language.postfixOps


/**
  * The client to load a json-home document and extract relevant information.
  * A client is responsible for a specific json-home host.
  *
  * @param host           the json home host to load the home document from
  * @param defaultHeaders possibility to put headers from app-context
  * @param system A [[akka.actor.ActorSystem]] required by akka-http and for determining the [[scala.concurrent.ExecutionContextExecutor]]
  * @param materializer A [[Materializer]] required by akka-http
  */

class JsonHomeClient(val host: JsonHomeHost,
                     val defaultHeaders: Map[String, String] = Map("Accept" -> "application/json-home"))(implicit val system: ActorSystem, materializer: Materializer) extends PlayJsonSupport {

  private implicit val executionContext = system.dispatcher
  private val headers = defaultHeaders.collect { case (k, v) => RawHeader(k, v) }.to[Seq]

  private def `application/json-home`: WithFixedCharset = MediaType.applicationWithFixedCharset("json-home", HttpCharsets.`UTF-8`, "json-home")
  private val parserSettings = ParserSettings(system).withCustomMediaTypes(`application/json-home`)
  private val clientConSettings = ClientConnectionSettings(system).withParserSettings(parserSettings)
  private val clientSettings = ConnectionPoolSettings(system).withConnectionSettings(clientConSettings)

  override def unmarshallerContentTypes = List(`application/json`, `application/json-home`)

  protected[jsonhomeclient] def jsonHome(): Future[JsValue] = {
    Http().singleRequest(request = HttpRequest(uri = host.jsonHomeUri.toString).withHeaders(headers), settings = clientSettings).flatMap(response =>
      Unmarshal(response.entity).to[JsValue]
    )
  }

}

