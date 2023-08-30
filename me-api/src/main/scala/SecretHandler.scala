import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest
import com.amazonaws.regions.Regions
import com.amazonaws.ClientConfiguration
import scala.util.{Try, Success, Failure}
object SecretHandler{
  def retrieveSecret(secretId: String): Try[String] = Try {
    val clientConfiguration = new ClientConfiguration()
    clientConfiguration.setConnectionTimeout(5000)
    clientConfiguration.setSocketTimeout(5000)
    val secretsManager = AWSSecretsManagerClientBuilder.standard()
      .withRegion(Regions.EU_CENTRAL_1)
      .withClientConfiguration(clientConfiguration)
      .build()
    val request = new GetSecretValueRequest().withSecretId(secretId)
    secretsManager.getSecretValue(request).getSecretString
  }
}