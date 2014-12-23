package gkh.jsonhomeclient

import com.ning.http.client.AsyncHttpClientConfig
import play.api.libs.ws.ning.NingWSClient

import scala.language.postfixOps

import org.scalatest.ConfigMap
import play.api.libs.json.Json
import com.sun.net.httpserver.{HttpServer, HttpExchange, HttpHandler}
import java.net.InetSocketAddress
import scala.concurrent.duration._
import akka.actor.ActorSystem

class JsonHomeCacheSpec extends IntegrationSpec {

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
      |    "http://spec.example.org/rels/artistWithOptionalParams": {
      |	     "href-template": "/artists{?artistId}",
      |      "href-vars": {
      |        "artistId": "http://spec.example.org/params/artist"
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

  private val server = JsonHomeHost("http://localhost:8000", Seq(
    DirectLinkRelationType("http://spec.example.org/rels/artists"),
    TemplateLinkRelationType("http://spec.example.org/rels/artist"),
    TemplateLinkRelationType("http://spec.example.org/rels/artistWithOptionalParams")
  ))
  private lazy val wsClient = new NingWSClient(new AsyncHttpClientConfig.Builder().build())
  private lazy val client = new JsonHomeClient(server, wsClient)
  private lazy val actorSystem = ActorSystem(getClass.getSimpleName)
  private lazy val jsonHomeCache = new JsonHomeCache(client, actorSystem)
  private var httpServer: HttpServer = _

  describe("JsonHomeCache") {

    import org.scalatest.concurrent.Eventually._

    it("should return Some(link) for href from jsonHomeCache") {
      jsonHomeCache.getUrl(DirectLinkRelationType("http://spec.example.org/rels/artists")) should be(Some("/artists"))
    }

    it("should return None for unknown href relation") {
      jsonHomeCache.getUrl(DirectLinkRelationType("http://spec.example.org/rels/unknown_relation")) should be(None)
    }

    it("should return Some(link) for href-template from jsonHomeCache") {
      jsonHomeCache.getUrl(TemplateLinkRelationType("http://spec.example.org/rels/artist")) should be(Some("/artists/{artist-id}"))
    }

    it("should return None when json home not found from jsonHomeCache") {
      val server = JsonHomeHost("http://localhost:8001/this_server_and_doc_does_not_exist", Seq())
      val cache = new JsonHomeCache(new JsonHomeClient(server, wsClient), actorSystem)
      cache.getUrl(DirectLinkRelationType("http://spec.example.org/rels/artists")) should be (None)
    }

    it("should continuously reload json-home") {
      val relAlbums = DirectLinkRelationType("http://spec.example.org/rels/albums")
      val server = JsonHomeHost("http://localhost:8000", Seq(DirectLinkRelationType("http://spec.example.org/rels/artists"), relAlbums))
      val client = new JsonHomeClient(server, wsClient)
      val cache = new JsonHomeCache(client, actorSystem, 20 milliseconds)

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


  override def beforeAll(configMap: ConfigMap) {
    httpServer = startServer(8000, JsonHomeHost.jsonHomePath)
  }

  override def afterAll(configMap: ConfigMap) {
    httpServer.stop(0)
  }

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
      val os = t.getResponseBody
      os.write(response.getBytes)
      os.close()
    }
  }

}
