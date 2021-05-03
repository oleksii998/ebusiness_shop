package controllers

import models.BonusCardStatus.BonusCardStatus
import models.{BonusCardRepository, BonusCardStatus}
import play.api.data.Form
import play.api.data.Forms.{longNumber, mapping, nonEmptyText, optional}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
class BonusCardsController @Inject()(bonusCardRepository: BonusCardRepository, val controllerComponents: ControllerComponents) extends BaseController {
  val createBonusCardForm: Form[CreateBonusCardForm] = Form {
    mapping(
      "customerId" -> longNumber,
      "number" -> nonEmptyText(12, 12),
      "status" -> nonEmptyText.verifying("Error.invalidStatus", status =>
        status.equals(BonusCardStatus.ACTIVE) ||
        status.equals(BonusCardStatus.BLOCKED) ||
        status.equals(BonusCardStatus.CLOSED))
    )(CreateBonusCardForm.apply)(CreateBonusCardForm.unapply)
  }

  val modifyBonusCardForm: Form[ModifyBonusCardForm] = Form {
    mapping(
      "status" -> nonEmptyText.verifying("Error.invalidStatus", status =>
        status.equals(BonusCardStatus.ACTIVE) ||
          status.equals(BonusCardStatus.BLOCKED) ||
          status.equals(BonusCardStatus.CLOSED))
    )(ModifyBonusCardForm.apply)(ModifyBonusCardForm.unapply)
  }

  def addBonusCard(): Action[AnyContent] = Action.async { implicit request =>
    createBonusCardForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      bonusCard => {
        bonusCardRepository.add(bonusCard)
          .map {
            case Success(bonusCard) => Ok(Json.toJson(bonusCard))
            case Failure(exception) => BadRequest(Json.toJson(exception.getMessage))
          }
      }
    )
  }

  def getBonusCard(id: Long): Action[AnyContent] = Action.async {
    bonusCardRepository.get(id).map {
      case Some(product) => Ok(Json.toJson(product))
      case None => Ok("{\"error\":\"not found\"}")
    }
  }

  def getAllBonusCard: Action[AnyContent] = Action.async {
    bonusCardRepository.getAll.map(products => Ok(Json.toJson(products)))
  }

  def modifyBonusCard(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyBonusCardForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      bonusCard => {
        bonusCardRepository.modify(id, bonusCard)
          .flatMap(result => result.map {
            case Success(_) => Ok("{\"result\":\"updated\"}")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removeBonusCard(id: Long): Action[AnyContent] = Action.async {
    bonusCardRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreateBonusCardForm(customerId: Long, number: String, status: BonusCardStatus)
case class ModifyBonusCardForm(status: BonusCardStatus)