package com.rojosam.services

import akka.actor.{Actor, ActorLogging, Props}
import com.rojosam.dto.ServicesDTO.RestApiService

object RestDistpatcher {
  def props(services: Seq[RestApiService]) = Props(classOf[RestDistpatcher], services)
}

class RestDistpatcher(services: Seq[RestApiService]) extends Actor with ActorLogging{

  log.error(services.toString())

  override def receive: Receive = {
    case s => log.info(s.toString)
  }
}
