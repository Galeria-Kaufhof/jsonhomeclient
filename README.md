# json-home-client

[![Build Status](https://travis-ci.org/Galeria-Kaufhof/jsonhomeclient.png?branch=master)](https://travis-ci.org/Galeria-Kaufhof/jsonhomeclient)

No-magic Scala client to consume [JSON home documents](http://tools.ietf.org/html/draft-nottingham-json-home-03) like this:

```json
{
  "resources": {
    "http://spec.example.org/rels/artists": {
      "href": "/artists"
    },
    "http://spec.example.org/rels/artist": {
	  "href-template": "/artists/{artist-id}",
      "href-vars": {
        "artist-id": "http://spec.example.org/params/artist"
      }
	}
  }
}
```

The json-home-client regularly loads json home documents from json home document providers, and caches them in memory.
When your application retrieves some information from this json-home-client, the cached json home documents are used so that
there'll be no network roundtrip.

This library uses [Play's JSON library](https://www.playframework.com/documentation/2.6.x/ScalaJson).

## Installation

You must add the json-home-client to the dependencies of the build file, e.g. add to `build.sbt`:

    libraryDependencies += "de.kaufhof" %% "json-home-client" % "3.0.3"

It is published to sonatype nexus for scala 2.11 and 2.12.

## Usage

At first you setup the `JsonHomeService` for various hosts that provide json home documents.
The first host serves a json home like the one shown above.

```scala
import de.kaufhof.jsonhomeclient._

// Specify which service/host provides which link relations
// By default, json home docs are assumed to be served from `/.well-known/home`.
val host1 = JsonHomeHost("http://some.host", Seq(
  // A link relation without params
  DirectLinkRelationType("http://spec.example.org/rels/artists"),
  // A link relation with template params
  TemplateLinkRelationType("http://spec.example.org/rels/artist")
))

// Create the client, it loads the json home doc from the host
//Make sure to shutdown the ActorSystem after usage
implicit val system: ActorSystem  = ActorSystem()
implicit val materializer: ActorMaterializer  = ActorMaterializer()
val client1 = new JsonHomeClient(host1)

// Create the cache, it will regularly (using the Akka scheduler) load json home doc
// using the client. The initialTimeToWait is used for requests when the schedule did not yet
// kick in (might happen e.g. in tests) so that the json home doc was not yet requested/loaded.
// In this case the json home doc is directly loaded from the client and the cache will wait for
// the result using the given `initialTimeToWait`. Decide whether the cache
// should log output verbose or not.
val cache1 = new JsonHomeCache(
  client1,
  updateInterval = 5 minutes,
  initialTimeToWait = 10 seconds,
  logVerbose = false
)

// The json home service, the interface for applications, set up with several json home caches
val jsonHomeService = JsonHomeService(Seq(cache1, cache2, ...))
```

Once the json home service is set up, you can use it to retrieve links for link-relations.

```scala
// Example 1: Get a link relation that does not expect template params
jsonHomeService.getUrl(
  host1,
  DirectLinkRelationType("http://spec.example.org/rels/artists")
) == Some("/artists")

// Example 2: Some link relation that expects template params
jsonHomeService.getUrl(
  host1,
  TemplateLinkRelationType("http://spec.example.org/rels/artist"),
  Map("artist-id" -> "acdc")
) == Some("/artists/acdc")
```

## Release

```
sbt

sbt Release
```

## License

The license is Apache 2.0, see LICENSE.
