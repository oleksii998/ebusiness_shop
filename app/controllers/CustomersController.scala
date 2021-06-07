package controllers

import models.{BonusCardRepository, CartRepository, CustomerRepository, OrderRepository, ProductRepository, TransactionRepository}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, BaseController, ControllerComponents, _}

import javax.inject.{Inject, Singleton}
import scala.concurrent._
import scala.util.{Failure, Success}

@Singleton
class CustomersController @Inject()(customerRepository: CustomerRepository,
                                    cartRepository: CartRepository,
                                    bonusCardRepository: BonusCardRepository,
                                    orderRepository: OrderRepository,
                                    transactionRepository: TransactionRepository,
                                    scc: DefaultSilhouetteControllerComponents)(implicit val ec: ExecutionContext) extends SilhouetteController(scc) {

  val createCustomerForm: Form[CreateCustomerForm] = Form {
    mapping(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText,
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText
    )(CreateCustomerForm.apply)(CreateCustomerForm.unapply)
  }

  val modifyCustomerForm: Form[ModifyCustomerForm] = Form {
    mapping(
      "email" -> optional(nonEmptyText),
      "password" -> optional(nonEmptyText),
      "firstName" -> optional(nonEmptyText),
      "lastName" -> optional(nonEmptyText)
    )(ModifyCustomerForm.apply)(ModifyCustomerForm.unapply)
  }

  def addCustomer(): Action[AnyContent] = Action.async { implicit request =>
    createCustomerForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      customer => {
        customerRepository.add(customer)
          .map {
            case Success(customer) => Ok(Json.toJson(customer))
            case Failure(exception) => BadRequest(Json.toJson(exception.getMessage))
          }
      }
    )
  }

  def getCustomer(id: Long): Action[AnyContent] = securedAction.async {
    customerRepository.get(id).map {
      case Some(customer) => Ok(Json.toJson(customer))
      case None => NotFound("{\"message\":\"Customer not found\"}")
    }
  }

  def getCustomerCart(id: Long): Action[AnyContent] = securedAction.async {
    cartRepository.getAllForCustomer(id).map(carts => Ok(Json.toJson(carts)))
  }

  def getCustomerBonusCards(id: Long): Action[AnyContent] = securedAction.async {
    bonusCardRepository.getAllForCustomer(id).map(bonusCards => Ok(Json.toJson(bonusCards)))
  }

  def getCustomerOrders(id: Long): Action[AnyContent] = securedAction.async {
    orderRepository.getAllForCustomer(id).map(orders => Ok(Json.toJson(orders)))
  }

  def getCustomerTransactions(id: Long): Action[AnyContent] = securedAction.async {
    transactionRepository.getAllForCustomer(id).map(transactions => Ok(Json.toJson(transactions)))
  }

  def getAllCustomers: Action[AnyContent] = securedAction.async {
    customerRepository.getAll.map(customers => Ok(Json.toJson(customers)))
  }

  def modifyCustomer(id: Long): Action[AnyContent] = securedAction.async { implicit request =>
    modifyCustomerForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      customer => {
        customerRepository.modify(id, customer)
          .flatMap(result => result.map {
            case Success(_) => Ok("{\"result\":\"updated\"}")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removeCustomer(id: Long): Action[AnyContent] = securedAction.async {
    customerRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }

  //VIEWS

  def addCustomerView(): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.customers.customerAdd(createCustomerForm))
  }

  def addCustomerViewResponse(): Action[AnyContent] = Action.async { implicit request =>
    createCustomerForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      customer => {
        customerRepository.add(customer)
          .map {
            case Success(customer) => Redirect(routes.CustomersController.addCustomerView()).flashing("success" -> "Customer added")
            case Failure(exception) => BadRequest(exception.getMessage)
          }
      }
    )
  }

  def getCustomerView(id: Long): Action[AnyContent] = Action.async {
    customerRepository.get(id).map {
      case Some(customer) => Ok(views.html.customers.customer(customer))
      case None => BadRequest("Customer not found")
    }
  }

  def getCustomerCartView(id: Long): Action[AnyContent] = Action.async {
    cartRepository.getAllForCustomer(id).map(carts => Ok(views.html.customers.customerCart(carts)))
  }

  def getCustomerBonusCardsView(id: Long): Action[AnyContent] = Action.async {
    bonusCardRepository.getAllForCustomer(id).map(bonusCards => Ok(views.html.customers.customerBonusCards(bonusCards)))
  }

  def getCustomerOrdersView(id: Long): Action[AnyContent] = Action.async {
    orderRepository.getAllForCustomer(id).map(orders => Ok(views.html.customers.customerOrders(orders)))
  }

  def getCustomerTransactionsView(id: Long): Action[AnyContent] = Action.async {
    transactionRepository.getAllForCustomer(id).map(transactions => Ok(views.html.customers.customerTransactions(transactions)))
  }

  def getAllCustomersView: Action[AnyContent] = Action.async {
    customerRepository.getAll.map( customers => Ok(views.html.customers.customers(customers)))
  }

  def modifyCustomerView(id: Long): Action[AnyContent] = Action.async { implicit request =>
    customerRepository.get(id).map {
      case Some(customer) =>
        val filled = modifyCustomerForm.fill(ModifyCustomerForm(Option.apply(customer.email),
          Option.apply(customer.password),
            Option.apply(customer.firstName),
              Option.apply(customer.lastName)))
        Ok(views.html.customers.customerModify(filled, customer.id))
      case None => BadRequest("Customer not found")
    }
  }

  def modifyCustomerViewResponse(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyCustomerForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      customer => {
        customerRepository.modify(id, customer)
          .flatMap(result => result.map {
            case Success(_) => Redirect(routes.CustomersController.modifyCustomerView(id)).flashing("success" -> "Customer updated")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removeCustomerView(id: Long): Action[AnyContent] = Action.async {
    customerRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Redirect("/customersView")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreateCustomerForm(email: String, password: String, firstName: String, lastName: String)
case class ModifyCustomerForm(email: Option[String], password: Option[String], firstName: Option[String], lastName: Option[String])
