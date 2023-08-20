import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object DBHandler{
// User case class
  case class User(email: String, hashedPassword: String)

// Users table definition
  class Users(tag: Tag) extends Table[User](tag, "users") {
  def email = column[String]("email", O.PrimaryKey)

  def hashedPassword = column[String]("hashed_password")

  def * = (email, hashedPassword) <> (User.tupled, User.unapply)
}

  val users = TableQuery[Users]

  // Function to authenticate user
  def authenticateUser(email: String, password: String)(implicit db: Database): Future[Either[String, User]] = {
    val query = users.filter(_.email === email).result.headOption
    db.run(query).map {
      case Some(user) if user.hashedPassword == password => Right(user) // You should use a proper password hash verification here
      case _ => Left("Invalid credentials.") // Generic error message
    }
  }
}