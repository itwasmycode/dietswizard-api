import scala.collection.JavaConverters._
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import java.util.{Date}
import org.slf4j.LoggerFactory
import scala.concurrent.{ExecutionContext, Future}
import play.api.libs.json._
import scala.util.{Try, Success, Failure}
import slick.jdbc.PostgresProfile.api._
import java.util.UUID

object LambdaHandler extends RequestHandler[APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent] {
  implicit val ec = ExecutionContext.global

  case class Request(accessToken: String)
  implicit val requestFormat: Format[Request] = Json.format[Request]

  case class Response(uuid: String, gender: Int, birthday: Date, premium: Boolean, status: Int)
  implicit val responseWrites: Writes[Response] = Json.writes[Response]

  override def handleRequest(input: APIGatewayProxyRequestEvent, context: Context): APIGatewayProxyResponseEvent = {
    val requestBody = input.getBody
    val request = Json.parse(requestBody).asOpt[Request]

    request match {
      case Some(req) =>
        val accessToken = req.accessToken

        DatabaseConfig.getDbConfig match {
          case Success(secretKey) =>
            TokenHandler.verifyAndDecodeJwtToken(accessToken, secretKey.secretKey) match {
              case Success(claimsSet) =>
                val expirationTime = claimsSet.getExpirationTime
                val currentTime = new Date()

                if (expirationTime.before(currentTime)) {
                  return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody("Access token expired")
                }

                val userId = claimsSet.getStringClaim("user_id").toInt
                DatabaseConfig.getDbConfig match {
                  case Success(dbConfig) =>
                    val db = Database.forURL(dbConfig.url, dbConfig.user, dbConfig.password, driver = "org.postgresql.Driver")
                    Await.result(DatabaseHandler.findUserById(userId)(db, ec), Duration.Inf) match {
                      case Right(user: DatabaseHandler.User) =>
                        val response = Response(user.uuid, user.gender, user.birthday, user.premium, user.status)
                        return new APIGatewayProxyResponseEvent()
                          .withStatusCode(200)
                          .withBody(Json.toJson(response).toString())
                      case Left(error) =>
                        return new APIGatewayProxyResponseEvent()
                          .withStatusCode(500)
                          .withBody(error)
                    }
                  case Failure(e) =>
                    return new APIGatewayProxyResponseEvent()
                      .withStatusCode(500)
                      .withBody("DB Configuration Failed")
                }
              case Failure(e) =>
                return new APIGatewayProxyResponseEvent()
                  .withStatusCode(400)
                  .withBody("Invalid access token")
            }
          case Failure(e) =>
            return new APIGatewayProxyResponseEvent()
              .withStatusCode(500)
              .withBody("Failed to retrieve secret key")
        }
      case None =>
        return new APIGatewayProxyResponseEvent()
          .withStatusCode(400)
          .withBody("Error decoding request")
    }
  }
}
