package com.rojosam.services

import akka.actor.{Actor, ActorLogging, Props}
import com.rojosam.dto.{PayloadResponse, ResultSet}
import com.rojosam.dto.ServicesDTO.DBService
import org.apache.tomcat.jdbc.pool.DataSource
import org.apache.tomcat.jdbc.pool.PoolProperties
import java.sql.{Connection}

import scala.collection.mutable

object DbService {
  def props(service:DBService)= Props(classOf[DbService], service)
}


class DbService(service: DBService) extends Actor with ActorLogging {

  val config = context.system.settings.config

  println(config.toString.substring(0, 600))

  var datasource:DataSource = _

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    log.error("****     Creating a DB Service")
    val p = new PoolProperties()
    p.setUrl(service.url)
    p.setDriverClassName(service.driverClassName)
    p.setUsername(service.user)
    p.setPassword(service.password)
    p.setJmxEnabled(true)
    p.setTestWhileIdle(false)
    p.setTestOnBorrow(true)
    p.setValidationQuery(service.validationQuery)
    p.setTestOnReturn(false)
    p.setValidationInterval(30000)
    p.setTimeBetweenEvictionRunsMillis(30000)
    p.setMaxActive(3)
    p.setMaxIdle(3)
    p.setInitialSize(1)
    p.setMaxWait(10000)
    p.setRemoveAbandonedTimeout(60)
    p.setMinEvictableIdleTimeMillis(30000)
    p.setMinIdle(1)
    p.setLogAbandoned(true)
    p.setRemoveAbandoned(true)
    p.setJdbcInterceptors(
      "org.apache.tomcat.jdbc.pool.interceptor.ConnectionState;"+
        "org.apache.tomcat.jdbc.pool.interceptor.StatementFinalizer");
    datasource = new DataSource()
    datasource.setPoolProperties(p)
    super.preStart()
  }




  def executeQuery(query:String):ResultSet.PayLoad = {
    var con: Connection = null
    val data = mutable.ArrayBuffer.empty[mutable.ArrayBuffer[Any]]
    val metaData = mutable.ArrayBuffer.empty[ResultSet.Column]
    try {
      con = datasource.getConnection()
      val st = con.createStatement()
      val rs = st.executeQuery(query)
      val md = rs.getMetaData
      for(i <- 1 to md.getColumnCount){
        metaData += ResultSet.Column(md.getColumnName(i), md.getColumnType(i), md.getColumnTypeName(i))
      }
      while (rs.next()) {
        val record = mutable.ArrayBuffer.empty[Any]
        for(i <- 1 to md.getColumnCount){
          record += rs.getObject(i)
        }
        data += record
      }
      rs.close()
      st.close()
    } catch {
      case e:Exception => log.error(e, "DbService error")
    }finally {
      if (con!=null){
        try {
          con.close()
        }catch {
          case e:Exception => log.error(e, "DbService error closing DB connection")
        }
      }
    }
    ResultSet.PayLoad(metaData, data)
  }

  override def receive: Receive = {
    case _ =>
      val result = executeQuery("select *, floor(rand() * 100) as \"rnd\" from intouch.test")
      log.warning(result.toString)
      sender ! PayloadResponse(200, "DbService Call", result)
  }
}
