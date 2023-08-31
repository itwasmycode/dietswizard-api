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

  case class UserToken(userTokenId: Int, userId: Int, refreshToken: String, expireDate: Instant)

  val userTokens = TableQuery[UserTokens]

  class UserTokens(tag: Tag) extends Table[UserToken](tag, "user_tokens") {
    def userTokenId = column[Int]("user_token_id", O.PrimaryKey, O.AutoInc)
    def userId = column[Int]("user_id")
    def refreshToken = column[String]("refresh_token")
    def expireDate = column[Instant]("expire_date")

    def * = (userTokenId, userId, refreshToken, expireDate) <> (UserToken.tupled, UserToken.unapply)
  }

  def authenticateUser(email: String, password: String, refreshToken: String, expireDate: Instant)(implicit db: Database, ec: ExecutionContext): Future[Either[String, User]] = {
    val query = users.filter(_.email === email)
    db.run(query.result.headOption).flatMap {
      case Some(user) if BCrypt.verifyer().verify(password.toCharArray, user.passwordHash.getBytes).verified =>
        val userToken = UserToken(0, user.id, refreshToken, expireDate)
        db.run(userTokens += userToken).map(_ => Right(user))
      case _ => Future.successful(Left("Invalid credentials"))
    }
  }

}
