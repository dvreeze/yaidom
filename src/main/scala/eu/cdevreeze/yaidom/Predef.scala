package eu.cdevreeze.yaidom

/**
 * Predef singleton object with implicit conversions. According to the Scala 2.10 compiler, these implicit conversions should
 * not occur in package objects.
 *
 * To avoid ambiguity, it may be wise to import the members with the fully qualified name, as follows:
 * {{{
 * import eu.cdevreeze.yaidom.Predef._
 * }}}
 */
object Predef {

  /** "Implicit class" for converting a `String` to an [[eu.cdevreeze.yaidom.EName]] */
  final class ToParsedEName(val s: String) {
    def ename: EName = EName.parse(s)
  }

  /** Implicit conversion enriching a `String` with a `ename` method that turns the `String` into an [[eu.cdevreeze.yaidom.EName]] */
  implicit def toParsedEName(s: String): ToParsedEName = new ToParsedEName(s)

  /** Namespace. It offers a method to create an [[eu.cdevreeze.yaidom.EName]] with that namespace from a given localPart */
  final class Namespace(val ns: String) {
    def ename(localPart: String): EName = EName(ns, localPart)

    /** Returns `ns`, that is, the namespace URI as `String` */
    override def toString: String = ns
  }

  /** "Implicit class" for converting a `String` to a `Namespace` */
  final class ToNamespace(val s: String) {
    def ns: Namespace = new Namespace(s)
  }

  /** Implicit conversion enriching a `String` with a `ns` method that turns the `String` into a `Namespace` */
  implicit def toNamespace(s: String): ToNamespace = new ToNamespace(s)

  /** "Implicit class" for converting a `Map[String, String]` to a [[eu.cdevreeze.yaidom.Scope.Declarations]] */
  final class ToNamespaces(val m: Map[String, String]) {
    def namespaces: Scope.Declarations = Scope.Declarations.fromMap(m)
  }

  /** Implicit conversion enriching a `Map[String, String]` with a `namespaces` method that turns the `Map` into a [[eu.cdevreeze.yaidom.Scope.Declarations]] */
  implicit def toNamespaces(m: Map[String, String]): ToNamespaces = new ToNamespaces(m)

  /** "Implicit class" for converting a `Map[String, String]` to a [[eu.cdevreeze.yaidom.Scope]] */
  final class ToScope(val m: Map[String, String]) {
    def scope: Scope = Scope.fromMap(m)
  }

  /** Implicit conversion enriching a `Map[String, String]` with a `scope` method that turns the `Map` into a [[eu.cdevreeze.yaidom.Scope]] */
  implicit def toScope(m: Map[String, String]): ToScope = new ToScope(m)
}
