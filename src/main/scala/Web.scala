import com.twitter.finagle.{Service, SimpleFilter}
import org.jboss.netty.handler.codec.http._
import org.jboss.netty.handler.codec.http.HttpMethod._
import org.jboss.netty.handler.codec.http.HttpResponseStatus._
import org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1
import org.jboss.netty.buffer.ChannelBuffers.copiedBuffer
import org.jboss.netty.util.CharsetUtil.UTF_8
import com.twitter.util.Future
import java.net.InetSocketAddress
import com.twitter.finagle.builder.{Server, ServerBuilder}
import util.Properties
import com.twitter.finagle.http._
import com.twitter.finagle.http.path._
import com.curt.vehicle._
import com.curt.database._
import org.json4s._
import org.json4s.JsonDSL._
import org.json4s.jackson.JsonMethods._
import scala.util.DynamicVariable
import org.squeryl.{Session, SessionFactory}
import text.Document
import akka.actor._
import akka.dispatch.Await
import akka.dispatch.Future
import akka.pattern.ask
import akka.util.Timeout
import akka.util.duration._
import akka.japi.Creator

/**
 * This example demonstrates a sophisticated HTTP server that handles exceptions
 * and performs authorization via a shared secret. The exception handling and
 * authorization code are written as Filters, thus isolating these aspects from
 * the main service (here called "Respond") for better code organization.
 */
object HttpServer {
	/**
	 * A simple Filter that catches exceptions and converts them to appropriate
	 * HTTP responses.
	 */
	class HandleExceptions extends SimpleFilter[HttpRequest, HttpResponse] {
		def apply(request: HttpRequest, service: Service[HttpRequest, HttpResponse]) = {

			// `handle` asynchronously handles exceptions.
			service(request) handle { case error =>
				val statusCode = error match {
					case _: IllegalArgumentException =>
						FORBIDDEN
					case _ =>
						INTERNAL_SERVER_ERROR
				}
				val errorResponse = new DefaultHttpResponse(HTTP_1_1, statusCode)
				errorResponse.setContent(copiedBuffer(error.getStackTraceString, UTF_8))

				errorResponse
			}
		}
	}

	class DatabaseSessionSupport extends SimpleFilter[HttpRequest, HttpResponse] {
		val dbSession = new DynamicVariable[Session](null)
		def apply(request: HttpRequest, continue: Service[HttpRequest, HttpResponse]) = {
			dbSession.withValue(SessionFactory.newSession) {
				dbSession.value.bindToCurrentThread
				try {
					continue(request)
				} finally {
					dbSession.value.close
					dbSession.value.unbindFromCurrentThread
				}
			}
		}
	}

	/**
	 * A simple Filter that checks that the request is valid by inspecting the
	 * "Authorization" header.
	 */
	class Authorize extends SimpleFilter[HttpRequest, HttpResponse] {
		def apply(request: HttpRequest, continue: Service[HttpRequest, HttpResponse]) = {
			continue(request)
			// if ("open sesame" == request.getHeader("Authorization")) {
			//   continue(request)
			// } else {
			//   Future.exception(new IllegalArgumentException("You don't know the secret"))
			// }
		}
	}

	/**
	 * The service itself. Simply echos back "hello world"
	 */
	class Respond extends Service[HttpRequest, HttpResponse] with Database{
		def apply(request: HttpRequest) = {
			val response = new DefaultHttpResponse(HTTP_1_1, OK)

			val Pattern = "^(.*)".r

			println(Path(request.getUri))

			request.getMethod -> Path(request.getUri) match {
				case GET -> Root / "vehicle" =>

					val system = ActorSystem("VehicleSystem")
					val vehicleActor = system.actorOf(Props[Vehicle], name = "vehicleActor")

					implicit val timeout = Timeout(5 seconds)
					val future = vehicleActor ? "years"
					val years = Await.result(future, timeout.duration).asInstanceOf[List[Int]]
					
					val l_json = ("ConfigOption" ->
								("Type" -> "Years") ~
								("Options" -> years)) ~
							("ProductMatch" -> 
								("Parts" -> years) ~
								("Groups" -> years))
					val json = compact(render(l_json))
					response.setContent(copiedBuffer(json, UTF_8))
				case GET -> Root / "vehicle" / Integer(year) =>

					val system = ActorSystem("VehicleSystem")
					val vehicleActor = system.actorOf(Props(new Vehicle(year)), name = "vehicleActor")

					implicit val timeout = Timeout(5 seconds)
					val future = vehicleActor ? "makes"
					val makes = Await.result(future,timeout.duration).asInstanceOf[List[String]]

					val l_json = ("ConfigOption" ->
								("Type" -> "Makes") ~
								("Options" -> makes)) ~
							("ProductMatch" -> 
								("Parts" -> makes) ~
								("Groups" -> makes))
					val json = compact(render(l_json))
					response.setContent(copiedBuffer(json, UTF_8))
				case GET -> Root / "vehicle" / Integer(year) / make =>

					val system = ActorSystem("VehicleSystem")
					val vehicleActor = system.actorOf(Props(new Vehicle(year,make)), name = "vehicleActor")

					implicit val timeout = Timeout(5 seconds)
					val future = vehicleActor ? "models"
					val models = Await.result(future,timeout.duration).asInstanceOf[List[String]]
					val l_json = ("ConfigOption" ->
								("Type" -> "Models") ~
								("Options" -> models)) ~
							("ProductMatch" -> 
								("Parts" -> models) ~
								("Groups" -> models))
					val json = compact(render(l_json))
					response.setContent(copiedBuffer(json, UTF_8))
				case GET -> Root / "vehicle" / Integer(year) / make / model =>

					val system = ActorSystem("VehicleSystem")
					val vehicleActor = system.actorOf(Props(new Vehicle(year,make,model)), name = "vehicleActor")

					implicit val timeout = Timeout(5 seconds)
					val future = vehicleActor ? "submodels"
					val submodels = Await.result(future,timeout.duration).asInstanceOf[List[String]]
					val l_json = ("ConfigOption" ->
								("Type" -> "Submodels") ~
								("Options" -> submodels)) ~
							("ProductMatch" -> 
								("Parts" -> submodels) ~
								("Groups" -> submodels))
					val json = compact(render(l_json))
					response.setContent(copiedBuffer(json, UTF_8))

				case GET -> Root / "vehicle" / Integer(year) / make / model / submodel =>				

				case GET -> Root / "vehicle" / Integer(year) / make / model / submodel / Pattern =>
					println("hit")

				case _ =>
					println(Path(request.getUri).toList)
					response.setContent(copiedBuffer("Bad Route",UTF_8))
			}

			com.twitter.util.Future.value(response)
		}
	}

	def main(args: Array[String]) {
		val port = Properties.envOrElse("PORT","8080").toInt
		println("Starting on port: " + port)
		val handleExceptions = new HandleExceptions
		val dbSession = new DatabaseSessionSupport
		val authorize = new Authorize
		val respond = new Respond

		// compose the Filters and Service together:
		val myService: Service[HttpRequest, HttpResponse] = handleExceptions andThen dbSession andThen  authorize andThen respond

		val server: Server = ServerBuilder()
			.codec(Http())
			.bindTo(new InetSocketAddress(port))
			.name("httpserver")
			.build(myService)

			println("Server started on port: " + port)
	}
}