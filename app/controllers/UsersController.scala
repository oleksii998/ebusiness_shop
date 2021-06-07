package controllers

import models.UserRepository
import play.api.data.Form
import play.api.data.Forms.{mapping, nonEmptyText, optional}
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class UsersController @Inject()(userRepository: UserRepository,
                                scc: DefaultSilhouetteControllerComponents)(implicit val ec: ExecutionContext) extends SilhouetteController(scc) {

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

  def getUser(id: Long): Action[AnyContent] = securedAction.async {
    userRepository.get(id).map {
      case Some(user) => Ok(Json.toJson(user))
      case None => NotFound("{\"message\": \"User not found\"}")
    }
  }

  def getAllUsers: Action[AnyContent] = securedAction.async {
    userRepository.getAll.map(users => Ok(Json.toJson(users)))
  }

  def modifyUser(id: Long): Action[AnyContent] = securedAction.async { implicit request =>
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

  def removeUser(id: Long): Action[AnyContent] = securedAction.async {
    userRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }

  //VIEWS

  def addUserView(): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.users.userAdd(createUserForm))
  }

  def addUserViewResponse(): Action[AnyContent] = Action.async { implicit request =>
    createUserForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      customer => {
        userRepository.add(customer)
          .map {
            case Success(_) => Redirect(routes.UsersController.addUserView()).flashing("success" -> "User added")
            case Failure(exception) => BadRequest(exception.getMessage)
          }
      }
    )
  }

  def getUserView(id: Long): Action[AnyContent] = Action.async {
    userRepository.get(id).map {
      case Some(user) => Ok(views.html.users.user(user))
      case None => BadRequest("User not found")
    }
  }

  def getAllUsersView: Action[AnyContent] = Action.async {
    userRepository.getAll.map(users => Ok(views.html.users.users(users)))
  }

  def modifyUserView(id: Long): Action[AnyContent] = Action.async { implicit request =>
    userRepository.get(id).map {
      case Some(user) =>
        val filled = modifyUserForm.fill(ModifyUserForm(Option.apply(user.email),
          Option.apply(user.password),
          Option.apply(user.firstName),
          Option.apply(user.lastName)))
        Ok(views.html.users.userModify(filled, user.id))
      case None => BadRequest("User not found")
    }
  }

  def modifyUserViewResponse(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyUserForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      user => {
        userRepository.modify(id, user)
          .flatMap(result => result.map {
            case Success(_) => Redirect(routes.UsersController.modifyUserView(id)).flashing("success" -> "User updated")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removeUserView(id: Long): Action[AnyContent] = Action.async {
    userRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Redirect("/usersView")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreateUserForm(email: String, password: String, firstName: String, lastName: String)
case class ModifyUserForm(email: Option[String], password: Option[String], firstName: Option[String], lastName: Option[String])

