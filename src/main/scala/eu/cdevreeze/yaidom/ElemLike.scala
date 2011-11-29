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

  /** Returns the descendant elements (not including this element). Very inefficient. */
  final def elems: immutable.Seq[E] = {
    @tailrec
    def elems(elms: immutable.IndexedSeq[E], acc: immutable.IndexedSeq[E]): immutable.IndexedSeq[E] = {
      val childElms: immutable.IndexedSeq[E] = elms.flatMap(_.childElems)

      val newAcc = acc ++ childElms
      if (childElms.isEmpty) newAcc else elems(childElms, newAcc)
    }

    elems(immutable.IndexedSeq(self), immutable.IndexedSeq())
  }

  /** Returns the descendant elements obeying the given predicate, that is, elems.filter(p). Not efficient. */
  final def elems(p: E => Boolean): immutable.Seq[E] = {
    @tailrec
    def elems(elms: immutable.IndexedSeq[E], acc: immutable.IndexedSeq[E]): immutable.IndexedSeq[E] = {
      val childElms: immutable.IndexedSeq[E] = elms.flatMap(_.childElems)

      val newAcc = acc ++ childElms.filter(p)
      if (childElms.isEmpty) newAcc else elems(childElms, newAcc)
    }

    elems(immutable.IndexedSeq(self), immutable.IndexedSeq())
  }

  /** Returns the descendant elements with the given expanded name. Not efficient. */
  final def elems(expandedName: ExpandedName): immutable.Seq[E] = elems(e => e.resolvedName == expandedName)

  /** Returns the descendant elements with the given expanded name, obeying the given predicate. Not efficient. */
  final def elems(expandedName: ExpandedName, p: E => Boolean): immutable.Seq[E] =
    elems(e => (e.resolvedName == expandedName) && p(e))

  /** Returns the descendant elements obeying the given predicate that have no ancestor obeying the predicate */
  final def firstElems(p: E => Boolean): immutable.Seq[E] = {
    @tailrec
    def firstElems(elms: immutable.IndexedSeq[E], acc: immutable.IndexedSeq[E]): immutable.IndexedSeq[E] = {
      val childElms: immutable.IndexedSeq[E] = elms.flatMap(_.childElems)
      val (matchingChildren, nonMatchingChildren) = childElms.partition(e => p(e))

      val newAcc = acc ++ matchingChildren
      if (nonMatchingChildren.isEmpty) newAcc else firstElems(nonMatchingChildren, newAcc)
    }

    firstElems(immutable.IndexedSeq(self), immutable.IndexedSeq())
  }

  /** Returns the descendant elements with the given expanded name that have no ancestor with the same name */
  final def firstElems(expandedName: ExpandedName): immutable.Seq[E] = firstElems(e => e.resolvedName == expandedName)

  /**
   * Returns the descendant elements with the given expanded name, obeying the given predicate, that have no ancestor
   * with the same name obeying the same predicate
   */
  final def firstElems(expandedName: ExpandedName, p: E => Boolean): immutable.Seq[E] =
    firstElems(e => (e.resolvedName == expandedName) && p(e))

  /** Returns the first found descendant element obeying the given predicate, if any, wrapped in an Option. */
  final def firstElemOption(p: E => Boolean): Option[E] = {
    @tailrec
    def firstElemOption(elms: immutable.IndexedSeq[E]): Option[E] = {
      val childElms: immutable.IndexedSeq[E] = elms.flatMap(_.childElems)
      val elmOption: Option[E] = childElms.find(p)

      if (elmOption.isDefined) elmOption else firstElemOption(childElms)
    }

    firstElemOption(immutable.IndexedSeq(self))
  }

  /** Returns the first found descendant element with the given expanded name, if any, wrapped in an Option */
  final def firstElemOption(expandedName: ExpandedName): Option[E] = firstElemOption(e => e.resolvedName == expandedName)

  /** Returns the first found descendant element with the given expanded name, obeying the given predicate, if any, wrapped in an Option */
  final def firstElemOption(expandedName: ExpandedName, p: E => Boolean): Option[E] =
    firstElemOption(e => (e.resolvedName == expandedName) && p(e))

  /** Finds the parent element, if any, searching in the tree with the given root element. Typically rather expensive. */
  final def findParentInTree(root: E): Option[E] = {
    if (root.childElems.exists(ch => ch == self)) Some(root) else {
      root.firstElemOption(e => e.childElems.exists(ch => ch == self))
    }
  }
}
