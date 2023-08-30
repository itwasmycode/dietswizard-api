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
