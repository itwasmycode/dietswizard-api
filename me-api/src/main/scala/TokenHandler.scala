import com.nimbusds.jose.{JWSAlgorithm, JWSHeader, Payload}
import com.nimbusds.jose.crypto.DirectEncrypter
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.{JWTClaimsSet, SignedJWT}

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
