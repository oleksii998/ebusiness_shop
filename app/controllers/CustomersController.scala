package controllers

import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.{Inject, Singleton}

@Singleton
class CustomersController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def addCustomer() = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def getCustomer(id: Long) = Action {
    NoContent
  }

  def modifyCustomer(id: Long) = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def removeCustomer(id: Long) = Action {
    NoContent
  }
}