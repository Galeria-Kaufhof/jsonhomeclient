package gkh.jsonhomeclient

import java.net.URI

/**
 * This class represents a system that provides a REST API and a json home
 * document to describe its published resources.
 *
 * @author <a href="mailto:martin.grotzke@inoio.de">Martin Grotzke</a>
 *
 * @param uri the URI to the host.
 * @param jsonHomeUri the URI to the json home document
 * @param rels the link relations provided by this host.
 */
case class JsonHomeHost(uri: URI, jsonHomeUri: URI, rels: Seq[LinkRelationType])

object JsonHomeHost {

  val jsonHomePath = "/.well-known/home"

  def apply(hostURL: String, rels: Seq[LinkRelationType]): JsonHomeHost = {
    val uri = new URI(hostURL)
    JsonHomeHost(uri, uri.resolve(jsonHomePath), rels)
  }
}