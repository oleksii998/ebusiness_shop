package controllers

import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.{Inject, Singleton}

@Singleton
class CartsController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def addCart() = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def getCart(id: Long) = Action {
    NoContent
  }

  def modifyCart(id: Long) = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def removeCart(id: Long) = Action {
    NoContent
  }
}
