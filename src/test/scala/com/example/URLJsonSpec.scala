package com.example

import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

//#set-up
class URLJsonSpec extends AnyWordSpec with Matchers {

  import com.example.{URL, URLPair}

  "URLJson" should {
    "marshall JSON to URL" in {
      """{
        |	"protocol": "http",
        |	"host": "google.com",
        |	"port": 80
        |}""".stripMargin === URL(
        "http",
        "google.com",
        Some(80)
      )

      """{
        |	"protocol": "https",
        |	"host": "google.com",
        |	"port": null
        |}""".stripMargin === URL(
        "https",
        "google.com",
        None
      )
    }
    "de-marshall URL to JSON" in {
      val url = URL("http", "google.com", Some(80))
      val urlHttps = URL("https", "google.com", None)

      url === """{
                |	"protocol": "http",
                |	"host": "google.com",
                |	"port": 80
                |}""".stripMargin
      urlHttps === """{
                     |	"protocol": "https",
                     |	"host": "google.com",
                     |	"port": null
                     |}""".stripMargin
    }
  }

  "URLPairJson" should {
    "marshall JSON to URLPair" in {
      val urlPair = URLPair(
        URL("https", "reddit.com", None),
        URL("https", "reddit.com", None)
      )
      """{
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
        |}""".stripMargin === urlPair

    }
  }
}
