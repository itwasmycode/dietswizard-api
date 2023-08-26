object InformationValidator {
  def validateEmail(email: String): Boolean = {
    val emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$"
    email.matches(emailRegex)
  }

  def validatePassword(password: String): Boolean = {
    password.length >= 8
  }
}