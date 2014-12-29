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

import akka.actor.ActorSystem
import com.damnhandy.uri.template.UriTemplate
import play.api.libs.ws.WSClient

import scala.concurrent.duration.{FiniteDuration, Duration}
import scala.language.implicitConversions

/**
 * The json-home service allows to resolve urls (href/href-template) for a given json-home host and a given
 * link releation type.
 *
 * @author <a href="mailto:martin.grotzke@inoio.de">Martin Grotzke</a>
 */
case class JsonHomeService(cachesByHost: Map[JsonHomeHost, JsonHomeCache]) {

  /**
   * Determines the url (json-home "href") for the given json home host and the given direct link relation.
   */
  def getUrl(host: JsonHomeHost, relation: DirectLinkRelationType): Option[String] = {
    cachesByHost.get(host).flatMap(_.getUrl(relation))
  }

  /**
   * Determines the url (json-home "href-template") for the given json home host and the given template link relation.
   * The href template variables are replaced using the provided params.
   */
  def getUrl(host: JsonHomeHost, relation: TemplateLinkRelationType, params: Map[String, Any]): Option[String] = {
    cachesByHost.get(host).flatMap(_.getUrl(relation)).map { hrefTemplate =>
      params.foldLeft(UriTemplate.fromTemplate(hrefTemplate))((res, param) => res.set(param._1, param._2)).expand()
    }
  }

}

object JsonHomeService {


  sealed trait CheckedFlag
  trait Checked extends CheckedFlag
  trait Unchecked extends CheckedFlag

  object JsonHomeServiceBuilder{
    implicit def enableBuild(builder: JsonHomeServiceBuilder[Checked, Checked, Checked, Checked, Checked]): {def build():JsonHomeService} = new {
      def build(): JsonHomeService = {
        val caches = for{
          host <- builder.hosts
          client <- builder.wsClient
          system <- builder.system
          interval <- builder.updateInterval
          delay <- builder.startDelay
        } yield {
          val c = new JsonHomeClient(host, client)
          new JsonHomeCache(c, system, interval, delay)
        }

        JsonHomeService(caches)
      }
    }

    implicit def enableBuildWithDefaultStartDelay(builder: JsonHomeServiceBuilder[Checked, Checked, Checked, Checked, Unchecked]): {def build():JsonHomeService} = new {
      def build(): JsonHomeService = {
        val caches = for{
          host <- builder.hosts
          client <- builder.wsClient
          system <- builder.system
          interval <- builder.updateInterval
        } yield {
          val c = new JsonHomeClient(host, client)
          new JsonHomeCache(c, system, updateInterval = interval)
        }

        JsonHomeService(caches)
      }
    }

    implicit def enableBuildWithDefaultUpdateInterval(builder: JsonHomeServiceBuilder[Checked, Checked, Checked, Unchecked, Checked]): {def build():JsonHomeService} = new {
      def build(): JsonHomeService = {
        val caches = for{
          host <- builder.hosts
          client <- builder.wsClient
          system <- builder.system
          delay <- builder.startDelay
        } yield {
          val c = new JsonHomeClient(host, client)
          new JsonHomeCache(c, system, initialTimeToWait = delay)
        }

        JsonHomeService(caches)
      }
    }

    implicit def enableBuildWithDefaultDurations(builder: JsonHomeServiceBuilder[Checked, Checked, Checked, Unchecked, Unchecked]): {def build():JsonHomeService} = new {
      def build(): JsonHomeService = {
        val caches = for{
          host <- builder.hosts
          client <- builder.wsClient
          system <- builder.system
        } yield {
          val c = new JsonHomeClient(host, client)
          new JsonHomeCache(c, system)
        }

        JsonHomeService(caches)
      }
    }
  }

  case class JsonHomeServiceBuilder[HostFlag, ClientFlag, SystemFlag, UpdateIntervalFlag, StartDelayFlag] private[jsonhomeclient](
                                     hosts: List[JsonHomeHost] = Nil,
                                     wsClient: Option[WSClient] = None,
                                     system: Option[ActorSystem] = None,
                                     updateInterval: Option[FiniteDuration] = None,
                                     startDelay: Option[FiniteDuration] = None) {

    def addHost(url: String, rels: LinkRelationType*) = {
      val host = JsonHomeHost(url, rels)
      this.copy[Checked, ClientFlag, SystemFlag, UpdateIntervalFlag, StartDelayFlag](hosts = host :: hosts)
    }

    def withWSClient(client: WSClient) = this.copy[HostFlag, Checked, SystemFlag, UpdateIntervalFlag, StartDelayFlag](wsClient = Some(client))

    def withStartDelay(delay: FiniteDuration) = this.copy[HostFlag, ClientFlag, SystemFlag, UpdateIntervalFlag, Checked](startDelay = Some(delay))

    def withCaching(system: ActorSystem) = this.copy[HostFlag, ClientFlag, Checked, UpdateIntervalFlag, StartDelayFlag](system = Some(system))

    def withUpdateInterval(updateInterval: FiniteDuration) = this.copy[HostFlag, ClientFlag, SystemFlag, Checked, StartDelayFlag](updateInterval = Some(updateInterval))

  }

  object Builder {
    def apply(): JsonHomeServiceBuilder[Unchecked, Unchecked, Unchecked, Unchecked, Unchecked] = JsonHomeServiceBuilder()
  }

  def apply(caches: Seq[JsonHomeCache]): JsonHomeService = {
    val cachesByHost = caches.foldLeft(Map.empty[JsonHomeHost, JsonHomeCache]) { (res, cache) =>
      res + (cache.host -> cache)
    }
    new JsonHomeService(cachesByHost)
  }

}