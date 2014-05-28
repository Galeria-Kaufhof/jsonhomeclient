package gkh.jsonhomeclient

import scala.language.postfixOps

import play.api.libs.json.JsValue
import play.api.libs.ws.WS
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * The client to load a json-home document and extract relevant information.
 * A client is responsible for a specific json-home host.
 *
 * @author <a href="mailto:martin.grotzke@inoio.de">Martin Grotzke</a>
 */
class JsonHomeClient(val host: JsonHomeHost) {

  import JsonHomeOperations._

  def getUrl(linkRelation: LinkRelationType): Future[Option[String]] = {
    jsonHome().map(json => getLinkUrl(json, linkRelation))
  }

  private[jsonhomeclient] def jsonHome(): Future[JsValue] = {
    WS.url(host.jsonHomeUri.toString)
      .withHeaders("Accept" -> "application/json-home").get().map(_.json)
  }
}

