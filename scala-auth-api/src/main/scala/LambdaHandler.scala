package LambdaPackage

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}

import com.nimbusds.jose._
import com.nimbusds.jose.crypto._
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT

import play.api.libs.json._

import java.util.{ Map => JavaMap }
import java.util.UUID
import java.util.Date
import java.time.Instant
import java.time.{Instant, ZoneOffset}
import scala.collection.JavaConverters._

import LambdaPackage.TestPurpose

class LambdaHandler() extends RequestHandler[JavaMap[String, String], String] {
  override def handleRequest(event: JavaMap[String, String], context: Context): String = {
    //originden alınacak pipi
    val scalaMap: scala.collection.mutable.Map[String, String] = event.asScala
    val immutableScalaMap: scala.collection.immutable.Map[String, String] = scalaMap.toMap
    val jwtIssuer = immutableScalaMap.getOrElse("origin","dietswizard")

    // Replace with your desired issuer
    val jwtAudience = immutableScalaMap.getOrElse("origin","dietswizard")

    def createJwtToken(email: String, secretKey: String, issuer: String, audience: String): scala.collection.immutable.Map[String,String] = {
      val now = new Date()
      val jwtId = java.util.UUID.randomUUID().toString
      val accessTokenExpirationMinutes = 15
      val accessTokenExpiration = now.getTime + accessTokenExpirationMinutes * 60 * 1000
      val claimsSet = new JWTClaimsSet.Builder()
        .subject("auth")
        .issuer(jwtIssuer)
        .audience(jwtAudience)
        .issueTime(now)
        .jwtID(jwtId)
        .claim("email", email)
        .expirationTime(new java.util.Date(accessTokenExpiration))
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
      "status" -> JsString("200"),
      "access_token" -> JsString("abc"),
      "refresh_token" -> JsString("123")
    )
    printSmt
    val jsonResponse: String = Json.stringify(Json.toJson(responseMap))
    jsonResponse

  }
}

// asdasdsadasd23232 232999282
// 232999282