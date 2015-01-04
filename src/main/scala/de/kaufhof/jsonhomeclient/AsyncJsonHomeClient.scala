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
package de.kaufhof.jsonhomeclient

import java.net.URI

import com.damnhandy.uri.template.UriTemplate
import org.slf4j.LoggerFactory
import play.api.Logger
import play.api.libs.json.JsValue
import play.api.libs.ws.{WSClient, WS}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * A json home client for various json home hosts.
 *
 * @author <a href="mailto:martin.grotzke@inoio.de">Martin Grotzke</a>
 */
class AsyncJsonHomeClient(ws: WSClient) {

  private val log = LoggerFactory.getLogger(getClass)

  /**
   * Determines the url (json-home "href") for the given json home host and the given direct link relation.
   */
  def getUrl(host: JsonHomeHost, relation: DirectLinkRelationType): Future[Option[String]] = {
    jsonHome(host).map(getLinkUrl(_, relation))
  }

  /**
   * Determines the url (json-home "href-template") for the given json home host and the given template link relation.
   * The href template variables are replaced using the provided params.
   */
  def getUrl(host: JsonHomeHost, relation: TemplateLinkRelationType, params: Map[String, Any]): Future[Option[String]] = {
    jsonHome(host).map(getLinkUrl(_, relation).map { hrefTemplate =>
      params.foldLeft(UriTemplate.fromTemplate(hrefTemplate))((res, param) => res.set(param._1, param._2)).expand()
    })
  }

  private def jsonHome(host: JsonHomeHost): Future[JsValue] = {
    ws.url(host.jsonHomeUri.toString)
      .withHeaders("Accept" -> "application/json-home").get().map(_.json)
  }

  private def getLinkUrl(json: JsValue, linkRelation: LinkRelationType): Option[String] = {
    linkRelation match {
      case DirectLinkRelationType(_) => (json \ "resources" \ linkRelation.name \ "href").asOpt[String]
      case TemplateLinkRelationType(_) => (json \ "resources" \ linkRelation.name \ "href-template").asOpt[String]
    }
  }

}

object AsyncJsonHomeClient {

  case class HostDefinition(uri: URI, linkRelationType: LinkRelationType*)

  case class Builder(wsClient: WSClient, hosts: Seq[HostDefinition] = Nil) {
    def build(): AsyncJsonHomeClient = new AsyncJsonHomeClient(wsClient)
  }

}