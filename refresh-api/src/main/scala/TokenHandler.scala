import com.nimbusds.jose.{JWSAlgorithm, JWSHeader, Payload}
import com.nimbusds.jose.crypto.{DirectEncrypter,MACSigner,MACVerifier}
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}


import scala.util.{Try, Success, Failure}

object TokenHandler {
  def verifyAndDecodeJwtToken(jwtToken: String, secretKey: String): Try[JWTClaimsSet] = {
    Try {
      val signedJWT = SignedJWT.parse(jwtToken)
      val verifier = new MACVerifier(secretKey.getBytes)
      if (!signedJWT.verify(verifier)) throw new Exception("JWT verification failed")
      signedJWT.getJWTClaimsSet
    }
  }
}