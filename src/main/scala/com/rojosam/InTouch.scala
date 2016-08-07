package com.rojosam

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl._

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.Credentials
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import com.rojosam.dto.{Parameters, UserDTO}
import com.rojosam.pages.{DebugParameters, Error404}
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.io.StdIn

object InTouch {


  def main(args: Array[String]) {

    implicit val system = ActorSystem("InTouch")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val config = ConfigFactory.load()
    val host = config.getString("in-touch.host")
    val port = config.getInt("in-touch.port")

    def myUserPassAuthenticator(credentials: Credentials): Future[Option[UserDTO]] =
      credentials match {
        case p@Credentials.Provided(id) =>
          Future {
            println(s"USER: $id")
            if (p.verify("p4ssw0rd")) Some(UserDTO(id, List("ADMIN, DB")))
            else None
          }
        case _ => Future.successful(None)
      }

    val route =
      path(Segments) { s =>
        authenticateBasicAsync(realm = config.getString("in-touch.realm"), myUserPassAuthenticator) { user =>
          parameterMultiMap { params =>
            extractRequest { req =>
              val p = Parameters().addUrlParameters(s).addQueryParameters(params).addHeaders(req.headers)
              get {
                if (p.version.isEmpty) {
                  if (s.nonEmpty) {
                    s.head match {
                      case "favicon.ico" => getFromResource("favicon.ico", ContentTypes.`application/octet-stream`)
                      case _ => complete(Error404())
                    }
                  } else {
                    println(s"INVALID REQUEST: [ ${s.mkString("/")} ]")
                    complete(Error404())
                  }
                } else {
                  complete(DebugParameters(user, s, params, p))
                }
              }
            }
          }
        }
      }

    val password: Array[Char] = config.getString("in-touch.cert-password").toCharArray

    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    val keystore: InputStream = getClass.getClassLoader.getResourceAsStream(config.getString("in-touch.cert-file"))

    require(keystore != null, "Keystore required!")
    ks.load(keystore, password)

    val keyManagerFactory: KeyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, password)

    val tmf: TrustManagerFactory = TrustManagerFactory.getInstance("SunX509")
    tmf.init(ks)
    val sslContext: SSLContext = SSLContext.getInstance("TLS")
    sslContext.init(keyManagerFactory.getKeyManagers, tmf.getTrustManagers, new SecureRandom)
    val https: HttpsConnectionContext = ConnectionContext.https(sslContext)
    Http().setDefaultServerHttpContext(https)
    val bindingFuture = Http().bindAndHandle(route, host, port)

    println(s"Server online at http://$host:$port/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }


}
