import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import java.util.{Map => JavaMap, HashMap => JavaHashMap}
import org.slf4j.LoggerFactory
import scala.concurrent.{ExecutionContext, Await}
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import play.api.libs.json._
import slick.jdbc.PostgresProfile.api._
import java.util.UUID
import org.mindrot.jbcrypt.BCrypt

class LambdaHandler() extends RequestHandler[JavaMap[String, String], JavaMap[String, String]] {

  val logger = LoggerFactory.getLogger(getClass)
  implicit val ec = ExecutionContext.global

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
        val result = Await.result(DatabaseHandler.findUserByEmail(email)(db, ec), Duration.Inf)
        result match {
          case Right(user) =>
            new JavaHashMap[String, String] {
              put("error", "User already exists")
            }
          case Left(_) =>
            val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray)
            val newUser = User(UUID.randomUUID().toString, email, hashedPassword)
            Await.result(DatabaseHandler.createUser(newUser)(db, ec), Duration.Inf) match {
              case Right(_) =>
                new JavaHashMap[String, String] {
                  put("success", "User created successfully")
                }
              case Left(error) =>
                new JavaHashMap[String, String] {
                  put("error", error)
                }
            }
        }
      case Failure(e) =>
        new JavaHashMap[String, String] {
          put("error", "Database configuration failed")
        }
    }
  }
}
