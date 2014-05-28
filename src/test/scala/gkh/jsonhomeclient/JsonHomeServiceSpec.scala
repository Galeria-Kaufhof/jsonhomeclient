package gkh.jsonhomeclient

import org.scalatest.{Matchers, FunSpec}
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.mockito.Matchers._

class JsonHomeServiceSpec extends FunSpec with Matchers with MockitoSugar {

  describe("JsonHomeService") {
    val server1 = JsonHomeHost("http://host1", Nil)
    val server2 = JsonHomeHost("http://host2", Nil)

    val cache1 = mockCache(server1, Some("/widgets"))
    val cache2 = mockCache(server2, Some("/widgets/{widget_id}/{more_para}"))

    val cut = JsonHomeService(Seq(cache1, cache2))

    it("should ask the correct json home client for a resource") {
      val rel = DirectLinkRelationType("widgets")
      cut.getUrl(server1, rel) should be(Some("/widgets"))
      verify(cache1).getUrl(rel)
      verify(cache2, never()).getUrl(any[LinkRelationType]())
    }

    it("should replace uri template variables for simple templated link") {
      val rel = TemplateLinkRelationType("widget")
      cut.getUrl(server2, rel, Map("widget_id" -> 42, "more_para" -> "foo")) should be(Some("/widgets/42/foo"))
    }

    it("should encode uri template variables for simple templated link") {
      val rel = TemplateLinkRelationType("widget")
      cut.getUrl(server2, rel, Map("widget_id" -> 42, "more_para" -> "/✓foo/foo€/foo$")) should be(Some("/widgets/42/%2F%E2%9C%93foo%2Ffoo%E2%82%AC%2Ffoo%24"))
    }
  }

  private def mockCache(server: JsonHomeHost, getUrlResult: Option[String]): JsonHomeCache = {
    val res = mock[JsonHomeCache]
    when(res.server).thenReturn(server)
    when(res.getUrl(any[LinkRelationType]())).thenReturn(getUrlResult)
    res
  }

}
