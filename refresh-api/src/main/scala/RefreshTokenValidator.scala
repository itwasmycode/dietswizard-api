import java.util.UUID

object RefreshTokenValidator {

  def validateUUID(uuid: String): Boolean = {
    try {
      UUID.fromString(uuid)
      true
    } catch {
      case _: IllegalArgumentException => false
    }
  }

}
