package controllers

import models.PromotionType.PromotionType
import models.{PromotionType, VoucherRepository}
import play.api.data.Form
import play.api.data.Forms._
import play.api.data.format.Formats.doubleFormat
import play.api.libs.json.Json
import play.api.mvc._

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

@Singleton
class VouchersController @Inject()(voucherRepository: VoucherRepository, scc: DefaultSilhouetteControllerComponents)(implicit val ec: ExecutionContext) extends SilhouetteController(scc) {
  val createVoucherForm: Form[CreateVoucherForm] = Form {
    mapping(
      "discount" -> of[Double],
      "name" -> nonEmptyText,
      "type" -> number.verifying("Error.invalidType", promotionType =>
        promotionType.equals(PromotionType.PERCENTAGE) ||
          promotionType.equals(PromotionType.CONSTANT)),
    )(CreateVoucherForm.apply)(CreateVoucherForm.unapply)
  }

  val modifyVoucherForm: Form[ModifyVoucherForm] = Form {
    mapping(
      "discount" -> optional(of[Double]),
      "name" -> optional(nonEmptyText),
      "type" -> optional(number.verifying("Error.invalidType", promotionType =>
        promotionType.equals(PromotionType.PERCENTAGE) ||
          promotionType.equals(PromotionType.CONSTANT))),
    )(ModifyVoucherForm.apply)(ModifyVoucherForm.unapply)
  }

  def addVoucher(): Action[AnyContent] = securedAction.async { implicit request =>
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
      case None => NotFound("{\"error\":\"Voucher not found\"}")
    }
  }

  def getAllVouchers: Action[AnyContent] = Action.async { implicit request =>
    voucherRepository.getAll.map(vouchers => Ok(Json.toJson(vouchers)))
  }

  def modifyVoucher(id: Long): Action[AnyContent] = securedAction.async { implicit request =>
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

  def removeVoucher(id: Long): Action[AnyContent] = securedAction.async { implicit request =>
    voucherRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Ok("{\"result\":\"removed\"}")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }

  //VIEWS

  def addVoucherView(): Action[AnyContent] = Action { implicit request: MessagesRequest[AnyContent] =>
    Ok(views.html.vouchers.voucherAdd(createVoucherForm))
  }

  def addVoucherViewResponse(): Action[AnyContent] = Action.async { implicit request =>
    createVoucherForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      voucher => {
        voucherRepository.add(voucher)
          .map {
            case Success(_) => Redirect(routes.VouchersController.addVoucherView()).flashing("success" -> "Voucher added")
            case Failure(exception) => BadRequest(exception.getMessage)
          }
      }
    )
  }

  def getVoucherView(id: Long): Action[AnyContent] = Action.async {
    voucherRepository.get(id).map {
      case Some(voucher) => Ok(views.html.vouchers.voucher(voucher))
      case None => BadRequest("Voucher not found")
    }
  }

  def getAllVouchersView: Action[AnyContent] = Action.async {
    voucherRepository.getAll.map(vouchers => Ok(views.html.vouchers.vouchers(vouchers)))
  }

  def modifyVoucherView(id: Long): Action[AnyContent] = Action.async { implicit request =>
    voucherRepository.get(id).map {
      case Some(voucher) =>
        val filled = modifyVoucherForm.fill(ModifyVoucherForm(Option.apply(voucher.discount),
          Option.apply(voucher.name),
          Option.apply(voucher.voucherType)))
        Ok(views.html.vouchers.voucherModify(filled, voucher.id))
      case None => BadRequest("Voucher not found")
    }
  }

  def modifyVoucherViewResponse(id: Long): Action[AnyContent] = Action.async { implicit request =>
    modifyVoucherForm.bindFromRequest.fold(
      error => {
        Future.successful(BadRequest(error.data.values.reduce((x, y) => x + "\n" + y)))
      },
      voucher => {
        voucherRepository.modify(id, voucher)
          .flatMap(result => result.map {
            case Success(_) => Redirect(routes.VouchersController.modifyVoucherView(id)).flashing("success" -> "Voucher updated")
            case Failure(exception) => BadRequest(exception.getMessage)
          })
      }
    )
  }

  def removeVoucherView(id: Long): Action[AnyContent] = Action.async {
    voucherRepository.remove(id)
      .flatMap(result => result.map {
        case Success(_) => Redirect("/vouchersView")
        case Failure(exception) => BadRequest(exception.getMessage)
      })
  }
}

case class CreateVoucherForm(discount: Double, name: String, voucherType: PromotionType)
case class ModifyVoucherForm(discount: Option[Double], name: Option[String], voucherType: Option[PromotionType])

