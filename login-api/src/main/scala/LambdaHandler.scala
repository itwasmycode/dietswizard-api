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



object LambdaHandler extends RequestHandler[APIGatewayProxyRequestEvent,APIGatewayProxyResponseEvent] {
  val logger = LoggerFactory.getLogger(getClass)
  implicit val ec = ExecutionContext.global

  case class Request(email: String, password: String)

  implicit val requestFormat: Format[Request] = Json.format[Request]

  override def handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    import requestFormat._
    val requestBody = input.getBody
    val request = Json.parse(requestBody).asOpt[Request]
    request match {
      case Some(req) =>
        val email = req.email
        val password = req.password

        if (!InformationValidator.validateEmail(email) || !InformationValidator.validatePassword(password)) {
          return new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withBody("Invalid input")
        }
    }
    val refreshToken = UUID.randomUUID().toString
    val expireDate = Instant.now().plus(14, ChronoUnit.DAYS)

    DatabaseConfig.getDbConfig match {
      case Success(dbConfig) =>
        val db = Database.forURL(dbConfig.url, dbConfig.user, dbConfig.password, driver = "org.postgresql.Driver")
        val result = Await.result(DatabaseHandler.authenticateUser(email, password, refreshToken, expireDate)(db, ec), Duration.Inf)
        result match {
          case Right(user) =>
            TokenHandler.createJwtToken(email, uuid, "dietswizard", "dietswizard") match {
              case Success(accessToken) =>
                val responseBody = Json.toJson(Map("accessToken" -> accessToken, "refreshToken" -> refreshToken)).toString
                return new APIGatewayProxyResponseEvent()
                  .withStatusCode(200)
                  .withBody(responseBody)
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
  }
}

