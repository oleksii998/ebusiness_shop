package models

import controllers.{CreateCartEntryForm, ModifyCartEntryForm}
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{Json, OFormat}
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@Singleton
class CartRepository @Inject()(databaseConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  private val databaseConfig = databaseConfigProvider.get[JdbcProfile]
  private val cart = TableQuery[CartTable]
  private val products = TableQuery[ProductTable]
  private val categories = TableQuery[CategoryTable]
  private val orders = TableQuery[OrderTable]
  private val promotions = TableQuery[PromotionTable]
  private val customers = TableQuery[CustomerTable]

  import databaseConfig._
  import databaseConfig.profile.api._

  def add(createCartEntryForm: CreateCartEntryForm): Future[Try[CartEntry]] = db.run {
    ((cart returning cart.map(_.id) into (
      (cartEntry, id) => cartEntry.copy(id = id)
      )) += CartEntry(0, createCartEntryForm.customerId,
      createCartEntryForm.productId, createCartEntryForm.quantity, Option.empty[Long])).asTry
  }

  def getAll: Future[Seq[CustomerCartEntry]] = db.run {
    (for {
      (((((cartEntryData, _), productData), promotionData), categoryData), customerData) <- cart
        .joinLeft(orders).on(_.orderId === _.id)
        .filter(result => result._2.isEmpty || result._2.map(_.status) === OrderStatus.BEING_MODIFIED)
        .join(products).on(_._1.productId === _.id)
        .joinLeft(promotions).on(_._2.id === _.productId)
        .join(categories).on(_._1._2.categoryId === _.id)
        .join(customers).on(_._1._1._1._1.customerId === _.id)
    } yield (cartEntryData.id, customerData, productData, promotionData, categoryData.name, cartEntryData.quantity)).result.map(cartEntries => {
      var seq = Seq[CustomerCartEntry]()
      for (cartEntry <- cartEntries) {
        seq :+= CustomerCartEntry(cartEntry._1, Option.apply(cartEntry._2), cartEntry._3, cartEntry._4, cartEntry._5, cartEntry._6)
      }
      seq
    })
  }

  def get(id: Long): Future[Option[CustomerCartEntry]] = db.run {
    (for {
      (((((cartEntryData, _), productData), promotionData), categoryData), customerData) <- cart.filter(_.id === id)
        .joinLeft(orders).on(_.orderId === _.id)
        .filter(result => result._2.isEmpty || result._2.map(_.status) === OrderStatus.BEING_MODIFIED)
        .join(products).on(_._1.productId === _.id)
        .joinLeft(promotions).on(_._2.id === _.productId)
        .join(categories).on(_._1._2.categoryId === _.id)
        .join(customers).on(_._1._1._1._1.customerId === _.id)
    } yield (cartEntryData.id, customerData, productData, promotionData, categoryData.name, cartEntryData.quantity)).result.headOption.map {
      case Some(cartEntry) => Option.apply(CustomerCartEntry(cartEntry._1, Option.apply(cartEntry._2), cartEntry._3, cartEntry._4, cartEntry._5, cartEntry._6))
      case None => Option.empty
    }
  }

  def getAllForCustomer(customerId: Long): Future[Seq[CustomerCartEntry]] = db.run {
    (for {
      ((((cartEntryData, _), productData), promotionData), categoryData) <- cart.filter(cartEntry => cartEntry.customerId === customerId)
        .joinLeft(orders).on(_.orderId === _.id)
        .filter(result => result._2.isEmpty || result._2.map(_.status) === OrderStatus.BEING_MODIFIED)
        .join(products).on(_._1.productId === _.id)
        .joinLeft(promotions.filter(_.active)).on(_._2.id === _.productId)
        .join(categories).on(_._1._2.categoryId === _.id)
    } yield (cartEntryData.id, productData, promotionData, categoryData.name, cartEntryData.quantity)).result.map(cartEntries => {
      var seq = Seq[CustomerCartEntry]()
      for (cartEntry <- cartEntries) {
        seq :+= CustomerCartEntry(cartEntry._1, Option.empty, cartEntry._2, cartEntry._3, cartEntry._4, cartEntry._5)
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
    db.run(cart.filter(cartEntry => cartEntry.id === id).result.headOption).map {
      case Some(cartEntry) =>
        val replacement = cartEntry.copy(quantity = modifyCartEntryForm.quantity)
        db.run(cart.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Cart not found")))
    }
  }
}

case class CustomerCartEntry(id: Long, customer: Option[Customer], product: Product, promotion: Option[Promotion], categoryName: String, quantity: Int)

object CustomerCartEntry {
  implicit val customerCartEntryFormat: OFormat[CustomerCartEntry] = Json.format[CustomerCartEntry]
}
