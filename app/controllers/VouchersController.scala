package controllers

import play.api.mvc.{AnyContent, BaseController, ControllerComponents, Request}

import javax.inject.{Inject, Singleton}

@Singleton
class VouchersController @Inject()(val controllerComponents: ControllerComponents) extends BaseController {
  def addVoucher() = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def getVoucher(id: Long) = Action {
    NoContent
  }

  def modifyVoucher(id: Long) = Action { implicit request: Request[AnyContent] =>
    NoContent
  }

  def removeVoucher(id: Long) = Action {
    NoContent
  }
}
