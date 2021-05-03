package controllers

import models.{OrderRepository, OrderStatus}
import models.OrderStatus.OrderStatus
import play.api.data.Form
import play.api.data.Forms.{longNumber, mapping, nonEmptyText, optional}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
class OrdersController @Inject()(orderRepository: OrderRepository, val controllerComponents: ControllerComponents) extends BaseController {

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

  def addOrder(): Action[AnyContent] = Action.async { implicit request =>
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

  def getOrder(id: Long): Action[AnyContent] = Action.async {
    orderRepository.get(id).map {
      case Some(product) => Ok(Json.toJson(product))
      case None => Ok("{\"error\":\"not found\"}")
    }
  }

  def getAllOrders: Action[AnyContent] = Action.async {
    orderRepository.getAll.map(orders => Ok(Json.toJson(orders)))
  }

  def modifyOrder(id: Long): Action[AnyContent] = Action.async { implicit request =>
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

  def removeOrder(id: Long): Action[AnyContent] = Action.async {
    orderRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreateOrderForm(customerId: Long, voucherId: Option[Long])
case class ModifyOrderForm(status: OrderStatus, voucherId: Option[Long])