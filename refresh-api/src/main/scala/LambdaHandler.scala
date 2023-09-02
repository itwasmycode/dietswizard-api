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

import java.time.temporal.ChronoUnit
import java.time.Instant


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
            SecretHandler.retrieveSecret("dietswizard-uuid") match {
              case Success(secret) =>
                val result = Await.result(DatabaseHandler.refreshAccessToken(refreshToken)(db, ec), Duration.Inf)
                result match {
                  case Right(email) =>
                    TokenHandler.createJwtToken(email, secret, "dietswizard", "dietswizard") match {
                      case Success(accessToken) =>
                        val responseBody = Map("accessToken" -> accessToken.toString)
                        return new APIGatewayProxyResponseEvent()
                          .withStatusCode(200)
                          .withHeaders(Map("Content-Type" -> "application/json").asJava)
                          .withBody(Json.toJson(responseBody).toString())
                      case Failure(e) =>
                        return new APIGatewayProxyResponseEvent()
                          .withStatusCode(400)
                          .withHeaders(Map("Content-Type" -> "application/json").asJava)
                          .withBody("Token generation failed.")
                    }
                  case Left(error) =>
                    return new APIGatewayProxyResponseEvent()
                      .withStatusCode(400)
                      .withHeaders(Map("Content-Type" -> "application/json").asJava)
                      .withBody(error.toString)
                }
              case Failure(e) =>
                return new APIGatewayProxyResponseEvent()
                  .withStatusCode(500)
                  .withHeaders(Map("Content-Type" -> "application/json").asJava)
                  .withBody(e.toString)
            }
          case Failure(e) =>
            return new APIGatewayProxyResponseEvent()
              .withStatusCode(500)
              .withHeaders(Map("Content-Type" -> "application/json").asJava)
              .withBody(e.toString)
        }
      case None =>
        return new APIGatewayProxyResponseEvent()
          .withStatusCode(400)
          .withHeaders(Map("Content-Type" -> "application/json").asJava)
          .withBody("Invalid body")
        }
    }
}

