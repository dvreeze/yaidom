package eu.cdevreeze.yaidom
package examples.xbrl

import java.{ util => jutil, io => jio }
import java.net.URI
import javax.xml.stream._
import scala.collection.immutable
import resource._
import xlink.{ Elem => _, _ }
import jinterop.StaxConversions._
import ExpandedName._
import Taxonomy._

/**
 * Very poor man's, low level, ad-hoc representation of an immutable XBRL taxonomy, at the level of XML Elems and XLinks.
 * Even at this low level of abstraction, interesting queries are possible, merely by using yaidom and Scala's
 * great Collections API.
 *
 * The term taxonomy is used rather loosely. The XBRL spec (http://www.xbrl.org/Specification/XBRL-RECOMMENDATION-2003-12-31+Corrected-Errata-2008-07-02.htm#_3.1)
 * speaks of a taxonomy as a schema with the linkbases directly referred from that schema. This wikipedia page
 * (http://en.wikipedia.org/wiki/XBRL) uses a far looser definition of taxonomies. Although the spec should be leading,
 * we use the looser definition, of a taxonomy as a set of taxonomy schemas and linkbases.
 *
 * It is assumed that taxonomies are resolved entirely and stored in memory before use, instead of discovering them
 * while using them.
 */
final class Taxonomy(
  val schemas: Map[URI, xlink.Elem],
  val linkbases: Map[URI, xlink.Elem],
  val otherFiles: Map[URI, xlink.Elem]) extends Immutable {

  require(schemas ne null)
  require(linkbases ne null)
  require(otherFiles ne null)

  require(numberOfFiles == (schemas.keySet ++ linkbases.keySet ++ otherFiles.keySet).size)

  require {
    schemas.values forall { root => root.resolvedName == XsdSchema }
  }

  def numberOfFiles: Int = schemas.size + linkbases.size + otherFiles.size

  def findSchemaRoot(url: URI): Option[Elem] = {
    require(url.isAbsolute)

    val schemaUrl: URI = new URI(url.getScheme, url.getSchemeSpecificPart(), null)
    val fragment = url.getFragment

    val schemaOption: Option[xlink.Elem] = schemas.get(schemaUrl)
    schemaOption map { schema => schema.wrappedElem }
  }

  def findElementDefinition(url: URI): Option[Elem] = {
    require(url.isAbsolute)

    val schemaUrl: URI = new URI(url.getScheme, url.getSchemeSpecificPart(), null)
    val fragment = url.getFragment

    val schemaOption: Option[Elem] = schemas.get(schemaUrl) map { _.wrappedElem }

    if (schemaOption.isEmpty) None else {
      val elemDefinitionOption: Option[Elem] = schemaOption.get firstElemOptionWhere { e =>
        (e.resolvedName == XsdElementDefinition) && (e.attributeOption("id".ename) == Some(fragment))
      }

      elemDefinitionOption
    }
  }

  def findSchemaRootAndElementDefinition(url: URI): Option[(Elem, Elem)] = {
    require(url.isAbsolute)

    val schemaUrl: URI = new URI(url.getScheme, url.getSchemeSpecificPart(), null)
    val fragment = url.getFragment

    val schemaOption: Option[Elem] = schemas.get(schemaUrl) map { _.wrappedElem }

    if (schemaOption.isEmpty) None else {
      val elemDefinitionOption: Option[Elem] = schemaOption.get firstElemOptionWhere { e =>
        (e.resolvedName == XsdElementDefinition) && (e.attributeOption("id".ename) == Some(fragment))
      }

      elemDefinitionOption map { elemDef => (schemaOption.get -> elemDef) }
    }
  }

  def findElementDefinitions(root: Elem): immutable.Seq[Elem] = {
    root elemsWhere { e => e.resolvedName == XsdElementDefinition }
  }

  def substitutionGroupOption(elemDef: Elem): Option[ExpandedName] = {
    require(elemDef.resolvedName == XsdElementDefinition)

    elemDef.attributeOption("substitutionGroup".ename) flatMap { v =>
      val qname = QName.parse(v)
      elemDef.scope.resolveQName(qname)
    }
  }

  def substitutionGroups: Set[ExpandedName] = {
    val elemDefs = schemas.values flatMap { root => findElementDefinitions(root.wrappedElem) }
    elemDefs flatMap { elemDef => substitutionGroupOption(elemDef) } toSet
  }

  def substitutionGroupElemDefinitionsFor(substitutionGroups: Set[ExpandedName]): Map[ExpandedName, Elem] = {
    def substitutionGroupElemDefinitionsIn(root: Elem): Map[ExpandedName, Elem] = {
      val tns = root.attribute("targetNamespace".ename)

      val elemDefs = findElementDefinitions(root)
      val filteredElemDefs = elemDefs filter { elemDef =>
        val nameOption = elemDef.attributeOption("name".ename)
        val enameOption = nameOption map { name => ExpandedName(tns, name) }

        nameOption.isDefined && enameOption.isDefined && substitutionGroups.contains(enameOption.get)
      }
      val result = filteredElemDefs map { elemDef => (ExpandedName(tns, elemDef.attribute("name".ename)) -> elemDef) }
      result.toMap
    }

    val result = schemas.values map { root => substitutionGroupElemDefinitionsIn(root.wrappedElem) }
    result.flatten.toMap
  }

  def substitutionGroupAncestries: immutable.Seq[List[ExpandedName]] = {
    val substGroups = substitutionGroups

    val substGroupParents: Map[ExpandedName, ExpandedName] =
      substitutionGroupElemDefinitionsFor(substGroups) mapValues { elemDef => substitutionGroupOption(elemDef) } filter { _._2.isDefined } mapValues { _.get }

    def ancestries(currentAncestries: immutable.Seq[List[ExpandedName]]): immutable.Seq[List[ExpandedName]] = {
      // Very inefficient
      val newAncestries: immutable.Seq[List[ExpandedName]] = currentAncestries map { ancestry =>
        if (substGroupParents.contains(ancestry.last))
          ancestry ::: List(substGroupParents(ancestry.last))
        else ancestry
      }
      if (newAncestries == currentAncestries) currentAncestries else ancestries(newAncestries)
    }

    val currAncestries = substGroups.toIndexedSeq[ExpandedName] map { substGroup => List(substGroup) }
    ancestries(currAncestries)
  }

  def isSubTaxonomyOf(other: Taxonomy): Boolean = {
    schemas.toSet.subsetOf(other.schemas.toSet) &&
      linkbases.toSet.subsetOf(other.linkbases.toSet) &&
      otherFiles.toSet.subsetOf(other.otherFiles.toSet)
  }
}

