package com.rojosam.pages

import akka.http.scaladsl.model.{HttpEntity, HttpResponse, StatusCodes}

/**
  * Created by eduardo on 8/6/16.
  */
object Error404 {

  def apply():HttpResponse = {
    HttpResponse(StatusCodes.NotFound,
      entity = HttpEntity("ERROR 404 - Service not found")
    )
  }

}
