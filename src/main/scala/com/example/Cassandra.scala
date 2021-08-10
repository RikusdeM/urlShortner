package com.example

import akka.stream.alpakka.cassandra.CassandraSessionSettings
import akka.stream.alpakka.cassandra.scaladsl.CassandraSession
import akka.stream.alpakka.cassandra.scaladsl.CassandraSessionRegistry
import akka.stream.alpakka.cassandra.CassandraWriteSettings
import akka.stream.alpakka.cassandra.scaladsl.CassandraFlow
import akka.stream.alpakka.cassandra.scaladsl.CassandraSource
import com.datastax.oss.driver.api.core.cql.{BoundStatement, PreparedStatement}
import akka.stream.scaladsl.{Sink, Source}
import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.example.Columns.{original_url, shortened_url}
import com.typesafe.scalalogging.LazyLogging
import java.util.concurrent.CompletionStage
import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration.SECONDS
import scala.util.{Failure, Success, Try}

trait Cassandra extends AkkaSystem with LazyLogging {
  import com.example.URL._

  implicit val cassandraSession: CassandraSession =
    try {
      val sessionSettings: CassandraSessionSettings = CassandraSessionSettings()
      CassandraSessionRegistry.get(actorSystem).sessionFor(sessionSettings)
    } catch {
      case exception: Exception =>
        throw new Exception(exception)
    }

  val version: Future[String] =
    cassandraSession
      .select("SELECT release_version FROM system.local;")
      .map(_.getString("release_version"))
      .runWith(Sink.head)

  def writeURLPair(
      urlPair: URLPair,
      checkIfExists: Boolean = true
  )(table: String): Future[immutable.Seq[URLPair]] = {

    def write: Future[immutable.Seq[URLPair]] = {
      val statementBinder: (URLPair, PreparedStatement) => BoundStatement =
        (urlPair, preparedStatement) =>
          preparedStatement.bind(
            urlString(urlPair.shortened)(shortened = true),
            urlString(urlPair.original)(shortened = false)
          )

      val written: Future[immutable.Seq[URLPair]] = Source(urlPair :: Nil)
        .via(
          CassandraFlow.create(
            CassandraWriteSettings.defaults,
            s"INSERT INTO $table(${shortened_url.toString},${original_url.toString}) VALUES (?, ?)",
            statementBinder
          )
        )
        .runWith(Sink.seq)
      written
    }

    readURLPair(urlPair.original)(table)(checkIfExists)
      .collect({
        case Some(Some(shortUrl)) =>
          immutable.Seq(URLPair(shortUrl, urlPair.original))
        case None =>
          val message =
            s"Could not find ${shortened_url.toString} for key: ${urlString(urlPair.original)(shortened = false)}" +
              s" in Cassandra. \n Generating it ..."
          throw CassandraLookupFailed(message)
      })
      .recoverWith({
        case e: Exception =>
          logger.error(e.toString, e)
          write
      })
  }

  def readURLPair(
      providedURL: URL
  )(table: String)(checkIfExists: Boolean): Future[Option[Option[URL]]] = {
    def lookup(
        key: String,
        value: String,
        shortened: Boolean
    ): Future[Option[Option[URL]]] = {
      CassandraSource(
        s"SELECT $value FROM $table WHERE $key = ? ALLOW FILTERING",
        urlString(providedURL)(shortened)
      ).map(rowToURL(_, value))
        .filter(url => url.isDefined)
        .runWith(Sink.headOption)
    }
    checkIfExists match {
      case true =>
        lookup(original_url.toString, shortened_url.toString, shortened = false)
      case false =>
        lookup(shortened_url.toString, original_url.toString, shortened = true)
    }
  }
}

case class CassandraLookupFailed(message: String) extends Exception(message) {
  def this(message: String, cause: Throwable) = {
    this(message)
    initCause(cause)
  }
}

object CassandraBootstrap extends Cassandra with Config {
  val keyspace: String = config.cassandra.keyspace
  val table: String = config.cassandra.table

  def createKeyspace(name: String): Future[CompletionStage[AsyncResultSet]] = {
    val keyspace = s"""CREATE KEYSPACE ${name}
                      |  WITH REPLICATION = {
                      |   'class' : 'NetworkTopologyStrategy',
                      |   'datacenter1' : 1
                      |  } ;""".stripMargin
    cassandraSession
      .underlying()
      .map { cqlSession =>
        cqlSession.executeAsync(keyspace)
      }
      .recoverWith {
        case exception: Exception =>
          logger.error(s"Could not create keyspace $name")
          Future.failed(exception)
      }
  }

  def createURLTable(
      name: String,
      keyspace: String
  ): Future[CompletionStage[AsyncResultSet]] = {
    val table = s"""CREATE TABLE ${keyspace}.${name} ( 
                   |   ${shortened_url.toString} text PRIMARY KEY,
                   |   ${original_url.toString} text );""".stripMargin
    cassandraSession
      .underlying()
      .map { cqlSession =>
        cqlSession.executeAsync(table)
      }
      .recoverWith {
        case exception: Exception =>
          logger.error(s"Could not create table $name in keyspace $keyspace")
          Future.failed(exception)
      }
  }

  def setupNew: Future[Try[String]] = {
    for {
      ks <- createKeyspace(keyspace)
      _ = ks.toCompletableFuture.get(5L, SECONDS)
      tb <- createURLTable(table, keyspace)
      _ = tb.toCompletableFuture.get(5L, SECONDS)
      done = Success(s"Created keyspace: $keyspace with table: $table")
    } yield {
      done
    }
  }

  def startCassandraConnection(retriesCount: Int): Unit = {
    if (retriesCount <= 10) {
      CassandraBootstrap.setupNew.onComplete {
        case Success(tryConn) => {
          tryConn match {
            case Success(s) => logger.info(s)
            case Failure(e) =>
              logger.error(e.toString + "retrying to setup Cassandra")
              startCassandraConnection(retriesCount + 1)
          }
        }
        case Failure(exception) => logger.error(exception.toString)
      }
    } else {
      Thread.sleep(config.myApp.routes.askTimeout.toMillis)
      startCassandraConnection(retriesCount + 1)
    }
  }
}

object Columns extends Enumeration {
  type Columns = Value
  val original_url, shortened_url = Value
}
