package models

import play.api.libs.json.{Json, OFormat}
import slick.jdbc.SQLiteProfile.api._

case class Product(id: Long, name: String, description: String, price: Double, quantity: Int, categoryId: Long, active: Boolean)

object Product {
  implicit val productFormat: OFormat[Product] = Json.format[Product]
}

class ProductTable(tag: Tag) extends Table[Product](tag, "Products") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def description = column[String]("description")
  def price = column[Double]("price")
  def quantity = column[Int]("quantity")
  def categoryId = column[Long]("category_id")
  def active = column[Boolean]("active")
  def category = foreignKey("product_category_FK", categoryId, TableQuery[CategoryTable])(_.id)
  def * = (id, name, description, price, quantity, categoryId, active) <> ((Product.apply _).tupled, Product.unapply)
}