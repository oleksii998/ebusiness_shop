package models

import controllers.{CreateBonusCardForm, ModifyBonusCardForm}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@Singleton
class BonusCardRepository @Inject()(databaseConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  private val databaseConfig = databaseConfigProvider.get[JdbcProfile]
  private val bonusCards = TableQuery[BonusCardTable]

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

  def getAll: Future[Seq[BonusCard]] = db.run {
    bonusCards.filter(_.active).result
  }

  def getAllForCustomer(customerId: Long): Future[Seq[BonusCard]] = db.run {
    bonusCards.filter(bonusCard => bonusCard.customerId === customerId && bonusCard.active).result
  }

  def get(id: Long): Future[Option[BonusCard]] = db.run {
    bonusCards.filter(bonusCard => bonusCard.id === id && bonusCard.active).result.headOption
  }

  def remove(id: Long): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(bonusCard) =>
        val replacement = bonusCard.copy(active = false)
        db.run(bonusCards.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Bonus card not found")))
    }
  }

  def modify(id: Long, modifyBonusCardForm: ModifyBonusCardForm): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(bonusCard) =>
        val replacement = bonusCard.copy(status = modifyBonusCardForm.status)
        db.run(bonusCards.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Bonus card not found")))
    }
  }
}