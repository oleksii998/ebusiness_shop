package models

import models.OrderStatus.OrderStatus
import play.api.libs.json.{Json, OFormat}
import slick.jdbc.SQLiteProfile.api._

case class Order(id: Long, customerId: Long, price: Double, promotionsDiscount: Double, voucherDiscount: Double, status: String, active: Boolean)

object Order {
  implicit val orderFormat: OFormat[Order] = Json.format[Order]
}

object OrderStatus extends Enumeration {
  type OrderStatus = String
  val PLACED = "Placed"
  val BEING_MODIFIED = "Being modified"
  val DELIVERED = "Delivered"
}

class OrderTable(tag: Tag) extends Table[Order](tag, "Orders") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Long]("customer_id")
  def price = column[Double]("price")
  def promotionsDiscount = column[Double]("promotions_discount")
  def voucherDiscount = column[Double]("voucher_discount")
  def status = column[OrderStatus]("status")
  def active = column[Boolean]("active")

  def customer = foreignKey("order_customer_FK", customerId, TableQuery[CustomerTable])(_.id)
  def * = (id, customerId, price, promotionsDiscount, voucherDiscount, status, active) <> ((Order.apply _).tupled, Order.unapply)
}