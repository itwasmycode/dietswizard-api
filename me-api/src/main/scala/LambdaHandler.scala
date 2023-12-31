import scala.collection.JavaConverters._
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import java.util.{Date}
import org.slf4j.LoggerFactory
import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration.Duration
import play.api.libs.json._
import scala.util.{Try, Success, Failure}
import slick.jdbc.PostgresProfile.api._
import java.util.UUID
import scala.collection.JavaConverters._

object LambdaHandler extends RequestHandler[APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent] {
  val logger = LoggerFactory.getLogger(getClass)
  implicit val ec = ExecutionContext.global

  case class Request(accessToken: String)
  implicit val requestFormat: Format[Request] = Json.format[Request]

  case class Response(
                       uuid: String,
                       gender: Option[Int],
                       birthday: Option[Date],
                       premium: Boolean,
                       status: Int
                     )
  implicit val responseWrites: Writes[Response] = Json.writes[Response]

  override def handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    val accessToken = input.getHeaders.get("Authorization").substring(7)
    SecretHandler.retrieveSecret("dietswizard-uuid") match {
          case Success(secret) =>
            logger.info("t1")
            DatabaseConfig.getDbConfig match {
              case Success(dbConfig) =>
                logger.info("t2")
                TokenHandler.verifyAndDecodeJwtToken(accessToken, secret) match {
                  case Success(claimsSet) =>
                    val expirationTime = claimsSet.getExpirationTime
                    logger.info(expirationTime.toString)
                    val currentTime = new Date()
                    logger.info("t3")
                    if (expirationTime.before(currentTime)) {
                      return new APIGatewayProxyResponseEvent()
                        .withStatusCode(401)
                        .withHeaders(Map("Content-Type" -> "application/json").asJava)
                        .withBody("Access token expired")
                    }

                    val userId = claimsSet.getStringClaim("userId").toInt
                    logger.info("t4")
                    val db = Database.forURL(dbConfig.url, dbConfig.user, dbConfig.password, driver = "org.postgresql.Driver")
                    Await.result(DatabaseHandler.findUserById(userId)(db, ec), Duration.Inf) match {
                      case Right(user: DatabaseHandler.User) =>
                        logger.info("t5")
                        val response = Response(
                          user.uuid,
                          user.gender,
                          user.birthday,
                          user.premium,
                          user.status
                        )
                       return new APIGatewayProxyResponseEvent()
                          .withStatusCode(200)
                         .withHeaders(Map("Content-Type" -> "application/json").asJava)
                         .withBody(Json.toJson(response).toString())
                      case Left(error) =>
                        return new APIGatewayProxyResponseEvent()
                          .withStatusCode(500)
                          .withHeaders(Map("Content-Type" -> "application/json").asJava)
                          .withBody(error.toString)
                    }
                  case Failure(e) =>
                    return new APIGatewayProxyResponseEvent()
                      .withStatusCode(400)
                      .withHeaders(Map("Content-Type" -> "application/json").asJava)
                      .withBody("Invalid access token")
                }
              case Failure(e) =>
                return new APIGatewayProxyResponseEvent()
                  .withStatusCode(500)
                  .withHeaders(Map("Content-Type" -> "application/json").asJava)
                  .withBody("Failed to retrieve secret key")
            }
          case Failure(e) =>
            return new APIGatewayProxyResponseEvent()
              .withStatusCode(500)
              .withHeaders(Map("Content-Type" -> "application/json").asJava)
              .withBody(e.toString)
        }
    }
}

