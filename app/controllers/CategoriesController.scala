package controllers

import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.{Inject, Singleton}

@Singleton
class CategoriesController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def addCategory() = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def getCategory(id: Long) = Action {
    NoContent
  }

  def modifyCategory(id: Long) = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def removeCategory(id: Long) = Action {
    NoContent
  }
}
