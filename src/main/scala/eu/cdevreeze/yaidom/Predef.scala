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
