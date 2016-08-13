package com.rojosam

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}
import javax.net.ssl._

import akka.actor.ActorSystem
import akka.http.scaladsl.marshalling.ToResponseMarshaller
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.rojosam.dto.{BasicResponse, Parameters, ServicesDTO, UserDTO}
import com.rojosam.pages.{DebugParameters, Error404}
import com.rojosam.services.{InTouchDistpacher, Security}
import com.typesafe.config.ConfigFactory
import akka.pattern.ask
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.io.StdIn
import collection.JavaConversions._

object InTouch {

  val log = LoggerFactory.getLogger("com.rojosam.InTouch")


  def main(args: Array[String]) {

    val config = ConfigFactory.load()

    implicit val system = ActorSystem("InTouch", config)
    implicit val timeout = Timeout(15 seconds)
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val host = config.getString("InTouch.host")
    val port = config.getInt("InTouch.port")

    val services = ServicesDTO.load(config.getObjectList("InTouch.services").toList)
    val distpacher = system.actorOf(InTouchDistpacher.props(services), name = "InTouchDispatcher")

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
                if (req.getHeader("InTouch-Debug").isPresent &&
                  req.getHeader("InTouch-Debug").get.value() == "true" && user.privileges.contains("ADMIN")) {
                  log.info("DEBUG MODE: ON - not service was called")
                  complete(DebugParameters(user, pathSegments, queryParameters, formFields, reqHeaders, params, req.method))
                } else {
                  val r = distpacher ? params
                  complete {
                    r.map {
                      case resp: BasicResponse => HttpResponse(status = resp.code, entity = HttpEntity(resp.message))
                    }
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
