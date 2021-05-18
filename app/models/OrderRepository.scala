package models

import controllers.{CreateOrderForm, CreateTransactionForm, ModifyOrderForm}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

@Singleton
class OrderRepository @Inject()(voucherRepository: VoucherRepository, databaseConfigProvider: DatabaseConfigProvider, cartRepository: CartRepository)(implicit executionContext: ExecutionContext) {
  private val databaseConfig = databaseConfigProvider.get[JdbcProfile]
  private val orders = TableQuery[OrderTable]

  import databaseConfig._
  import databaseConfig.profile.api._

  def add(createOrderForm: CreateOrderForm): Future[Future[Try[Order]]] = {
    cartRepository.getAllForCustomer(createOrderForm.customerId).map(cartEntries => {
      val price = cartEntries.map(cartEntry => cartEntry.product.price * cartEntry.quantity).sum
      val promotionsDiscount = cartEntries.map(cartEntry => {
        if (cartEntry.promotion.isDefined && cartEntry.promotion.get.active) {
          var discount = cartEntry.promotion.get.discount
          if (cartEntry.promotion.get.promotionType == PromotionType.PERCENTAGE) {
            discount = cartEntry.product.price * discount
          }
          discount
        } else {
          0d
        }
      }).sum
      var voucherDiscount = 0d
      if (createOrderForm.voucherId.isDefined) {
        voucherDiscount = Await.result(voucherRepository.get(createOrderForm.voucherId.get).map {
          case Some(voucher) =>
            if (voucher.active) {
              var discount = voucher.discount
              if (voucher.voucherType == VoucherType.PERCENTAGE) {
                discount = price * discount
              }
              discount
            } else {
              0d
            }
          case None => 0d
        }, Duration.Inf)
      }
      db.run(((orders returning orders.map(_.id) into (
        (order, id) => order.copy(id = id)
        )) += Order(0, createOrderForm.customerId, price, promotionsDiscount, voucherDiscount, OrderStatus.PLACED, active = true)).asTry).map {
        case Success(order) =>
          cartRepository.addOrderIdForCustomerCart(createOrderForm.customerId, order.id)
          Try.apply(order)
        case Failure(error) => Failure(error)
      }
    })
  }

  def getAll: Future[Seq[Order]] = db.run {
    orders.filter(_.active).result
  }

  def get(id: Long): Future[Option[Order]] = db.run {
    orders.filter(order => order.active && order.id === id).result.headOption
  }

  def getAllForCustomer(customerId: Long): Future[Seq[Order]] = db.run {
    orders.filter(order => order.customerId === customerId && order.active).result
  }

  def remove(id: Long): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(order) =>
        val replacement = order.copy(active = false)
        db.run(orders.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Order not found")))
    }
  }

  def modify(id: Long, modifyOrderForm: ModifyOrderForm): Future[Future[Future[Try[Int]]]] = {
    get(id).map {
      case Some(order) =>
        if (order.status.equals(OrderStatus.DELIVERED)) {
          Future.successful(Future.successful(Failure(new RuntimeException("Cannot modify already delivered order"))))
        } else {
          if (modifyOrderForm.status.equals(OrderStatus.BEING_MODIFIED) || modifyOrderForm.status.equals(OrderStatus.PLACED)) {
            if (modifyOrderForm.status.equals(OrderStatus.BEING_MODIFIED) || order.status.equals(OrderStatus.BEING_MODIFIED)) {
              cartRepository.getAllForCustomer(order.customerId).map(cartEntries => {
                val price = cartEntries.map(cartEntry => cartEntry.product.price * cartEntry.quantity).sum
                val promotionsDiscount = cartEntries.map(cartEntry => {
                  if (cartEntry.promotion.isDefined && cartEntry.promotion.get.active) {
                    var discount = cartEntry.promotion.get.discount
                    if (cartEntry.promotion.get.promotionType == PromotionType.PERCENTAGE) {
                      discount = cartEntry.product.price * discount
                    }
                    discount
                  } else {
                    0d
                  }
                }).sum
                var voucherDiscount = 0d
                if (modifyOrderForm.voucherId.isDefined) {
                  voucherDiscount = Await.result(voucherRepository.get(modifyOrderForm.voucherId.get).map {
                    case Some(voucher) =>
                      if (voucher.active) {
                        var discount = voucher.discount
                        if (voucher.voucherType == VoucherType.PERCENTAGE) {
                          discount = price * discount
                        }
                        discount
                      } else {
                        0d
                      }
                    case None => 0d
                  }, Duration.Inf)
                }
                db.run(orders.filter(_.id === id).update(order.copy(price = price, promotionsDiscount = promotionsDiscount, voucherDiscount = voucherDiscount, status = modifyOrderForm.status)).asTry).map {
                  case Success(result) =>
                    cartRepository.addOrderIdForCustomerCart(order.customerId, order.id)
                    Try.apply(result)
                  case Failure(error) => Failure(error)
                }
              })
            } else if(order.status.equals(OrderStatus.BEING_MODIFIED)) {
              Future.successful(Future.successful(Failure(new RuntimeException("Cannot modify already delivered order"))))
            } else {
              Future.successful(Future.successful(Failure(new RuntimeException("Order is not in modifiable state"))))
            }
          } else if(modifyOrderForm.voucherId.isDefined) {
            Future.successful(Future.successful(Failure(new RuntimeException("Order is not in modifiable state"))))
          } else {
            Future.successful(db.run(orders.filter(_.id === id).update(order.copy(status = modifyOrderForm.status)).asTry))
          }
        }
      case None =>
        Future.successful(Future.successful(Failure(new RuntimeException("Order not found"))))
    }
  }
}