package gkh.jsonhomeclient

import java.net.URI

class JsonHomeHostSpec extends UnitSpec {

  describe("JsonHomeHost") {
    it("should build the correct host URI and json home URI for a simple base host") {
      JsonHomeHost("http://localhost", Seq()) should
        be(JsonHomeHost(URI.create("http://localhost"), URI.create("http://localhost/.well-known/home"), Seq()))
    }
    it("should build the correct host URI and json home URI for a subpath host") {
      JsonHomeHost("http://localhost/subpath/subpath2", Seq()) should
        be(JsonHomeHost(URI.create("http://localhost/subpath/subpath2"), URI.create("http://localhost/subpath/subpath2/.well-known/home"), Seq()))
    }
  }

}
