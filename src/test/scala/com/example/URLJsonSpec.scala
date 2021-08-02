package com.example

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class URLJsonSpec extends AnyWordSpec with Matchers {

  import URL._
  "URLJson" should {
    "marshall JSON to URL" in {
      val url = URL(
        "http",
        "google.com",
        Some(80)
      )
      jsonToURL("""{
        |	"protocol" : "http",
        |	"host" : "google.com",
        |	"port" : 80
        |}""".stripMargin) should ===(url)

      val url2 = URL(
        "https",
        "google.com",
        None
      )
      jsonToURL("""{
        |	"protocol" : "https",
        |	"host" : "google.com",
        |	"port" : null
        |}""".stripMargin) should ===(url2)
    }
    "un-marshall URL to JSON" in {
      val url = URL("http", "google.com", Some(80))
      val urlHttps = URL("https", "google.com", None)
      """{"protocol":"http","host":"google.com","port":80}""" should ===(
        URLToJson(url)
      )
      """{"protocol":"https","host":"google.com","port":null}""" should ===(
        URLToJson(urlHttps)
      )
    }
  }

  import URLPair._
  "URLPairJson" should {
    "marshall JSON to URLPair" in {
      val urlPair = URLPair(
        URL("https", "reddit.com", None),
        URL("https", "reddit.com", None)
      )
      jsonToURLPair("""{
        |	"shortened": {
        |		"protocol": "https",
        |		"host": "reddit.com",
        |		"port": null
        |	},
        |	"original": {
        |		"protocol": "https",
        |		"host": "reddit.com",
        |		"port": null
        |	}
        |}""".stripMargin) should ===(urlPair)

    }
    "un-marshall URLPair to JSON" in {
      import URLPair._
      val urlPair = URLPair(
        URL("https", "reddit.com", Option(80)),
        URL("https", "www.reddit.com", None)
      )
      jsonToURLPair("""{
        |	"shortened": {
        |		"protocol": "https",
        |		"host": "reddit.com",
        |		"port": 80
        |	},
        |	"original": {
        |		"protocol": "https",
        |		"host": "www.reddit.com",
        |		"port": null
        |	}
        |}""".stripMargin) should ===(urlPair)
    }
  }

  import URLSimple._
  "URLSimpleJson" should {
    "marshall JSON to URLSimple" in {
      val simpleURL = URLSimple("www.facebook.com")
      jsonToURLSimple("""{
                        |  "url":"www.facebook.com"
                        |}""".stripMargin) should ===(simpleURL)

    }
    "un-marshall URL to JSON" in {
      val url = URLSimple("http://google.com")
      """{"url":"http://google.com"}""" should ===(
        URLSimpleToJson(url)
      )
    }
  }
}
