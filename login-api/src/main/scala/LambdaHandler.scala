import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import scala.concurrent.{Future, ExecutionContext, Await}
import scala.util.{Try, Success, Failure}
import org.slf4j.LoggerFactory
import java.util.{Map => JavaMap}
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.duration._
import java.util.{Map => JavaMap, HashMap => JavaHashMap}
import play.api.libs.json._


class LambdaHandler() extends RequestHandler[JavaMap[String, String], JavaMap[String, String]] {

  val logger = LoggerFactory.getLogger(getClass)
  implicit val ec = ExecutionContext.global

  val jwtSecret = SecretHandler.retrieveSecret("dietswizard-uuid").get
  val jwtParsed = Json.parse(jwtSecret) \ "uuid"
  val uuid = (jwtParsed \ "uuid").as[String]
  override def handleRequest(input: JavaMap[String, String], context: Context): JavaMap[String, String] = {
    val email = input.get("email")
    val password = input.get("password")

    if (!InformationValidator.validateEmail(email) || !InformationValidator.validatePassword(password)) {
      return new JavaHashMap[String, String] {
        put("error", "Invalid input")
      }
    }

    DatabaseConfig.getDbConfig match {
      case Success(dbConfig) =>
        val db = Database.forURL(dbConfig.url, dbConfig.user, dbConfig.password, driver="org.postgresql.Driver")
        val result = Await.result(DatabaseHandler.authenticateUser(email, password)(db, ec), Duration.Inf)
        result match {
          case Right(user) =>
            TokenHandler.createJwtToken(email, uuid, "dietswizard", "dietswizard") match {
              case Success(token) =>
                new JavaHashMap[String, String] {
                  put("token", token)
                }
              case Failure(e) =>
                new JavaHashMap[String, String] {
                  put("error", "Token generation failed")
                }
            }
          case Left(error) =>
            new JavaHashMap[String, String] {
              put("error", error)
            }
        }
      case Failure(e) =>
        new JavaHashMap[String, String] {
          put("error", "Database configuration failed")
        }
    }
  }
}
