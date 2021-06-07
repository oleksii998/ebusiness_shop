package controllers

import models.OrderStatus.OrderStatus
import models.{CustomerRepository, OrderRepository, OrderStatus, TransactionRepository, VoucherRepository}
import play.api.data.Form
import play.api.data.Forms.{longNumber, mapping, nonEmptyText, optional}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesAbstractController, MessagesControllerComponents, MessagesRequest}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class OrdersController @Inject()(orderRepository: OrderRepository,
                                 customerRepository: CustomerRepository,
                                 voucherRepository: VoucherRepository,
                                 transactionRepository: TransactionRepository,
                                 scc: DefaultSilhouetteControllerComponents)(implicit val ec: ExecutionContext) extends SilhouetteController(scc) {

  val createOrderForm: Form[CreateOrderForm] = Form {
    mapping(
      "customerId" -> longNumber,
      "voucherId" -> optional(longNumber)
    )(CreateOrderForm.apply)(CreateOrderForm.unapply)
  }

  val modifyOrderForm: Form[ModifyOrderForm] = Form {
    mapping(
      "status" -> nonEmptyText.verifying("Error.invalidStatus", status =>
        status.equals(OrderStatus.PLACED) ||
          status.equals(OrderStatus.BEING_MODIFIED) ||
          status.equals(OrderStatus.DELIVERED)),
      "voucherId" -> optional(longNumber)
    )(ModifyOrderForm.apply)(ModifyOrderForm.unapply)
  }

  def addOrder(): Action[AnyContent] = securedAction.async { implicit request =>
    createOrderForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      order => {
        orderRepository.add(order)
          .flatMap(result => result.map {
            case Success(order) => Ok(Json.toJson(order))
            case Failure(exception) => BadRequest(Json.toJson(exception.getMessage))
          })
      }
    )
  }

  def getOrder(id: Long): Action[AnyContent] = securedAction.async {
    orderRepository.get(id).map {
      case Some(product) => Ok(Json.toJson(product))
      case None => NotFound("{\"error\":\"Order not found\"}")
    }
  }

  def getAllOrders: Action[AnyContent] = securedAction.async {
    orderRepository.getAll.map(orders => Ok(Json.toJson(orders)))
  }

  def modifyOrder(id: Long): Action[AnyContent] = securedAction.async { implicit request =>
    modifyOrderForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      order => {
        orderRepository.modify(id, order)
          .flatMap(result =>
            result.flatMap(result2 =>
              result2.map {
                case Success(_) => Ok("{\"result\":\"updated\"}")
                case Failure(exception) => BadRequest(exception.getMessage)
              })
          )
      }
    )
  }

  def removeOrder(id: Long): Action[AnyContent] = securedAction.async {
    orderRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }

  //VIEWS

  def addOrderView(): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
    customerRepository.getAll.map(customers =>
      Await.result(voucherRepository.getAll.map(vouchers =>
        Ok(views.html.orders.orderAdd(createOrderForm, customers, vouchers))
      ), Duration.Inf))
  }

  def addOrderViewResponse(): Action[AnyContent] = Action.async { implicit request =>
    createOrderForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      order => {
        orderRepository.add(order)
          .flatMap(result => result.map {
            case Success(_) => Redirect(routes.OrdersController.addOrderView()).flashing("success" -> "Order added")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def getOrderView(id: Long): Action[AnyContent] = Action.async {
    orderRepository.get(id).map {
      case Some(user) => Ok(views.html.orders.order(user))
      case None => BadRequest("Order not found")
    }
  }

  def getAllOrdersView: Action[AnyContent] = Action.async {
    orderRepository.getAll.map(orders => Ok(views.html.orders.orders(orders)))
  }

  def modifyOrderView(id: Long): Action[AnyContent] = Action.async { implicit request =>
    orderRepository.get(id).map {
      case Some(order) =>
        val filled = modifyOrderForm.fill(ModifyOrderForm(order.status,
          Option.empty))
        val vouchers = Await.result(voucherRepository.getAll, Duration.Inf)
        Ok(views.html.orders.orderModify(filled, order.id, vouchers))
      case None => BadRequest("Order not found")
    }
  }

  def modifyOrderViewResponse(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyOrderForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      order => {
        orderRepository.modify(id, order)
          .flatMap(result => result.flatMap(result2 => result2.map {
            case Success(_) => Redirect(routes.OrdersController.modifyOrderView(id)).flashing("success" -> "Order updated")
            case Failure(exception) => BadRequest(exception.getMessage)
          }))
      }
    )
  }

  def removeOrderView(id: Long): Action[AnyContent] = Action.async {
    orderRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Redirect("/ordersView")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreateOrderForm(customerId: Long, voucherId: Option[Long])

case class ModifyOrderForm(status: OrderStatus, voucherId: Option[Long])