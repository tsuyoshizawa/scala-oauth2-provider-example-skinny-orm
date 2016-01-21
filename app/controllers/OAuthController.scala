package controllers

import models._
import play.api.libs.json.{Json, Writes}
import play.api.mvc.{Action, Controller}
import scalikejdbc.DB

import scala.concurrent.Future
import scalaoauth2.provider._
import scalaoauth2.provider.OAuth2ProviderActionBuilders._

class OAuthController extends Controller with OAuth2Provider {

  implicit val authInfoWrites = new Writes[AuthInfo[Account]] {
    def writes(authInfo: AuthInfo[Account]) = {
      Json.obj(
        "account" -> Json.obj(
          "email" -> authInfo.user.email
        ),
        "clientId" -> authInfo.clientId,
        "redirectUri" -> authInfo.redirectUri
      )
    }
  }

  override val tokenEndpoint = new TokenEndpoint {
    override val handlers = Map(
      OAuthGrantType.AUTHORIZATION_CODE -> new AuthorizationCode(),
      OAuthGrantType.REFRESH_TOKEN -> new RefreshToken(),
      OAuthGrantType.CLIENT_CREDENTIALS -> new ClientCredentials(),
      OAuthGrantType.PASSWORD -> new Password()
    )
  }

  def accessToken = Action.async { implicit request =>
    issueAccessToken(new MyDataHandler())
  }

  def resources = AuthorizedAction(new MyDataHandler()) { request =>
    Ok(Json.toJson(request.authInfo))
  }

  class MyDataHandler extends DataHandler[Account] {

    // common

    override def validateClient(request: AuthorizationRequest): Future[Boolean] = DB.readOnly { implicit session =>
      Future.successful((for {
        clientCredential <- request.clientCredential
      } yield OauthClient.validate(clientCredential.clientId, clientCredential.clientSecret.getOrElse(""), request.grantType)).contains(true))
    }

    override def getStoredAccessToken(authInfo: AuthInfo[Account]): Future[Option[AccessToken]] = DB.readOnly { implicit session =>
      Future.successful(OauthAccessToken.findByAuthorized(authInfo.user, authInfo.clientId.getOrElse("")).map(toAccessToken))
    }

    override def createAccessToken(authInfo: AuthInfo[Account]): Future[AccessToken] = DB.localTx { implicit session =>
      val clientId = authInfo.clientId.getOrElse(throw new InvalidClient())
      val oauthClient = OauthClient.findByClientId(clientId).getOrElse(throw new InvalidClient())
      val accessToken = OauthAccessToken.create(authInfo.user, oauthClient)
      Future.successful(toAccessToken(accessToken))
    }

    private val accessTokenExpireSeconds = 3600
    private def toAccessToken(accessToken: OauthAccessToken) = {
      AccessToken(
        accessToken.accessToken,
        Some(accessToken.refreshToken),
        None,
        Some(accessTokenExpireSeconds),
        accessToken.createdAt.toDate
      )
    }

    override def findUser(request: AuthorizationRequest): Future[Option[Account]] = DB.readOnly { implicit session =>
      request match {
        case request: PasswordRequest =>
          Future.successful(Account.authenticate(request.username, request.password))
        case request: ClientCredentialsRequest =>
          val maybeAccount = request.clientCredential.flatMap { clientCredential =>
            OauthClient.findClientCredentials(
              clientCredential.clientId,
              clientCredential.clientSecret.getOrElse("")
            )
          }
          Future.successful(maybeAccount)
        case _ =>
          Future.successful(None)
      }
    }

    // Refresh token grant

    override def findAuthInfoByRefreshToken(refreshToken: String): Future[Option[AuthInfo[Account]]] = DB.readOnly { implicit session =>
      Future.successful(OauthAccessToken.findByRefreshToken(refreshToken).flatMap { accessToken =>
        for {
          account <- accessToken.account
          client <- accessToken.oauthClient
        } yield {
          AuthInfo(
            user = account,
            clientId = Some(client.clientId),
            scope = None,
            redirectUri = None
          )
        }
      })
    }

    override def refreshAccessToken(authInfo: AuthInfo[Account], refreshToken: String): Future[AccessToken] = DB.localTx { implicit session =>
      val clientId = authInfo.clientId.getOrElse(throw new InvalidClient())
      val client = OauthClient.findByClientId(clientId).getOrElse(throw new InvalidClient())
      val accessToken = OauthAccessToken.refresh(authInfo.user, client)
      Future.successful(toAccessToken(accessToken))
    }

    // Authorization code grant

    override def findAuthInfoByCode(code: String): Future[Option[AuthInfo[Account]]] = DB.readOnly { implicit session =>
      Future.successful(OauthAuthorizationCode.findByCode(code).flatMap { authorization =>
        for {
          account <- authorization.account
          client <- authorization.oauthClient
        } yield {
          AuthInfo(
            user = account,
            clientId = Some(client.clientId),
            scope = None,
            redirectUri = authorization.redirectUri
          )
        }
      })
    }

    override def deleteAuthCode(code: String): Future[Unit] = DB.localTx { implicit session =>
      Future.successful(OauthAuthorizationCode.delete(code))
    }

    // Protected resource

    override def findAccessToken(token: String): Future[Option[AccessToken]] = DB.readOnly { implicit session =>
      Future.successful(OauthAccessToken.findByAccessToken(token).map(toAccessToken))
    }

    override def findAuthInfoByAccessToken(accessToken: AccessToken): Future[Option[AuthInfo[Account]]] = DB.readOnly { implicit session =>
      Future.successful(OauthAccessToken.findByAccessToken(accessToken.token).flatMap { case accessToken =>
        for {
          account <- accessToken.account
          client <- accessToken.oauthClient
        } yield {
          AuthInfo(
            user = account,
            clientId = Some(client.clientId),
            scope = None,
            redirectUri = None
          )
        }
      })
    }
  }
}
