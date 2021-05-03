package models

import controllers.{CreateCartEntryForm, ModifyCartEntryForm}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{Json, OFormat}
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Try}

@Singleton
class CartRepository @Inject()(databaseConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  private val databaseConfig = databaseConfigProvider.get[JdbcProfile]
  private val cart = TableQuery[CartTable]
  private val products = TableQuery[ProductTable]
  private val categories = TableQuery[CategoryTable]
  private val orders = TableQuery[OrderTable]
  private val promotions = TableQuery[PromotionTable]

  import databaseConfig._
  import databaseConfig.profile.api._

  def add(createCartEntryForm: CreateCartEntryForm): Future[Try[CartEntry]] = db.run {
    ((cart returning cart.map(_.id) into (
      (cartEntry, id) => cartEntry.copy(id = id)
      )) += CartEntry(0, createCartEntryForm.userId,
      createCartEntryForm.productId, createCartEntryForm.quantity, Option.empty[Long])).asTry
  }

  def getAll: Future[Seq[CartEntry]] = db.run {
    cart.filter(_.orderId.column.isEmpty).result
  }

  def get(id: Long): Future[Option[CartEntry]] = db.run {
    cart.filter(cartEntry => cartEntry.id === id).result.headOption
  }

  def getAllForCustomer(customerId: Long): Future[Seq[CustomerCartEntry]] = db.run {
    (for {
      ((((cartEntryData, _), productData), promotionData), categoryData) <- cart.filter(cartEntry => cartEntry.customerId === customerId)
        .joinLeft(orders).on(_.orderId === _.id)
        .filter(result => result._2.isEmpty || result._2.map(_.status) === OrderStatus.BEING_MODIFIED)
        .join(products).on(_._1.productId === _.id)
        .joinLeft(promotions).on(_._2.id === _.productId)
        .join(categories).on(_._1._2.categoryId === _.id)
    } yield (cartEntryData.id, productData, promotionData, categoryData.name, cartEntryData.quantity)).result.map(cartEntries => {
      var seq = Seq[CustomerCartEntry]()
      for (cartEntry <- cartEntries) {
        seq :+= CustomerCartEntry(cartEntry._1, cartEntry._2, cartEntry._3, cartEntry._4, cartEntry._5)
      }
      seq
    })
  }

  def addOrderIdForCustomerCart(customerId: Long, orderId: Long): Future[Try[Int]] = {
    db.run(cart.filter(_.customerId === customerId).map(cartEntry => cartEntry.orderId).update(Option.apply(orderId)).transactionally.asTry)
  }

  def remove(id: Long): Future[Try[Int]] = db.run(
    cart.filter(cart => cart.id === id).delete.asTry)

  def modify(id: Long, modifyCartEntryForm: ModifyCartEntryForm): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(cartEntry) =>
        val replacement = cartEntry.copy(quantity = modifyCartEntryForm.quantity)
        db.run(cart.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Cart not found")))
    }
  }
}

case class CustomerCartEntry(id: Long, product: Product, promotion: Option[Promotion], categoryName: String, quantity: Int)

object CustomerCartEntry {
  implicit val customerCartEntryFormat: OFormat[CustomerCartEntry] = Json.format[CustomerCartEntry]
}