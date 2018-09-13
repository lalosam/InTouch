package com.rojosam.services

import akka.actor.{Actor, ActorLogging, Props}
import akka.util.Timeout
import com.rojosam.dto.{BasicResponse, Parameters, ResponseDTO}
import com.rojosam.dto.ServicesDTO.{DBService, InTouchService, RestApiService, TransformService}

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import akka.pattern.{ask, pipe}

import scala.concurrent.duration._


object InTouchDistpacher {
  def props(services: Seq[InTouchService]) = Props(classOf[InTouchDistpacher], services)
}

class InTouchDistpacher(services: Seq[InTouchService]) extends Actor with ActorLogging {

  val errorActor = context.actorOf(ErrorActor.props)

  implicit val timeout = Timeout(15 seconds)

  val groupOfServices = services.groupBy {
    case s: DBService => "db"
    case s: RestApiService => "rest"
    case s: TransformService => "transform"
  }

  val servicesType = groupOfServices.flatMap{ group =>
    for(serv <- group._2)yield serv.id -> group._1
  }


  log.warning(servicesType.toString)

  val servicesMap =  groupOfServices.map{ case (serviceType, servicesList) =>
    serviceType match {
      case "db" => "db" -> context.actorOf(DbDistpatcher.props(servicesList.asInstanceOf[List[DBService]]))
      case "rest" => "rest" -> context.actorOf(RestDistpatcher.props(servicesList.asInstanceOf[List[RestApiService]]))
    }
  }

  log.info(groupOfServices.toString())
  log.info(servicesMap.toString())

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.debug("PreStart distpatcher")
    super.preStart()
  }

  @scala.throws[Exception](classOf[Exception])
  override def postStop(): Unit = {
    log.debug("PostStop distpatcher")
    super.postStop()
  }

  @scala.throws[Exception](classOf[Exception])
  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.debug("PreRestart distpatcher")
    super.preRestart(reason, message)
  }

  @scala.throws[Exception](classOf[Exception])
  override def postRestart(reason: Throwable): Unit = {
    log.debug("PostRestart distpatcher")
    super.postRestart(reason)
  }

  override def receive = {
    case p:Parameters =>
      if(p.version.isEmpty) {
        sender ! BasicResponse(404, "Service not found")
      }else{
        val serviceType = servicesType.get(p.service.get)
        if (serviceType.isDefined) {
          val service = servicesMap.getOrElse(serviceType.get, errorActor)
          pipe(service ? p) to sender
        } else {
          sender ! BasicResponse(404, s"Invalid service ->  ${p.version.get}@${p.service.get}")
        }
      }
    case _            =>
      sender ! "Other"
  }
}