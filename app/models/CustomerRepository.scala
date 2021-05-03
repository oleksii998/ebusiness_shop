package models

import controllers.{CreateCategoryForm, CreateCustomerForm, ModifyCategoryForm, ModifyCustomerForm}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.lifted.TableQuery

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Try}

@Singleton
class CustomerRepository @Inject()(databaseConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) {
  private val databaseConfig = databaseConfigProvider.get[JdbcProfile]
  private val customers = TableQuery[CustomerTable]

  import databaseConfig._
  import databaseConfig.profile.api._

  def add(createCustomerForm: CreateCustomerForm): Future[Try[Customer]] = db.run {
    ((customers returning customers.map(_.id) into (
      (customer, id) => customer.copy(id = id)
      )) += Customer(0, createCustomerForm.email,
      createCustomerForm.password,
      createCustomerForm.firstName,
      createCustomerForm.lastName,
      active = true)).asTry
  }

  def getAll: Future[Seq[Customer]] = db.run {
    customers.filter(_.active).result
  }

  def get(id: Long): Future[Option[Customer]] = db.run {
    customers.filter(customer => customer.id === id && customer.active).result.headOption
  }

  def remove(id: Long): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(customer) =>
        val replacement = customer.copy(active = false)
        db.run(customers.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Customer not found")))
    }
  }

  def modify(id: Long, modifyCustomerForm: ModifyCustomerForm): Future[Future[Try[Int]]] = {
    get(id).map {
      case Some(customer) =>
        var email = customer.email
        if (modifyCustomerForm.email.isDefined) {
          email = modifyCustomerForm.email.get
        }
        var password = customer.password
        if (modifyCustomerForm.password.isDefined) {
          password = modifyCustomerForm.password.get
        }
        var firstName = customer.firstName
        if (modifyCustomerForm.firstName.isDefined) {
          firstName = modifyCustomerForm.firstName.get
        }
        var lastName = customer.lastName
        if (modifyCustomerForm.lastName.isDefined) {
          lastName = modifyCustomerForm.lastName.get
        }
        val replacement = customer.copy(email = email, password = password, firstName = firstName, lastName = lastName)
        db.run(customers.filter(_.id === id).update(replacement).asTry)
      case None =>
        Future.successful(Failure(new RuntimeException("Customer not found")))
    }
  }
}