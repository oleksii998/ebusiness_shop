package models

import controllers.{CreateBonusCardForm, ModifyBonusCardForm}
import models.BonusCardStatus.BonusCardStatus
import play.api.db.slick.DatabaseConfigProvider
import play.api.libs.json.{Json, OFormat}
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@Singleton
class BonusCardRepository @Inject()(databaseConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {

  private val databaseConfig = databaseConfigProvider.get[JdbcProfile]
  private val bonusCards = TableQuery[BonusCardTable]
  private val customers = TableQuery[CustomerTable]

  import databaseConfig._
  import databaseConfig.profile.api._

  def add(createBonusCardForm: CreateBonusCardForm): Future[Try[BonusCard]] = db.run {
    ((bonusCards returning bonusCards.map(_.id) into (
      (bonusCard, id) => bonusCard.copy(id = id)
      )) += BonusCard(0, createBonusCardForm.customerId,
      createBonusCardForm.number,
      createBonusCardForm.status,
      active = true)).asTry
  }

  def getAll: Future[Seq[CustomerBonusCard]] = db.run {
    (for {
      (bonusCardData, customerData) <- bonusCards.filter(_.active)
        .join(customers).on(_.customerId === _.id)
    } yield(bonusCardData, customerData)).result.map(entries => {
      var seq = Seq[CustomerBonusCard]()
      for (entry <- entries) {
        seq :+= CustomerBonusCard(entry._1.id, entry._1.number, entry._1.status, entry._2)
      }
      seq
    })
  }

  def getAllForCustomer(customerId: Long): Future[Seq[CustomerBonusCard]] = db.run {
    (for {
      (bonusCardData, customerData) <- bonusCards.filter(bonusCard => bonusCard.customerId === customerId && bonusCard.active)
        .join(customers).on(_.customerId === _.id)
    } yield(bonusCardData, customerData)).result.map(entries => {
      var seq = Seq[CustomerBonusCard]()
      for (entry <- entries) {
        seq :+= CustomerBonusCard(entry._1.id, entry._1.number, entry._1.status, entry._2)
      }
      seq
    })
  }

  def get(id: Long): Future[Option[CustomerBonusCard]] = db.run {
    (for {
      (bonusCardData, customerData) <- bonusCards.filter(bonusCard => bonusCard.id === id && bonusCard.active)
        .join(customers).on(_.customerId === _.id)
    } yield(bonusCardData, customerData)).result.headOption.map {
      case Some(entry) => Option.apply(CustomerBonusCard(entry._1.id, entry._1.number, entry._1.status, entry._2))
      case None => Option.empty
    }
  }

  def remove(id: Long): Future[Future[Try[Int]]] = {
    db.run(bonusCards.filter(bonusCard => bonusCard.id === id && bonusCard.active).result.headOption).map {
      case Some(bonusCard) =>
        val replacement = bonusCard.copy(active = false)
        db.run(bonusCards.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Bonus card not found")))
    }
  }

  def modify(id: Long, modifyBonusCardForm: ModifyBonusCardForm): Future[Future[Try[Int]]] = {
    db.run(bonusCards.filter(bonusCard => bonusCard.id === id && bonusCard.active).result.headOption).map {
      case Some(bonusCard) =>
        val replacement = bonusCard.copy(status = modifyBonusCardForm.status)
        db.run(bonusCards.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Bonus card not found")))
    }
  }
}

case class CustomerBonusCard(id: Long, number: String, status: BonusCardStatus, customer: Customer)

object CustomerBonusCard {
  implicit val customerBonusCardFormat: OFormat[CustomerBonusCard] = Json.format[CustomerBonusCard]
}
