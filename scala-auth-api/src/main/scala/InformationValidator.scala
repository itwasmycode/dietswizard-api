import scala.util.{Try, Success, Failure}

object InformationValidator {
  def validateEmail(email: String): Try[String] = {
    val emailPattern = "^[A-Za-z0-9+_.-]+@(.+)$".r
    email match {
      case emailPattern(_*) => Success(email)
      case _ => Failure(new IllegalArgumentException("Invalid email format"))
    }
  }

  def validatePassword(password: String): Try[String] = {
    if (password.length > 6) Success(password)
    else Failure(new IllegalArgumentException("Password must be greater than 6 characters"))
  }

  def validateCredentials(email: Option[String], password: Option[String]): Try[Unit] = {
    for {
      e <- email.toRight(new IllegalArgumentException("Email not provided")).toTry
      p <- password.toRight(new IllegalArgumentException("Password not provided")).toTry
      _ <- validateEmail(e)
      _ <- validatePassword(p)
    } yield ()
  }
}