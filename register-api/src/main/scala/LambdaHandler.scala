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
            .withHeaders(Map("Content-Type" -> "application/json").asJava)
            .withBody("Invalid input")
        }

        DatabaseConfig.getDbConfig match {
          case Success(dbConfig) =>
            val db = Database.forURL(dbConfig.url, dbConfig.user, dbConfig.password, driver = "org.postgresql.Driver")
            Await.result(DatabaseHandler.findUserByEmail(email)(db, ec), Duration.Inf) match {
              case Right(user) =>
                return new APIGatewayProxyResponseEvent()
                  .withStatusCode(400)
                  .withHeaders(Map("Content-Type" -> "application/json").asJava)
                  .withBody("User already exists")
              case Left(_) =>
                val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray)
                val newUser = DatabaseHandler.User(0, UUID.randomUUID().toString, email, hashedPassword)
                Await.result(DatabaseHandler.createUser(newUser)(db, ec), Duration.Inf) match {
                  case Right(_) =>
                    return new APIGatewayProxyResponseEvent()
                      .withStatusCode(200)
                      .withHeaders(Map("Content-Type" -> "application/json").asJava)
                      .withBody("User created successfully.")

                  case Left(error) =>
                    logger.info("hey")
                    logger.info(error)

                    return new APIGatewayProxyResponseEvent()
                      .withStatusCode(500)
                      .withHeaders(Map("Content-Type" -> "application/json").asJava)
                      .withBody(error.toString)
                }
            }
          case Failure(e) =>

            return new APIGatewayProxyResponseEvent()
              .withStatusCode(500)
              .withHeaders(Map("Content-Type" -> "application/json").asJava)
              .withBody("DB Configuration Failed")
        }
      case None =>
        return new APIGatewayProxyResponseEvent()
          .withStatusCode(400)
          .withHeaders(Map("Content-Type" -> "application/json").asJava)
          .withBody("Error decoding request")
    }
  }
}