object Taxonomy {

  type Producer = ((List[URI]) => Taxonomy)

  val SchemaNamespace = URI.create("http://www.w3.org/2001/XMLSchema")
  val XsdSchema = ExpandedName(SchemaNamespace.toString, "schema")
  val XsdElementDefinition = ExpandedName(SchemaNamespace.toString, "element")

  val XmlNamespace = URI.create("http://www.w3.org/XML/1998/namespace")
  val XmlLang = ExpandedName(XmlNamespace.toString, "lang")

  val XbrliNamespace = URI.create("http://www.xbrl.org/2003/instance")
  val XbrliItem = ExpandedName(XbrliNamespace.toString, "item")
  val XbrliTuple = ExpandedName(XbrliNamespace.toString, "tuple")

  val XbrlLinkbaseNamespace = URI.create("http://www.xbrl.org/2003/linkbase")
  val XbrlLinkbase = ExpandedName(XbrlLinkbaseNamespace.toString, "linkbase")

  val XbrlLabelLink = ExpandedName(XbrlLinkbaseNamespace.toString, "labelLink")
  val XbrlLabelArc = ExpandedName(XbrlLinkbaseNamespace.toString, "labelArc")
  val XbrlConceptLabelArcRole = "http://www.xbrl.org/2003/arcrole/concept-label"

  def apply(elems: Map[URI, Elem]): Taxonomy = {
    val schemas = elems collect { case (uri, elem) if elem.resolvedName == XsdSchema => (uri, elem) }
    val linkbaseElems: Map[URI, Elem] = elems collect { case (uri, elem) if elem.resolvedName == XbrlLinkbase => (uri, elem) }
    val linkbases: Map[URI, xlink.Elem] = linkbaseElems mapValues { e => xlink.Elem(e) }
    val otherElems = (elems -- schemas.keySet) -- linkbases.keySet

    val taxonomySchemas = schemas mapValues { e => xlink.Elem(e) }
    val taxonomyOtherElems = otherElems mapValues { e => xlink.Elem(e) }
    new Taxonomy(taxonomySchemas, linkbases, taxonomyOtherElems)
  }

  final class FileBasedTaxonomyProducer extends Producer {

    def apply(uris: List[URI]): Taxonomy = {
      val elms: Map[URI, Elem] = {
        // I tried to use par collections here, but saw too much locking going on (analyzing with jvisualvm), so chickened out
        // Thread dumps showed locking inside Elem creation, during UUID creation
        val result = uris flatMap { uri => readFiles(new jio.File(uri), XMLInputFactory.newInstance).toList }
        result.toMap
      }
      Taxonomy(elms)
    }

    private def readFiles(dir: jio.File, xmlInputFactory: XMLInputFactory): Map[URI, Elem] = {
      require(dir.isDirectory && dir.exists, "Directory '%s' must be an existing directory".format(dir.getPath))

      def endsWithOneOf(f: jio.File, nameEndings: List[String]): Boolean =
        nameEndings exists { ending => f.getName.endsWith(ending) }

      val files: List[jio.File] = dir.listFiles.toList
      val normalFiles: List[jio.File] = files filter { _.isFile } filter { f => endsWithOneOf(f, List(".xml", ".xsd", ".xbrl")) }
      val dirs: List[jio.File] = files filter { _.isDirectory } filter { dir => !endsWithOneOf(dir, List(".svn", ".git")) }

      // Recursive calls (not tail-recursive)
      val readNormalFiles = {
        val result = normalFiles map { file => readFile(file, xmlInputFactory) }
        result.toMap
      }
      val recursivelyReadFiles = dirs flatMap { dir => readFiles(dir, xmlInputFactory) }
      readNormalFiles ++ recursivelyReadFiles
    }

    private def readFile(file: jio.File, xmlInputFactory: XMLInputFactory): (URI, Elem) = {
      require(file.isFile && file.exists, "File '%s' must be an existing file".format(file.getPath))

      val rootElem: Elem = {
        def createReader(): XMLEventReader = xmlInputFactory.createXMLEventReader(new jio.FileInputStream(file))

        val result = managed(createReader()) map { xmlEventReader =>
          convertToElem(xmlEventReader.toSeq)
        }
        result.opt.getOrElse(sys.error("Could not parse file '%s' as XML".format(file.getPath)))
      }
      (file.toURI, rootElem)
    }
  }
}
