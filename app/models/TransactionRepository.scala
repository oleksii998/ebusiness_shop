package models

import controllers.{CreateTransactionForm, ModifyTransactionForm}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import javax.inject.{Inject, Singleton}
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Try}

@Singleton
class TransactionRepository @Inject()(orderRepository: OrderRepository, databaseConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  private val databaseConfig = databaseConfigProvider.get[JdbcProfile]
  private val transactions = TableQuery[TransactionTable]
  private val orders = TableQuery[OrderTable]

  import databaseConfig._
  import databaseConfig.profile.api._

  def add(createTransactionForm: CreateTransactionForm): Future[Future[Try[Transaction]]] = {
    orderRepository.get(createTransactionForm.orderId).map {
      case Some(order) =>
        val finalPrice = order.price - order.promotionsDiscount - order.voucherDiscount
        db.run(((transactions returning transactions.map(_.id) into (
          (transaction, id) => transaction.copy(id = id)
          )) += Transaction(0, createTransactionForm.orderId,
          finalPrice,
          TransactionStatus.COMPLETED,
          active = true)).asTry)
      case None => Future.successful(Failure(new RuntimeException("Could not find order " + createTransactionForm.orderId)))
    }
  }

  def getAll: Future[Seq[Transaction]] = db.run {
    transactions.filter(_.active).result
  }

  def get(id: Long): Future[Option[Transaction]] = db.run {
    transactions.filter(transaction => transaction.id === id && transaction.active).result.headOption
  }

  def getAllForCustomer(customerId: Long): Future[Seq[Transaction]] = {
    db.run((for {
      (transactionData, _) <- transactions.join(orders).on(_.orderId === _.id)
        .filter(result => result._1.active && result._2.customerId === customerId)
    } yield transactionData).result)
  }

  def remove(id: Long): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(transaction) =>
        val replacement = transaction.copy(active = false)
        db.run(transactions.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Transaction not found")))
    }
  }

  def modify(id: Long, modifyTransactionForm: ModifyTransactionForm): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(transaction) =>
        val replacement = transaction.copy(status = modifyTransactionForm.status)
        db.run(transactions.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Transaction not found")))
    }
  }
}
