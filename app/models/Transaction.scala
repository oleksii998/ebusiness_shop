package models

import models.TransactionStatus.TransactionStatus
import play.api.libs.json.{Json, OFormat}
import slick.jdbc.SQLiteProfile.api._

case class Transaction(id: Long, orderId: Long, finalPrice: Double, status: TransactionStatus, active: Boolean)

object Transaction {
  implicit val transactionFormat: OFormat[Transaction] = Json.format[Transaction]
}

object TransactionStatus extends Enumeration {
  type TransactionStatus = Int
  val COMPLETED = 0
  val FAILED = 1
}

class TransactionTable(tag: Tag) extends Table[Transaction](tag, "Transactions") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def orderId = column[Long]("order_id")
  def finalPrice = column[Double]("final_price")
  def status = column[TransactionStatus]("status")
  def active = column[Boolean]("active")

  def order = foreignKey("transaction_order_FK", orderId, TableQuery[OrderTable])(_.id)

  def * = (id, orderId, finalPrice, status, active) <> ((Transaction.apply _).tupled, Transaction.unapply)
}