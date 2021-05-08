package models

import controllers.{CreatePromotionForm, CreateVoucherForm, ModifyPromotionForm, ModifyVoucherForm}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@Singleton
class VoucherRepository @Inject()(databaseConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  private val databaseConfig = databaseConfigProvider.get[JdbcProfile]
  private val vouchers = TableQuery[VoucherTable]

  import databaseConfig._
  import databaseConfig.profile.api._

  def add(createVoucherForm: CreateVoucherForm): Future[Try[Voucher]] = db.run {
    ((vouchers returning vouchers.map(_.id) into (
      (voucher, id) => voucher.copy(id = id)
      )) += Voucher(0, createVoucherForm.name,
      createVoucherForm.discount,
      createVoucherForm.voucherType,
      active = true)).asTry
  }

  def getAll: Future[Seq[Voucher]] = db.run {
    vouchers.filter(_.active).result
  }

  def get(id: Long): Future[Option[Voucher]] = db.run {
    vouchers.filter(product => product.id === id && product.active).result.headOption
  }

  def remove(id: Long): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(voucher) =>
        val replacement = voucher.copy(active = false)
        db.run(vouchers.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Voucher not found")))
    }
  }

  def modify(id: Long, modifyVoucherForm: ModifyVoucherForm): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(voucher) =>
        var discount = voucher.discount
        if (modifyVoucherForm.discount.isDefined) {
          discount = modifyVoucherForm.discount.get
        }
        var voucherType = voucher.voucherType
        if (modifyVoucherForm.voucherType.isDefined) {
          voucherType = modifyVoucherForm.voucherType.get
        }
        val replacement = voucher.copy(discount = discount, voucherType = voucherType)
        db.run(vouchers.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Voucher not found")))
    }
  }
}