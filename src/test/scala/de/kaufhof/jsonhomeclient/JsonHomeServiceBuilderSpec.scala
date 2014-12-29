package de.kaufhof.jsonhomeclient

import akka.actor.ActorSystem
import de.kaufhof.jsonhomeclient.JsonHomeService.JsonHomeServiceBuilder._
import play.api.libs.ws.WS
import play.api.test.FakeApplication

import scala.concurrent.duration._
import scala.language.implicitConversions


/**
 *
 */
class JsonHomeServiceBuilderSpec extends UnitSpec {
  implicit val app = FakeApplication()
  val system = ActorSystem(getClass.getSimpleName)


  override protected def afterAll(): Unit = {
    system.shutdown()
  }


  describe("JsonHomeServiceBuilder") {
    it("should return a Builder with a JsonHomeHost") {
      val builderWithOneHost = JsonHomeService.Builder().addHost("url", DirectLinkRelationType("rel"))
      builderWithOneHost.hosts.size should be(1)
      builderWithOneHost.hosts.head.rels should be(Seq(DirectLinkRelationType("rel")))
    }

    it("should return a Builder with a WS Client") {
      JsonHomeService.Builder().withWSClient(WS.client).wsClient should be(Some(WS.client))
    }

    it("should return a Builder with an ActorSystem and an update interval and start delay") {

      val builder = JsonHomeService.Builder().withCaching(system).withUpdateInterval(1.minute).withStartDelay(1.minute)
      builder.system should be(Some(system))
      builder.updateInterval should be(Some(1.minute))
      builder.startDelay should be(Some(1.minute))
    }

    it("should return a JsonHomeService with user-defined durations when calling build()") {


      val builder = JsonHomeService.Builder()
        .addHost("http://host", DirectLinkRelationType("rel"), TemplateLinkRelationType("rel2"))
        .addHost("https://host2", DirectLinkRelationType("rel3"))
        .withWSClient(WS.client)
        .withCaching(system)
        .withUpdateInterval(1.minute)
        .withStartDelay(2.minutes)

      builder.hosts.size should be(2)
      builder.wsClient should be(Some(WS.client))
      builder.startDelay should be(Some(2.minutes))
      builder.updateInterval should be(Some(1.minute))
      builder.system should be(Some(system))

      val service = builder.build()
      service.cachesByHost.size should be(2)
    }

    it("should return a JsonHomeService with a default update interval when calling build()") {

      val builder = JsonHomeService.Builder()
        .addHost("http://host", DirectLinkRelationType("rel"), TemplateLinkRelationType("rel2"))
        .addHost("https://host2", DirectLinkRelationType("rel3"))
        .withWSClient(WS.client)
        .withCaching(system)
        .withStartDelay(2.minutes)

      builder.hosts.size should be(2)
      builder.wsClient should be(Some(WS.client))
      builder.startDelay should be(Some(2.minutes))
      builder.updateInterval should be(None)
      builder.system should be(Some(system))

      val service = builder.build()
      service.cachesByHost.size should be(2)

    }

    it("should return a JsonHomeService with a default start delay when calling build()") {

      val builder = JsonHomeService.Builder()
        .addHost("http://host", DirectLinkRelationType("rel"), TemplateLinkRelationType("rel2"))
        .addHost("https://host2", DirectLinkRelationType("rel3"))
        .withWSClient(WS.client)
        .withCaching(system)
        .withUpdateInterval(1.minute)

      builder.hosts.size should be(2)
      builder.wsClient should be(Some(WS.client))
      builder.startDelay should be(None)
      builder.updateInterval should be(Some(1.minute))
      builder.system should be(Some(system))

      val service = builder.build()
      service.cachesByHost.size should be(2)

    }

    it("should return a JsonHomeService with default durations when calling build()") {

      val builder = JsonHomeService.Builder()
        .addHost("http://host", DirectLinkRelationType("rel"), TemplateLinkRelationType("rel2"))
        .addHost("https://host2", DirectLinkRelationType("rel3"))
        .withWSClient(WS.client)
        .withCaching(system)

      builder.hosts.size should be(2)
      builder.wsClient should be(Some(WS.client))
      builder.startDelay should be(None)
      builder.updateInterval should be(None)
      builder.system should be(Some(system))

      val service = builder.build()
      service.cachesByHost.size should be(2)

    }


  }
}
