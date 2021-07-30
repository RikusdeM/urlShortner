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

object Routes extends Cassandra with AkkaSystem {
  import Helpers._
  val helloRoutes =
    path("hello" / Segment) { helloString =>
      get {
        onComplete(sayHallo(s"Hallo to ${helloString}")) {
          case Success(value) =>
            complete(
              HttpEntity(
                ContentTypes.`text/html(UTF-8)`,
                s"<h1>$value</h1>"
              )
            )
          case Failure(exception) =>
            complete(InternalServerError, exception.toString)
        }
      }
    }
  val mathRoutes = path("divide" / IntNumber / IntNumber) {
    import akka.http.scaladsl.model.StatusCodes.InternalServerError

    (a, b) =>
      onComplete(divide(a, b)) {
        case Success(value) => complete(s"The result was $value")
        case Failure(ex) =>
          complete(
            InternalServerError,
            s"An error occurred: ${ex.getMessage}"
          )
      }
  }
  val cassandraRoutes = path("cassandra") {
    import akka.http.scaladsl.model.StatusCodes.InternalServerError
      onComplete(version) {
        case Success(value) => complete(s"The result was $value")
        case Failure(ex) =>
          complete(
            InternalServerError,
            s"An error occurred: ${ex.getMessage}"
          )
      }
  }


  val routes = helloRoutes ~ mathRoutes ~ cassandraRoutes
//  override implicit val actorSystem: ActorSystem = system
}
object Helpers {
  val sayHallo: String => Future[String] = (greeting: String) =>
    Future {
      greeting
    }

  def divide(a: Int, b: Int): Future[Int] =
    Future {
      a / b
    }
}

