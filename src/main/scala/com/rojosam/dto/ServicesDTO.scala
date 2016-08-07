package com.rojosam.dto

/**
  * Created by eduardo on 8/6/16.
  */
object ServicesDTO {

  trait InTouchService

  case class DBService(id:String,
                       dbName:String,
                       dbUrl:String,
                       dbPort:Int,
                       user:String,
                       password:String,
                       maxOpenConnections: Int) extends InTouchService

  case class SSHService(id:String,
                        host:String,
                        port:Int,
                        user:String,
                        sshKey:String) extends InTouchService

  case class RestApiService() extends InTouchService
  case class TransformService() extends InTouchService
  case class SequenceService(id:String, services: List[InTouchService]) extends InTouchService

}
