package com.example

import akka.http.scaladsl.model.StatusCodes.{BadRequest, InternalServerError}
import akka.http.scaladsl.model._
import scala.util.{Failure, Success, Try}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route, StandardRoute}
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
        post {
          parameters('url.as[String]) { urlStr =>
            stringToURL(urlStr) match {
              case Some(url) =>
                handleWrite {
                  onComplete(
                    writeURLPair(URLPair(url))(table)
                  )
                }
              case None => invalidInputError
            }
          }
        }
      }
    } ~
      pathPrefix("trex") {
        path("shorten") {
          post {
            entity(as[URLSimple]) { urlSimple =>
              URL(urlSimple) match {
                case Some(url) =>
                  handleWrite {
                    onComplete(
                      writeURLPair(URLPair(url))(table)
                    )
                  }
                case None => invalidInputError
              }
            }
          }
        }
      } ~
      path("trex") {
        get {
          parameters('url.as[String]) { urlStr =>
            stringToURL(urlStr) match {
              case Some(url) =>
                handleRead {
                  onComplete(
                    readURLPair(url)(table)(false)
                  )
                }
              case None => invalidInputError
            }
          }
        }
      } ~
      pathPrefix("trex") {
        get {
          entity(as[URLSimple]) { urlSimple =>
            URL.apply(urlSimple) match {
              case Some(url) =>
                handleRead {
                  onComplete(
                    readURLPair(url)(table)(false)
                  )
                }
              case None => invalidInputError
            }
          }
        }
      }

  val routes: Route = cassandraRoutes ~ URLAppRoutes
}

object RoutesHelpers {
  type ReadFun = Directive1[Try[Option[Option[URL]]]] => Route
  val handleRead: ReadFun = (sd: Directive1[Try[Option[Option[URL]]]]) => {
    sd {
      case Success(optionalURL) =>
        optionalURL match {
          case Some(Some(urlFinal)) => {
            complete(
              HttpEntity(
                ContentTypes.`application/json`,
                URLSimple(urlString(urlFinal)(false))
              )
            )
          }
          case _ => cassandraReadError
        }
      case Failure(ex) =>
        internalServerError(ex)
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
        internalServerError(ex)
    }
  }

  val internalServerError: Throwable => StandardRoute = (ex: Throwable) =>
    complete(
      InternalServerError,
      s"An error occurred: ${ex.getMessage}"
    )

  val invalidInputError: StandardRoute = complete(
    BadRequest,
    s"Please provide a valid url input"
  )

  val cassandraReadError: StandardRoute =
    complete(
      InternalServerError,
      "Could not read entry form Cassandra"
    )
}
