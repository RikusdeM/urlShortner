package com.example

//#user-routes-spec
//#test-top
import akka.actor.ActorSystem
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshalling.Marshal
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives.{as, entity}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{RouteTestTimeout, ScalatestRouteTest}
import com.datastax.oss.driver.api.core.CqlSession
import com.example.URL.{stringToURL, urlString}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.circe.generic.auto._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
import org.cassandraunit.CQLDataLoader
import org.cassandraunit.dataset.cql.ClassPathCQLDataSet
import org.cassandraunit.utils.EmbeddedCassandraServerHelper
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.ExecutionContextExecutor
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

   def startCassandra(): Unit = {
    EmbeddedCassandraServerHelper.startEmbeddedCassandra()
    cassandraSession.underlying().onComplete {
      case Success(cqlSession) =>
        new CQLDataLoader(cqlSession)
          .load(
            new ClassPathCQLDataSet("schema.cql", config.cassandra.keyspace)
          )
      case Failure(exception) => throw new Exception(exception)
    }
  }

//  override def afterAll(): Unit = {
//    EmbeddedCassandraServerHelper.stopEmbeddedCassandra()
//  }

  "URLRoutes" should {

    val originalUrl = "www.google.com"
    "return shortened URL on (GET /trex/shorten?url=$URL)" in {
      val request = HttpRequest(uri = s"/trex/shorten?url=$originalUrl")

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entity(as[URLSimple])
      }
    }

    "return original URL on (GET /trex/trex?url=$URL)" in {
      val shortenedUrl = "http://AcwMh6H3"
      val request = HttpRequest(uri = s"/trex/?url=$shortenedUrl")

      //      request ~> routes ~> check {
      //        status should ===(StatusCodes.OK)
      //        contentType should ===(ContentTypes.`application/json`)
      //        entity(as[String]) should ===(originalUrl)
      //      }
    }


    "return shortened URL on (POST /trex/shorten)" in {
      val url = URLSimple(originalUrl)
      val userEntity = Marshal(url).to[MessageEntity].futureValue // futureValue is from ScalaFutures

      val request = Post("/trex/shorten").withEntity(userEntity)

      request ~> routes ~> check {
        status should ===(StatusCodes.OK)
        contentType should ===(ContentTypes.`application/json`)
        entity(as[URLSimple])
      }
    }

//
//    "be able to remove users (DELETE /users)" in {
//      // user the RequestBuilding DSL provided by ScalatestRouteSpec:
//      val request = Delete(uri = "/users/Kapi")
//
//      request ~> routes ~> check {
//        status should ===(StatusCodes.OK)
//
//        // we expect the response to be json:
//        contentType should ===(ContentTypes.`application/json`)
//
//        // and no entries should be in the list:
//        entityAs[String] should ===("""{"description":"User Kapi deleted."}""")
//      }
//    }
//    //#actual-test
  }
}
//#set-up
//#user-routes-spec
