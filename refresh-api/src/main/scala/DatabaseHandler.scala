import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{Future, ExecutionContext}
import java.util.{UUID, Date}
import org.slf4j.LoggerFactory
import java.time.Instant
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

  case class UserToken(userTokenId: Int, userId: Int, refreshToken: String, expireDate: Instant)

  val userTokens = TableQuery[UserTokens]

  class UserTokens(tag: Tag) extends Table[UserToken](tag, "user_tokens") {
    def userTokenId = column[Int]("user_token_id", O.PrimaryKey, O.AutoInc)
    def userId = column[Int]("user_id")
    def refreshToken = column[String]("refresh_token")
    def expireDate = column[Instant]("expire_date")

    def * = (userTokenId, userId, refreshToken, expireDate) <> (UserToken.tupled, UserToken.unapply)
  }

  def refreshAccessToken(refreshToken: String)(implicit db: Database, ec: ExecutionContext): Future[Either[String, User]] = {
    val query = userTokens.filter(_.refreshToken === refreshToken)
    db.run(query.result.headOption).flatMap {
      case Some(userToken) if userToken.expireDate.isAfter(Instant.now) =>
        val userQuery = users.filter(_.id === userToken.userId)
        db.run(userQuery.result.headOption).flatMap {
          case Some(user) => Right(user)
          case None => Left("User not found.")
        }
      case Some(_) => Left("Refresh token expired.")
      case None => Future.successful(Left("Invalid refresh token."))
    }
  }
}
