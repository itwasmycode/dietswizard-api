import play.api.libs.json.{Json,JsValue}
import scala.util.Try

object DatabaseConfig {

  case class DbConfig(url: String, user: String, password: String)

  private val SecretName = "dietswizard-db-prod"


  def parseDbConfig(secret: String): Try[DbConfig] = Try {
    val dbConfigJson: JsValue = Json.parse(secret)
    DbConfig(
      (dbConfigJson \ "url").as[String],
      (dbConfigJson \ "user").as[String],
      (dbConfigJson \ "password").as[String]
    )
  }

  def getDbConfig: Try[DbConfig] = {
    for {
      secret <- SecretHandler.retrieveSecret(SecretName)
      config <- parseDbConfig(secret)
    } yield config
  }
}
