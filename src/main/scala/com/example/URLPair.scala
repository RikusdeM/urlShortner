package com.example

import com.typesafe.scalalogging.LazyLogging
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.language.implicitConversions

case class URL(protocol: String, host: String, port: Option[Int])
object URL extends LazyLogging {
  implicit def URLToJson(url: URL): String = {
    url.asJson.noSpaces
  }
  implicit def jsonToURL(url: String): URL = {
    decode[URL](url) match {
      case Right(value) => value
      case Left(e) =>
        logger.error(e.toString)
        URL("", "", None)
    }
  }
}
case class URLPair(shortened: URL, original: URL)
object URLPair extends LazyLogging {
  implicit def URLPairToJson(urlPair: URLPair): String = {
    urlPair.asJson.noSpaces
  }
  implicit def jsonToURL(urlPair: String): URLPair = {
    decode[URLPair](urlPair) match {
      case Right(value) => value
      case Left(e) =>
        logger.error(e.toString)
        URLPair(URL("", "", None), URL("", "", None))
    }
  }
}
