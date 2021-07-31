package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.InternalServerError
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.io.StdIn
import scala.util.{Failure, Success}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import io.circe.generic.auto._

object Routes extends Cassandra with AkkaSystem {

  import URL._

//  val helloRoutes =
//    path("hello" / Segment) { helloString =>
//      get {
//        onComplete(sayHallo(s"Hallo to ${helloString}")) {
//          case Success(value) =>
//            complete(
//              HttpEntity(
//                ContentTypes.`text/html(UTF-8)`,
//                s"<h1>$value</h1>"
//              )
//            )
//          case Failure(exception) =>
//            complete(InternalServerError, exception.toString)
//        }
//      }
//    }
//  val mathRoutes = path("divide" / IntNumber / IntNumber) {
//    import akka.http.scaladsl.model.StatusCodes.InternalServerError
//
//    (a, b) =>
//      onComplete(divide(a, b)) {
//        case Success(value) => complete(s"The result was $value")
//        case Failure(ex) =>
//          complete(
//            InternalServerError,
//            s"An error occurred: ${ex.getMessage}"
//          )
//      }
//  }
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
    path("trex") {
      get {
        parameters('url.as[String]) { url =>
          val urlPair = URLPair(stringToURL(url))
          onComplete(writeURLPair(urlPair)("urls.url")) {
            case Success(value) => complete(s"The result was $value")
            case Failure(ex) =>
              complete(
                InternalServerError,
                s"An error occurred: ${ex.getMessage}"
              )
          }
        }
      }
    } ~
      path("trex") {
        post {
          entity(as[URLSimple]) { url =>
            val urlPair = URLPair(URL(url))
            onComplete(writeURLPair(urlPair)("urls.url")) {
              case Success(value) => complete(s"The result was $value")
              case Failure(ex) =>
                complete(
                  InternalServerError,
                  s"An error occurred: ${ex.getMessage}"
                )
            }
          }
        }
      }

  val routes: Route = cassandraRoutes ~ URLAppRoutes
}
