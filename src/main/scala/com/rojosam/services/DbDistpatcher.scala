package com.rojosam.services

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.rojosam.dto.{BasicResponse, Parameters, ResponseDTO}
import com.rojosam.dto.ServicesDTO.DBService

import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.{ask, pipe}
import akka.util.Timeout

import scala.collection.mutable
import scala.concurrent.duration._

object DbDistpatcher {
  def props(services: Seq[DBService]) = Props(classOf[DbDistpatcher], services)
}

class DbDistpatcher(services: Seq[DBService]) extends Actor with ActorLogging  {

  log.info(services.toString())
  implicit val timeout = Timeout(15 seconds)

  val serviceConfig = services.map(s => s.id -> s).toMap
  val serviceActors = mutable.HashMap.empty[String, ActorRef]

  override def receive: Receive = {
    case p:Parameters =>
      log.info(p.toString)
      var service = serviceActors.get(p.service.get)
      if(service.isEmpty){
        service = Some(context.actorOf(DbService.props(serviceConfig(p.service.get))))
        serviceActors.put(p.service.get, service.get)
      }
      pipe(service.get ? p) to sender
  }
}
