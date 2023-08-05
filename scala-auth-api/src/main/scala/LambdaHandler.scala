import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}

import play.api.libs.json._

import java.util.{ Map => JavaMap }
import scala.collection.JavaConverters._



class LambdaHandler() extends RequestHandler[JavaMap[String, String], String] {
  override def handleRequest(event: JavaMap[String, String], context: Context): String = {
    //originden alınacak pipi
    val scalaMap: scala.collection.mutable.Map[String, String] = event.asScala
    val jwtIssuer = event.getOrElse("origin","dietswizard")
    // Replace with your desired issuer
    val jwtAudience = event.getOrElse("origin","dietswizard")

    def createJwtToken(email: String, secretKey: String, issuer: String, audience: String): String = {
      val now = new Date()
      val jwtId = java.util.UUID.randomUUID().toString
      val expirationTime = DateUtils.toSecondsSinceEpoch(Instant.now().plusSeconds(3600))
      val claimsSet = new JWTClaimsSet.Builder()
        .subject("auth")
        .issuer(jwtIssuer)
        .audience(jwtAudience)
        .issueTime(now)
        .jwtID(jwtId)
        .claim("email", email)
        .expirationTime(new java.util.Date(expirationTime * 1000))
        .build()

      val jwsHeader = new JWSHeader(JWSAlgorithm.HS256)
      val signedJWT = new SignedJWT(jwsHeader, claimsSet)

      // Create a HMAC signer with the secret key
      val signer = new MACSigner(secretKey.getBytes)

      // Sign the JWT token
      signedJWT.sign(signer)

      // Serialize the JWT token to a compact string
      Map("access_token" -> signedJWT.serialize(), "refresh_token" -> jwtId)
    }

    val responseMap: Map[String, JsValue] = Map(
      "status" -> "",
      "access_token" -> "",
      "refresh_token" -> ""
    )

    val jsonResponse: String = Json.stringify(Json.toJson(responseMap))
    jsonResponse

  }
}

// asdasdsadasd23232 232999282
// 232999282