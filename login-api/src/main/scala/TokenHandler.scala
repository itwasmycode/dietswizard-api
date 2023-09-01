import scala.util.{Try, Success, Failure}

import java.util.UUID
import java.util.Date
import java.time.{Instant, ZoneOffset}

import com.nimbusds.jose.{JWSAlgorithm, JWSHeader, Payload}
import com.nimbusds.jose.crypto.DirectEncrypter
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}
import com.nimbusds.jwt.JWTClaimsSet

object TokenHandler {
  def createJwtToken(email: String,userId: Int, secretKey: String, issuer: String, audience: String): Try[String] = {
    (for {
      now <- Try(new Date())
      accessTokenExpirationMinutes = 1
      accessTokenExpiration = now.getTime + accessTokenExpirationMinutes * 60 * 1000
      claimsSet <- Try(
        new JWTClaimsSet.Builder()
          .subject("auth")
          .issuer(issuer)
          .audience(audience)
          .issueTime(now)
          .claim("email", email)
          .claim("userId", userId.toString)
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