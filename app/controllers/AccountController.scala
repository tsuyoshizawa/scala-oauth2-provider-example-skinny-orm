package controllers

import scalikejdbc._
import models._
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.{Json}
import play.api.mvc.{Action, Controller}

import scalaoauth2.provider._

class AccountController extends Controller with OAuth2Provider {

  val formEmail        = Form("email" -> text)
  val formPassword     = Form("password" -> text)
  val formClientId     = Form("client_id" -> text)
  val formClientSecret = Form("client_secret" -> text)

  def create = Action {implicit request =>
    val email = formEmail.bindFromRequest.get
    val password = formPassword.bindFromRequest.get
    val clientId     = formClientId.bindFromRequest.get
    val clientSecret = formClientSecret.bindFromRequest.get

    var status = ""
    var msg = ""
    isUniqueAccount(email, clientId) match {
      case (false, attributes) =>
        status = "failure"
        msg = s"error, ${attributes.toString}"
      case _ =>
        status = "success"
        Account.createWithAttributes('email -> email, 'password -> password)
        val ownerId      = Account.findBy(sqls.eq(Account.column.email, email)).get.id

        //TODO customize grant_type
        OauthClient.createWithAttributes(
          'owner_id -> ownerId, 'client_id -> clientId, 'client_secret -> clientSecret, 'grant_type -> "client_credentials"
        )
    }

    Ok(Json.parse(s"""{"status":"${status}", "msg":"${msg}"}"""))
  }

  def isUniqueAccount(email: String, clientId: String): (Boolean, List[String]) = {
    var stack: List[String] = List()

    val account = Account.findBy(sqls.eq(Account.column.email, email))
    account match {
      case None    => stack = "email" :: stack
      case Some(a) => return (false, List("email"))
    }

    val client = OauthClient.findBy(sqls.eq(OauthClient.column.clientId, clientId))
    client match {
      case None    => return (true,  "client_id" :: stack)
      case Some(c) => return (false, "client_id" :: stack)
    }
  }

}
