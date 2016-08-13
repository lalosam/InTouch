package com.rojosam.services

import akka.actor.{Actor, ActorLogging, Props}

object ErrorActor {
  def props = Props(classOf[ErrorActor])
}

class ErrorActor extends Actor with ActorLogging{
  override def receive: Receive = {
    case p => log.error(s"ERROR: $p")
  }
}
