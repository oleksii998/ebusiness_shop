package models

import controllers.{CreateProductForm, CreatePromotionForm, ModifyProductForm, ModifyPromotionForm}
import models.BonusCardStatus.BonusCardStatus
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{Json, OFormat}
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@Singleton
class PromotionRepository @Inject()(databaseConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  private val databaseConfig = databaseConfigProvider.get[JdbcProfile]
  private val promotions = TableQuery[PromotionTable]
  private val products = TableQuery[ProductTable]

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

  def getAll: Future[Seq[ProductPromotion]] = db.run {
    (for {
      (promotionData, productData) <- promotions.filter(_.active)
        .join(products).on(_.productId === _.id)
    } yield (promotionData, productData)).result.map(entries => {
      var seq = Seq[ProductPromotion]()
      for (entry <- entries) {
        seq :+= ProductPromotion(entry._2, entry._1)
      }
      seq
    })
  }

  def get(id: Long): Future[Option[ProductPromotion]] = db.run {
    (for {
      (promotionData, productData) <- promotions.filter(x => x.id === id && x.active)
        .join(products).on(_.productId === _.id)
    } yield (promotionData, productData)).result.headOption.map {
      case Some(entry) => Option.apply(ProductPromotion(entry._2, entry._1))
      case None => Option.empty
    }
  }

  def remove(id: Long): Future[Future[Try[Int]]] = {
    db.run(promotions.filter(promotion => promotion.id === id && promotion.active).result.headOption).map {
      case Some(promotion) =>
        val replacement = promotion.copy(active = false)
        db.run(promotions.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Promotion not found")))
    }
  }

  def modify(id: Long, modifyPromotionForm: ModifyPromotionForm): Future[Future[Try[Int]]] = {
    db.run(promotions.filter(promotion => promotion.id === id && promotion.active).result.headOption).map {
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
        Future.successful(Failure(new RuntimeException("Promotion not found")))
    }
  }
}

case class ProductPromotion(product: Product, promotion: Promotion)

object ProductPromotion {
  implicit val productPromotionFormat: OFormat[ProductPromotion] = Json.format[ProductPromotion]
}
