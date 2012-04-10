package eu.cdevreeze.yaidom

/**
 * `Document` indexed on `ElemPath`. With the index, parent nodes can be found reasonably efficiently.
 */
final class IndexedDocument(val document: Document) extends Immutable {

  /** The index on `ElemPath`. Computed only once, during construction of this `IndexedDocument`. */
  val index: Map[ElemPath, Elem] = document.documentElement.getIndexByElemPath

  /**
   * Finds the parent of the given `Elem`, if any, wrapped in an `Option`.
   * If the given `Elem` is not found, or has no parent (which means it is the document root element), `None` is returned.
   */
  def findParent(elm: Elem): Option[Elem] = {
    val parentElmOption: Option[Elem] =
      for {
        path <- index find { case (path, e) => e == elm } map { case (path, e) => path }
        parentPath <- path.parentPathOption
        foundElm <- index.get(parentPath)
      } yield foundElm

    parentElmOption
  }
}
