package com.example

import akka.actor.ActorSystem

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait AkkaSystem {

  implicit val system: ActorSystem = ActorSystem("my-system")
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.global

}
