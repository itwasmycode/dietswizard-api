import java.util.UUID
import java.util.Date
import java.time.Instant
import java.time.{Instant, ZoneOffset}

import com.nimbusds.jose._
import com.nimbusds.jose.crypto._
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.jwt._

import scala.util.{Try, Success, Failure}

object JwtHandler {


  def createJwtToken(email: String, userId: String, secretKey: String, issuer: String, audience: String): Try[String] = {
    (for {
      now <- Try(new Date())
      jwtId = java.util.UUID.randomUUID().toString
      accessTokenExpirationMinutes = 15
      accessTokenExpiration = now.getTime + accessTokenExpirationMinutes * 60 * 1000
      claimsSet <- Try(
        new JWTClaimsSet.Builder()
          .subject("auth")
          .issuer(issuer)
          .audience(audience)
          .issueTime(now)
          .claim("email", email)
          .claim("userId", userId)
          .expirationTime(new java.util.Date(accessTokenExpiration))
          .build()
      )
      jwsHeader = new JWSHeader(JWSAlgorithm.HS256)
      signedJWT = new SignedJWT(jwsHeader, claimsSet)
      signer <- Try(new MACSigner(secretKey.getBytes))
      _ <- Try(signedJWT.sign(signer))
    } yield signedJWT.serialize()
      ).orElse(Failure(new Exception("JWT creation failed")))
  }
}

