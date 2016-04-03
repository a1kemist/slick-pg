package com.github.tminglei.slickpg

import slick.driver.PostgresDriver
import slick.jdbc.{PositionedResult, JdbcType}

/** simple inet string wrapper */
case class InetString(value: String) {
  lazy val isIPv6 = value.contains(":")
  lazy val address = value.split("/")(0)
  lazy val masklen: Int = {
    val parts = value.split("/")
    if (parts.length > 1) parts(1).toInt
    else if (isIPv6) 128
    else 32
  }
}

/** simple mac addr string wrapper */
case class MacAddrString(value: String)

/**
 * simple inet/macaddr support; if all you want is just getting from / saving to db, and using pg json operations/methods, it should be enough
 */
trait PgNetSupport extends net.PgNetExtensions with utils.PgCommonJdbcTypes { driver: PostgresDriver =>
  import driver.api._

  /// alias
  trait NetImplicits extends SimpleNetImplicits

  trait SimpleNetImplicits {
    implicit val simpleInetTypeMapper: JdbcType[InetString] =
      new GenericJdbcType[InetString]("inet",
        (v) => InetString(v),
        (v) => v.value,
        hasLiteralForm = false
      )
    implicit val simpleMacAddrTypeMapper: JdbcType[MacAddrString] =
      new GenericJdbcType[MacAddrString]("macaddr",
        (v) => MacAddrString(v),
        (v) => v.value,
        hasLiteralForm = false
      )

    implicit def simpleInetColumnExtensionMethods(c: Rep[InetString]) = {
        new InetColumnExtensionMethods[InetString, InetString](c)
      }
    implicit def simpleInetOptionColumnExtensionMethods(c: Rep[Option[InetString]]) = {
        new InetColumnExtensionMethods[InetString, Option[InetString]](c)
      }

    implicit def simpleMacAddrColumnExtensionMethods(c: Rep[MacAddrString]) = {
        new MacAddrColumnExtensionMethods[MacAddrString, MacAddrString](c)
      }
    implicit def simpleMacAddrOptionColumnExtensionMethods(c: Rep[Option[MacAddrString]]) = {
        new MacAddrColumnExtensionMethods[MacAddrString, Option[MacAddrString]](c)
      }
  }

  trait SimpleNetPlainImplicits {
    import scala.reflect.classTag
    import utils.PlainSQLUtils._
    {
      addNextArrayConverter((r) => utils.SimpleArrayUtils.fromString(InetString.apply)(r.nextString()))
      addNextArrayConverter((r) => utils.SimpleArrayUtils.fromString(MacAddrString.apply)(r.nextString()))
    }

    if (driver.isInstanceOf[ExPostgresDriver]) {
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("inet", classTag[InetString])
      driver.asInstanceOf[ExPostgresDriver].bindPgTypeToScala("macaddr", classTag[MacAddrString])
    }

    implicit class PgNetPositionedResult(r: PositionedResult) {
      def nextIPAddr() = nextIPAddrOption().orNull
      def nextIPAddrOption() = r.nextStringOption().map(InetString)
      def nextMacAddr() = nextMacAddrOption().orNull
      def nextMacAddrOption() = r.nextStringOption().map(MacAddrString)
    }

    /////////////////////////////////////////////////////////////////
    implicit val getIPAddr = mkGetResult(_.nextIPAddr())
    implicit val getIPAddrOption = mkGetResult(_.nextIntOption())
    implicit val setIPAddr = mkSetParameter[InetString]("inet", _.value)
    implicit val setIPAddrOption = mkOptionSetParameter[InetString]("inet", _.value)

    implicit val getMacAddr = mkGetResult(_.nextMacAddr())
    implicit val getMacAddrOption = mkGetResult(_.nextMacAddrOption())
    implicit val setMacAddr = mkSetParameter[MacAddrString]("macaddr", _.value)
    implicit val setMacAddrOption = mkOptionSetParameter[MacAddrString]("macaddr", _.value)
  }
}
