package com.rojosam.pages

import akka.http.javadsl.model.HttpHeader
import akka.http.scaladsl.model._
import com.rojosam.dto.{Parameters, UserDTO}

import scalatags.Text.all._
import scalatags.Text.{Modifier => _, _}

object DebugParameters {

  val descriptionStyle = "text-align:right; background-color:#084B8A; color: #EFEFEF;"
  val valueStyle = "text-align:left; background-color:#EDEDED; color: #0C2262;"

  private def valueToTable(name:String, data: Any):Seq[Modifier] ={
    val (size, tags:Seq[Modifier]) = data match {
      case iterableData:Iterable[Any] =>
        if(iterableData.nonEmpty) {
          val t = for (d <- iterableData)
            yield td(style := valueStyle)(d.toString)
          (iterableData.size, t)
        }else{
          (1, Seq(td(style := valueStyle)(" < EMPTY >")))
        }
      case d => (1, Seq(td(style := valueStyle)(data.toString)))
    }
    if(size == 1){
      Seq(tr(
        td(style := descriptionStyle)(s"$name:"),
        tags.head
      ))
    }else{
      Seq(tr(
        td(style := descriptionStyle, rowspan:= size)(s"$name:"),
        tags.head)) ++
        tags.drop(1).map(t => tr(t))
    }

  }

  def apply(user:UserDTO, urlParam: List[String], qryParam: Map[String, List[String]], formFields: Map[String, List[String]],
            headers: Seq[HttpHeader], param:Parameters, method:HttpMethod):HttpResponse = {
    HttpResponse(StatusCodes.OK,
      entity = HttpEntity(ContentTypes.`text/html(UTF-8)`,
        html(
          h1(style := "color: #084B8A;")(user.userName),
          br,
          table(
            valueToTable("METHOD", method.value),
            valueToTable("PRIVILEGES", user.privileges),
            valueToTable("VERSION", urlParam.head),
            valueToTable("SERVICE", urlParam(1)),
            valueToTable("ENTITY", urlParam(2)),
            valueToTable("URL PARAMETERS", urlParam.drop(3)),
            valueToTable("QUERY PARAMETERS", qryParam),
            valueToTable("FORM FIELDS", formFields),
            valueToTable("HEADERS", headers),
            valueToTable("PARAMETERS", param)
          )
        ).toString()
      )
    )
  }
}
