package com.rojosam.dto

import com.typesafe.config.{Config, ConfigObject}
import org.slf4j.LoggerFactory

/**
  * Created by eduardo on 8/6/16.
  */
object ServicesDTO {

  val log = LoggerFactory.getLogger("com.rojosam.dto.ServicesDTO")

  trait InTouchService{
    def id:String
    def version:String
  }

  case class DBService(id:String,
                       version:String,
                       url:String,
                       driverClassName:String,
                       user:String,
                       password:String,
                       validationQuery:String,
                       maxOpenConnections: Int) extends InTouchService

  def configToDB(service: Config): InTouchService = {
    DBService(
      service.getString("id"),
      service.getString("version"),
      service.getString("url"),
      service.getString("driverClassName"),
      service.getString("user"),
      service.getString("password"),
      service.getString("validationQuery"),
      service.getInt("maxOpenConnections")
    )
  }

  case class SSHService(id:String,
                        version:String,
                        host:String,
                        port:Int,
                        user:String,
                        sshKey:String) extends InTouchService

  case class RestApiService(id:String,
                            version:String,
                            host:String,
                            port:Int) extends InTouchService

  case class TransformService(id:String,
                              version:String) extends InTouchService

  case class SequenceService(id:String,
                             version:String,
                             services: List[InTouchService]) extends InTouchService


  def load(serviceList: List[ConfigObject]): Seq[InTouchService] = {
    for(serviceObj <- serviceList) yield{
      val serviceConfig = serviceObj.toConfig
      val serviceType = serviceConfig.getString("type")
      val service = serviceType match {
        case "db"   => configToDB(serviceConfig)
        case "rest" => configToRest(serviceConfig)
      }
      log.info(service.toString())
      service
    }
  }

  def configToRest(service: Config): InTouchService = {
    RestApiService(
      service.getString("id"),
      service.getString("version"),
      service.getString("host"),
      service.getInt("port")
    )
  }

}
