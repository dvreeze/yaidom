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
    require(args.length >= 2, "Usage: FindConceptLabels <language code> <url> ... ")
    val languageCode = args(0)
    val uris = args.drop(1).flatMap(arg => arg.split(',').map(s => new URI(s))).toList

    for (uri <- uris) {
      require(new jio.File(uri).exists, "Taxonomy directory '%s' not found".format(uri.toString))
    }
    println("Reading taxonomy ...")

    val taxonomyProducer = new Taxonomy.FileBasedTaxonomyProducer
    val taxonomy: Taxonomy = taxonomyProducer(uris)

    val substitutionGroups: Set[ExpandedName] = taxonomy.substitutionGroups
    println("Found substitution groups: %s".format(substitutionGroups.mkString(", ")))

    val substitutionGroupElemDefs: Map[ExpandedName, Elem] = taxonomy.substitutionGroupElemDefinitionsFor(substitutionGroups)
    val parentSubstitutionGroups: Map[ExpandedName, ExpandedName] =
      substitutionGroupElemDefs.mapValues(elemDef => taxonomy.substitutionGroupOption(elemDef)).filter(_._2.isDefined).mapValues(_.get)
    println("Substitution group parents: %s".format(parentSubstitutionGroups.mkString(", ")))

    val substGroupAncestries: immutable.Seq[List[ExpandedName]] = taxonomy.substitutionGroupAncestries
    println("Substitution group ancestries: %s".format(substGroupAncestries.mkString(", ")))
    val itemOrTupleSubstGroups: Set[ExpandedName] =
      substGroupAncestries.filter(ancestry => ancestry.contains(XbrliItem) || ancestry.contains(XbrliTuple)).map(_.head).toSet
    println("Item or tuple substitution groups: %s".format(itemOrTupleSubstGroups.mkString(", ")))

    println("Found %d linkbases".format(taxonomy.linkbases.size))

    val labelLinks: Map[URI, immutable.Seq[ExtendedLink]] = taxonomy.linkbases.mapValues(linkbase => findLabelLinks(linkbase))
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
            elementDefinition = elemDef,
            languageOption = conceptLabel.languageOption,
            labelText = conceptLabel.labelText))
        }
      })
    println("Found %d resolved concept-labels with language %s".format(resolvedConceptLabels.size, languageCode))

    val itemOrTupleConceptLabels: immutable.Seq[ResolvedConceptLabel] =
      resolvedConceptLabels.filter(conceptLabel => {
        val substGroupOption: Option[ExpandedName] = taxonomy.substitutionGroupOption(conceptLabel.elementDefinition)
        substGroupOption.isDefined && itemOrTupleSubstGroups.contains(substGroupOption.get)
      })
    println("Found %d resolved concept-labels for items/tuples with language %s".format(itemOrTupleConceptLabels.size, languageCode))

    val props = new jutil.Properties
    for (conceptLabel <- itemOrTupleConceptLabels) {
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
    val elementDefinition: Elem,
    val languageOption: Option[String],
    val labelText: String) extends Immutable {
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
          labelText = toResourceOption.get.wrappedElem.firstTextValueOption.getOrElse(""))
        Some(conceptLabel)
      }
    }

    val labelArcs = labelLink.arcXLinks.filter(arc => arc.wrappedElem.resolvedName == XbrlLabelArc)
    labelArcs.flatMap(labelArc => labelArcToConceptLabelOption(labelArc))
  }
}
