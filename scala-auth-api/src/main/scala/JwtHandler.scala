import java.util.UUID
import java.util.Date
import java.time.Instant
import java.time.{Instant, ZoneOffset}

import com.nimbusds.jose._
import com.nimbusds.jose.crypto._
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT

object JwtHandler {
  def createJwtToken(email: String, userId: String, secretKey: String, issuer: String, audience: String): scala.collection.immutable.Map[String, String] = {
    val now = new Date()
    val jwtId = java.util.UUID.randomUUID().toString
    val accessTokenExpirationMinutes = 15
    val accessTokenExpiration = now.getTime + accessTokenExpirationMinutes * 60 * 1000
    val claimsSet = new JWTClaimsSet.Builder()
      .subject("auth")
      .issuer(issuer)
      .audience(audience)
      .issueTime(now)
      .claim("email", email)
      .claim("userId",userId)
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
}

