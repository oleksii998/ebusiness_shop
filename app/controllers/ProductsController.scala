package controllers

import models.ProductRepository
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats.doubleFormat
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, BaseController, ControllerComponents, _}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.util.{Failure, Success}

@Singleton
class ProductsController @Inject()(val controllerComponents: ControllerComponents, productRepository: ProductRepository) extends BaseController {

  val createProductForm: Form[CreateProductForm] = Form {
    mapping(
      "name" -> nonEmptyText,
      "description" -> nonEmptyText,
      "price" -> of[Double],
      "quantity" -> number,
      "categoryId" -> longNumber
    )(CreateProductForm.apply)(CreateProductForm.unapply)
  }

  val modifyProductForm: Form[ModifyProductForm] = Form {
    mapping(
      "name" -> optional(nonEmptyText),
      "description" -> optional(nonEmptyText),
      "price" -> optional(of[Double]),
      "quantity" -> optional(number),
      "categoryId" -> optional(longNumber)
    )(ModifyProductForm.apply)(ModifyProductForm.unapply)
  }

  def addProduct(): Action[AnyContent] = Action.async { implicit request =>
    createProductForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      product => {
        productRepository.add(product)
          .map {
            case Success(product) => Ok(Json.toJson(product))
            case Failure(exception) => BadRequest(Json.toJson(exception.getMessage))
          }
      }
    )
  }

  def addProductAndRender(): Action[AnyContent] = Action.async { implicit request =>
    createProductForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      product => {
        productRepository.add(product)
          .map {
            case Success(product) => Ok(Json.toJson(product))
            case Failure(exception) => BadRequest(Json.toJson(exception.getMessage))
          }
      }
    )
  }

  def getProduct(id: Long): Action[AnyContent] = Action.async { implicit request =>
    productRepository.get(id).map {
      case Some(product) => Ok(Json.toJson(product))
      case None => Ok("{\"error\":\"not found\"}")
    }
  }

  def getAllProducts: Action[AnyContent] = Action.async { implicit request =>
    productRepository.getAll.map(products => Ok(Json.toJson(products)))
  }

  def modifyProduct(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyProductForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      product => {
        productRepository.modify(id, product)
          .flatMap(result => result.map {
            case Success(_) => Ok("{\"result\":\"updated\"}")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removeProduct(id: Long): Action[AnyContent] = Action.async { implicit request =>
    productRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreateProductForm(name: String, description: String, price: Double, quantity: Int, categoryId: Long)
case class ModifyProductForm(name: Option[String], description: Option[String], price: Option[Double], quantity: Option[Int], categoryId: Option[Long])

