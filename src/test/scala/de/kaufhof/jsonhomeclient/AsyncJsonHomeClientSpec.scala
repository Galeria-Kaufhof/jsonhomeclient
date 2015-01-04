package de.kaufhof.jsonhomeclient

import java.net.InetSocketAddress

import akka.actor.ActorSystem
import com.ning.http.client.AsyncHttpClientConfig
import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import org.scalatest.ConfigMap
import play.api.libs.json.Json
import play.api.libs.ws.ning.NingWSClient
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.duration._
import scala.language.postfixOps

class AsyncJsonHomeClientSpec extends IntegrationSpec with FutureAwaits with DefaultAwaitTimeout {

  private var json = Json.parse(
    """
      |{
      |  "resources": {
      |    "http://spec.example.org/rels/artists": {
      |	     "href": "/artists"
      |	   },
      |    "http://spec.example.org/rels/artist": {
      |	     "href-template": "/artists/{artist_id}",
      |      "href-vars": {
      |        "artist_id": "http://spec.example.org/params/artist"
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

  private val port = 8001
  private val server = JsonHomeHost(s"http://localhost:$port", Seq(
    DirectLinkRelationType("http://spec.example.org/rels/artists"),
    TemplateLinkRelationType("http://spec.example.org/rels/artist"),
    TemplateLinkRelationType("http://spec.example.org/rels/artistWithOptionalParams")
  ))
  private lazy val wsClient = new NingWSClient(new AsyncHttpClientConfig.Builder().build())
  private lazy val actorSystem = ActorSystem(getClass.getSimpleName)
  private lazy val jsonHomeCache = AsyncJsonHomeClient.Builder(wsClient).build()
  private var httpServer: HttpServer = _

  describe("JsonHomeCache") {

    import org.scalatest.concurrent.Eventually._

    it("should return Some(link) for href from jsonHomeCache") {
      await(jsonHomeCache.getUrl(server, DirectLinkRelationType("http://spec.example.org/rels/artists"))) should be(Some("/artists"))
    }

    it("should return None for unknown href relation") {
      await(jsonHomeCache.getUrl(server, DirectLinkRelationType("http://spec.example.org/rels/unknown_relation"))) should be(None)
    }

    it("should return Some(link) for href-template from jsonHomeCache") {
      await(jsonHomeCache.getUrl(server, TemplateLinkRelationType("http://spec.example.org/rels/artist"), Map("artist_id" -> 42))) should be(Some("/artists/42"))
    }

  }


  override def beforeAll(configMap: ConfigMap) {
    httpServer = startServer(port, JsonHomeHost.jsonHomePath)
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
