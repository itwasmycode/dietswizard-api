import com.amazonaws.services.lambda.runtime.{Context}
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}

import scala.collection.JavaConverters._
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}

import java.util.{Base64, Map => JavaMap, HashMap => JavaHashMap}
import org.slf4j.LoggerFactory
import scala.concurrent.{ExecutionContext, Future,Await}

import play.api.libs.json._


import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import slick.jdbc.PostgresProfile.api._
import java.util.UUID
import at.favre.lib.crypto.bcrypt._

import java.time.{Instant, ChronoUnit}




object LambdaHandler extends RequestHandler[APIGatewayProxyRequestEvent,APIGatewayProxyResponseEvent] {
  val logger = LoggerFactory.getLogger(getClass)
  implicit val ec = ExecutionContext.global

  case class RefreshRequest(refreshToken: String)
  implicit val refreshRequestFormat: Format[RefreshRequest] = Json.format[RefreshRequest]

  override def handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    import refreshRequestFormat._
    val requestBody = input.getBody
    val request = Json.parse(requestBody).asOpt[RefreshRequest]

    request match {
      case Some(req) =>
        val refreshToken = req.refreshToken

        DatabaseConfig.getDbConfig match {
          case Success(dbConfig) =>
            val db = Database.forURL(dbConfig.url, dbConfig.user, dbConfig.password, driver = "org.postgresql.Driver")
            val result = Await.result(DatabaseHandler.refreshAccessToken(refreshToken)(db, ec), Duration.Inf)
            result match {
              case Right(email) =>
                val expireDate = Instant.now().plus(1, ChronoUnit.DAYS)
                TokenHandler.createJwtToken(email, refreshToken, "dietswizard", "dietswizard") match {
                  case Success(accessToken) =>
                    val responseBody = Map("accessToken" -> accessToken.toString).asJava
                    return new APIGatewayProxyResponseEvent()
                      .withStatusCode(200)
                      .withBody(responseBody.toString)
                  case Failure(e) =>
                    return new APIGatewayProxyResponseEvent()
                      .withStatusCode(400)
                      .withBody("Token generation failed.")
                }
              case Left(error) =>
                return new APIGatewayProxyResponseEvent()
                  .withStatusCode(400)
                  .withBody(error.toString)
            }
          case Failure(e) =>
            return new APIGatewayProxyResponseEvent()
              .withStatusCode(500)
              .withBody(e.toString)
        }
      case None =>
        return new APIGatewayProxyResponseEvent()
          .withStatusCode(400)
          .withBody("Invalid request body.")
    }
  }
}
