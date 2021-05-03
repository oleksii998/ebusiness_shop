package models

import models.BonusCardStatus.BonusCardStatus
import play.api.libs.json.{Json, OFormat}
import slick.jdbc.SQLiteProfile.api._

case class BonusCard(id: Long, customerId: Long, number: String, status: BonusCardStatus, active: Boolean)

object BonusCard {
  implicit val bonusCardFormat: OFormat[BonusCard] = Json.format[BonusCard]
}

object BonusCardStatus extends Enumeration {
  type BonusCardStatus = String
  val ACTIVE = "Active"
  val BLOCKED = "Blocked"
  val CLOSED = "Closed"
}

class BonusCardTable(tag: Tag) extends Table[BonusCard](tag, "BonusCards") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def customerId = column[Long]("customer_id")
  def number = column[String]("number")
  def status = column[BonusCardStatus]("status")
  def active = column[Boolean]("active")

  def customer = foreignKey("bonus_card_customer_FK", customerId, TableQuery[CustomerTable])(_.id)
  def * = (id, customerId, number, status, active) <> ((BonusCard.apply _).tupled, BonusCard.unapply)
}