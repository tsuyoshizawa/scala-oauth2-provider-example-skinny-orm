package models

import java.security.MessageDigest

import org.joda.time.DateTime
import scalikejdbc._
import skinny.orm._

case class Account(id: Long, email: String, password: String, createdAt: DateTime)
object Account extends SkinnyCRUDMapper[Account] {

  override def defaultAlias = createAlias("a")
  val ownerAlias = createAlias("owner")

  override def extract(rs: WrappedResultSet, a: ResultName[Account]) = new Account(
    id = rs.get(a.id),
    email = rs.get(a.email),
    password = rs.get(a.password),
    createdAt = rs.get(a.createdAt)
  )

  private def digestString(s: String): String = {
    val md = MessageDigest.getInstance("SHA-1")
    md.update(s.getBytes)
    md.digest.foldLeft("") { (s, b) =>
      s + "%02x".format(if (b < 0) b + 256 else b)
    }
  }

  def authenticate(email: String, password: String)(implicit s: DBSession): Option[Account] = {
    val hashedPassword = digestString(password)
    val a = Account.defaultAlias
    Account.where(sqls.eq(a.email, email).and.eq(a.password, hashedPassword)).apply().headOption
  }
}
