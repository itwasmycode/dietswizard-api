import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import slick.jdbc.PostgresProfile.api._

import scala.util.{Try, Success, Failure}
import play.api.libs.json._

import java.util.{ Map => JavaMap }
import scala.collection.JavaConverters._

class LambdaHandler() extends RequestHandler[JavaMap[String, String], String] {
  override def handleRequest(event: JavaMap[String, String], context: Context): String = {
    val scalaMap: scala.collection.mutable.Map[String, String] = event.asScala
    val immutableScalaMap: scala.collection.immutable.Map[String, String] = scalaMap.toMap

    val jwtIssuer : Option[String] = Option(immutableScalaMap.getOrElse("origin","dietswizard"))
    val jwtAudience: Option[String] = Option(immutableScalaMap.getOrElse("origin","dietswizard"))
    val email :Option[String] = Option(immutableScalaMap.getOrElse("email",null))
    val userId :Option[String] = Option(immutableScalaMap.getOrElse("userId",null))
    val password: Option[String] = Option(immutableScalaMap.getOrElse("password",null))

    val secretId = "dietswizard-db-prod"
    val secretsManager = AWSSecretsManagerClientBuilder.standard().build()
    val request = new GetSecretValueRequest().withSecretId(secretId)
    val secretValue = secretsManager.getSecretValue(request)

    val dbConfigJson: JsValue = Json.parse(secretValue.getSecretString)

    val url_db = (dbConfigJson \ "url").as[String]
    val user_db = (dbConfigJson \ "user").as[String]
    val password_db = (dbConfigJson \ "password").as[String]

    val db = Database.forURL(url_db, user_db, password_db)

    def authenticateAndCreateToken(
                                    email: Option[String],
                                    password: Option[String],
                                    userId: Option[String],
                                    secretKey: Option[String],
                                    jwtIssuer: Option[String],
                                    jwtAudience: Option[String]
                                  )(implicit db: Database): Either[Int, Map[String, String]] = {
      InformationValidator.validateCredentials(email, password) match {
        case Success(_) =>
          for {
            e <- email.toRight(500)
            u <- userId.toRight(500)
            s <- secretKey.toRight(500)
            i <- jwtIssuer.toRight(500)
            a <- jwtAudience.toRight(500)
            jwtTry <- Try(JwtHandler.createJwtToken(e, u, s, i, a)).toOption.toRight(500)
            jwt <- jwtTry.toOption.toRight(500)
          } yield {
            Map(
              "status" -> "200",
              "access_token" -> jwt,
              "refresh_token" -> "123"
            )
          }
        case Failure(_) =>
          Left(401)
      }
    }

    authenticateAndCreateToken(email, password, userId, Some("a2d15685-7a71-496d-a55d-39397ae7a89d"), jwtIssuer, jwtAudience)(db) match {
      case Right(responseMap) => Json.toJson(responseMap).toString()
      case Left(errorCode) => Json.toJson(Map("error" -> errorCode.toString)).toString()
    }
  }
}
