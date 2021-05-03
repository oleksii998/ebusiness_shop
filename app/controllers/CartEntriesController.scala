package controllers

import models.CartRepository
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, BaseController, ControllerComponents, _}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
class CartEntriesController @Inject()(val controllerComponents: ControllerComponents, cartRepository: CartRepository) extends BaseController {

  val createCartForm: Form[CreateCartEntryForm] = Form {
    mapping(
      "customerId" -> longNumber,
      "productId" -> longNumber,
      "quantity" -> number
    )(CreateCartEntryForm.apply)(CreateCartEntryForm.unapply)
  }

  val modifyCartForm: Form[ModifyCartEntryForm] = Form {
    mapping(
      "quantity" -> number
    )(ModifyCartEntryForm.apply)(ModifyCartEntryForm.unapply)
  }

  def addCartEntry(): Action[AnyContent] = Action.async { implicit request =>
    createCartForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      cart => {
        cartRepository.add(cart)
          .map {
            case Success(cart) => Ok(Json.toJson(cart))
            case Failure(exception) => BadRequest(Json.toJson(exception.getMessage))
          }
      }
    )
  }

  def getCartEntry(id: Long): Action[AnyContent] = Action.async { implicit request =>
    cartRepository.get(id).map {
      case Some(product) => Ok(Json.toJson(product))
      case None => Ok("{\"error\":\"not found\"}")
    }
  }

  def modifyCartEntry(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyCartForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      cart => {
        cartRepository.modify(id, cart)
          .flatMap(result => result.map {
            case Success(_) => Ok("{\"result\":\"updated\"}")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removeCartEntry(id: Long): Action[AnyContent] = Action.async {
    cartRepository.remove(id).map {
      case Success(_) => Ok("{\"result\":\"deleted\"}")
      case Failure(exception) => BadRequest(exception.getMessage)
    }
  }
}
case class CreateCartEntryForm(userId: Long, productId: Long, quantity: Int)
case class ModifyCartEntryForm(quantity: Int)
