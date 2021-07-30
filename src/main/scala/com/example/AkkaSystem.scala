package com.example

import akka.actor.ActorSystem

import scala.concurrent.ExecutionContext

trait AkkaSystem {

  implicit val system = ActorSystem("my-system")
  implicit val executionContext = ExecutionContext.global

}
