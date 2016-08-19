package com.rojosam.dto

trait ResponseDTO {
  def code:Int
  def message:String
}

case class BasicResponse(code: Int, message:String) extends ResponseDTO
case class PayloadResponse(code: Int, message:String, payload:ResultSet.DbPayLoad) extends ResponseDTO
