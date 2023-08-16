
import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}



import play.api.libs.json._

import java.util.{ Map => JavaMap }

import scala.collection.JavaConverters._


class LambdaHandler() extends RequestHandler[JavaMap[String, String], String] {
  override def handleRequest(event: JavaMap[String, String], context: Context): String = {
    //originden alınacak pipi
    val scalaMap: scala.collection.mutable.Map[String, String] = event.asScala
    val immutableScalaMap: scala.collection.immutable.Map[String, String] = scalaMap.toMap
    val jwtIssuer = immutableScalaMap.getOrElse("origin","dietswizard")

    // Replace with your desired issuer
    val jwtAudience = immutableScalaMap.getOrElse("origin","dietswizard")
    val email = immutableScalaMap.getOrElse("email",null)
    val userId = immutableScalaMap.getOrElse("userId",null)

    val testIt = JwtHandler.createJwtToken(email,userId,"test",jwtIssuer,jwtAudience)
    //email: String, userId: String, secretKey: String, issuer: String, audience: String
    val responseMap: Map[String, JsValue] = Map(
      "status" -> JsString("200"),
      "access_token" -> JsString("abc"),
      "refresh_token" -> JsString("123")
    )

    println(testIt)
    val jsonResponse: String = Json.stringify(Json.toJson(responseMap))
    jsonResponse

  }
}

// asdasdsadasd23232 232999282
// 232999282