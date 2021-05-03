package models

import play.api.libs.json.{Json, OFormat}
import slick.jdbc.SQLiteProfile.api._

case class User(id: Long, email: String, password: String, firstName: String, lastName: String, active: Boolean)

object User {
  implicit val userFormat: OFormat[User] = Json.format[User]
}

class UserTable(tag: Tag) extends Table[User](tag, "Users") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email")
  def password = column[String]("password")
  def firstName = column[String]("firstName")
  def lastName = column[String]("lastName")
  def active = column[Boolean]("active")
  def * = (id, email, password, firstName, lastName, active) <> ((User.apply _).tupled, User.unapply)
}