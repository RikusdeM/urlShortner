package com.example

import com.datastax.oss.driver.api.core.cql.Row
import com.typesafe.scalalogging.LazyLogging
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.language.implicitConversions
import scala.util.Random

case class URLSimple(url: String)

case class URL(protocol: String, host: String, port: Option[Int])
object URL extends LazyLogging {
  val protocolSeparator = "://"
  val portSeparator = ':'
  def apply(urlSimple: URLSimple): URL = {
    stringToURL(urlSimple.url)
  }
  def apply():URL = {
    URL("", "", None)
  }
  def urlString(url: URL)(shortened: Boolean): String =
    url.port match {
      case Some(port) if !shortened =>
        s"${url.protocol}$protocolSeparator${url.host}:${port.toString}"
      case _ => s"${url.protocol}$protocolSeparator${url.host}"
    }
  def stringToURL(urlString: String): URL = {
    urlString.split(protocolSeparator).toList match {
      case protocol :: hostPort =>
        hostPort.mkString("").split(portSeparator).toList match {
          case host :: port :: Nil =>
            URL(protocol, host, Some(port.mkString("").toInt))
          case host :: Nil => URL(protocol, host.mkString(""), None)
        }
      case _ => throw new Exception("Please provide valid URL")
    }
  }
  implicit def URLToJson(url: URL): String = {
    url.asJson.noSpaces
  }
  implicit def jsonToURL(url: String): URL = {
    decode[URL](url) match {
      case Right(value) => value
      case Left(e) =>
        logger.error(e.toString)
        URL()
    }
  }
//  implicit def rowToURL(cassandraRow:Row) = {
//    cassandraRow.getS
//  }
}
case class URLPair(shortened: URL, original: URL)
object URLPair extends LazyLogging {
  import Helpers._
  def apply(originalURL: URL): URLPair = {
    URLPair(
      URL(
        originalURL.protocol,
        randomID(8),
        originalURL.port
      ),
      originalURL
    )
  }

  implicit def URLPairToJson(urlPair: URLPair): String = {
    urlPair.asJson.noSpaces
  }
  implicit def jsonToURL(urlPair: String): URLPair = {
    decode[URLPair](urlPair) match {
      case Right(value) => value
      case Left(e) =>
        logger.error(e.toString)
        URLPair(URL(), URL())
    }
  }
}
object Helpers {
  def randomID(length: Int): String =
    Random.alphanumeric.take(length).mkString("")
}
