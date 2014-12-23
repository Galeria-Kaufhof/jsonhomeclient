package de.kaufhof.jsonhomeclient

import org.scalatest.Matchers
import org.mockito.Mockito._
import org.mockito.Matchers._

class JsonHomeServiceSpec extends UnitSpec {

  val server1 = JsonHomeHost("http://host1", Nil)
  val server2 = JsonHomeHost("http://host2", Nil)

  describe("JsonHomeService") {
    val cache1 = mockCache(server1, Some("/widgets"))
    val cache2 = mockCache(server2, Some("/widgets/{widget_id}/{more_para}"))

    val jsonHomeService = JsonHomeService(Seq(cache1, cache2))

    it("should ask the correct json home client for a resource") {
      val rel = DirectLinkRelationType("widgets")
      jsonHomeService.getUrl(server1, rel) should be(Some("/widgets"))
      verify(cache1).getUrl(rel)
      verify(cache2, never()).getUrl(any[LinkRelationType]())
    }

    it("should replace uri template variables for simple templated link") {
      val rel = TemplateLinkRelationType("widget")
      jsonHomeService.getUrl(server2, rel, Map("widget_id" -> 42, "more_para" -> "foo")) should be(Some("/widgets/42/foo"))
    }

    it("should replace uri for form style query expansion, see http://tools.ietf.org/html/rfc6570#section-3.2.8") {
      assertUrl("/widgets{?optionalParam}", Map("optionalParam" -> true), "/widgets?optionalParam=true")
      assertUrl("/widgets{?optionalParam}", Map(), "/widgets")
    }

    it("should encode uri template variables for simple templated link") {
      val rel = TemplateLinkRelationType("widget")
      jsonHomeService.getUrl(server2, rel, Map("widget_id" -> 42, "more_para" -> "/✓foo/foo€/foo$")) should be(Some("/widgets/42/%2F%E2%9C%93foo%2Ffoo%E2%82%AC%2Ffoo%24"))
    }
  }

  private def assertUrl(urlTemplate: String, params: Map[String, Any], expectedExpandedUrl: String) {
    val jsonHomeService = JsonHomeService(Seq(mockCache(server2, Some(urlTemplate))))
    jsonHomeService.getUrl(server2, TemplateLinkRelationType("ignored_because_of_mock"), params) should be(Some(expectedExpandedUrl))
  }


  private def mockCache(server: JsonHomeHost, getUrlResult: Option[String]): JsonHomeCache = {
    val res = mock[JsonHomeCache]
    when(res.host).thenReturn(server)
    when(res.getUrl(any[LinkRelationType]())).thenReturn(getUrlResult)
    res
  }

}
