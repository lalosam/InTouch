package com.rojosam

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl._

import akka.actor.ActorSystem
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.rojosam.dto.{Parameters, UserDTO}
import com.rojosam.pages.{DebugParameters, Error404}
import com.rojosam.services.{InTouchDistpacher, Security}
import com.typesafe.config.ConfigFactory
import akka.pattern.ask
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.io.StdIn

object InTouch {

  val log = LoggerFactory.getLogger("com.rojosam.InTouch")


  def main(args: Array[String]) {


    implicit val system = ActorSystem("InTouch")
    implicit val timeout = Timeout(15 seconds)
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val config = ConfigFactory.load()
    val host = config.getString("InTouch.host")
    val port = config.getInt("InTouch.port")

    val distpacher = system.actorOf(InTouchDistpacher.props, name = "InTouchDispatcher")



    val route =
      path(Segments) { pathSegments =>
        authenticateBasicAsync(realm = config.getString("InTouch.realm"),
          Security.myUserPassAuthenticator) { user =>
          parameterMultiMap { queryParameters =>
            formFieldMultiMap { formFields =>
              extractRequest { req =>
                val reqHeaders = req.headers.filter(h => h.name() != "Authorization")
                val params = Parameters().addUrlParameters(pathSegments).addQueryParameters(queryParameters).
                  addFormParameters(formFields).addHeaders(reqHeaders)
                val r = distpacher ? params
                r.foreach(f => log.debug(s"--------------> $f"))
                if (req.getHeader("InTouch-Debug").isPresent &&
                  req.getHeader("InTouch-Debug").get.value() == "true" && user.privileges.contains("ADMIN")) {
                  log.info("DEBUG MODE: ON")
                  complete(DebugParameters(user, pathSegments, queryParameters, formFields, reqHeaders, params, req.method))
                } else {
                  get {
                    if (params.version.isEmpty) {
                      if (pathSegments.nonEmpty) {
                        pathSegments.head match {
                          case "favicon.ico" => getFromResource("favicon.ico", ContentTypes.`application/octet-stream`)
                          case _ => complete(Error404())
                        }
                      } else {
                        log.error(s"INVALID REQUEST: [ ${pathSegments.mkString("/")} ]")
                        complete(Error404())
                      }
                    } else {
                      complete(Error404())
                    }
                  } ~
                    put {
                      complete(Error404())
                    } ~
                    post {

                      log.debug(formFields.toString)

                      complete(Error404())

                    } ~
                    delete {
                      complete(Error404())
                    }
                }
              }
            }
          }
        }
      }

    val password: Array[Char] = config.getString("InTouch.cert.password").toCharArray

    val ks: KeyStore = KeyStore.getInstance("PKCS12")
    val keystore: InputStream = getClass.getClassLoader.getResourceAsStream(config.getString("InTouch.cert.file"))

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

    log.info(s"Server online at http://$host:$port/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }


}
