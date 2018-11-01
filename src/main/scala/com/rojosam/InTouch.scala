package com.rojosam

import java.io.InputStream
import java.security.{KeyStore, SecureRandom}

import akka.Done
import javax.net.ssl._
import akka.actor.ActorSystem
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.marshalling.{Marshal, ToResponseMarshallable, ToResponseMarshaller}
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.{ConnectionContext, Http, HttpsConnectionContext}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.rojosam.dto._
import com.rojosam.pages.{DebugParameters, Error404}
import com.rojosam.services.{InTouchDistpacher, Security}
import com.typesafe.config.ConfigFactory
import akka.pattern.ask
import com.rojosam.marshallers.{DbPlayLoadMarshaller, Unmarshallers}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.io.StdIn
import collection.JavaConversions._
import scala.concurrent.{Await, Future, Promise}

object InTouch extends DbPlayLoadMarshaller with Unmarshallers{

  val log = LoggerFactory.getLogger("com.rojosam.InTouch")

  var bindigFuture:Future[ServerBinding] = null

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
            extractRequest { req =>
              implicit val reqHeaders = req.headers.filter(h => h.name() != "Authorization")
              val params = Parameters().addUrlParameters(pathSegments).addQueryParameters(queryParameters).
                addHeaders(reqHeaders).addMethod(req.method.value).addUser(user)
              formFieldMultiMap { formFields =>
                params.addFormParameters(formFields)
                if (req.getHeader("InTouch-Debug").isPresent &&
                  req.getHeader("InTouch-Debug").get.value() == "true" && user.privileges.contains("ADMIN")) {
                  log.info("DEBUG MODE: ON - not service was called")
                  complete(DebugParameters(user, pathSegments, queryParameters, formFields, reqHeaders, params, req.method))
                } else {
                  val r = distpacher ? params
                  complete {
                    r.map[ToResponseMarshallable] {
                      case resp: BasicResponse => HttpResponse(status = resp.code, entity = HttpEntity(resp.message))
                      case resp: DBPayloadResponse => resp
                    }
                  }
                }
              }~
                entity(as[Parameters]) { data =>
                  // formFieldMultiMap { }
                  complete(s"$data")
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



    val f = for {
      bindigFuture <- Http().bindAndHandle(route, host, port)
      waitOnFuture  <- Promise[Done].future
    } yield waitOnFuture

    sys.addShutdownHook {
      InTouch.bindigFuture
        .flatMap(_.unbind()) // trigger unbinding from the port
        .onComplete(_ => system.terminate()) // and shutdown when done
    }
    Await.ready(f, Duration.Inf)

    // val bindingFuture = Http().bindAndHandle(route, host, port)

    log.info(s"Server online at http://$host:$port/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
  }



}
