import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{Future, ExecutionContext}
import at.favre.lib.crypto.bcrypt._
import org.slf4j.LoggerFactory

object DatabaseHandler {

  val logger = LoggerFactory.getLogger(getClass)

  case class User(id: Int, email: String, passwordHash: String)

  val users = TableQuery[Users]

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Int]("user_id", O.PrimaryKey, O.AutoInc)
    def email = column[String]("email")
    def passwordHash = column[String]("password")

    def * = (id, email, passwordHash) <> (User.tupled, User.unapply)
  }

  def authenticateUser(email: String, password: String)(implicit db: Database, ec: ExecutionContext): Future[Either[String, User]] = {
    val query = users.filter(_.email === email)
    db.run(query.result.headOption).map {
      case Some(user) if BCrypt.verifyer().verify(password.toCharArray, user.passwordHash.getBytes).verified => Right(user)
      case _ => Left("Invalid credentials")
    }
  }
}
