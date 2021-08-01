package com.example

import akka.http.scaladsl.Http
import com.example.Routes.routes
import scala.io.StdIn
import scala.util.{Failure, Success, Try}

object QuickstartApp extends App with Cassandra with AkkaSystem with Config {

  val bindingFuture = Http().newServerAt(config.myApp.host, config.myApp.port).bind(routes)

  def startCassandra(retries: Int): Unit = {
    if (retries <= 10) {
      CassandraBootstrap.setupNew.onComplete {
        case Success(tryConn) => {
          tryConn match {
            case Success(s) => logger.info(s)
            case Failure(e) =>
              logger.error(e.toString + "retrying to setup Cassandra")
              startCassandra(retries + 1)
          }
        }
        case Failure(exception) => logger.error(exception.toString)
      }
    }
    else {
      Thread.sleep(config.myApp.routes.askTimeout.toMillis)
      startCassandra(retries + 1)
    }
  }

  startCassandra(0)

  logger.info(
    s"Server online at http://${config.myApp.host}:${config.myApp.port}/\nPress RETURN to stop..."
  )
  StdIn.readLine() // let it run until user presses return
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done

}
