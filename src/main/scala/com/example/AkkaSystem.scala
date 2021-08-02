package com.example

import akka.actor.ActorSystem
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait AkkaSystem {
  implicit val actorSystem: ActorSystem = ActorSystem("my-system")
  implicit val executionContext: ExecutionContextExecutor =
    ExecutionContext.global
}
