package models

import models.PromotionType.PromotionType
import play.api.libs.json.{Json, OFormat}
import slick.jdbc.SQLiteProfile.api._

case class Promotion(id: Long, productId: Long, discount: Double, promotionType: PromotionType, active: Boolean)

object Promotion {
  implicit val promotionFormat: OFormat[Promotion] = Json.format[Promotion]
}

object PromotionType extends Enumeration {
  type PromotionType = Int
  val PERCENTAGE = 0
  val CONSTANT = 1
}

class PromotionTable(tag: Tag) extends Table[Promotion](tag, "Promotions") {
  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
  def productId = column[Long]("product_id")
  def discount = column[Double]("discount")
  def promotionType = column[PromotionType]("type")
  def active = column[Boolean]("active")

  def product = foreignKey("promotion_product_FK", productId, TableQuery[ProductTable])(_.id)

  def * = (id, productId, discount, promotionType, active) <> ((Promotion.apply _).tupled, Promotion.unapply)
}