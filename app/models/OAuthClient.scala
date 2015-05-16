package models

import org.joda.time.DateTime
import scalikejdbc._
import skinny.orm._

case class OauthClient(
  id: Long,
  ownerId: Long,
  owner: Option[Account] = None,
  grantType: String,
  clientId: String,
  clientSecret: String,
  redirectUri: Option[String],
  createdAt: DateTime
)

object OauthClient extends SkinnyCRUDMapper[OauthClient] {

  override val tableName = "oauth_client"
  override def defaultAlias = createAlias("oc")

  override def extract(rs: WrappedResultSet, oc: ResultName[OauthClient]) = new OauthClient(
    id = rs.long(oc.id),
    ownerId = rs.long(oc.ownerId),
    grantType = rs.string(oc.grantType),
    clientId = rs.string(oc.clientId),
    clientSecret = rs.string(oc.clientSecret),
    redirectUri = rs.stringOpt(oc.redirectUri),
    createdAt = rs.jodaDateTime(oc.createdAt)
  )

  innerJoinWithDefaults(Account, (c, owner) => sqls.eq(c.ownerId, owner.id)).byDefaultEvenIfAssociated
  val owner = belongsToWithAlias[Account](Account -> Account.ownerAlias, (c, owner) => c.copy(owner = owner)).byDefault

  def validate(clientId: String, clientSecret: String, grantType: String)(implicit s: DBSession): Boolean = {
    val oc = OauthClient.defaultAlias
    OauthClient.where(sqls
      .eq(oc.clientId, clientId).and
      .eq(oc.clientSecret, clientSecret)
    ).apply().headOption.map { client =>
      grantType == client.grantType || grantType == "refresh_token"
    }.getOrElse(false)
  }

  def findByClientId(clientId: String)(implicit s: DBSession): Option[OauthClient] = {
    val oc = OauthClient.defaultAlias
    OauthClient.where(sqls
      .eq(oc.clientId, clientId)
    ).apply().headOption
  }

  def findClientCredentials(clientId: String, clientSecret: String)(implicit session: DBSession): Option[Account] = {
    val oc = OauthClient.defaultAlias
    OauthClient.where(sqls
      .eq(oc.clientId, clientId).and
      .eq(oc.clientSecret, clientSecret).and
      .eq(oc.grantType, "client_credentials")
    ).apply().headOption.flatMap { _.owner }
  }
}
