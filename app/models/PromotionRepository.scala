package models

import controllers.{CreateProductForm, CreatePromotionForm, ModifyProductForm, ModifyPromotionForm}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@Singleton
class PromotionRepository @Inject()(databaseConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  private val databaseConfig = databaseConfigProvider.get[JdbcProfile]
  private val promotions = TableQuery[PromotionTable]

  import databaseConfig._
  import databaseConfig.profile.api._

  def add(createPromotionForm: CreatePromotionForm): Future[Try[Promotion]] = db.run {
    ((promotions returning promotions.map(_.id) into (
      (promotion, id) => promotion.copy(id = id)
      )) += Promotion(0, createPromotionForm.productId,
      createPromotionForm.discount,
      createPromotionForm.promotionType,
      active = true)).asTry
  }

  def getAll: Future[Seq[Promotion]] = db.run {
    promotions.filter(_.active).result
  }

  def get(id: Long): Future[Option[Promotion]] = db.run {
    promotions.filter(promotion => promotion.id === id && promotion.active).result.headOption
  }

  def remove(id: Long): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(promotion) =>
        val replacement = promotion.copy(active = false)
        db.run(promotions.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Product not found")))
    }
  }

  def modify(id: Long, modifyPromotionForm: ModifyPromotionForm): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(promotion) =>
        var productId = promotion.productId
        if (modifyPromotionForm.productId.isDefined) {
          productId = modifyPromotionForm.productId.get
        }
        var discount = promotion.discount
        if (modifyPromotionForm.discount.isDefined) {
          discount = modifyPromotionForm.discount.get
        }
        var promotionType = promotion.promotionType
        if (modifyPromotionForm.promotionType.isDefined) {
          promotionType = modifyPromotionForm.promotionType.get
        }
        val replacement = promotion.copy(productId = productId, discount = discount, promotionType = promotionType)
        db.run(promotions.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Product not found")))
    }
  }
}