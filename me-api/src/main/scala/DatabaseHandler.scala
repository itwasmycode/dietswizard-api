object DatabaseHandler {
  case class User(user_id: Int, email: String, passwordHash: String, uuid: String, gender: Int, birthday: Date, premium: Boolean, status: Int)

  class Users(tag: Tag) extends Table[User](tag, "users") {
    def uuid = column[String]("uuid")
    def gender = column[Int]("gender")
    def birthday = column[Date]("birthday")
    def premium = column[Boolean]("premium")
    def status = column[Int]("status")
    def * = (user_id, email, passwordHash, uuid, gender, birthday, premium, status) <> (User.tupled, User.unapply)
  }

  def findUserByEmail(email: String)(implicit db: Database, ec: ExecutionContext): Future[Either[String, User]] = {
    db.run(users.filter(_.email === email).result.headOption).map {
      case Some(user) => Right(user)
      case None => Left("User not found")
    }
  }
}
