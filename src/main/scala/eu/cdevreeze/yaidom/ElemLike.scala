package eu.cdevreeze.yaidom

import scala.collection.immutable
import scala.annotation.tailrec

/**
 * Supertrait for Elems and other element-like objects.
 */
trait ElemLike[E <: ElemLike[E]] { self: E =>

  /** Resolved name of the element, as ExpandedName */
  def resolvedName: ExpandedName

  /** Returns the child elements */
  def childElems: immutable.Seq[E]

  /** Returns the child elements obeying the given predicate */
  final def childElems(p: E => Boolean): immutable.Seq[E] = childElems.filter(p)

  /** Returns the child elements with the given expanded name */
  final def childElems(expandedName: ExpandedName): immutable.Seq[E] = childElems(e => e.resolvedName == expandedName)

  /** Returns the child elements with the given expanded name, obeying the given predicate */
  final def childElems(expandedName: ExpandedName, p: E => Boolean): immutable.Seq[E] =
    childElems(e => (e.resolvedName == expandedName) && p(e))

  /** Returns the single child element with the given expanded name, if any, and None otherwise */
  final def childElemOption(expandedName: ExpandedName): Option[E] = {
    val result = childElems(expandedName)
    require(result.size <= 1, "Expected at most 1 child element %s, but found %d of them".format(expandedName, result.size))
    result.headOption
  }

  /** Returns the single child element with the given expanded name, and throws an exception otherwise */
  final def childElem(expandedName: ExpandedName): E = {
    val result = childElems(expandedName)
    require(result.size == 1, "Expected exactly 1 child element %s, but found %d of them".format(expandedName, result.size))
    result.head
  }

  /** Returns the descendant elements (not including this element) */
  final def elems: immutable.Seq[E] = {
    @tailrec
    def elems(elms: immutable.IndexedSeq[E], acc: immutable.IndexedSeq[E]): immutable.IndexedSeq[E] = {
      val childElms: immutable.IndexedSeq[E] = elms.flatMap(_.childElems)

      if (childElms.isEmpty) acc else elems(childElms, acc ++ childElms)
    }

    elems(immutable.IndexedSeq(self), immutable.IndexedSeq())
  }

  /** Returns the descendant elements obeying the given predicate */
  final def elems(p: E => Boolean): immutable.Seq[E] = elems.filter(p)

  /** Returns the descendant elements with the given expanded name */
  final def elems(expandedName: ExpandedName): immutable.Seq[E] = elems(e => e.resolvedName == expandedName)

  /** Returns the descendant elements with the given expanded name, obeying the given predicate */
  final def elems(expandedName: ExpandedName, p: E => Boolean): immutable.Seq[E] =
    elems(e => (e.resolvedName == expandedName) && p(e))

  /** Finds the parent element, if any, searching in the tree with the given root element. Typically expensive. */
  final def findParentInTree(root: E): Option[E] =
    (root.elems :+ root).find(e => e.childElems.exists(ch => ch == self))
}
