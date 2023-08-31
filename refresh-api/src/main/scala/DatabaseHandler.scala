import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{Future, ExecutionContext}
import java.util.{UUID, Date}
import org.slf4j.LoggerFactory

object DatabaseHandler {

  val logger = LoggerFactory.getLogger(getClass)

  case class User(id: Int, email: String, passwordHash: String)
  case class UserToken(userTokenId: Int, userId: Int, refreshToken: String, expireDate: java.util.Date)

  val users = TableQuery[Users]
  val userTokens = TableQuery[UserTokens]

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Int]("user_id", O.PrimaryKey)
    def email = column[String]("email")
    def passwordHash = column[String]("password")

    def * = (id, email, passwordHash) <> (User.tupled, User.unapply)
  }

  class UserTokens(tag: Tag) extends Table[UserToken](tag, "user_tokens") {
    def userTokenId = column[Int]("user_token_id", O.PrimaryKey, O.AutoInc)

    def userId = column[Int]("user_id")

    def refreshToken = column[String]("refresh_token")

    def expireDate = column[java.sql.Date]("expire_date")

    def created_at = column[java.sql.Date]("created_at")

    def updated_at = column[java.sql.Date]("updated_at")

    def * = (userTokenId, userId, refreshToken, expireDate) <> (UserToken.tupled, UserToken.unapply)
  }
  // Find user by email
  def findUserByEmail(email: String)(implicit db: Database, ec: ExecutionContext): Future[Either[String, User]] = {
    db.run(users.filter(_.email === email).result.headOption).map {
      case Some(user) => Right(user)
      case None => Left("User not found")
    }
  }

  // Find user id by refresh token
  def findUserIdByRefreshToken(refreshToken: String)(implicit db: Database, ec: ExecutionContext): Future[Either[String, (Int, Date)]] = {
    db.run(userTokens.filter(_.refreshToken === refreshToken).result.headOption).map {
      case Some(userToken) => Right((userToken.userId, userToken.expireDate))
      case None => Left("Refresh token not found")
    }
  }
}
