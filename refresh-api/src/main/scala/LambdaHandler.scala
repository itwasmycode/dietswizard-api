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
        val refreshToken = req.refreshToken.getOrElse("")
        if (!RefreshTokenValidator.validateUUID(refreshToken)) {
          return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Invalid refresh token")
        }

        DatabaseConfig.getDbConfig match {
          case Success(dbConfig) =>
            val db = Database.forURL(dbConfig.url, dbConfig.user, dbConfig.password, driver = "org.postgresql.Driver")
            Await.result(DatabaseHandler.findUserIdByRefreshToken(refreshToken)(db, ec), Duration.Inf) match {
              case Right((userId, expireDate)) =>
                if (expireDate.before(new Date())) {
                  return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Refresh token expired")
                }

                Await.result(DatabaseHandler.findUserById(userId)(db, ec), Duration.Inf) match {
                  case Right(user) =>
                    val email = user.email
                    val newToken = TokenHandler.createJwtToken(email, secretKey, issuer, audience)
                    newToken match {
                      case Success(token) =>
                        return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody(token)
                      case Failure(e) =>
                        return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Error creating new JWT token")
                    }
                  case Left(error) =>
                    return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody(error)
                }
              case Left(error) =>
                return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody(error)
            }
          case Failure(e) =>
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("DB Configuration Failed")
        }
      case None =>
        return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Error decoding request")
    }
  }
}