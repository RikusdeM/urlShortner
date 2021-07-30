package com.example

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.example.Routes.routes

import scala.concurrent.ExecutionContext
import scala.io.StdIn
import scala.util.Failure
import scala.util.Success

//#main-class
object QuickstartApp extends App with Cassandra with AkkaSystem {

  val bindingFuture = Http().newServerAt("localhost", 8080).bind(routes)

  CassandraBootstrap.setup

  logger.info(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}
