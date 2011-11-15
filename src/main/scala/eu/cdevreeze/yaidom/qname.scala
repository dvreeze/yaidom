package eu.cdevreeze.yaidom

/**
 * Qualified name. See http://www.w3.org/TR/xml-names11/.
 * Semantically like a QName in anti-xml, and not like a QName in Java.
 *
 * There are 2 types of QNames:
 * <ul>
 * <li>UnprefixedNames, which only contain a local part</li>
 * <li>PrefixedNames, which combine a non-empty prefix with a local part</li>
 * </ul>
 *
 * QNames are meaningless outside their scope, which resolves the QName as an ExpandedName.
 */
sealed trait QName extends Immutable {

  def localPart: String
}

final case class UnprefixedName(override val localPart: String) extends QName {
  require(localPart ne null)
  require(localPart.size > 0)

  /** The String representation as it appears in XML, that is, the localPart */
  override def toString: String = localPart
}

final case class PrefixedName(prefix: String, override val localPart: String) extends QName {
  require(prefix ne null)
  require(prefix.size > 0)
  require(localPart ne null)
  require(localPart.size > 0)

  /** The String representation as it appears in XML E.g. <b>xs:schema</b> */
  override def toString: String = "%s:%s".format(prefix, localPart)
}

object QName {

  /** Creates a QName from an optional prefix and a localPart */
  def apply(prefix: Option[String], localPart: String): QName =
    prefix.map(pref => PrefixedName(pref, localPart)).getOrElse(UnprefixedName(localPart))

  /** Creates a PrefixedName from a prefix and a localPart */
  def apply(prefix: String, localPart: String): QName = PrefixedName(prefix, localPart)

  /** Creates an UnprefixedName from a localPart */
  def apply(localPart: String): QName = UnprefixedName(localPart)

  /** Parses a String into a QName. The String must conform to the <b>toString</b> format of a PrefixedName or UnprefixedName */
  def parse(s: String): QName = {
    val arr = s.split(':')
    require(arr.size <= 2, "Expected at most 1 colon in QName '%s'".format(s))

    arr.size match {
      case 1 => QName(s)
      case 2 => QName(arr(0), arr(1))
      case _ => sys.error("Did not expect more than 1 colon in QName '%s'".format(s))
    }
  }
}
