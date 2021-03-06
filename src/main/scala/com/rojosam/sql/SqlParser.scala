package com.rojosam.sql

import org.slf4j.LoggerFactory

import scala.collection.mutable.ArrayBuffer


object SqlParser {

  val log = LoggerFactory.getLogger("com.rojosam.sql.SqlParser")

  def parse(query:String, parameters:Map[String, List[String]]):Option[(String, Iterable[Any])] ={
    log.debug(query)
    val params = ArrayBuffer[Any]()
    var foundCurrency = false
    var isParameter = false
    var notFirstParameter = false
    val finalQuery = new StringBuilder
    val parameterName = new StringBuilder
    for(c <- query.toCharArray){
      if(isParameter && c == '}'){
        val p = parameterName.toString()
        val parametersList = parameters.get(p)
        if(parametersList.isEmpty) return None // Invalid parameter
        for(parameterValue <- parametersList.get){
          params += parameterValue
          if(notFirstParameter) finalQuery.append(", ")
          finalQuery.append("?")
          notFirstParameter = true
        }
        parameterName.clear()
        isParameter = false
        foundCurrency = false
        notFirstParameter = false
      } else if (c == '{' && foundCurrency){
        isParameter = true
      }else if(isParameter && c != '}'){
        parameterName.append(c)
      }else if(c == '$') {
        if(foundCurrency){
          if(isParameter){
            parameterName.append(c)
          }else{
            finalQuery.append(c)
          }
        }
        foundCurrency = true
      }else{
        if(foundCurrency){
          finalQuery.append('$')
          foundCurrency = false
        }
        finalQuery.append(c)
      }
    }
    log.info(finalQuery.toString())
    log.info(params.mkString(", "))
    Some(finalQuery.toString(), params)
  }

}
