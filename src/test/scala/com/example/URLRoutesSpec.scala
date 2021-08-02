package com.example

import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{as, complete, entity}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.example
import com.example.CassandraBootstrap.startCassandraConnection
import com.example.URL.{defaultServiceProtocol, protocolSeparator}
import com.github.nosan.embedded.cassandra.api.connection.DefaultCassandraConnectionFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import com.github.nosan.embedded.cassandra.EmbeddedCassandraFactory
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

class URLRoutesSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with ScalatestRouteTest
    with BeforeAndAfterAll
    with Config
    with Cassandra {

  lazy val testKit: ActorTestKit = ActorTestKit()
  implicit val testSystem: ActorSystem = testKit.system.classicSystem
  implicit val testExecutionContext: ExecutionContextExecutor =
    testKit.system.executionContext
  lazy val routes: Route = Routes.routes

  implicit def default(implicit system: ActorSystem) =
    RouteTestTimeout(5.seconds)

  val shortenedUrl = "http://AcwMh6H3"
  val originalUrl = "www.google.com"

  val cassandraFactory = new EmbeddedCassandraFactory()
  val cassandra = cassandraFactory.create()

  override def beforeAll(): Unit = {
    cassandra.start()
    startCassandraConnection(0)
    Thread.sleep(10000)
    val table: String = config.cassandra.keyspace + "." + config.cassandra.table
    val originalURLPair = for {
      shortened <- URL(URLSimple(shortenedUrl))
      original <- URL(URLSimple(originalUrl))
    } yield { URLPair(shortened, original) }

    originalURLPair match {
      case Some(urlPair) =>
        writeURLPair(urlPair)(table).map(uP =>
          logger.info(s"Written URLPair $uP")
        ).recoverWith {
          case exception: Exception => throw new Exception(exception)
        }
      case None =>
        logger.error(s"Could not write initial URLPair")
    }

    Thread.sleep(1000)
  }

  override def afterAll(): Unit = {
    cassandraSession.close(testExecutionContext).onComplete({
      case Success(value) => cassandra.stop()
      case Failure(exception) => logger.error("Could not close CassandraSession")
    })
  }

  "URLRoutes" should {
    "return shortened URL on (GET /trex/shorten?url=$URL)" in {
      val request = HttpRequest(uri = s"/trex/shorten?url=$originalUrl")
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entity(as[URLSimple])
      }
    }

    "return original URL on (GET /trex?url=$URL)" in {
      val request = HttpRequest(uri = s"/trex?url=$shortenedUrl")
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[URLSimple].url should ===(
          s"${defaultServiceProtocol}${protocolSeparator}${originalUrl}"
        )
      }
    }

    "return shortened URL on (POST /trex/shorten)" in {
      val url = URLSimple(originalUrl)
      val userEntity =
        Marshal(url)
          .to[MessageEntity]
          .futureValue

      val request = Post("/trex/shorten").withEntity(userEntity)
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entity(as[URLSimple])
      }
    }

    "return original URL on (POST /trex)" in {
      val url = URLSimple(shortenedUrl)
      val userEntity =
        Marshal(url)
          .to[MessageEntity]
          .futureValue

      val request = Post("/trex").withEntity(userEntity)
      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entityAs[URLSimple].url should ===(
          s"${defaultServiceProtocol}${protocolSeparator}${originalUrl}"
        )
      }
    }
  }
}
