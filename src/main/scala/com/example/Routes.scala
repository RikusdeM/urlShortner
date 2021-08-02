package com.example

import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model._
import scala.util.{Failure, Success, Try}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import com.example.URL.urlString
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

object Routes extends Cassandra with AkkaSystem with Config {

  import URL._
  import RoutesHelpers._

  val table: String = config.cassandra.keyspace + "." + config.cassandra.table

  val cassandraRoutes =
    path("cassandraVersion") {
      onComplete(version) {
        case Success(value) => complete(s"The result was $value")
        case Failure(ex) =>
          complete(
            InternalServerError,
            s"An error occurred: ${ex.getMessage}"
          )
      }
    }

  val URLAppRoutes =
    pathPrefix("trex") {
      path("shorten") {
        get {
          parameters('url.as[String]) { url =>
            val urlPair = URLPair(stringToURL(url))
            handleWrite {
              onComplete(
                writeURLPair(urlPair)(table)
              )
            }
          }
        }
      }
    } ~
      pathPrefix("trex") {
        path("shorten") {
          post {
            entity(as[URLSimple]) { urlSimple =>
              val urlPair = URLPair(URL(urlSimple))
              handleWrite {
                onComplete(
                  writeURLPair(urlPair)(table)
                )
              }
            }
          }
        }
      } ~
      path("trex") {
        get {
          parameters('url.as[String]) { urlStr =>
            val url = stringToURL(urlStr)
            handleRead {
              onComplete(
                readURLPair(url)(table)
              )
            }
          }
        }
      } ~
      pathPrefix("trex") {
        post {
          entity(as[URLSimple]) { urlSimple =>
            handleRead {
              onComplete(
                readURLPair(URL.apply(urlSimple))(table)
              )
            }
          }
        }
      }

  val routes: Route = cassandraRoutes ~ URLAppRoutes
}

object RoutesHelpers {
  type ReadFun = Directive1[Try[URL]] => Route
  val handleRead: ReadFun = (sd: Directive1[Try[URL]]) => {
    sd {
      case Success(value) =>
        complete(
          HttpEntity(
            ContentTypes.`text/plain(UTF-8)`,
            urlString(value)(false)
          )
        )

      case Failure(ex) =>
        complete(
          InternalServerError,
          s"An error occurred: ${ex.getMessage}"
        )
    }
  }
  type WriteFun = Directive1[Try[Seq[URLPair]]] => Route
  val handleWrite: WriteFun = (sd: Directive1[Try[Seq[URLPair]]]) => {
    sd {
      case Success(value) =>
        complete(
          HttpEntity(
            ContentTypes.`application/json`,
            URLSimple(value.head.shortened)
          )
        )
      case Failure(ex) =>
        complete(
          InternalServerError,
          s"An error occurred: ${ex.getMessage}"
        )
    }
  }
}
