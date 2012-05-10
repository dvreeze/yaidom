package eu.cdevreeze.yaidom
package resolved

/**
 * Predef singleton object with implicit conversions. According to the Scala 2.10 compiler, these implicit conversions should
 * not occur in package objects.
 *
 * To avoid ambiguity, it may be wise to import the members with the fully qualified name, as follows:
 * {{{
 * import eu.cdevreeze.yaidom.resolved.Predef._
 * }}}
 */
object Predef {

  /** "Implicit class" for converting a normal yaidom `Elem` to a [[eu.cdevreeze.yaidom.resolved.Elem]] */
  final class ToResolvedElem(val e: eu.cdevreeze.yaidom.Elem) {
    def resolvedElem: Elem = Elem(e)
  }

  /** Implicit conversion enriching a normal yaidom `Elem` with a `resolvedElem` method that turns the yaidom `Elem` into a [[eu.cdevreeze.yaidom.resolved.Elem]] */
  implicit def toResolvedElem(e: eu.cdevreeze.yaidom.Elem): ToResolvedElem = new ToResolvedElem(e)
}
