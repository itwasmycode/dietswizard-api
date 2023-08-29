import com.amazonaws.services.lambda.runtime.{Context, RequestStreamHandler}
import com.amazonaws.services.lambda.runtime.events.{APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent}
import java.io.{InputStream, OutputStream,OutputStreamWriter}
import java.nio.charset.StandardCharsets
import org.json4s._
import org.json4s.jackson.JsonMethods._
import java.util.{Base64, Map => JavaMap, HashMap => JavaHashMap}
import org.slf4j.LoggerFactory
import scala.concurrent.{ExecutionContext, Future,Await}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}
import slick.jdbc.PostgresProfile.api._
import java.util.UUID
import at.favre.lib.crypto.bcrypt._
import play.api.libs.json.Json


object Handler extends RequestStreamHandler {


  val logger = LoggerFactory.getLogger(getClass)
  implicit val ec = ExecutionContext.global


  override def handleRequest(input: InputStream, output: OutputStream, context: Context): Unit = {
    implicit val formats = DefaultFormats

    // parse input stream
    val requestEvent = parse(input)

    val body = (requestEvent \ "body").extract[String]
    val isBase64Encoded = (requestEvent \ "isBase64Encoded").extract[Boolean]

    val decodedBytes = if (isBase64Encoded) {
      Base64.getDecoder.decode(body)
    } else {
      body.getBytes(StandardCharsets.UTF_8)
    }

    val decodedString = new String(decodedBytes, StandardCharsets.UTF_8)

    logger.info("Decoded string: {}", decodedString)
    /*
    request match {
      case Some(req) =>
        logger.info(req.toString)
        val email = req.email
        val password = req.password

        if (!InformationValidator.validateEmail(email) || !InformationValidator.validatePassword(password)) {
          return new APIGatewayProxyResponseEvent()
            .withStatusCode(400)
            .withBody("Invalid input")
        }

        DatabaseConfig.getDbConfig match {
          case Success(dbConfig) =>
            val db = Database.forURL(dbConfig.url, dbConfig.user, dbConfig.password, driver = "org.postgresql.Driver")
            Await.result(DatabaseHandler.findUserByEmail(email)(db, ec), Duration.Inf) match {
              case Right(user) =>
                return new APIGatewayProxyResponseEvent()
                  .withStatusCode(400)
                  .withBody("User already exists")
              case Left(_) =>
                val hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray)
                val newUser = DatabaseHandler.User(UUID.randomUUID(), email, hashedPassword)
                Await.result(DatabaseHandler.createUser(newUser)(db, ec), Duration.Inf) match {
                  case Right(_) =>
                    return new APIGatewayProxyResponseEvent()
                      .withStatusCode(200)
                      .withBody("User created successfully.")

                  case Left(error) =>
                    logger.info("hey")
                    logger.info(error)

                    return new APIGatewayProxyResponseEvent()
                      .withStatusCode(500)
                      .withBody(error.toString)
                }
            }
          case Failure(e) =>

            return new APIGatewayProxyResponseEvent()
              .withStatusCode(500)
              .withBody("DB Configuration Failed")
        }
      case None =>
        return new APIGatewayProxyResponseEvent()
          .withStatusCode(400)
          .withBody("Error decoding request")
    }
    */
    val writer = new OutputStreamWriter(output, StandardCharsets.UTF_8)
    val response = s"""{"statusCode": 200, "body": "$decodedString"}"""
    writer.write(response)
    writer.close()
  }
}