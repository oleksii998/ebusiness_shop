package controllers

import models.TransactionStatus.TransactionStatus
import models.{TransactionRepository, TransactionStatus}
import play.api.data.Form
import play.api.data.Forms.{longNumber, mapping, number}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents, MessagesAbstractController, MessagesControllerComponents, MessagesRequest}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class TransactionsController @Inject()(transactionRepository: TransactionRepository, scc: DefaultSilhouetteControllerComponents)(implicit val ec: ExecutionContext) extends SilhouetteController(scc) {

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

  def addTransaction(): Action[AnyContent] = securedAction.async { implicit request =>
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

  def getTransaction(id: Long): Action[AnyContent] = securedAction.async { implicit request =>
    transactionRepository.get(id).map {
      case Some(transaction) => Ok(Json.toJson(transaction))
      case None => NotFound("{\"error\":\"Transaction not found\"}")
    }
  }

  def getAllTransactions: Action[AnyContent] = securedAction.async { implicit request =>
    transactionRepository.getAll.map(promotions => Ok(Json.toJson(promotions)))
  }

  def modifyTransaction(id: Long): Action[AnyContent] = securedAction.async { implicit request =>
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

  def removeTransaction(id: Long): Action[AnyContent] = securedAction.async { implicit request =>
    transactionRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }

  //VIEWS

  def addTransactionView(): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.transactions.transactionAdd(createTransactionForm))
  }

  def addTransactionViewResponse(): Action[AnyContent] = Action.async { implicit request =>
    createTransactionForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      transaction => {
        transactionRepository.add(transaction)
          .flatMap(result => result.map {
            case Success(_) => Redirect(routes.TransactionsController.addTransactionView()).flashing("success" -> "Transaction added")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def getTransactionView(id: Long): Action[AnyContent] = Action.async {
    transactionRepository.get(id).map {
      case Some(transaction) => Ok(views.html.transactions.transaction(transaction))
      case None => BadRequest("Transaction not found")
    }
  }

  def getAllTransactionsView: Action[AnyContent] = Action.async {
    transactionRepository.getAll.map(transactions => Ok(views.html.transactions.transactions(transactions)))
  }

  def modifyTransactionView(id: Long): Action[AnyContent] = Action.async { implicit request =>
    transactionRepository.get(id).map {
      case Some(transaction) =>
        val filled = modifyTransactionForm.fill(ModifyTransactionForm(transaction.status))
        Ok(views.html.transactions.transactionModify(filled, transaction.id))
      case None => BadRequest("Transaction not found")
    }
  }

  def modifyTransactionViewResponse(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyTransactionForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      transaction => {
        transactionRepository.modify(id, transaction)
          .flatMap(result => result.map {
            case Success(_) => Redirect(routes.TransactionsController.modifyTransactionView(id)).flashing("success" -> "Transaction updated")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removeTransactionView(id: Long): Action[AnyContent] = Action.async {
    transactionRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Redirect("/transactionsView")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreateTransactionForm(orderId: Long)
case class ModifyTransactionForm(status: TransactionStatus)
