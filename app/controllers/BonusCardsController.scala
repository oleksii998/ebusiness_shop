package controllers

import models.BonusCardStatus.BonusCardStatus
import models.{BonusCardRepository, BonusCardStatus, CustomerRepository}
import play.api.data.Form
import play.api.data.Forms.{longNumber, mapping, nonEmptyText}
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class BonusCardsController @Inject()(bonusCardRepository: BonusCardRepository,
                                     customerRepository: CustomerRepository,
                                     scc: DefaultSilhouetteControllerComponents)(implicit val ec: ExecutionContext) extends SilhouetteController(scc) {
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

  def addBonusCard(): Action[AnyContent] = securedAction.async { implicit request =>
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

  def getBonusCard(id: Long): Action[AnyContent] = securedAction.async {
    bonusCardRepository.get(id).map {
      case Some(product) => Ok(Json.toJson(product))
      case None => NotFound("{\"error\":\"Bonus card not found\"}")
    }
  }

  def getAllBonusCard: Action[AnyContent] = securedAction.async {
    bonusCardRepository.getAll.map(products => Ok(Json.toJson(products)))
  }

  def modifyBonusCard(id: Long): Action[AnyContent] = securedAction.async { implicit request =>
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

  def removeBonusCard(id: Long): Action[AnyContent] = securedAction.async {
    bonusCardRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }

  //VIEWS

  def addBonusCardView(): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
    customerRepository.getAll.map(customers => {
      Ok(views.html.bonusCards.bonusCardAdd(createBonusCardForm, customers))
    })
  }

  def addBonusCardViewResponse(): Action[AnyContent] = Action.async { implicit request =>
    createBonusCardForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      bonusCard => {
        bonusCardRepository.add(bonusCard)
          .map {
            case Success(_) => Redirect(routes.BonusCardsController.addBonusCardView()).flashing("success" -> "Bonus card added")
            case Failure(exception) => BadRequest(exception.getMessage)
          }
      }
    )
  }

  def getBonusCardView(id: Long): Action[AnyContent] = Action.async {
    bonusCardRepository.get(id).map {
      case Some(bonusCard) => Ok(views.html.bonusCards.bonusCard(bonusCard))
      case None => BadRequest("Bonus card not found")
    }
  }

  def getAllBonusCardsView: Action[AnyContent] = Action.async {
    bonusCardRepository.getAll.map(bonusCards => Ok(views.html.bonusCards.bonusCards(bonusCards)))
  }

  def modifyBonusCardView(id: Long): Action[AnyContent] = Action.async { implicit request =>
    bonusCardRepository.get(id).map {
      case Some(bonusCard) =>
        val filled = modifyBonusCardForm.fill(ModifyBonusCardForm(bonusCard.status))
        Ok(views.html.bonusCards.bonusCardModify(filled, bonusCard.id))
      case None => BadRequest("Bonus card not found")
    }
  }

  def modifyBonusCardViewResponse(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyBonusCardForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      bonusCard => {
        bonusCardRepository.modify(id, bonusCard)
          .flatMap(result => result.map {
            case Success(_) => Redirect(routes.BonusCardsController.modifyBonusCardView(id)).flashing("success" -> "Bonus card updated")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removeBonusCardView(id: Long): Action[AnyContent] = Action.async {
    bonusCardRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Redirect("/bonusCardsView")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreateBonusCardForm(customerId: Long, number: String, status: BonusCardStatus)
case class ModifyBonusCardForm(status: BonusCardStatus)