package models

import models.VoucherType.VoucherType
import play.api.libs.json.{Json, OFormat}
import slick.jdbc.SQLiteProfile.api._

case class Voucher(id: Long, name: String, discount: Double, voucherType: VoucherType, active: Boolean)

object Voucher {
  implicit val voucherFormat: OFormat[Voucher] = Json.format[Voucher]
}

object VoucherType extends Enumeration {
  type VoucherType = Int
  val PERCENTAGE = 0
  val CONSTANT = 1
}

class VoucherTable(tag: Tag) extends Table[Voucher](tag, "Vouchers") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def name = column[String]("name")
  def discount = column[Double]("discount")
  def promotionType = column[VoucherType]("type")
  def active = column[Boolean]("active")
  def * = (id, name, discount, promotionType, active) <> ((Voucher.apply _).tupled, Voucher.unapply)
}