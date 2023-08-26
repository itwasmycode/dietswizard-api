import play.api.libs.json._

case class Request(email: String, password: String)

object Request {
  implicit val reads: Reads[Request] = Json.reads[Request]
}
