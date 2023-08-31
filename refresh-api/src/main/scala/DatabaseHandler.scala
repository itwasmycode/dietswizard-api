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

  def refreshAccessToken(refreshToken: String)(implicit db: Database, ec: ExecutionContext): Future[Either[String, String]] = {
    val query = userTokens.filter(_.refreshToken === refreshToken)
    db.run(query.result.headOption).flatMap {
      case Some(userToken) if userToken.expireDate.isAfter(Instant.now) =>
        val userQuery = users.filter(_.id === userToken.userId)
        db.run(userQuery.result.headOption).flatMap {
          case Some(user) => Future.successful(Right(user.email))
          case None => Future.successful(Left("User not found."))
        }
      case Some(_) => Future.successful(Left("Refresh token expired."))
      case None => Future.successful(Left("Invalid refresh token."))
    }
  }
}
