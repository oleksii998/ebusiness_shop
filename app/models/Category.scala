package models

import play.api.libs.json.{Json, OFormat}
import slick.jdbc.SQLiteProfile.api._

case class Category(id: Long, name: String, description: String, active: Boolean)

object Category {
  implicit val categoryFormat: OFormat[Category] = Json.format[Category]
}

class CategoryTable(tag: Tag) extends Table[Category](tag, "Categories") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def description = column[String]("description")
  def active = column[Boolean]("active")
  def * = (id, name, description, active) <> ((Category.apply _).tupled, Category.unapply)
}