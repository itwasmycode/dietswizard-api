import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{Future, ExecutionContext}
import java.util.UUID
import org.slf4j.LoggerFactory
object DatabaseHandler {

  val logger = LoggerFactory.getLogger(getClass)

  case class User(id: Int, email: String, passwordHash: String)

  val users = TableQuery[Users]

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Int]("user_id", O.PrimaryKey)

    def email = column[String]("email")

    def passwordHash = column[String]("password")

    def * = (id, email, passwordHash) <> (User.tupled, User.unapply)
  }

  // Find user by email
  def findUserByEmail(email: String)(implicit db: Database, ec: ExecutionContext): Future[Either[String, User]] = {
    db.run(users.filter(_.email === email).result.headOption).map {
      case Some(user) => Right(user)
      case None => Left("User not found")
    }
  }

  // Create a new user
  def createUser(user: User)(implicit db: Database, ec: ExecutionContext): Future[Either[String, Unit]] = {
    db.run(users += user).map(_ => Right(())).recover {
      case e => Left(e.getMessage)
    }
  }
}
