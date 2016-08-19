package com.rojosam.services

import akka.actor.{Actor, ActorLogging, Props}
import com.rojosam.dto.{BasicResponse, Parameters, PayloadResponse, ResultSet}
import com.rojosam.dto.ServicesDTO.DBService
import org.apache.tomcat.jdbc.pool.DataSource
import org.apache.tomcat.jdbc.pool.PoolProperties
import java.sql.{Connection, SQLException}

import com.mysql.jdbc.exceptions.MySQLIntegrityConstraintViolationException

import collection.JavaConversions._
import com.rojosam.sql.SqlParser

import scala.collection.mutable

object DbService {
  def props(service:DBService)= Props(classOf[DbService], service)
}


class DbService(service: DBService) extends Actor with ActorLogging {

  val config = context.system.settings.config.getConfig("InTouch." + service.id)

  val entityMap = config.getConfigList("entities").toList.
    map(e => s"${e.getString("type")}@v${e.getString("version")}@${e.getString("id")}" ->
      (e.getString("query"), e.getStringList("roles").toSet)).toMap

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


  def updateQuery(query:String, params:Iterable[Any]):Int = {
    var count = 0
    var con: Connection = null
    try {
      con = datasource.getConnection()
      val st = con.prepareStatement(query)
      for(p <- params.zipWithIndex){
        st.setObject(p._2 + 1, p._1)
      }
      count = st.executeUpdate()
      st.close()
    } catch {
      case sqlE:SQLException => log.error(sqlE, s"Vendor error code: ${sqlE.getErrorCode}, SQL State: ${sqlE.getSQLState}")
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
    count
  }


  def executeQuery(query:String, params:Iterable[Any]):ResultSet.DbPayLoad = {
    var con: Connection = null
    val data = mutable.ArrayBuffer.empty[mutable.ArrayBuffer[Any]]
    val metaData = mutable.ArrayBuffer.empty[ResultSet.Column]
    try {
      con = datasource.getConnection()
      val st = con.prepareStatement(query)
      for(p <- params.zipWithIndex){
        st.setObject(p._2 + 1, p._1)
      }
      val rs = st.executeQuery()
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
      case sqlE:SQLException => log.error(sqlE, s"${sqlE.getErrorCode}, ${sqlE.getSQLState}")
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
    ResultSet.DbPayLoad(metaData, data)
  }

  override def receive: Receive = {
    case p:Parameters =>
      // val query = "select *, floor(rand() * 100) as \"rnd\" from intouch.test where value in (${v}) and number > ${urlParam0}"
      val queryAndRoles = entityMap.getOrElse(s"${p.method.get}@${p.version.get}@${p.entityId.get}", ("", Set("")))
      val grantedAccess = queryAndRoles._2.intersect(p.user.get.privileges).nonEmpty
      val resp = if(!grantedAccess){
        BasicResponse(403, "Forbidden")
      } else if(queryAndRoles._1.length == 0 ) {
        BasicResponse(404, "Invalid Operation")
      }else{
        val parsedQuery = SqlParser.parse(queryAndRoles._1, p.parameters)
        if(parsedQuery.isDefined){
          val (query, parameters) = parsedQuery.get
          p.method.get match {
            case "GET" =>
              val result = executeQuery (query, parameters)
              PayloadResponse (200, "DbService Call", result)
            case "POST" =>
              val count = updateQuery(query, parameters)
              if (count > 0) BasicResponse(201, "Item Created") else BasicResponse(409, "Already Exist")
            case "PUT" =>
              val count = updateQuery(query, parameters)
              if (count > 0) BasicResponse(200, "Item Updated") else BasicResponse(404, "Item not found")
            case "DELETE" =>
              val count = updateQuery(query, parameters)
              if (count > 0) BasicResponse(200, "Item Deleted") else BasicResponse(404, "Item not found")
          }
        }else{
          BasicResponse(404, "Invalid Parameters")
        }
      }
      sender ! resp
  }
}
