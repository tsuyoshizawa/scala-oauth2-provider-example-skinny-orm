package models

import models.OauthAccessToken._
import org.joda.time.DateTime
import scalikejdbc._
import skinny.orm.SkinnyCRUDMapper

case class OauthAuthorizationCode(
                                   id: Long,
                                   accountId: Long,
                                   account: Option[Account] = None,
                                   oauthClientId: Long,
                                   oauthClient: Option[OauthClient] = None,
                                   code: String,
                                   redirectUri: Option[String],
                                   createdAt: DateTime)

object OauthAuthorizationCode extends SkinnyCRUDMapper[OauthAuthorizationCode] {

  override val tableName = "oauth_authorization_code"
  override def defaultAlias = createAlias("oac")

  belongsTo[Account](Account, (oac, account) => oac.copy(account = account)).byDefault
  belongsTo[OauthClient](OauthClient, (oac, client) => oac.copy(oauthClient = client)).byDefault

  override def extract(rs: WrappedResultSet, oac: ResultName[OauthAuthorizationCode]) = new OauthAuthorizationCode(
    id = rs.long(oac.id),
    accountId = rs.long(oac.accountId),
    oauthClientId = rs.long(oac.oauthClientId),
    code = rs.string(oac.code),
    redirectUri = rs.stringOpt(oac.redirectUri),
    createdAt = rs.jodaDateTime(oac.createdAt)
  )

  def findByCode(code: String)(implicit session: DBSession): Option[OauthAuthorizationCode] = {
    val oac = OauthAuthorizationCode.defaultAlias
    val expireAt = new DateTime().minusMinutes(30)
    OauthAuthorizationCode.where(
      sqls
        .eq(oac.code, code).and
        .gt(oac.createdAt, expireAt)
    ).apply().headOption
  }
}
