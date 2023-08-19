
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}



import play.api.libs.json._

import java.util.{ Map => JavaMap }

import scala.collection.JavaConverters._


class LambdaHandler() extends RequestHandler[JavaMap[String, String], String] {
  override def handleRequest(event: JavaMap[String, String], context: Context): String = {
    //originden alınacak pipi
    val scalaMap: scala.collection.mutable.Map[String, String] = event.asScala
    val immutableScalaMap: scala.collection.immutable.Map[String, String] = scalaMap.toMap

    val jwtIssuer : Option[String] = Option(immutableScalaMap.getOrElse("origin","dietswizard"))
    val jwtAudience: Option[String] = Option(immutableScalaMap.getOrElse("origin","dietswizard"))
    val email :Option[String] = Option(immutableScalaMap.getOrElse("email",null))
    val userId :Option[String] = Option(immutableScalaMap.getOrElse("userId",null))
    val password: Option[String] = Option(immutableScalaMap.getOrElse("password",null))

    def authenticateAndCreateToken(
                                    email: String,
                                    password: String,
                                    userId: String,
                                    secretKey: String = "a2d15685-7a71-496d-a55d-39397ae7a89d",
                                    jwtIssuer: String,
                                    jwtAudience: String
                                  ): Either[Int, Map[String, String]] = {
      InformationHandler.validateCredentials(email, password) match {
        case Success(_) =>
          JwtHandler.createJwtToken(email, userId, secretKey, jwtIssuer, jwtAudience) match {
            case Some(jwt) =>
              Right(Map(
                "status" -> "200",
                "access_token" -> jwt,
                "refresh_token" -> "123" // Replace with actual refresh token logic
              ))
            case None =>
              Left(500) // Internal Server Error
          }
        case Failure(e) =>
          Left(401) // Unauthorized
      }
    }

    //val jsonResponse: String = Json.stringify(Json.toJson(responseMap))
    jsonResponse

  }
}

