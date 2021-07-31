package com.example

import akka.actor.ActorSystem
import akka.stream.alpakka.cassandra.CassandraSessionSettings
import akka.stream.alpakka.cassandra.scaladsl.CassandraSession
import akka.stream.alpakka.cassandra.scaladsl.CassandraSessionRegistry
import akka.stream.alpakka.cassandra.CassandraWriteSettings
import akka.stream.alpakka.cassandra.scaladsl.CassandraFlow
import akka.stream.alpakka.cassandra.scaladsl.CassandraSource
import com.datastax.oss.driver.api.core.cql.{BoundStatement, PreparedStatement}
import akka.stream.scaladsl.{Sink, Source}
import com.datastax.oss.driver.api.core.cql.AsyncResultSet
import com.typesafe.scalalogging.LazyLogging

import java.util.concurrent.CompletionStage
import scala.collection.immutable
import scala.concurrent.Future
import scala.concurrent.duration.SECONDS
import scala.util.{Failure, Random, Success, Try}

trait Cassandra extends AkkaSystem with LazyLogging {
  import com.example.URL._

  val sessionSettings = CassandraSessionSettings()
  implicit val cassandraSession: CassandraSession =
    CassandraSessionRegistry.get(system).sessionFor(sessionSettings)

  val version: Future[String] =
    cassandraSession
      .select("SELECT release_version FROM system.local;")
      .map(_.getString("release_version"))
      .runWith(Sink.head)

  def writeURLPair(
      urlPair: URLPair
  )(table: String): Future[immutable.Seq[URLPair]] = {
    val statementBinder: (URLPair, PreparedStatement) => BoundStatement =
      (urlPair, preparedStatement) =>
        preparedStatement.bind(
          urlString(urlPair.shortened)(true),
          urlString(urlPair.original)(false)
        )

    val written: Future[immutable.Seq[URLPair]] = Source(urlPair :: Nil)
      .via(
        CassandraFlow.create(
          CassandraWriteSettings.defaults,
          s"INSERT INTO $table(shortenedURL,originalURL) VALUES (?, ?)",
          statementBinder
        )
      )
      .runWith(Sink.seq)
    written
  }

  def readURLPair(shortedURL: URL)(table: String): Future[URL] = {
    CassandraSource(
      s"SELECT originalURL FROM $table WHERE shortenedurl = ?",
      urlString(shortedURL)(true)
    ).map(rowToURL).runWith(Sink.head)
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
                   |   shortenedURL text PRIMARY KEY,             
                   |   originalURL text );""".stripMargin
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

  def setup = {
    createKeyspace(keyspace).map(kf =>
      Try {
        kf.toCompletableFuture.get(5L, SECONDS)
      } match {
        case Success(_) =>
          logger.info(s"Created keyspace : $keyspace")
          createURLTable(table, keyspace).map(tf =>
            Try {
              tf.toCompletableFuture.get(5L, SECONDS)
            } match {
              case Success(_) =>
                logger.info(s"Created table : $table")
              case Failure(e) =>
                logger.error(e.toString)
            }
          )
        case Failure(e) =>
          logger.error(e.toString)
      }
    )
  }
}
