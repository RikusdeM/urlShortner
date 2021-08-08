package com.example

import org.mockito.scalatest.IdiomaticMockito
import org.scalatest.wordspec.AnyWordSpec
import com.datastax.oss.driver.api.core.cql.Row
import org.scalatest.matchers.should.Matchers
import URL._
import Helpers.randomID

class URLMock extends AnyWordSpec with Matchers with IdiomaticMockito {

  "an URL" should {
    val url: URL = mock[URL]
    val row: Row = mock[Row]
    "have the following behaviour " in {
      val urlStrHost = "google.com"
      val urlPort = Some(80)

      urlString(url)(false) shouldBe protocolSeparator
      urlString(url)(true) shouldBe protocolSeparator
      urlString(URL("http", urlStrHost, urlPort))(
        true
      ) shouldBe s"$defaultServiceProtocol$protocolSeparator$urlStrHost"
      stringToURL(
        s"$defaultServiceProtocol$protocolSeparator$urlStrHost"
      ) shouldBe (Some(
        URL(defaultServiceProtocol, urlStrHost, None)
      ))
      rowToURL(row) shouldBe None
    }

    "RandomID" should {
      "have the following behavior " in {
        val length = 8
        randomID(length).length shouldBe length
      }
    }
  }
}
