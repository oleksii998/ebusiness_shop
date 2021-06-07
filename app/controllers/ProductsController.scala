package controllers

import models.{CategoryRepository, ProductRepository}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats.doubleFormat
import play.api.libs.json.Json
import play.api.mvc.{AnyContent, BaseController, ControllerComponents, _}

import javax.inject.{Inject, Singleton}
import scala.concurrent._
import scala.concurrent.duration.Duration
import scala.util.{Failure, Success}

@Singleton
class ProductsController @Inject()(productRepository: ProductRepository,
                                   categoryRepository: CategoryRepository,
                                   scc: DefaultSilhouetteControllerComponents)(implicit val ec: ExecutionContext) extends SilhouetteController(scc) {

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

  def addProduct(): Action[AnyContent] = securedAction.async { implicit request =>
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
      case None => NotFound("{\"error\":\"Product not found\"}")
    }
  }

  def getAllProducts: Action[AnyContent] = Action.async { implicit request =>
    productRepository.getAll.map(products => Ok(Json.toJson(products)))
  }

  def modifyProduct(id: Long): Action[AnyContent] = securedAction.async { implicit request =>
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

  def removeProduct(id: Long): Action[AnyContent] = securedAction.async { implicit request =>
    productRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }

  //VIEWS

  def addProductView(): Action[AnyContent] = Action.async { implicit request: MessagesRequest[AnyContent] =>
    categoryRepository.getAll.map(category => Ok(views.html.products.productAdd(createProductForm, category)))
  }

  def addProductViewResponse(): Action[AnyContent] = Action.async { implicit request =>
    createProductForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      product => {
        productRepository.add(product)
          .map {
            case Success(_) => Redirect(routes.UsersController.addUserView()).flashing("success" -> "Product added")
            case Failure(exception) => BadRequest(exception.getMessage)
          }
      }
    )
  }

  def getProductView(id: Long): Action[AnyContent] = Action.async {
    productRepository.get(id).map {
      case Some(product) => Ok(views.html.products.product(product))
      case None => BadRequest("Product not found")
    }
  }

  def getAllProductsView: Action[AnyContent] = Action.async {
    productRepository.getAll.map(products => Ok(views.html.products.products(products)))
  }

  def modifyProductView(id: Long): Action[AnyContent] = Action.async { implicit request =>
    productRepository.get(id).map {
      case Some(productCategory) =>
        val categories = Await.result(categoryRepository.getAll, Duration.Inf)
        val filled = modifyProductForm.fill(ModifyProductForm(Option.apply(productCategory.product.name),
          Option.apply(productCategory.product.description),
          Option.apply(productCategory.product.price),
          Option.apply(productCategory.product.quantity),
          Option.apply(productCategory.product.categoryId)))
        Ok(views.html.products.productModify(filled, productCategory.product.id, categories))
      case None => BadRequest("Product not found")
    }
  }

  def modifyProductViewResponse(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyProductForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      user => {
        productRepository.modify(id, user)
          .flatMap(result => result.map {
            case Success(_) => Redirect(routes.ProductsController.modifyProductView(id)).flashing("success" -> "Product updated")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removeProductView(id: Long): Action[AnyContent] = Action.async {
    productRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Redirect("/productsView")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreateProductForm(name: String, description: String, price: Double, quantity: Int, categoryId: Long)

case class ModifyProductForm(name: Option[String], description: Option[String], price: Option[Double], quantity: Option[Int], categoryId: Option[Long])

