package com.rojosam.dto

import java.lang.Iterable

import akka.http.javadsl.model.HttpHeader
import org.slf4j.LoggerFactory

object Parameters {
  def  apply() = new Parameters()

  def  apply(urlParam: List[String], qryParam: Map[String, List[String]]):Parameters = {
    apply().addUrlParameters(urlParam).addQueryParameters(qryParam)
  }
}

class Parameters {

  val log = LoggerFactory.getLogger(classOf[Parameters])

  private var _version:Option[String] = None
  private var _service:Option[String] = None
  private var _entityId:Option[String] = None
  private var _method:Option[String] = None
  private var _param =  Map[String, List[String]]().withDefaultValue(List())

  def addUrlParameters(urlParam: List[String]):Parameters = {
    if(urlParam.length > 2) {
      _version  = Some(urlParam.head)
      _service  = Some(urlParam(1))
      _entityId = Some(urlParam(2))
      _param = _param ++ urlParam.drop(3).zipWithIndex.map {
        case (value, index) => (s"urlParam$index", List(value))
      }.toMap
    }
    log.debug(s"URL parameters added [$urlParam]")
    this
  }

  def addQueryParameters(qryParam: Map[String, List[String]]):Parameters = {
    _param = _param ++ qryParam.filterKeys(k => !k.startsWith("urlParam"))
    log.debug(s"Query parameters added [$qryParam]")
    this
  }

  def addFormParameters(formParam: Map[String, List[String]]):Parameters = {
    _param = _param ++ formParam.filterKeys(k => !k.startsWith("urlParam"))
    log.debug(s"Form parameters added [$formParam]")
    this
  }

  def addHeaders(headers: Seq[HttpHeader]):Parameters = {
    // Remove Authorization header by security
    _param = _param ++ headers.filter(h => !h.name().startsWith("urlParam")).
      map(h => (h.name(), List(h.value()))).toMap
    log.debug(s"Headers parameters added [$headers]")
    this
  }

  def addMethod(method:String):Parameters = {
    _method = Some(method)
    this
  }

  override def toString = s"Parameters(${_version}, ${_service}, ${_entityId}, ${_method}, ${_param})"

  def version    = _version
  def service    = _service
  def entityId   = _entityId
  def parameters = _param
  def method     = _method

}
