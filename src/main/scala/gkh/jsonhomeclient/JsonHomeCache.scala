package gkh.jsonhomeclient

import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.ActorSystem
import scala.util.{Failure, Success}
import java.util.concurrent.atomic.AtomicReference
import org.slf4j.LoggerFactory
import scala.concurrent.duration._
import java.util.concurrent.TimeoutException
import java.lang.System.currentTimeMillis


/**
 * A cache that regularly reloads a json-home document, so that queries on the json-home document are
 * performed in memory and do not cause any I/O.
 *
 * @author <a href="mailto:martin.grotzke@inoio.de">Martin Grotzke</a>
 */
class JsonHomeCache(client: JsonHomeClient, system: ActorSystem, updateInterval: FiniteDuration = 1 minute) {

  private val logger = LoggerFactory.getLogger(getClass)

  private val relsToUrls = new AtomicReference[Map[LinkRelationType, String]](Map.empty)

  private val timeToWait = 2 seconds

  private var initialWaitDone = false

  system.scheduler.schedule(0 seconds, updateInterval) {
    client.jsonHome().onComplete {
      case Success(json) => {
        play.api.Logger.debug(s"Got json home from ${client.host.jsonHomeUri}: $json}")
        val map = client.host.rels.foldLeft(Map.empty[LinkRelationType, String]) { (res, rel) =>
          JsonHomeOperations.getLinkUrl(json, rel) match {
            case Some(url) => res + (rel -> url)
            case None => res
          }
        }
        if (map != relsToUrls) {
          play.api.Logger.debug(s"Setting rel->url map: $map")
          relsToUrls.set(map)
        }
      }
      case Failure(t) => logger.warn(s"An error has occured while loading json home: $t")
    }
  }

  def server: JsonHomeHost = client.host

  def getUrl(rel: LinkRelationType): Option[String] = {
    var timeToWaitLeft = timeToWait.toMillis
    while (relsToUrls.get().isEmpty) {
      if (timeToWaitLeft < 0 || initialWaitDone) {
        initialWaitDone = true
        throw new TimeoutException(s"JSON Home from '" + client.host.jsonHomeUri + "' was not loaded within $timeToWait")
      }
      val start = currentTimeMillis()
      Thread.sleep(50)
      timeToWaitLeft -= (currentTimeMillis() - start)
    }

    relsToUrls.get().get(rel)
  }
}
