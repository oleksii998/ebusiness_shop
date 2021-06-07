package controllers

import models.{CartRepository, CustomerRepository, ProductRepository}
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, _}

import javax.inject.{Inject, Singleton}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class CartEntriesController @Inject()(cartRepository: CartRepository,
                                      customerRepository: CustomerRepository,
                                      productRepository: ProductRepository,
                                      scc: DefaultSilhouetteControllerComponents)(implicit val ec: ExecutionContext) extends SilhouetteController(scc) {

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

  def addCartEntry(): Action[AnyContent] = securedAction.async { implicit request =>
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

  def getAllCartEntries: Action[AnyContent] = securedAction.async { implicit request =>
    cartRepository.getAll.map(cart => Ok(Json.toJson(cart)))
  }

  def getCartEntry(id: Long): Action[AnyContent] = securedAction.async { implicit request =>
    cartRepository.get(id).map {
      case Some(cart) => Ok(Json.toJson(cart))
      case None => NotFound("{\"message\":\"Cart entry not found\"}")
    }
  }

  def modifyCartEntry(id: Long): Action[AnyContent] = securedAction.async { implicit request =>
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

  def removeCartEntry(id: Long): Action[AnyContent] = securedAction.async {
    cartRepository.remove(id).map {
      case Success(_) => Ok("{\"result\":\"deleted\"}")
      case Failure(exception) => BadRequest(exception.getMessage)
    }
  }

  //VIEWS

  def addCartEntryView(): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
    customerRepository.getAll.map(customers =>
      Await.result(productRepository.getAll.map(products =>
        Ok(views.html.cart.cartEntryAdd(createCartForm, customers, products.map(_.product)))
      ), Duration.Inf))
  }

  def addCartEntryViewResponse(): Action[AnyContent] = Action.async { implicit request =>
    createCartForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      cartEntry => {
        cartRepository.add(cartEntry)
          .map {
            case Success(_) => Redirect(routes.CartEntriesController.addCartEntryView()).flashing("success" -> "Cart entry added")
            case Failure(exception) => BadRequest(exception.getMessage)
          }
      }
    )
  }

  def getCartEntryView(id: Long): Action[AnyContent] = Action.async {
    cartRepository.get(id).map {
      case Some(cartEntry) => Ok(views.html.cart.cartEntry(cartEntry))
      case None => BadRequest("Bonus card not found")
    }
  }

  def getAllCartEntriesView: Action[AnyContent] = Action.async {
    cartRepository.getAll.map(cartEntries => Ok(views.html.cart.cartEntries(cartEntries)))
  }

  def modifyCartEntryView(id: Long): Action[AnyContent] = Action.async { implicit request =>
    cartRepository.get(id).map {
      case Some(cartEntry) =>
        val filled = modifyCartForm.fill(ModifyCartEntryForm(cartEntry.quantity))
        Ok(views.html.cart.cartEntryModify(filled, cartEntry.id))
      case None => BadRequest("Cart entry not found")
    }
  }

  def modifyCartEntryViewResponse(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyCartForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      cartEntry => {
        cartRepository.modify(id, cartEntry)
          .flatMap(result => result.map {
            case Success(_) => Redirect(routes.CartEntriesController.modifyCartEntryView(id)).flashing("success" -> "Cart entry updated")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removeCartEntryView(id: Long): Action[AnyContent] = Action.async {
    cartRepository.remove(id)
      .map {
        case Success(_) => Redirect("/cart/entriesView")
        case Failure(exception) => BadRequest(exception.getMessage)
      }
  }
}

case class CreateCartEntryForm(customerId: Long, productId: Long, quantity: Int)

case class ModifyCartEntryForm(quantity: Int)
