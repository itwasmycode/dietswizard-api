import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import java.io.{InputStream, OutputStream}
import java.nio.charset.StandardCharsets

import java.util.{Base64,Map => JavaMap, HashMap => JavaHashMap}
import org.slf4j.LoggerFactory
import scala.concurrent.{ExecutionContext, Await}
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import play.api.libs.json._
import slick.jdbc.PostgresProfile.api._
import java.util.UUID
import at.favre.lib.crypto.bcrypt._

class LambdaHandler() extends RequestHandler[JavaMap[String, String], JavaMap[String, String]] {
  val logger = LoggerFactory.getLogger(getClass)
  implicit val ec = ExecutionContext.global
  val objectMapper = new ObjectMapper().registerModule(DefaultScalaModule)

  override def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = {
    val event = objectMapper.readValue(input, classOf[APIGatewayProxyEvent])
    val body = new String(Base64.getDecoder.decode(event.body), StandardCharsets.UTF_8)
    val request = objectMapper.readValue(body, classOf[Request])
    val response = new JavaHashMap[String, String]()

    val email = request.email
    val password = request.password

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
            val newUser = DatabaseHandler.User(UUID.randomUUID(), email, hashedPassword)
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
