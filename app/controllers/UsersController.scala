package controllers

import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.{Inject, Singleton}

@Singleton
class UsersController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def addUser() = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def getUser(id: Long) = Action {
    NoContent
  }

  def modifyUser(id: Long) = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def removeUser(id: Long) = Action {
    NoContent
  }
}
