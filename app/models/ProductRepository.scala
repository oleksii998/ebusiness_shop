package models

import controllers.{CreateProductForm, ModifyProductForm}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@Singleton
class ProductRepository @Inject()(databaseConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  private val databaseConfig = databaseConfigProvider.get[JdbcProfile]
  private val products = TableQuery[ProductTable]

  import databaseConfig._
  import databaseConfig.profile.api._

  def add(createProductForm: CreateProductForm): Future[Try[Product]] = db.run {
    ((products returning products.map(_.id) into (
      (product, id) => product.copy(id = id)
      )) += Product(0, createProductForm.name,
      createProductForm.description,
      createProductForm.price,
      createProductForm.quantity,
      createProductForm.categoryId,
      active = true)).asTry
  }

  def getAll: Future[Seq[Product]] = db.run {
    products.filter(_.active).result
  }

  def get(id: Long): Future[Option[Product]] = db.run {
    products.filter(product => product.id === id && product.active).result.headOption
  }

  def remove(id: Long): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(product) =>
        val replacement = product.copy(active = false)
        db.run(products.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Product not found")))
    }
  }

  def modify(id: Long, modifyProductForm: ModifyProductForm): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(product) =>
        var name = product.name
        if (modifyProductForm.name.isDefined) {
          name = modifyProductForm.name.get
        }
        var description = product.description
        if (modifyProductForm.description.isDefined) {
          description = modifyProductForm.description.get
        }
        var price = product.price
        if (modifyProductForm.price.isDefined) {
          price = modifyProductForm.price.get
        }
        var quantity = product.quantity
        if (modifyProductForm.quantity.isDefined) {
          quantity = modifyProductForm.quantity.get
        }
        var categoryId = product.categoryId
        if (modifyProductForm.categoryId.isDefined) {
          categoryId = modifyProductForm.categoryId.get
        }
        val replacement = product.copy(name = name, description = description, price = price, quantity = quantity, categoryId = categoryId)
        db.run(products.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Product not found")))
    }
  }
}