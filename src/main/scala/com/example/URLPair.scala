package com.example

import com.datastax.oss.driver.api.core.cql.Row
import com.example.Columns.original_url
import com.typesafe.scalalogging.LazyLogging
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import scala.language.implicitConversions
import scala.util.Random

case class URLSimple(url: String)
object URLSimple {
  import URL._
  def apply(url: URL): URLSimple = {
    URLSimple(urlString(url)(true))
  }
  implicit def URLSimpleToJson(urlSimple: URLSimple): String = {
    urlSimple.asJson.noSpaces
  }
}

case class URL(protocol: String, host: String, port: Option[Int])
object URL extends LazyLogging {
  val protocolSeparator = "://"
  val portSeparator = ':'
  val defaultServiceProtocol = "http"
  val URLException = new Exception("PLEASE PROVIDE VALID URL")

  def apply(urlSimple: URLSimple): URL = {
    stringToURL(urlSimple.url)
  }
  def apply(): URL = {
    URL("", "", None)
  }
  def urlString(url: URL)(shortened: Boolean): String =
    url.port match {
      case Some(port) if !shortened =>
        s"${url.protocol}$protocolSeparator${url.host}:${port.toString}"
      case _ => s"${url.protocol}$protocolSeparator${url.host}"
    }

  val hostPortSplit: (String, String) => URL = (hostPort:String, protocol:String) => {
    hostPort.split(portSeparator).toList match {
      case host :: port :: Nil =>
        URL(protocol, host, Some(port.toInt))
      case host :: Nil => URL(protocol, host, None)
      case _ => throw new Exception("Cannot split URL correctly")
    }
  }

  def stringToURL(urlString: String): URL = {
    urlString.split(protocolSeparator).toList match {
      case protocol :: hostPort :: Nil =>
        hostPortSplit(hostPort,protocol)
      case hostPort :: Nil =>
        hostPortSplit(hostPort,defaultServiceProtocol)
      case _ => throw URLException
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
  def rowToURL(cassandraRow: Row): URL = {
    stringToURL(
      cassandraRow.getString(s"${original_url.toString}")
    )
  }
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
