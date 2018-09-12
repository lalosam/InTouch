package com.rojosam.marshallers


import java.nio.charset.StandardCharsets

import akka.http.scaladsl.marshalling.{Marshaller, ToResponseMarshaller}
import akka.http.scaladsl.model.{HttpEntity, _}
import com.rojosam.dto.{DBPayloadResponse, ResultSet}
import org.json4s.NoTypeHints
import org.json4s.native.Serialization

import scalatags.Text.all._
import scalatags.Text.{Modifier => _, _}
import org.json4s.native.Serialization.{read, write}

import scala.concurrent.{ExecutionContext, Future}
import scala.xml._

/**
  * Created by eduardo on 8/19/16.
  */
trait DbPlayLoadMarshaller {

  implicit def dbPayLoadMarshaller(implicit ec:ExecutionContext, headers: Seq[HttpHeader]): ToResponseMarshaller[DBPayloadResponse] = Marshaller.oneOf(
    Marshaller.withOpenCharset(MediaTypes.`text/plain`) { (response, cs) =>
      HttpResponse(status=response.code,entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, dbPayLoadToTextPlain(response.payload)))
    },
    Marshaller.withOpenCharset(MediaTypes.`text/csv`) { (response, cs) =>
      val delimiterHeader = headers.filter(h => h.name() == "InTouch-Delimiter")
      val delimiter = if(delimiterHeader.nonEmpty) delimiterHeader.head.value() else ","
      HttpResponse(status=response.code, entity = HttpEntity(MediaTypes.`text/csv` withCharset cs, dbPayLoadToCsv(response.payload, delimiter)))
    },
    Marshaller.withOpenCharset(MediaTypes.`text/html`) { (response, cs) =>
      HttpResponse(status=response.code, entity = HttpEntity(MediaTypes.`text/html` withCharset cs, dbPayLoadToHTML(response.payload)))
    },
    Marshaller.withOpenCharset(MediaTypes.`application/xml`) { (response, cs) =>
      HttpResponse(status=response.code, entity = HttpEntity(MediaTypes.`application/xml` withCharset cs, dbPayLoadToXML(response.payload)))
    },
    Marshaller.withFixedContentType(MediaTypes.`application/json`) { response =>
      HttpResponse(status=response.code, entity = HttpEntity(MediaTypes.`application/json`, dbPayLoadToJSON(response.payload)))
    }
  )


  def dbPayLoadToJSON(payload:ResultSet.DbPayLoad):String = {
    implicit val formats = Serialization.formats(NoTypeHints)
    val cols = payload.columns.size
    val colNames = new Array[String](cols)
    val colTypes = new Array[Int](cols)
    for(c <- payload.columns.zipWithIndex){
      colNames(c._2) = c._1.name
      colTypes(c._2) = c._1.colType
    }
    val data = payload.data.map{
      r => r.zipWithIndex.map{ case(c, i) =>
        colNames(i) -> c
      }.toMap
    }
    write(data)
  }

  def textElem(name: String, text: String) =  Elem(null, name, Null, TopScope, true, Text(text))

  def dbPayLoadToXML(payload:ResultSet.DbPayLoad):String = {
    val cols = payload.columns.size
    val colNames = new Array[String](cols)
    for(c <- payload.columns.zipWithIndex){
      colNames(c._2) = c._1.name.toLowerCase
    }
    val xml = <root>
      <metadata>
        {payload.columns.map(c =>
        <col>
          <name>{c.name}</name>
          <type>{c.colType}</type>
          <typename>{c.colTypeName}</typename>
        </col>)}
      </metadata>
      <data> {payload.data.map{r =>
        <row>{r.zipWithIndex.map{case (c, i) =>
          textElem(colNames(i), if(c != null) c.toString else "")
          }}
        </row>}}
      </data>
    </root>
    new PrettyPrinter(80, 2).format(xml)
  }

  def dbPayLoadToHTML(payload:ResultSet.DbPayLoad):String = {
    val headerStyle = "text-align:center; background-color:#084B8A; color: #EFEFEF;"
    val valueStyle1 = "text-align:left; background-color:#EDEDED; color: #0C2262;"
    val valueStyle2 = "text-align:left; background-color:#CDCDCD; color: #0C2262;"

    html(
      table(
        tr(
          (for(h <- payload.columns) yield th(style := headerStyle)(h.name.toUpperCase())).asInstanceOf[Seq[Modifier]]
        ),
        payload.data.zipWithIndex.map{ case (r, idx) =>
          tr(r.map(c => td(style := (if(idx % 2 == 0) valueStyle1 else valueStyle2))
          (if(c == null) "null" else c.toString)).asInstanceOf[Seq[Modifier]])
        }.asInstanceOf[Seq[Modifier]]
      )
    ).toString()
  }

  def dbPayLoadToTextPlain(payload:ResultSet.DbPayLoad):String = {
    val cols = payload.columns.size
    val maxSizes = new Array[Int](cols)
    for(c <- payload.columns.zipWithIndex){
      maxSizes(c._2) = c._1.name.length
    }

    for(r <- payload.data){
      for(c <- r.zipWithIndex){
        if(c._1 != null && maxSizes(c._2) < c._1.toString.length) maxSizes(c._2) = c._1.toString.length
      }
    }

    val sb = new StringBuilder
    sb.append(
      maxSizes.map(w => "".padTo(w, '\u2500')).mkString("\u250c\u2500", "\u2500\u252c\u2500", "\u2500\u2510")
    ).append("\n").append(
      payload.columns.zipWithIndex.map{ case (col, idx) =>
        col.name.toUpperCase.padTo(maxSizes(idx), ' ')
      }.mkString("\u2502 ", " \u2502 ", " \u2502")
    ).append("\n").append(
      maxSizes.map(w => "".padTo(w, '\u2500')).mkString("\u251c\u2500", "\u2500\u253c\u2500", "\u2500\u2524")
    ).append("\n").append(
      payload.data.map{row =>
        row.zipWithIndex.map{ case (value, idx) =>
          val v = if(value == null) "null" else value
          v.toString.padTo(maxSizes(idx), ' ')
        }.mkString("\u2502 ", " \u2502 ", " \u2502")
      }.mkString("\n")
    ).append("\n").append(
      maxSizes.map(w => "".padTo(w, '\u2500')).mkString("\u2514\u2500", "\u2500\u2534\u2500", "\u2500\u2518")
    ).toString()
  }

  def dbPayLoadToCsv(payload:ResultSet.DbPayLoad, delimiterName:String):String = {
    val delimiter = delimiterName.toUpperCase match {
      case "TAB" => "\t"
      case "COMA" => ","
      case "PIPE" => "|"
      case d => d
    }
    payload.columns.map(c => c.name.toUpperCase).mkString(delimiter) + "\n" +
    payload.data.map(row => row.mkString(delimiter)).mkString("\n")
  }
}
