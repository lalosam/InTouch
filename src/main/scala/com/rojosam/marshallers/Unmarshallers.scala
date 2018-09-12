package com.rojosam.marshallers

import akka.http.scaladsl.model._
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, FromRequestUnmarshaller, Unmarshaller}
import akka.stream.Materializer
import com.rojosam.dto.Parameters

trait Unmarshallers {


  implicit def inTouchUnmarshaller(implicit headers: Seq[HttpHeader]): FromRequestUnmarshaller[Parameters] =
    Unmarshaller.firstOf[HttpRequest, Parameters](
      Unmarshaller.strict{request =>
        request.entity.getContentType() match {
          case ContentTypes.`text/plain(UTF-8)` =>
            println("plain")
          case x => println("Other")
        }
        Parameters()
      }
    )
}
