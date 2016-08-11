package com.rojosam.services

import akka.actor.Actor.Receive
import akka.actor.{Actor, ActorLogging, Props}
import com.rojosam.dto.Parameters

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


object InTouchDistpacher {
  def props = Props(classOf[InTouchDistpacher])
}

class InTouchDistpacher extends Actor with ActorLogging {

  var count = 0



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
      count += 1
      log.info(s"*********** $count - $p")
      sender ! s"ACTOR $count ->  $p"
    case _            =>
      sender ! "Other"
  }
}