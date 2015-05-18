package models

import java.security.SecureRandom

import org.joda.time.DateTime
import scalikejdbc._
import skinny.orm.SkinnyCRUDMapper

import scala.util.Random

case class OauthAccessToken(
  id: Long,
  accountId: Long,
  account: Option[Account] = None,
  oauthClientId: Long,
  oauthClient: Option[OauthClient] = None,
  accessToken: String,
  refreshToken: String,
  createdAt: DateTime
)

object OauthAccessToken extends SkinnyCRUDMapper[OauthAccessToken] {

  override val tableName = "oauth_access_token"
  override def defaultAlias = createAlias("oat")

  belongsTo[Account](Account, (oat, account) => oat.copy(account = account)).byDefault
  belongsTo[OauthClient](OauthClient, (oat, client) => oat.copy(oauthClient = client)).byDefault

  override def extract(rs: WrappedResultSet, oat: ResultName[OauthAccessToken]) = new OauthAccessToken(
    id = rs.long(oat.id),
    accountId = rs.long(oat.accountId),
    oauthClientId = rs.long(oat.oauthClientId),
    accessToken = rs.string(oat.accessToken),
    refreshToken = rs.string(oat.refreshToken),
    createdAt = rs.jodaDateTime(oat.createdAt)
  )

  def create(account: Account, client: OauthClient)(implicit session: DBSession): OauthAccessToken = {
    def randomString(length: Int) = new Random(new SecureRandom()).alphanumeric.take(length).mkString
    val accessToken = randomString(40)
    val refreshToken = randomString(40)
    val createdAt = new DateTime()

    val oauthAccessToken = new OauthAccessToken(
      id = 0,
      accountId = account.id,
      oauthClientId = client.id,
      accessToken = accessToken,
      refreshToken = refreshToken,
      createdAt = createdAt
    )

    val generatedId = OauthAccessToken.createWithNamedValues(
      column.accountId -> oauthAccessToken.accountId,
      column.oauthClientId -> oauthAccessToken.oauthClientId,
      column.accessToken -> oauthAccessToken.accessToken,
      column.refreshToken -> oauthAccessToken.refreshToken,
      column.createdAt -> oauthAccessToken.createdAt
    )
    oauthAccessToken.copy(id = generatedId)
  }

  def delete(account: Account, client: OauthClient)(implicit session: DBSession): Int = {
    OauthAccessToken.deleteBy(sqls
      .eq(column.accountId, account.id).and
      .eq(column.oauthClientId, client.id)
    )
  }

  def refresh(account: Account, client: OauthClient)(implicit session: DBSession): OauthAccessToken = {
    delete(account, client)
    create(account, client)
  }

  def findByAccessToken(accessToken: String)(implicit session: DBSession): Option[OauthAccessToken] = {
    val oat = OauthAccessToken.defaultAlias
    OauthAccessToken.where(sqls.eq(oat.accessToken, accessToken)).apply().headOption
  }

  def findByAuthorized(account: Account, clientId: String)(implicit session: DBSession): Option[OauthAccessToken] = {
    val oat = OauthAccessToken.defaultAlias
    val oac = OauthClient.defaultAlias
    OauthAccessToken.where(sqls
      .eq(oat.accountId, account.id).and
      .eq(oac.clientId, clientId)
    ).apply().headOption
  }

  def findByRefreshToken(refreshToken: String)(implicit session: DBSession): Option[OauthAccessToken] = {
    val expireAt = new DateTime().minusMonths(1)
    val oat = OauthAccessToken.defaultAlias
    OauthAccessToken.where(sqls
      .eq(oat.refreshToken, refreshToken).and
      .gt(oat.createdAt, expireAt)
    ).apply().headOption
  }
}
