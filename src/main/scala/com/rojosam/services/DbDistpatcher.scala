package com.rojosam.services

import akka.actor.{Actor, ActorLogging, Props}
import com.rojosam.dto.{BasicResponse, Parameters, ResponseDTO}
import com.rojosam.dto.ServicesDTO.DBService

object DbDistpatcher {
  def props(services: Seq[DBService]) = Props(classOf[DbDistpatcher], services)
}

class DbDistpatcher(services: Seq[DBService]) extends Actor with ActorLogging  {

  log.error(services.toString())

  override def receive: Receive = {
    case p:Parameters =>
      log.info(p.toString)
      sender ! BasicResponse(200, "DB Call")
  }
}
