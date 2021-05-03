package controllers

import models.TransactionStatus.TransactionStatus
import models.{TransactionRepository, TransactionStatus}
import play.api.data.Form
import play.api.data.Forms.{longNumber, mapping, number}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
class TransactionsController @Inject()(transactionRepository: TransactionRepository, val controllerComponents: ControllerComponents) extends BaseController {

  val createTransactionForm: Form[CreateTransactionForm] = Form {
    mapping(
      "orderId" -> longNumber
    )(CreateTransactionForm.apply)(CreateTransactionForm.unapply)
  }

  val modifyTransactionForm: Form[ModifyTransactionForm] = Form {
    mapping(
      "status" -> number.verifying("Error.invalidStatus", status => status.equals(TransactionStatus.COMPLETED) ||
        status.equals(TransactionStatus.FAILED))
    )(ModifyTransactionForm.apply)(ModifyTransactionForm.unapply)
  }

  def addTransaction(): Action[AnyContent] = Action.async { implicit request =>
    createTransactionForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      transaction => {
        transactionRepository.add(transaction)
          .flatMap(result =>
            result.map {
              case Success(transaction) => Ok(Json.toJson(transaction))
              case Failure(exception) => BadRequest(Json.toJson(exception.getMessage))
            })
      }
    )
  }

  def getTransaction(id: Long): Action[AnyContent] = Action.async { implicit request =>
    transactionRepository.get(id).map {
      case Some(transaction) => Ok(Json.toJson(transaction))
      case None => Ok("{\"error\":\"not found\"}")
    }
  }

  def getAllTransactions: Action[AnyContent] = Action.async { implicit request =>
    transactionRepository.getAll.map(promotions => Ok(Json.toJson(promotions)))
  }

  def modifyTransaction(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyTransactionForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      transaction => {
        transactionRepository.modify(id, transaction)
          .flatMap(result => result.map {
            case Success(_) => Ok("{\"result\":\"updated\"}")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removeTransaction(id: Long): Action[AnyContent] = Action.async { implicit request =>
    transactionRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreateTransactionForm(orderId: Long)
case class ModifyTransactionForm(status: TransactionStatus)
