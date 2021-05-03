package controllers

import models.CategoryRepository
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, optional}
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
class CategoriesController @Inject()(categoryRepository: CategoryRepository, val controllerComponents: ControllerComponents) extends BaseController {

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

  def addCategory(): Action[AnyContent] = Action.async { implicit request =>
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
      case None => Ok("{\"error\":\"not found\"}")
    }
  }

  def getAllCategories: Action[AnyContent] = Action.async {
    categoryRepository.getAll.map(products => Ok(Json.toJson(products)))
  }

  def modifyCategory(id: Long): Action[AnyContent] = Action.async { implicit request =>
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

  def removeCategory(id: Long): Action[AnyContent] = Action.async {
    categoryRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreateCategoryForm(name: String, description: String)
case class ModifyCategoryForm(name: Option[String], description: Option[String])
