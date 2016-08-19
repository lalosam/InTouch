package com.rojosam.services

import akka.http.scaladsl.server.directives.Credentials
import com.rojosam.dto.UserDTO
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Security {

  val log = LoggerFactory.getLogger("com.rojosam.services.Security")

  def myUserPassAuthenticator(credentials: Credentials): Future[Option[UserDTO]] =
    credentials match {
      case p@Credentials.Provided(id) =>
        Future {
          log.info(s"USER: $id")
          if (p.verify("p4ssw0rd")) Some(UserDTO(id, Set("ADMIN", "DB", "DBSERV1")))
          else None
        }
      case _ => Future.successful(None)
    }
}
