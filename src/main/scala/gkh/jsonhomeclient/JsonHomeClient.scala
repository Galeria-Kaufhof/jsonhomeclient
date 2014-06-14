package gkh.jsonhomeclient

import scala.language.postfixOps

import play.api.libs.json.JsValue
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.libs.ws._
import play.api.Play.current

/**
 * The client to load a json-home document and extract relevant information.
 * A client is responsible for a specific json-home host.
 *
 * @author <a href="mailto:martin.grotzke@inoio.de">Martin Grotzke</a>
 */
class JsonHomeClient(val host: JsonHomeHost) {

  private[jsonhomeclient] def jsonHome(): Future[JsValue] = {
    WS.url(host.jsonHomeUri.toString)
      .withHeaders("Accept" -> "application/json-home").get().map(_.json)
  }

}

