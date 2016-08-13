package com.rojosam.services

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging}
import com.rojosam.dto.ServicesDTO.DBService


class DbActor(service: DBService) extends Actor with ActorLogging {
  override def receive: Receive = ???
}
