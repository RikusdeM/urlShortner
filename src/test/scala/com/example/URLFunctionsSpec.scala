package com.example

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class URLFunctionsSpec extends AnyWordSpec with Matchers with Config{
  import URL._

  "URL" should {
    val url = URL("http","google.com",None)
    val url2 = URL("http","google.com",Some(80))
    val url3 = URL("https","www.google.com",None)
    val urlStr = "http://google.com"
    val urlStr2 = "http://google.com:80"
    val urlStr3 = "https://www.google.com"
    "convert URL to String" in {
      urlString(url)(false) should===(urlStr)
      urlString(url2)(false) should===(urlStr2)
      urlString(url3)(false) should===(urlStr3)
    }
    "convert a String to URL" in {
      stringToURL(urlStr).get should ===(url)
      stringToURL(urlStr2).get should ===(url2)
      stringToURL(urlStr3).get should ===(url3)
    }
    "convert a URL to a URLPair" in {
      val urlPair = URLPair(url)
      val urlPair2 = URLPair(url2)
      val urlPair3 = URLPair(url3)
      urlPair.original should===(url)
      urlPair.shortened.host.length should ===(config.myApp.shortenedUrlLength)
      urlPair.shortened.port should ===(url.port)

      urlPair2.original should===(url2)
      urlPair2.shortened.host.length should ===(config.myApp.shortenedUrlLength)
      urlPair2.shortened.port should ===(url2.port)

      urlPair3.original should===(url3)
      urlPair3.shortened.host.length should ===(config.myApp.shortenedUrlLength)
      urlPair3.shortened.port should ===(url3.port)
    }

  }

}
