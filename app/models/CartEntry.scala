package models

import play.api.libs.json.{Json, OFormat}
import slick.jdbc.SQLiteProfile.api._

case class CartEntry(id: Long, customerId: Long, productId: Long, quantity: Int, orderId: Option[Long])

object CartEntry {
  implicit val cartFormat: OFormat[CartEntry] = Json.format[CartEntry]
}

class CartTable(tag: Tag) extends Table[CartEntry](tag, "Carts") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Long]("customer_id")
  def productId = column[Long]("product_id")
  def quantity = column[Int]("quantity")
  def orderId = column[Option[Long]]("order_id")

  def customer = foreignKey("cart_customer_FK", customerId, TableQuery[CustomerTable])(_.id)
  def product = foreignKey("cart_product_FK", productId, TableQuery[ProductTable])(_.id)

  def * = (id, customerId, productId, quantity, orderId) <> ((CartEntry.apply _).tupled, CartEntry.unapply)
}