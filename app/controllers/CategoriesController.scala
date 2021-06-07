package controllers

import models.CategoryRepository
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, optional}
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class CategoriesController @Inject()(categoryRepository: CategoryRepository, scc: DefaultSilhouetteControllerComponents)(implicit val ec: ExecutionContext) extends SilhouetteController(scc) {

  val createCategoryForm: Form[CreateCategoryForm] = Form {
    mapping(
      "name" -> nonEmptyText,
      "description" -> nonEmptyText
    )(CreateCategoryForm.apply)(CreateCategoryForm.unapply)
  }

  val modifyCategoryForm: Form[ModifyCategoryForm] = Form {
    mapping(
      "name" -> optional(nonEmptyText),
      "description" -> optional(nonEmptyText)
    )(ModifyCategoryForm.apply)(ModifyCategoryForm.unapply)
  }

  def addCategory(): Action[AnyContent] = securedAction.async { implicit request =>
    createCategoryForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      category => {
        categoryRepository.add(category)
          .map {
            case Success(category) => Ok(Json.toJson(category))
            case Failure(exception) => BadRequest(Json.toJson(exception.getMessage))
          }
      }
    )
  }

  def getCategory(id: Long): Action[AnyContent] = Action.async {
    categoryRepository.get(id).map {
      case Some(product) => Ok(Json.toJson(product))
      case None => NotFound("{\"error\":\"Category not found\"}")
    }
  }

  def getAllCategories: Action[AnyContent] = Action.async {
    categoryRepository.getAll.map(products => Ok(Json.toJson(products)))
  }

  def modifyCategory(id: Long): Action[AnyContent] = securedAction.async { implicit request =>
    modifyCategoryForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      category => {
        categoryRepository.modify(id, category)
          .flatMap(result => result.map {
            case Success(_) => Ok("{\"result\":\"updated\"}")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removeCategory(id: Long): Action[AnyContent] = securedAction.async {
    categoryRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }

  //VIEWS

  def addCategoryView(): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.categories.categoryAdd(createCategoryForm))
  }

  def addCategoryViewResponse(): Action[AnyContent] = Action.async { implicit request =>
    createCategoryForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      category => {
        categoryRepository.add(category)
          .map {
            case Success(_) => Redirect(routes.CategoriesController.addCategoryView()).flashing("success" -> "Category added")
            case Failure(exception) => BadRequest(exception.getMessage)
          }
      }
    )
  }

  def getCategoryView(id: Long): Action[AnyContent] = Action.async {
    categoryRepository.get(id).map {
      case Some(category) => Ok(views.html.categories.category(category))
      case None => BadRequest("Category not found")
    }
  }

  def getAllCategoriesView: Action[AnyContent] = Action.async {
    categoryRepository.getAll.map(categories => Ok(views.html.categories.categories(categories)))
  }

  def modifyCategoryView(id: Long): Action[AnyContent] = Action.async { implicit request =>
    categoryRepository.get(id).map {
      case Some(category) =>
        val filled = modifyCategoryForm.fill(ModifyCategoryForm(Option.apply(category.name),
          Option.apply(category.description)))
        Ok(views.html.categories.categoryModify(filled, category.id))
      case None => BadRequest("Category not found")
    }
  }

  def modifyCategoryViewResponse(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyCategoryForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      category => {
        categoryRepository.modify(id, category)
          .flatMap(result => result.map {
            case Success(_) => Redirect(routes.CategoriesController.modifyCategoryView(id)).flashing("success" -> "Category updated")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removeCategoryView(id: Long): Action[AnyContent] = Action.async {
    categoryRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Redirect("/categoriesView")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreateCategoryForm(name: String, description: String)
case class ModifyCategoryForm(name: Option[String], description: Option[String])
