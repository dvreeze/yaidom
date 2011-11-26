package eu.cdevreeze.yaidom
package examples.xbrl

import java.{ util => jutil, io => jio }
import java.net.URI
import scala.collection.immutable
import xlink._
import ExpandedName._
import Taxonomy._

import java.net.URI

object FindConceptLabels {

  def main(args: Array[String]) {
    require(args.length == 2, "Usage: FindConceptLabels <url> <language code>")
    val uri = new URI(args(0))
    val languageCode = args(1)

    require(new jio.File(uri).exists, "Taxonomy directory '%s' not found".format(uri.toString))
    println("Reading taxonomy ...")

    val taxonomyProducer = new Taxonomy.FileBasedTaxonomyProducer
    val taxonomy: Taxonomy = taxonomyProducer(uri)

    val labelLinkbases: Map[URI, XLinkPart] =
      taxonomy.linkbases.filter(uriAndLinkbase => isLabelLinkbase(uriAndLinkbase._2))
    println("Found %d label linkbases (of %d linkbases)".format(labelLinkbases.size, taxonomy.linkbases.size))

    val labelLinks: Map[URI, immutable.Seq[ExtendedLink]] = labelLinkbases.mapValues(linkbase => findLabelLinks(linkbase))
    println("Found %d label links".format(labelLinks.values.flatten.size))

    val conceptLabels: immutable.Seq[ConceptLabel] =
      (for {
        (uri, links) <- labelLinks
        link <- links
      } yield findConceptLabels(uri, link)).flatten.toIndexedSeq
    println("Found %d concept-labels".format(conceptLabels.size))

    val conceptLabelsSearchedFor: immutable.Seq[ConceptLabel] =
      conceptLabels.filter(conceptLabel => conceptLabel.languageOption == Some(languageCode))
    println("Found %d concept-labels with language %s".format(conceptLabelsSearchedFor.size, languageCode))

    val resolvedConceptLabels: immutable.Seq[ResolvedConceptLabel] =
      conceptLabelsSearchedFor.flatMap(conceptLabel => {
        val schemaRootOption: Option[Elem] = taxonomy.findSchemaRoot(conceptLabel.conceptUri)
        val elemDefOption: Option[Elem] = taxonomy.findElementDefinition(conceptLabel.conceptUri)

        if (schemaRootOption.isEmpty) None else {
          elemDefOption.map(elemDef => new ResolvedConceptLabel(
            schemaRoot = schemaRootOption.get,
            conceptUri = conceptLabel.conceptUri,
            elementDefinition = elemDef,
            languageOption = conceptLabel.languageOption,
            labelText = conceptLabel.labelText))
        }
      })
    println("Found %d resolved concept-labels with language %s".format(resolvedConceptLabels.size, languageCode))

    val props = new jutil.Properties
    for (conceptLabel <- resolvedConceptLabels) {
      val tnsOption = conceptLabel.schemaRoot.attributeOption("targetNamespace".ename)
      val localName = conceptLabel.elementDefinition.attribute("name".ename)
      val name: ExpandedName = ExpandedName(tnsOption, localName)

      props.put(name.toString, conceptLabel.labelText)
    }
    val os = new jio.ByteArrayOutputStream
    props.storeToXML(os, null)
    val propertiesXmlString = new String(os.toByteArray, "utf-8")

    println()
    println("Properties file content:")
    println()
    println(propertiesXmlString)
  }

  final class ConceptLabel(
    val conceptUri: URI, val languageOption: Option[String], val labelText: String) extends Immutable {

    require(conceptUri.isAbsolute)
  }

  final class ResolvedConceptLabel(
    val schemaRoot: Elem,
    val conceptUri: URI,
    val elementDefinition: Elem,
    val languageOption: Option[String],
    val labelText: String) extends Immutable {

    require(conceptUri.isAbsolute)
  }

  def isLabelLinkbase(xlink: XLinkPart): Boolean = {
    xlink.wrappedElem.resolvedName == XbrlLabelLinkbase
  }

  def findLabelLinks(root: XLinkPart): immutable.Seq[ExtendedLink] = {
    root.elems.collect({ case link: ExtendedLink if link.resolvedName == XbrlLabelLink => link })
  }

  def findConceptLabels(currentUri: URI, labelLink: ExtendedLink): immutable.Seq[ConceptLabel] = {
    def labelArcToConceptLabelOption(arc: Arc): Option[ConceptLabel] = {
      val fromLocatorOption: Option[Locator] = labelLink.locatorXLinks.find(loc => loc.label == arc.from)
      val toResourceOption: Option[Resource] = labelLink.resourceXLinks.find(res => res.label == arc.to)

      if (fromLocatorOption.isEmpty || toResourceOption.isEmpty) None else {
        val conceptLabel = new ConceptLabel(
          conceptUri = currentUri.resolve(fromLocatorOption.get.href),
          languageOption = toResourceOption.get.wrappedElem.attributeOption(XmlLang),
          labelText = toResourceOption.get.wrappedElem.firstTextValue)
        Some(conceptLabel)
      }
    }

    val labelArcs = labelLink.arcXLinks.filter(arc => arc.wrappedElem.resolvedName == XbrlLabelArc)
    labelArcs.flatMap(labelArc => labelArcToConceptLabelOption(labelArc))
  }
}
