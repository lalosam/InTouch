package com.rojosam.dto

object ResultSet {

  case class Column(name: String, colType: Int, colTypeName:String)
  case class DbPayLoad(columns:Iterable[Column], data:Iterable[Iterable[Any]])

}

