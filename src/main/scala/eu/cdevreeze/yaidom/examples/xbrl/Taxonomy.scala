package eu.cdevreeze.yaidom
package examples.xbrl

import java.{ util => jutil, io => jio }
import java.net.URI
import javax.xml.stream._
import scala.collection.immutable
import resource._
import xlink._
import parse._
import ExpandedName._
import Taxonomy._

/**
 * Poor man's low level representation of an immutable XBRL taxonomy, at the level of XML Elems and XLinks.
 * Even at this low level of abstraction, interesting queries are possible, merely by using yaidom and Scala's
 * great Collections API.
 *
 * There is a clear distinction between producers of a Taxonomy (from file system, a web site and the like) and
 * consumers of a Taxonomy (such as queries, for example to find concept labels). The assumption is that
 * taxonomies are resolved entirely and stored in memory before use, instead of discovering them while using them.
 *
 * In a REPL session, we can set up a taxonomy (read from file system) for querying like this:
 * <pre>
 * import java.{ util => jutil, io => jio }
 * val taxonomyRootDir = new jio.File("/taxonomy-rootdir")
 * taxonomyRootDir.exists // just checking...
 *
 * import eu.cdevreeze.yaidom._
 * import eu.cdevreeze.yaidom.xlink._
 * import eu.cdevreeze.yaidom.examples.xbrl._
 *
 * val uri = taxonomyRootDir.toURI
 * val taxonomy = (new Taxonomy.FileBasedTaxonomyProducer).apply(uri)
 * </pre>
 */
final class Taxonomy(
  val schemas: Map[URI, Elem],
  val linkbases: Map[URI, XLinkPart],
  val otherFiles: Map[URI, Elem]) extends Immutable {

  require(schemas ne null)
  require(linkbases ne null)
  require(otherFiles ne null)

  require(schemas.size + linkbases.size + otherFiles.size == (schemas.keySet ++ linkbases.keySet ++ otherFiles.keySet).size)

  require(schemas.values.forall(root => root.resolvedName == XsdSchema))

  def linkbaseElems: Map[URI, Elem] = linkbases.mapValues(xlink => xlink.wrappedElem)

  def findSchemaRoot(url: URI): Option[Elem] = {
    require(url.isAbsolute)

    val schemaUrl: URI = new URI(url.getScheme, url.getSchemeSpecificPart(), null)
    val fragment = url.getFragment

    val schemaOption: Option[Elem] = schemas.get(schemaUrl)
    schemaOption
  }

  def findElementDefinition(url: URI): Option[Elem] = {
    require(url.isAbsolute)

    val schemaUrl: URI = new URI(url.getScheme, url.getSchemeSpecificPart(), null)
    val fragment = url.getFragment

    val schemaOption: Option[Elem] = schemas.get(schemaUrl)

    if (schemaOption.isEmpty) None else {
      val elemDefinitionOption: Option[Elem] = schemaOption.get.elems(e => {
        (e.resolvedName == XsdElementDefinition) && (e.attributeOption("id".ename) == Some(fragment))
      }).headOption

      elemDefinitionOption
    }
  }
}

object Taxonomy {

  type Producer = ((URI) => Taxonomy)

  val SchemaNamespace = URI.create("http://www.w3.org/2001/XMLSchema")
  val XsdSchema = ExpandedName(SchemaNamespace.toString, "schema")
  val XsdElementDefinition = ExpandedName(SchemaNamespace.toString, "element")

  val XmlNamespace = URI.create("http://www.w3.org/XML/1998/namespace")
  val XmlLang = ExpandedName(XmlNamespace.toString, "lang")

  val XbrlLinkbaseNamespace = URI.create("http://www.xbrl.org/2003/linkbase")
  val XbrlLinkbase = ExpandedName(XbrlLinkbaseNamespace.toString, "linkbase")

  val XbrlLabelLinkbase = ExpandedName(XbrlLinkbaseNamespace.toString, "linkbase")
  val XbrlLabelLink = ExpandedName(XbrlLinkbaseNamespace.toString, "labelLink")
  val XbrlLabelArc = ExpandedName(XbrlLinkbaseNamespace.toString, "labelArc")
  val XbrlConceptLabelArcRole = "http://www.xbrl.org/2003/arcrole/concept-label"

  def apply(elems: Map[URI, Elem]): Taxonomy = {
    val schemas = elems collect { case (uri, elem) if elem.resolvedName == XsdSchema => (uri, elem) }
    val linkbaseElems: Map[URI, Elem] = elems collect { case (uri, elem) if elem.resolvedName == XbrlLinkbase => (uri, elem) }
    val linkbases: Map[URI, XLinkPart] = linkbaseElems mapValues (e => XLinkPart(e))
    val otherElems = (elems -- schemas.keySet) -- linkbases.keySet

    new Taxonomy(schemas, linkbases, otherElems)
  }

  final class FileBasedTaxonomyProducer extends Producer {

    def apply(uri: URI): Taxonomy = {
      val xmlInputFactory = XMLInputFactory.newFactory
      val elems = readFiles(new jio.File(uri), xmlInputFactory)
      Taxonomy(elems)
    }

    private def readFiles(dir: jio.File, xmlInputFactory: XMLInputFactory): Map[URI, Elem] = {
      require(dir.isDirectory && dir.exists, "Directory '%s' must be an existing directory".format(dir.getPath))

      def endsWithOneOf(f: jio.File, nameEndings: List[String]): Boolean =
        nameEndings.exists(ending => f.getName.endsWith(ending))

      val files: List[jio.File] = dir.listFiles.toList
      val normalFiles: List[jio.File] = files.filter(_.isFile).filter(f => endsWithOneOf(f, List(".xml", ".xsd", ".xbrl")))
      val dirs: List[jio.File] = files.filter(_.isDirectory).filter(dir => !endsWithOneOf(dir, List(".svn", ".git")))

      // Recursive calls (not tail-recursive)
      val readNormalFiles = normalFiles.map(file => readFile(file, xmlInputFactory)).toMap
      val recursivelyReadFiles = dirs.flatMap(dir => readFiles(dir, xmlInputFactory))
      readNormalFiles ++ recursivelyReadFiles
    }

    private def readFile(file: jio.File, xmlInputFactory: XMLInputFactory): (URI, Elem) = {
      require(file.isFile && file.exists, "File '%s' must be an existing file".format(file.getPath))

      val rootElem: Elem = {
        def createReader(): XMLEventReader = xmlInputFactory.createXMLEventReader(new jio.FileInputStream(file))

        managed(createReader()).map({ xmlEventReader =>
          val elemReader = new ElemReader(xmlEventReader)
          elemReader.readElem()
        }).opt.getOrElse(sys.error("Could not parse file '%s' as XML".format(file.getPath)))
      }
      (file.toURI, rootElem)
    }
  }
}
