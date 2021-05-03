package controllers

import models.UserRepository
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, optional}
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
class UsersController @Inject()(userRepository: UserRepository, val controllerComponents: ControllerComponents) extends BaseController {

  val createUserForm: Form[CreateUserForm] = Form {
    mapping(
      "email" -> nonEmptyText,
      "password" -> nonEmptyText,
      "firstName" -> nonEmptyText,
      "lastName" -> nonEmptyText
    )(CreateUserForm.apply)(CreateUserForm.unapply)
  }

  val modifyUserForm: Form[ModifyUserForm] = Form {
    mapping(
      "email" -> optional(nonEmptyText),
      "password" -> optional(nonEmptyText),
      "firstName" -> optional(nonEmptyText),
      "lastName" -> optional(nonEmptyText)
    )(ModifyUserForm.apply)(ModifyUserForm.unapply)
  }

  def addUser(): Action[AnyContent] = Action.async { implicit request =>
    createUserForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      user => {
        userRepository.add(user)
          .map {
            case Success(user) => Ok(Json.toJson(user))
            case Failure(exception) => BadRequest(Json.toJson(exception.getMessage))
          }
      }
    )
  }

  def getUser(id: Long): Action[AnyContent] = Action.async {
    userRepository.get(id).map {
      case Some(user) => Ok(Json.toJson(user))
      case None => Ok("{\"error\":\"not found\"}")
    }
  }

  def getAllUsers: Action[AnyContent] = Action.async {
    userRepository.getAll.map(users => Ok(Json.toJson(users)))
  }

  def modifyUser(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyUserForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      user => {
        userRepository.modify(id, user)
          .flatMap(result => result.map {
            case Success(_) => Ok("{\"result\":\"updated\"}")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removeUser(id: Long): Action[AnyContent] = Action.async {
    userRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreateUserForm(email: String, password: String, firstName: String, lastName: String)
case class ModifyUserForm(email: Option[String], password: Option[String], firstName: Option[String], lastName: Option[String])

