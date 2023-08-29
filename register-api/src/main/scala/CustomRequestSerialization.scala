import play.api.libs.json._

case class CustomRequest(
                   body: String,
                   resource: String,
                   path: String,
                   httpMethod: String,
                   isBase64Encoded: Boolean,
                   queryStringParameters: Map[String, Map[String,String]],
                   multiValueQueryStringParameters: Map[String, List[String]],
                   pathParameters: Map[String, Map[String,String]],
                   stageVariables: Map[String, String],
                   headers: Map[String, Map[String,String]],
                   multiValueHeaders: Map[String, List[String]],
                   requestContext: RequestContext
                 )

case class RequestContext(
                           accountId: String,
                           resourceId: String,
                           stage: String,
                           requestId: String,
                           requestTime: String,
                           requestTimeEpoch: Long,
                           identity: Identity,
                           path: String,
                           resourcePath: String,
                           httpMethod: String,
                           apiId: String,
                           protocol: String
                         )

case class Identity(
                     cognitoIdentityPoolId: Option[String],
                     accountId: Option[String],
                     cognitoIdentityId: Option[String],
                     caller: Option[String],
                     accessKey: Option[String],
                     sourceIp: String,
                     cognitoAuthenticationType: Option[String],
                     cognitoAuthenticationProvider: Option[String],
                     userArn: Option[String],
                     userAgent: String,
                     user: Option[String]
                   )

object CustomRequest {
  implicit val identityFormat: Format[Identity] = Json.format[Identity]
  implicit val requestContextFormat: Format[RequestContext] = Json.format[RequestContext]
  implicit val customRequestFormat: Format[CustomRequest] = Json.format[CustomRequest]
}
