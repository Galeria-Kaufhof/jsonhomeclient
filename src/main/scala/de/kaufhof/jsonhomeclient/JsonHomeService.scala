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

  sealed trait SystemFlag
  trait WithSystem extends SystemFlag
  trait WithoutSystem extends SystemFlag

  sealed trait HostFlag
  trait WithHost extends HostFlag
  trait WithoutHost extends HostFlag




  sealed trait WSClientFlag
  trait WithWSClient extends WSClientFlag
  trait WithoutWSClient extends WSClientFlag

  sealed trait UpdateIntervalFlag
  trait WithUpdateInterval extends UpdateIntervalFlag
  trait WithoutUpdateInterval extends UpdateIntervalFlag

  sealed trait StartDelayFlag
  trait WithStartDelay extends StartDelayFlag
  trait WithoutStartDelay extends StartDelayFlag



  object JsonHomeServiceBuilder{
    implicit def enableCachedClientBuild[A <: WithWSClient, B <: WithSystem, C <: WithUpdateInterval, D <: WithStartDelay, H >: HostsExist[A, B, C, D]](builder: Builder[A, B, C, D] with H): {def build():JsonHomeService} = new {
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

    implicit def enableBuildWithDefaultStartDelay[A <: WithWSClient, B <: WithSystem, C <: WithUpdateInterval, D <: WithoutStartDelay, H >: HostsExist[A, B, C, D]](builder: Builder[A, B, C, D] with H): {def build():JsonHomeService} = new {
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

    implicit def enableBuildWithDefaultUpdateInterval[A <: WithWSClient, B <: WithSystem, C <: WithoutUpdateInterval, D <: WithStartDelay, H >: HostsExist[A, B, C, D]](builder: Builder[A, B, C, D] with H): {def build():JsonHomeService} = new {
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

    implicit def enableBuildWithDefaultDurations[A <: WithWSClient, B <: WithSystem, C <: WithoutUpdateInterval, D <: WithoutStartDelay, H >: HostsExist[A, B, C, D]](builder: Builder[A, B, C, D] with H): {def build():JsonHomeService} = new {
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

    implicit def enableAsyncClientBuild[A <: WithWSClient, B <: WithoutSystem, C <: WithoutUpdateInterval, D <: WithoutStartDelay](builder: Builder[A, B, C, D] with OneHost[A, B, C, D]): {def build():JsonHomeClient} = new {
      def build(): JsonHomeClient = {
        new JsonHomeClient(builder.hosts.head, builder.wsClient.get)
      }
    }
  }

  trait Builder[CF <: WSClientFlag, SF <: SystemFlag, UF <: UpdateIntervalFlag, SDF <: StartDelayFlag]{
    def hosts: List[JsonHomeHost]
    def wsClient: Option[WSClient]
    def system: Option[ActorSystem]
    def updateInterval: Option[FiniteDuration]
    def startDelay: Option[FiniteDuration]

    def withWSClient(client: WSClient): Builder[WithWSClient, SF, UF, SDF] with HostHandling[WithWSClient, SF, UF, SDF]
    def withStartDelay(delay: FiniteDuration): Builder[CF, SF, UF, WithStartDelay] with HostHandling[CF, SF, UF, WithStartDelay]
    def withUpdateInterval(updateInterval: FiniteDuration): Builder[CF, SF, WithUpdateInterval, SDF] with HostHandling[CF, SF, WithUpdateInterval, SDF]
    def withCaching(system: ActorSystem): Builder[CF, WithSystem, UF, SDF] with HostHandling[CF, WithSystem, UF, SDF]
  }

  trait HostHandling[CF <: WSClientFlag, SF <: SystemFlag, UF <: UpdateIntervalFlag, SDF <: StartDelayFlag]{self: Builder[CF, SF, UF, SDF] =>}

  trait ZeroHosts[CF <: WSClientFlag, SF <: SystemFlag, UF <: UpdateIntervalFlag, SDF <: StartDelayFlag] extends HostHandling[CF, SF, UF, SDF]{self: Builder[CF, SF, UF, SDF] =>
    def addHost(url: String, rels: LinkRelationType*): JsonHomeServiceBuilderOne[CF, SF, UF, SDF] with OneHost[CF, SF, UF, SDF] = {
      val host = JsonHomeHost(url, rels)
      JsonHomeServiceBuilderOne(host :: hosts, wsClient, system, updateInterval, startDelay)
    }
  }

  trait HostsExist[CF <: WSClientFlag, SF <: SystemFlag, UF <: UpdateIntervalFlag, SDF <: StartDelayFlag] extends HostHandling[CF, SF, UF, SDF]{self: Builder[CF, SF, UF, SDF] =>
    def addHost(url: String, rels: LinkRelationType*): JsonHomeServiceBuilderMany[CF, SF, UF, SDF] with ManyHosts[CF, SF, UF, SDF] = {
      val host = JsonHomeHost(url, rels)
      JsonHomeServiceBuilderMany(host :: hosts, wsClient, system, updateInterval, startDelay)
    }
  }

  trait OneHost[CF <: WSClientFlag, SF <: SystemFlag, UF <: UpdateIntervalFlag, SDF <: StartDelayFlag] extends HostsExist[CF, SF, UF, SDF]{self: Builder[CF, SF, UF, SDF] =>}

  trait ManyHosts[CF <: WSClientFlag, SF <: SystemFlag, UF <: UpdateIntervalFlag, SDF <: StartDelayFlag] extends HostsExist[CF, SF, UF, SDF]{self: Builder[CF, SF, UF, SDF] =>}



  case class JsonHomeServiceBuilderZero[CF <: WSClientFlag, SF <: SystemFlag, UF <: UpdateIntervalFlag, SDF <: StartDelayFlag] private[jsonhomeclient](
                                     hosts: List[JsonHomeHost] = Nil,
                                     wsClient: Option[WSClient] = None,
                                     system: Option[ActorSystem] = None,
                                     updateInterval: Option[FiniteDuration] = None,
                                     startDelay: Option[FiniteDuration] = None) extends Builder[CF, SF, UF, SDF] with ZeroHosts[CF, SF, UF, SDF]{


    def withWSClient(client: WSClient) = this.copy[WithWSClient, SF, UF, SDF](wsClient = Some(client))

    def withStartDelay(delay: FiniteDuration) = this.copy[CF, SF, UF, WithStartDelay](startDelay = Some(delay))

    def withCaching(system: ActorSystem) = this.copy[CF, WithSystem, UF, SDF](system = Some(system))

    def withUpdateInterval(updateInterval: FiniteDuration) = this.copy[CF, SF, WithUpdateInterval, SDF](updateInterval = Some(updateInterval))

  }

  case class JsonHomeServiceBuilderOne[CF <: WSClientFlag, SF <: SystemFlag, UF <: UpdateIntervalFlag, SDF <: StartDelayFlag] private[jsonhomeclient](
                                                                                      hosts: List[JsonHomeHost] = Nil,
                                                                                      wsClient: Option[WSClient] = None,
                                                                                      system: Option[ActorSystem] = None,
                                                                                      updateInterval: Option[FiniteDuration] = None,
                                                                                      startDelay: Option[FiniteDuration] = None) extends Builder[CF, SF, UF, SDF] with OneHost[CF, SF, UF, SDF]{


    def withWSClient(client: WSClient): JsonHomeServiceBuilderOne[WithWSClient, SF, UF, SDF] with OneHost[WithWSClient, SF, UF, SDF] = this.copy[WithWSClient, SF, UF, SDF](wsClient = Some(client))

    def withStartDelay(delay: FiniteDuration): JsonHomeServiceBuilderOne[CF, SF, UF, WithStartDelay] with OneHost[CF, SF, UF, WithStartDelay] = this.copy[CF, SF, UF, WithStartDelay](startDelay = Some(delay))

    def withCaching(system: ActorSystem): JsonHomeServiceBuilderOne[CF, WithSystem, UF, SDF] with OneHost[CF, WithSystem, UF, SDF] = this.copy[CF, WithSystem, UF, SDF](system = Some(system))

    def withUpdateInterval(updateInterval: FiniteDuration): JsonHomeServiceBuilderOne[CF, SF, WithUpdateInterval, SDF] with OneHost[CF, SF, WithUpdateInterval, SDF] = this.copy[CF, SF, WithUpdateInterval, SDF](updateInterval = Some(updateInterval))

  }

  case class JsonHomeServiceBuilderMany[CF <: WSClientFlag, SF <: SystemFlag, UF <: UpdateIntervalFlag, SDF <: StartDelayFlag] private[jsonhomeclient](
                                                                                     hosts: List[JsonHomeHost] = Nil,
                                                                                     wsClient: Option[WSClient] = None,
                                                                                     system: Option[ActorSystem] = None,
                                                                                     updateInterval: Option[FiniteDuration] = None,
                                                                                     startDelay: Option[FiniteDuration] = None) extends Builder[CF, SF, UF, SDF] with ManyHosts[CF, SF, UF, SDF]{


    def withWSClient(client: WSClient): JsonHomeServiceBuilderMany[WithWSClient, SF, UF, SDF] with ManyHosts[WithWSClient, SF, UF, SDF]  = this.copy[WithWSClient, SF, UF, SDF](wsClient = Some(client))

    def withStartDelay(delay: FiniteDuration): JsonHomeServiceBuilderMany[CF, SF, UF, WithStartDelay] with ManyHosts[CF, SF, UF, WithStartDelay] = this.copy[CF, SF, UF, WithStartDelay](startDelay = Some(delay))

    def withCaching(system: ActorSystem): JsonHomeServiceBuilderMany[CF, WithSystem, UF, SDF] with ManyHosts[CF, WithSystem, UF, SDF]  = this.copy[CF, WithSystem, UF, SDF](system = Some(system))

    def withUpdateInterval(updateInterval: FiniteDuration): JsonHomeServiceBuilderMany[CF, SF, WithUpdateInterval, SDF] with ManyHosts[CF, SF, WithUpdateInterval, SDF]  = this.copy[CF, SF, WithUpdateInterval, SDF](updateInterval = Some(updateInterval))

  }


  object Builder {
    def apply(): Builder[WithoutWSClient, WithoutSystem, WithoutUpdateInterval, WithoutStartDelay] with ZeroHosts[WithoutWSClient, WithoutSystem, WithoutUpdateInterval, WithoutStartDelay] = JsonHomeServiceBuilderZero()
  }

  def apply(caches: Seq[JsonHomeCache]): JsonHomeService = {
    val cachesByHost = caches.foldLeft(Map.empty[JsonHomeHost, JsonHomeCache]) { (res, cache) =>
      res + (cache.host -> cache)
    }
    new JsonHomeService(cachesByHost)
  }

}
