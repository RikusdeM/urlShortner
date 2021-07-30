package com.example

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging

import java.time.Duration

case class RoutesConf(askTimeout: Duration)
case class MyAppConf(host: String, port: Int, routes: RoutesConf)
case class CassandraConf(
    host: String,
    port: Int,
    keyspace: String,
    table: String
)
case class AppConf(myApp: MyAppConf, cassandra: CassandraConf)

trait Config extends LazyLogging {
  val conf = ConfigFactory.load()
  val config =
    try {
      val myAppConf = conf.getConfig("myApp")
      val routesConf = myAppConf.getConfig("routes")
      val cassandraConf = conf.getConfig("cassandra")
      AppConf(
        MyAppConf(
          myAppConf.getString("host"),
          myAppConf.getInt("port"),
          RoutesConf(routesConf.getDuration("askTimeout"))
        ),
        CassandraConf(
          cassandraConf.getString("host"),
          cassandraConf.getInt("port"),
          cassandraConf.getString("keyspace"),
          cassandraConf.getString("table")
        )
      )
    } catch {
      case e: Exception =>
        logger.error(s"Could not load appConfig : ${e.toString}")
        AppConf(
          MyAppConf(
            "localhost",
            8080,
            RoutesConf(Duration.ofSeconds(5L))
          ),
          CassandraConf("localhost", 9042, "URLs", "URL")
        )
    }
}
