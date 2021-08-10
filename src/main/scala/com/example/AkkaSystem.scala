package com.example

import akka.actor.ActorSystem
import scala.concurrent.ExecutionContext

trait AkkaSystem {
  implicit val actorSystem: ActorSystem = ActorSystem("my-system")
  implicit val executionContext:ExecutionContext = actorSystem.dispatcher
}
