package gkh.jsonhomeclient

import play.api.libs.json.JsValue

/**
 * Ops on a json home document.
 *
 * @author <a href="mailto:martin.grotzke@inoio.de">Martin Grotzke</a>
 */
object JsonHomeOperations {

  def getLinkUrl(json: JsValue, linkRelation: LinkRelationType): Option[String] = {
    linkRelation match {
      case DirectLinkRelationType(_) => (json \ "resources" \ linkRelation.name \ "href").asOpt[String]
      case TemplateLinkRelationType(_) => {
        (json \ "resources" \ linkRelation.name \ "href-template").asOpt[String].map{url =>
          val params = (json \ "resources" \ linkRelation.name \ "href-vars").as[Map[String, String]]
          params.foldLeft(url) { (url, spec) =>
            url.replace(spec._1, spec._2)
          }
        }
      }
    }
  }
}
