package com.rojosam.dto

import java.lang.Iterable

import akka.http.javadsl.model.HttpHeader

object Parameters {
  def  apply() = new Parameters()

  def  apply(urlParam: List[String], qryParam: Map[String, List[String]]):Parameters = {
    apply().addUrlParameters(urlParam).addQueryParameters(qryParam)
  }
}

class Parameters {



  private var _version:Option[String] = None
  private var _service:Option[String] = None
  private var _entityId:Option[String] = None
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
    println(s"URL parameters added [$urlParam]")
    this
  }

  def addQueryParameters(qryParam: Map[String, List[String]]):Parameters = {
    _param = _param ++ qryParam.filterKeys(k => !k.startsWith("urlParam"))
    println(s"Query parameters added [$qryParam]")
    this
  }

  def addHeaders(headers: Seq[HttpHeader]):Parameters = {
    // Remove Authorization header by security
    _param = _param ++ headers.filter(h => !h.name().startsWith("urlParam") && h.name() != "Authorization").
      map(h => (h.name(), List(h.value()))).toMap
    println(s"Headers parameters added [$headers]")
    this
  }

  override def toString = s"Parameters(${_version}, ${_service}, ${_entityId}, ${_param})"

  def version = _version
  def service = _service
  def entityId = _entityId
  def parameters = _param
}
