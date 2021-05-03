package controllers

import models.PromotionType.PromotionType
import models.{PromotionType, VoucherRepository}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats.doubleFormat
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, BaseController, ControllerComponents}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
class VouchersController @Inject()(voucherRepository: VoucherRepository, val controllerComponents: ControllerComponents) extends BaseController {

  val createVoucherForm: Form[CreateVoucherForm] = Form {
    mapping(
      "discount" -> of[Double],
      "type" -> number.verifying("Error.invalidType", promotionType =>
        promotionType.equals(PromotionType.PERCENTAGE) ||
          promotionType.equals(PromotionType.CONSTANT)),
    )(CreateVoucherForm.apply)(CreateVoucherForm.unapply)
  }

  val modifyVoucherForm: Form[ModifyVoucherForm] = Form {
    mapping(
      "discount" -> optional(of[Double]),
      "type" -> optional(number.verifying("Error.invalidType", promotionType =>
        promotionType.equals(PromotionType.PERCENTAGE) ||
          promotionType.equals(PromotionType.CONSTANT))),
    )(ModifyVoucherForm.apply)(ModifyVoucherForm.unapply)
  }

  def addVoucher(): Action[AnyContent] = Action.async { implicit request =>
    createVoucherForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      voucher => {
        voucherRepository.add(voucher)
          .map {
            case Success(voucher) => Ok(Json.toJson(voucher))
            case Failure(exception) => BadRequest(Json.toJson(exception.getMessage))
          }
      }
    )
  }

  def getVoucher(id: Long): Action[AnyContent] = Action.async { implicit request =>
    voucherRepository.get(id).map {
      case Some(voucher) => Ok(Json.toJson(voucher))
      case None => Ok("{\"error\":\"not found\"}")
    }
  }

  def getAllVouchers: Action[AnyContent] = Action.async { implicit request =>
    voucherRepository.getAll.map(vouchers => Ok(Json.toJson(vouchers)))
  }

  def modifyVoucher(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyVoucherForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(Json.toJson(error.data)))
      },
      voucher => {
        voucherRepository.modify(id, voucher)
          .flatMap(result => result.map {
            case Success(_) => Ok("{\"result\":\"updated\"}")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removeVoucher(id: Long): Action[AnyContent] = Action.async { implicit request =>
    voucherRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreateVoucherForm(discount: Double, voucherType: PromotionType)
case class ModifyVoucherForm(discount: Option[Double], voucherType: Option[PromotionType])

