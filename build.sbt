lazy val akkaHttpVersion = "10.2.5"
lazy val akkaVersion = "2.6.15"
lazy val alpakkaVersion = "3.0.2"
lazy val circeVersion = "0.14.1"
lazy val akkaHttpJsonVersion = "1.35.3"

lazy val root = (project in file("."))
  .settings(
    inThisBuild(
      List(
        organization := "com.rikus",
        scalaVersion := "2.12.12"
      )
    ),
    name := "trex-rikus-de-milander",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "de.heikoseeberger" %% "akka-http-circe" % akkaHttpJsonVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.lightbend.akka" %% "akka-stream-alpakka-cassandra" % alpakkaVersion,
      "io.circe" %% "circe-core" % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser" % circeVersion,
      "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.1.4" % Test
    )
  )
  .enablePlugins(sbtdocker.DockerPlugin, JavaAppPackaging)

resolvers += Resolver.mavenCentral
resolvers += Resolver.typesafeIvyRepo("releases")
resolvers += Resolver.typesafeRepo("releases")
resolvers += Resolver.sonatypeRepo("releases")

docker / dockerfile := {
  val appDir: File = stage.value
  val targetDir = "/app"

  new Dockerfile {
    from("openjdk:8-jre")
    entryPoint(s"$targetDir/bin/${executableScriptName.value}")
    copy(appDir, targetDir, chown = "daemon:daemon")
  }
}