import org.json4s._
import org.json4s.native.JsonMethods._

case class CustomRequest(
                   body: String,
                   resource: String,
                   path: String,
                   httpMethod: String,
                   isBase64Encoded: Boolean,
                   queryStringParameters: Map[String, String],
                   multiValueQueryStringParameters: Map[String, List[String]],
                   pathParameters: Map[String, String],
                   stageVariables: Map[String, String],
                   headers: Map[String, String],
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