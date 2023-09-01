import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{Future, ExecutionContext}
import java.util.UUID
import org.slf4j.LoggerFactory
import java.sql.Date  // Changed from java.util.Date to java.sql.Date

object DatabaseHandler {
  case class User(
                   user_id: Int,
                   email: String,
                   passwordHash: String,
                   uuid: String,
                   gender: Option[Int],
                   birthday: Option[Date],
                   premium: Boolean,
                   status: Int
                 )

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def user_id = column[Int]("user_id", O.PrimaryKey)

    def email = column[String]("email")

    def passwordHash = column[String]("password")

    def uuid = column[String]("uuid")

    def gender = column[Option[Int]]("gender")

    def birthday = column[Option[Date]]("birthday")

    def premium = column[Boolean]("premium")

    def status = column[Int]("status")

    def * = (
      user_id, email, passwordHash, uuid,
      gender, birthday, premium, status
    ) <> (User.tupled, User.unapply)
  }


  val users = TableQuery[Users]

  def findUserById(userId: Int)(implicit db: Database, ec: ExecutionContext): Future[Either[String, User]] = {
    db.run(users.filter(_.user_id === userId).result.headOption).map {
      case Some(user) => Right(user)
      case None => Left("User not found")
    }
  }
}
