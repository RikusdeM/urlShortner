package com.example

import akka.http.scaladsl.Http
import com.example.CassandraBootstrap.startCassandraConnection
import com.example.Routes.routes

import scala.io.StdIn
import scala.util.{Failure, Success, Try}

object QuickstartApp extends App with Cassandra with AkkaSystem with Config {

  val bindingFuture =
    Http().newServerAt(config.myApp.host, config.myApp.port).bind(routes)

  startCassandraConnection(0)

  logger.info(
    s"Server online at http://${config.myApp.host}:${config.myApp.port}/\nPress RETURN to stop..."
  )
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete { _ =>
      cassandraSession.close(executionContext)
      actorSystem.terminate()
    } // and shutdown when done

}
