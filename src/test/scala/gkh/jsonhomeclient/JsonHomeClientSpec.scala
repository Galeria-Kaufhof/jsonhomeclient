package gkh.jsonhomeclient

import scala.language.postfixOps

import org.scalatest.{ConfigMap, BeforeAndAfterAll, Matchers, FunSpec}
import org.scalatest.mock.MockitoSugar
import play.api.libs.json.Json
import com.sun.net.httpserver.{HttpServer, HttpExchange, HttpHandler}
import java.net.InetSocketAddress
import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import akka.actor.ActorSystem
import org.scalatest.time.{Millis, Span}
import play.api.{Play, GlobalSettings}
import play.api.test.FakeApplication
import play.api.libs.concurrent.Akka
import java.util.concurrent.TimeoutException

class JsonHomeClientSpec extends FunSpec with Matchers with BeforeAndAfterAll with MockitoSugar {

  private var json = Json.parse(
    """
      |{
      |  "resources": {
      |    "http://spec.example.org/rels/artists": {
      |	     "href": "/artists"
      |	   },
      |    "http://spec.example.org/rels/artist": {
      |	     "href-template": "/artists/{artist-id}",
      |      "href-vars": {
      |        "artist-id": "http://spec.example.org/params/artist"
      |      }
      |	   },
      |    "http://spec.example.org/rels/artistpara": {
      |	     "href-template": "/artists/{artist-id}?q={more-para}",
      |      "href-vars": {
      |        "artist-id": "http://spec.example.org/params/artist",
      |        "more-para": "http://spec.example.org/params/morePara"
      |      }
      |	   }
      |  }
      |}
    """.stripMargin)

  private val relWithHref = DirectLinkRelationType("http://spec.example.org/rels/artists")
  private val relWithHrefTemplate = TemplateLinkRelationType("http://spec.example.org/rels/artist")
  private val server = JsonHomeHost("http://localhost:8000", Seq(relWithHref, relWithHrefTemplate))
  private val client = new JsonHomeClient(server)

  private var system: ActorSystem = _
  private var httpServer: HttpServer = _

  object TestGlobal extends GlobalSettings

  override def beforeAll(configMap: ConfigMap) {
    httpServer = startServer(8000, JsonHomeHost.jsonHomePath)
    implicit val app = FakeApplication(withGlobal = Some(TestGlobal))
    Play.start(app)

    system = Akka.system
  }

  override def afterAll(configMap: ConfigMap) {
    Play.stop()
    httpServer.stop(0)
  }

  describe("JsonHomeOperations") {

    import JsonHomeOperations._

    it("should return Some(link) by direct link relation") {
      getLinkUrl(json, DirectLinkRelationType("http://spec.example.org/rels/artists")) should be(Some("/artists"))
    }

    it("should return Some(link) by template link relation") {
      getLinkUrl(json, TemplateLinkRelationType("http://spec.example.org/rels/artist")) should be(Some("/artists/{http://spec.example.org/params/artist}"))
      getLinkUrl(json, TemplateLinkRelationType("http://spec.example.org/rels/artistpara")) should be(Some("/artists/{http://spec.example.org/params/artist}?q={http://spec.example.org/params/morePara}"))
    }

  }

  describe("JsonHomeClient") {

    it("should return the Future(link) for direct link") {
      val url = client.getUrl(relWithHref)
      await(url) should be(Some("/artists"))
    }

    it("should return the Future(link) for template link") {
      val url = client.getUrl(relWithHrefTemplate)
      await(url) should be(Some("/artists/{http://spec.example.org/params/artist}"))
    }

  }

  describe("JsonHomeCache") {

    import org.scalatest.concurrent.Eventually._

    // increase timeout for slow build servers...
    implicit val patienceConfig = PatienceConfig(timeout = scaled(Span(2000, Millis)))

    it("should return Some(link) for href from cache") {
      val cache = new JsonHomeCache(client, system)
      // the cache is populated by a scheduled task, thus we need to retry...
      eventually {
        cache.getUrl(relWithHref) should be(Some("/artists"))
      }
    }

    it("should return Some(link) for href-template from cache") {
      val cache = new JsonHomeCache(client, system)
      // the cache is populated by a scheduled task, thus we need to retry...
      eventually {
        cache.getUrl(relWithHrefTemplate) should be(Some("/artists/{http://spec.example.org/params/artist}"))
      }
    }

    it("should return None when json home not found from cache") {
      val server = JsonHomeHost("http://localhost:8001/home", Seq(relWithHref))
      val client = new JsonHomeClient(server)
      val cache = new JsonHomeCache(client, system)
      intercept[TimeoutException] {
        cache.getUrl(relWithHref)
      }
    }

    it("should continuously reload json-home") {

      val relAlbums = DirectLinkRelationType("http://spec.example.org/rels/albums")
      val server = JsonHomeHost("http://localhost:8000/home", Seq(relWithHref, relAlbums))
      val client = new JsonHomeClient(server)
      val cache = new JsonHomeCache(client, system, 20 milliseconds)

      eventually {
        cache.getUrl(relAlbums) should be(None)
      }

      // let the http server provide a different json-home
      json = Json.parse(
        """
          |{
          |  "resources": {
          |    "http://spec.example.org/rels/artists": {
          |	     "href": "/artists"
          |	   },
          |    "http://spec.example.org/rels/albums": {
          |	     "href": "/albums"
          |	   }
          |  }
          |}
        """.stripMargin)

      eventually {
        cache.getUrl(relAlbums) should be(Some("/albums"))
      }

    }

  }

  implicit val timeout = 1 second

  private def await[T](future: Future[T])(implicit timeout: Duration): T = Await.result(future, timeout)

  private def startServer[T](port: Int, context: String): HttpServer = {
    val server = HttpServer.create(new InetSocketAddress(port), 0)
    server.createContext(context, new MyHandler())
    server.setExecutor(null) // creates a default executor
    server.start()
    server
  }

  private class MyHandler extends HttpHandler {
    override def handle(t: HttpExchange) {
      val response = json.toString()
      t.getResponseHeaders.add("Content-Type", "application/json-home")
      t.sendResponseHeaders(200, response.length())
      val os = t.getResponseBody()
      os.write(response.getBytes())
      os.close()
    }
  }

}
