package com.rojosam.pages

import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpResponse, StatusCodes}
import com.rojosam.dto.{Parameters, UserDTO}

import scalatags.Text.all._

object DebugParameters {

  val descriptionStyle = "text-align:right; background-color:#084B8A; color: #EFEFEF;"
  val valueStyle = "text-align:left; background-color:#BDBDBD; color: #0C2262;"

  def apply(user:UserDTO, urlParam: List[String], qryParam: Map[String, List[String]], param:Parameters):HttpResponse = {
    HttpResponse(StatusCodes.OK,
      entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
        html(
          h1(style := "color: #084B8A;")(user.userName),
          br,
          table(
            tr(
              td(style := descriptionStyle)("PRIVILEGES:"),
              td(style := valueStyle)(user.privileges.mkString(", "))
            ),
            tr(
              td(style := descriptionStyle)("VERSION:"),
              td(style := valueStyle)(urlParam.head)
            ),
            tr(
              td(style := descriptionStyle)("SERVICE:"),
              td(style := valueStyle)(urlParam(1))
            ),
            tr(
              td(style := descriptionStyle)("ENTITY ID:"),
              td(style := valueStyle)(urlParam(2))
            ),
            tr(
              td(style := descriptionStyle)("URL PARAMETERS:"),
              td(style := valueStyle)(urlParam.drop(3).mkString(" | "))
            ),
            tr(
              td(style := descriptionStyle)("QUERY PARAMETERS:"),
              td(style := valueStyle)(qryParam.mkString(", "))
            ),
            tr(
              td(style := descriptionStyle)("PARAMETERS:"),
              td(style := valueStyle)(param.toString)
            )
          )
        ).toString()
      )
    )
  }
}
