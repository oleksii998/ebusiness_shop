package controllers

import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.{Inject, Singleton}

@Singleton
class OrdersController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def addOrder() = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def getOrder(id: Long) = Action {
    NoContent
  }

  def modifyOrder(id: Long) = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def removeOrder(id: Long) = Action {
    NoContent
  }
}
