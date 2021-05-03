package models

import play.api.libs.json.{Json, OFormat}
import slick.jdbc.SQLiteProfile.api._

case class Customer(id: Long, email: String, password: String, firstName: String, lastName: String, active: Boolean)

object Customer {
  implicit val bonusCardFormat: OFormat[Customer] = Json.format[Customer]
}

class CustomerTable(tag: Tag) extends Table[Customer](tag, "Customers") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def email = column[String]("email")
  def password = column[String]("password")
  def firstName = column[String]("firstName")
  def lastName = column[String]("lastName")
  def active = column[Boolean]("active")
  def * = (id, email, password, firstName, lastName, active) <> ((Customer.apply _).tupled, Customer.unapply)
}