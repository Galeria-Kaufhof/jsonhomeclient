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

import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import scala.util.control.NonFatal
import scala.util.{Try, Failure, Success}
import scala.concurrent.duration._
import play.api.libs.json.JsValue
import scala.concurrent.Await
import play.api.Logger

/**
 * A cache that regularly reloads a json-home document, so that queries on the json-home document are
 * performed in memory and do not cause any I/O.
 *
 * @author <a href="mailto:martin.grotzke@inoio.de">Martin Grotzke</a>
 */
class JsonHomeCache(client: JsonHomeClient, system: ActorSystem, updateInterval: FiniteDuration = 30 minutes,
                    initialTimeToWait: FiniteDuration = 10 seconds) {

  private val log = Logger(getClass)

  @volatile
  private var relsToUrls: Option[Map[LinkRelationType, String]] = None

  private val updateTask = system.scheduler.schedule(0 seconds, updateInterval) {
    client.jsonHome().onComplete {
      case Success(jsonHome) => buildAndSetLinkRelationTypeMap(jsonHome)
      case Failure(t) => onFailure(t)
    }
  }

  private def onFailure(t: Throwable): Unit = {
    log.warn(s"An error has occured while loading json home from ${client.host}: $t")
    // Set to some empty map so that getUrl does not always block for 10 seconds! This would lead to
    // a request time > 10 sec for each request.
    if(relsToUrls.isEmpty) {
      relsToUrls = Some(Map.empty)
    }
  }

  private def buildAndSetLinkRelationTypeMap(jsonHome: JsValue) {
    log.debug(s"Got json home from ${client.host.jsonHomeUri}: $jsonHome}")
    val map = client.host.rels.foldLeft(Map.empty[LinkRelationType, String]) { (res, rel) =>
      getLinkUrl(jsonHome, rel) match {
        case Some(url) => res + (rel -> url)
        case None => res
      }
    }
    if (relsToUrls.isEmpty || map != relsToUrls.get) {
      log.debug(s"Setting rel->url map: $map")
      relsToUrls = Some(map)
    }
  }

  private def getLinkUrl(json: JsValue, linkRelation: LinkRelationType): Option[String] = {
    linkRelation match {
      case DirectLinkRelationType(_) => (json \ "resources" \ linkRelation.name \ "href").asOpt[String]
      case TemplateLinkRelationType(_) => (json \ "resources" \ linkRelation.name \ "href-template").asOpt[String]
    }
  }

  def host: JsonHomeHost = client.host

  def getUrl(rel: LinkRelationType): Option[String] = {
    if (relsToUrls.isEmpty) {
      // eagerly load data from client if not here yet due to async loading from scheduler
      Try(buildAndSetLinkRelationTypeMap(Await.result(client.jsonHome(), initialTimeToWait))).recover {
        case NonFatal(e) => onFailure(e)
      }
    }
    relsToUrls.flatMap(_.get(rel))
  }

  def shutdown(): Unit = {
    updateTask.cancel()
  }

}
