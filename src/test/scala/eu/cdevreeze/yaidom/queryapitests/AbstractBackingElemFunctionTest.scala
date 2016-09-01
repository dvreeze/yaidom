package eu.cdevreeze.yaidom.queryapitests

import java.io.File

import org.junit.Test
import org.scalatest.Suite

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.PathBuilder
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.BackingElemFunctionApi

abstract class AbstractBackingElemFunctionTest extends Suite {

  private val XsNamespace = "http://www.w3.org/2001/XMLSchema"
  private val XLinkNamespace = "http://www.w3.org/1999/xlink"
  private val LinkNamespace = "http://www.xbrl.org/2003/linkbase"
  private val XbrliNamespace = "http://www.xbrl.org/2003/instance"

  private val XbrliItemEName = EName(XbrliNamespace, "item")
  private val XbrliTupleEName = EName(XbrliNamespace, "tuple")

  private val XsSchemaEName = EName(XsNamespace, "schema")
  private val XsElementEName = EName(XsNamespace, "element")
  private val XsAttributeEName = EName(XsNamespace, "attribute")
  private val XsComplexTypeEName = EName(XsNamespace, "complexType")
  private val XsSimpleTypeEName = EName(XsNamespace, "simpleType")
  private val XsAnnotationEName = EName(XsNamespace, "annotation")
  private val XsComplexContentEName = EName(XsNamespace, "complexContent")
  private val XsSimpleContentEName = EName(XsNamespace, "simpleContent")
  private val XsGroupEName = EName(XsNamespace, "group")
  private val XsAllEName = EName(XsNamespace, "all")
  private val XsChoiceEName = EName(XsNamespace, "choice")
  private val XsSequenceEName = EName(XsNamespace, "sequence")
  private val XsAttributeGroupEName = EName(XsNamespace, "attributeGroup")
  private val XsAnyAttributeEName = EName(XsNamespace, "anyAttribute")
  private val XsUniqueEName = EName(XsNamespace, "unique")
  private val XsKeyEName = EName(XsNamespace, "key")
  private val XsKeyrefEName = EName(XsNamespace, "keyref")
  private val XsNotationEName = EName(XsNamespace, "notation")
  private val XsImportEName = EName(XsNamespace, "import")
  private val XsIncludeEName = EName(XsNamespace, "include")
  private val XsRedefineEName = EName(XsNamespace, "redefine")
  private val XsRestrictionEName = EName(XsNamespace, "restriction")
  private val XsExtensionEName = EName(XsNamespace, "extension")
  private val XsListEName = EName(XsNamespace, "list")
  private val XsUnionEName = EName(XsNamespace, "union")
  private val XsAppinfoEName = EName(XsNamespace, "appinfo")
  private val XsDocumentationEName = EName(XsNamespace, "documentation")
  private val XsSelectorEName = EName(XsNamespace, "selector")
  private val XsFieldEName = EName(XsNamespace, "field")
  private val XsAnyEName = EName(XsNamespace, "any")

  private val LinkLinkbaseEName = EName(LinkNamespace, "linkbase")

  private val LinkSchemaRefEName = EName(LinkNamespace, "schemaRef")
  private val LinkLinkbaseRefEName = EName(LinkNamespace, "linkbaseRef")
  private val LinkRoleRefEName = EName(LinkNamespace, "roleRef")
  private val LinkArcroleRefEName = EName(LinkNamespace, "arcroleRef")

  private val SubstitutionGroupEName = EName("substitutionGroup")
  private val TargetNamespaceEName = EName("targetNamespace")
  private val ElementFormDefaultEName = EName("elementFormDefault")
  private val AttributeFormDefaultEName = EName("attributeFormDefault")

  // Element type, without any restrictions
  type E

  def docElem: E

  def ops: BackingElemFunctionApi.Aux[E]

  @Test def testResolvedName(): Unit = {
    assertResult(XsSchemaEName)(ops.resolvedName(docElem))
    assertResult(Some(XsNamespace))(ops.resolvedName(docElem).namespaceUriOption)
    assertResult("schema")(ops.resolvedName(docElem).localPart)
  }

  @Test def testQName(): Unit = {
    assertResult(QName("xs:schema"))(ops.qname(docElem))
    assertResult(Some("xs"))(ops.qname(docElem).prefixOption)
    assertResult("schema")(ops.qname(docElem).localPart)
  }

  @Test def testDocUri(): Unit = {
    assertResult("file")(ops.docUri(docElem).getScheme)
    assertResult("some-data.xsd")((new File(ops.docUri(docElem))).getName)
    assertResult(true)((new File(ops.docUri(docElem))).isFile)
    assertResult(Some(ops.docUri(docElem)))(ops.docUriOption(docElem))

    assertResult(Set(ops.docUri(docElem))) {
      ops.findAllElemsOrSelf(docElem).map(e => ops.docUri(e)).toSet
    }
    assertResult(Set(Some(ops.docUri(docElem)))) {
      ops.findAllElemsOrSelf(docElem).map(e => ops.docUriOption(e)).toSet
    }
  }

  @Test def testDefaultBaseUri(): Unit = {
    assertResult("file")(ops.baseUri(docElem).getScheme)
    assertResult("some-data.xsd")((new File(ops.baseUri(docElem))).getName)
    assertResult(true)((new File(ops.baseUri(docElem))).isFile)
    assertResult(Some(ops.baseUri(docElem)))(ops.baseUriOption(docElem))

    assertResult(Set(ops.baseUri(docElem))) {
      ops.findAllElemsOrSelf(docElem).map(e => ops.baseUri(e)).toSet
    }
    assertResult(Set(Some(ops.baseUri(docElem)))) {
      ops.findAllElemsOrSelf(docElem).map(e => ops.baseUriOption(e)).toSet
    }
  }

  @Test def testResolvedAttributes(): Unit = {
    assertResult(
      Map(
        TargetNamespaceEName -> "http://www.sometaxonomy/0.1/basis/some2/items/some-data",
        AttributeFormDefaultEName -> "unqualified",
        ElementFormDefaultEName -> "qualified")) {

        ops.resolvedAttributes(docElem).toMap
      }

    val linkbaseRefElems = ops.filterElems(docElem, e => ops.resolvedName(e) == LinkLinkbaseRefEName)

    assertResult(Set(Some("simple"))) {
      linkbaseRefElems.map(e => ops.attributeOption(e, EName(XLinkNamespace, "type"))).toSet
    }
    assertResult(Set("simple")) {
      linkbaseRefElems.map(e => ops.attribute(e, EName(XLinkNamespace, "type"))).toSet
    }
  }

  @Test def testQNameAttributes(): Unit = {
    val elementElems = ops.filterElemsOrSelf(docElem, e => ops.resolvedName(e) == XsElementEName)

    assertResult(true)(elementElems.size >= 100)

    assertResult(Set(QName("xbrli:item"))) {
      elementElems.flatMap(e => ops.attributeAsQNameOption(e, SubstitutionGroupEName)).toSet
    }
    assertResult(Set(QName("xbrli:item"))) {
      elementElems.map(e => ops.attributeAsQName(e, SubstitutionGroupEName)).toSet
    }

    assertResult(Set(XbrliItemEName)) {
      elementElems.flatMap(e => ops.attributeAsResolvedQNameOption(e, SubstitutionGroupEName)).toSet
    }
    assertResult(Set(XbrliItemEName)) {
      elementElems.map(e => ops.attributeAsResolvedQName(e, SubstitutionGroupEName)).toSet
    }
  }

  @Test def testAttributes(): Unit = {
    assertResult(
      Map(
        QName("targetNamespace") -> "http://www.sometaxonomy/0.1/basis/some2/items/some-data",
        QName("attributeFormDefault") -> "unqualified",
        QName("elementFormDefault") -> "qualified")) {

        ops.attributes(docElem).toMap
      }

    val linkbaseRefElems = ops.filterElems(docElem, e => ops.resolvedName(e) == LinkLinkbaseRefEName)

    assertResult(Set(Some("simple"))) {
      linkbaseRefElems.map(e => ops.findAttributeByLocalName(e, "type")).toSet
    }
  }

  @Test def testScope(): Unit = {
    assertResult(
      Scope.from(
        "xs" -> XsNamespace,
        "xbrli" -> XbrliNamespace,
        "xlink" -> XLinkNamespace,
        "link" -> LinkNamespace,
        "nl-types" -> "http://www.sometaxonomy/0.1/basis/sbr/types/nl-types",
        "some2-codes" -> "http://www.sometaxonomy/0.1/basis/some2/types/some2-codes",
        "num" -> "http://www.xbrl.org/dtr/type/numeric",
        "some2-types" -> "http://www.sometaxonomy/0.1/basis/some2/types/some2-types",
        "nl-codes" -> "http://www.sometaxonomy/0.1/basis/sbr/types/nl-codes",
        "some-i" -> "http://www.sometaxonomy/0.1/basis/some2/items/some-data")) {

        ops.scope(docElem)
      }

    assertResult(Set(ops.scope(docElem))) {
      ops.findAllElemsOrSelf(docElem).map(e => ops.scope(e)).toSet
    }

    assertResult(Declarations.from(ops.scope(docElem).prefixNamespaceMap.toIndexedSeq: _*)) {
      ops.namespaces(docElem)
    }
    assertResult(Set(Declarations.from())) {
      ops.findAllElems(docElem).map(e => ops.namespaces(e)).toSet
    }
  }

  @Test def testText(): Unit = {
    assertResult(Set("")) {
      ops.findAllElemsOrSelf(docElem).map(e => ops.text(e).trim).toSet
    }
    assertResult(true) {
      ops.findAllElemsOrSelf(docElem).map(e => ops.text(e)).filter(_.nonEmpty).nonEmpty
    }
    assertResult(ops.findAllElemsOrSelf(docElem).map(e => ops.trimmedText(e))) {
      ops.findAllElemsOrSelf(docElem).map(e => ops.text(e).trim)
    }
    assertResult(ops.findAllElemsOrSelf(docElem).map(e => ops.normalizedText(e))) {
      ops.findAllElemsOrSelf(docElem).map(e => ops.text(e).trim)
    }
  }

  @Test def testFilterElemsOrSelf(): Unit = {
    // Non-existing elements

    val bogusElems = ops.filterElemsOrSelf(docElem, e => ops.resolvedName(e) == EName("schema"))

    assertResult(0)(bogusElems.size)
    assertResult(ops.findAllElemsOrSelf(docElem).filter(e => ops.resolvedName(e) == EName("schema")))(bogusElems)

    // xs:schema elements

    val xsSchemaElems = ops.filterElemsOrSelf(docElem, e => ops.resolvedName(e) == XsSchemaEName)

    assertResult(1)(xsSchemaElems.size)
    assertResult(Set(XsSchemaEName)) {
      xsSchemaElems.map(e => ops.resolvedName(e)).toSet
    }
    assertResult(ops.findAllElemsOrSelf(docElem).filter(e => ops.resolvedName(e) == XsSchemaEName)) {
      xsSchemaElems
    }

    // xs:import elements

    val xsImportElems = ops.filterElemsOrSelf(docElem, e => ops.resolvedName(e) == XsImportEName)

    assertResult(7)(xsImportElems.size)
    assertResult(Set(XsImportEName)) {
      xsImportElems.map(e => ops.resolvedName(e)).toSet
    }
    assertResult(ops.findAllElemsOrSelf(docElem).filter(e => ops.resolvedName(e) == XsImportEName)) {
      xsImportElems
    }

    // link:linkbaseRef elements

    val linkbaseRefElems = ops.filterElemsOrSelf(docElem, e => ops.resolvedName(e) == LinkLinkbaseRefEName)

    assertResult(17)(linkbaseRefElems.size)
    assertResult(Set(LinkLinkbaseRefEName)) {
      linkbaseRefElems.map(e => ops.resolvedName(e)).toSet
    }
    assertResult(ops.findAllElemsOrSelf(docElem).filter(e => ops.resolvedName(e) == LinkLinkbaseRefEName)) {
      linkbaseRefElems
    }

    // xs:appinfo child elements

    val appinfoChildElems =
      for {
        annotElem <- ops.filterElemsOrSelf(docElem, e => ops.resolvedName(e) == XsAnnotationEName)
        appinfoElem <- ops.filterElemsOrSelf(annotElem, e => ops.resolvedName(e) == XsAppinfoEName)
        appinfoChildElem <- ops.filterElemsOrSelf(appinfoElem, e => ops.parent(e) == appinfoElem)
      } yield appinfoChildElem

    assertResult(17)(appinfoChildElems.size)
    assertResult(Set(LinkLinkbaseRefEName)) {
      appinfoChildElems.map(e => ops.resolvedName(e)).toSet
    }

    val appinfoChildElems2 =
      for {
        annotElem <- ops.findAllElemsOrSelf(docElem).filter(e => ops.resolvedName(e) == XsAnnotationEName)
        appinfoElem <- ops.findAllElemsOrSelf(annotElem).filter(e => ops.resolvedName(e) == XsAppinfoEName)
        appinfoChildElem <- ops.findAllElemsOrSelf(appinfoElem).filter(e => ops.parent(e) == appinfoElem)
      } yield appinfoChildElem

    assertResult(appinfoChildElems2)(appinfoChildElems)
  }

  @Test def testFilterElems(): Unit = {
    // Non-existing elements

    val bogusElems = ops.filterElems(docElem, e => ops.resolvedName(e) == EName("schema"))
    assertResult(ops.findAllElems(docElem).filter(e => ops.resolvedName(e) == EName("schema")))(bogusElems)

    assertResult(0)(bogusElems.size)

    // xs:schema elements

    val xsSchemaElems = ops.filterElems(docElem, e => ops.resolvedName(e) == XsSchemaEName)
    assertResult(ops.findAllElems(docElem).filter(e => ops.resolvedName(e) == XsSchemaEName))(xsSchemaElems)

    assertResult(0)(xsSchemaElems.size)

    // xs:import elements

    val xsImportElems = ops.filterElems(docElem, e => ops.resolvedName(e) == XsImportEName)

    assertResult(7)(xsImportElems.size)
    assertResult(Set(XsImportEName))(xsImportElems.map(e => ops.resolvedName(e)).toSet)
    assertResult(ops.findAllElems(docElem).filter(e => ops.resolvedName(e) == XsImportEName))(xsImportElems)

    // link:linkbaseRef elements

    val linkbaseRefElems = ops.filterElems(docElem, e => ops.resolvedName(e) == LinkLinkbaseRefEName)

    assertResult(17)(linkbaseRefElems.size)
    assertResult(Set(LinkLinkbaseRefEName))(linkbaseRefElems.map(e => ops.resolvedName(e)).toSet)
    assertResult(ops.findAllElems(docElem).filter(e => ops.resolvedName(e) == LinkLinkbaseRefEName))(linkbaseRefElems)

    // xs:appinfo child elements

    val appinfoChildElems =
      for {
        annotElem <- ops.filterElems(docElem, e => ops.resolvedName(e) == XsAnnotationEName)
        appinfoElem <- ops.filterElems(annotElem, e => ops.resolvedName(e) == XsAppinfoEName)
        appinfoChildElem <- ops.filterElems(appinfoElem, e => ops.parent(e) == appinfoElem)
      } yield appinfoChildElem

    assertResult(17)(appinfoChildElems.size)
    assertResult(Set(LinkLinkbaseRefEName))(appinfoChildElems.map(e => ops.resolvedName(e)).toSet)

    val appinfoChildElems2 =
      for {
        annotElem <- ops.findAllElems(docElem).filter(e => ops.resolvedName(e) == XsAnnotationEName)
        appinfoElem <- ops.findAllElems(annotElem).filter(e => ops.resolvedName(e) == XsAppinfoEName)
        appinfoChildElem <- ops.findAllElems(appinfoElem).filter(e => ops.parent(e) == appinfoElem)
      } yield appinfoChildElem

    assertResult(appinfoChildElems2)(appinfoChildElems)
  }

  @Test def testFilterChildElems(): Unit = {
    // Non-existing elements

    val bogusElems = ops.filterChildElems(docElem, (e => ops.resolvedName(e) == EName("schema")))
    assertResult(ops.findAllChildElems(docElem).filter(e => ops.resolvedName(e) == EName("schema")))(bogusElems)

    assertResult(0)(bogusElems.size)

    // xs:schema elements

    val xsSchemaElems = ops.filterChildElems(docElem, e => ops.resolvedName(e) == XsSchemaEName)
    assertResult(ops.findAllChildElems(docElem).filter(e => ops.resolvedName(e) == XsSchemaEName))(xsSchemaElems)

    assertResult(0)(xsSchemaElems.size)

    // xs:import elements

    val xsImportElems = ops.filterChildElems(docElem, e => ops.resolvedName(e) == XsImportEName)

    assertResult(7)(xsImportElems.size)
    assertResult(Set(XsImportEName))(xsImportElems.map(e => ops.resolvedName(e)).toSet)
    assertResult(ops.findAllChildElems(docElem).filter(e => ops.resolvedName(e) == XsImportEName))(xsImportElems)

    // link:linkbaseRef elements

    val linkbaseRefElems = ops.filterChildElems(docElem, e => ops.resolvedName(e) == LinkLinkbaseRefEName)

    assertResult(0)(linkbaseRefElems.size)

    // xs:appinfo child elements

    val appinfoChildElems =
      for {
        annotElem <- ops.filterChildElems(docElem, e => ops.resolvedName(e) == XsAnnotationEName)
        appinfoElem <- ops.filterChildElems(annotElem, e => ops.resolvedName(e) == XsAppinfoEName)
        appinfoChildElem <- ops.filterChildElems(appinfoElem, e => ops.parent(e) == appinfoElem)
      } yield appinfoChildElem

    assertResult(17)(appinfoChildElems.size)
    assertResult(Set(LinkLinkbaseRefEName))(appinfoChildElems.map(e => ops.resolvedName(e)).toSet)

    val appinfoChildElems2 =
      for {
        annotElem <- ops.findAllChildElems(docElem).filter(e => ops.resolvedName(e) == XsAnnotationEName)
        appinfoElem <- ops.findAllChildElems(annotElem).filter(e => ops.resolvedName(e) == XsAppinfoEName)
        appinfoChildElem <- ops.findAllChildElems(appinfoElem).filter(e => ops.parent(e) == appinfoElem)
      } yield appinfoChildElem

    assertResult(appinfoChildElems2)(appinfoChildElems)
  }

  @Test def testFindTopmostElemsOrSelf(): Unit = {
    // Non-existing elements

    val bogusElems = ops.findTopmostElemsOrSelf(docElem, e => ops.resolvedName(e) == EName("schema"))

    assertResult(0)(bogusElems.size)

    // xs:schema elements

    val xsSchemaElems = ops.findTopmostElemsOrSelf(docElem, e => ops.resolvedName(e) == XsSchemaEName)

    assertResult(1)(xsSchemaElems.size)
    assertResult(Set(XsSchemaEName))(xsSchemaElems.map(e => ops.resolvedName(e)).toSet)

    // xs:appinfo elements

    val appinfoElems =
      ops.findTopmostElemsOrSelf(docElem, e => ops.resolvedName(e) == XsAppinfoEName || ops.parentOption(e).exists(e => ops.resolvedName(e) == XsAppinfoEName))

    assertResult(1)(appinfoElems.size)

    val appinfoTreeElems =
      ops.filterElemsOrSelf(docElem, e => ops.resolvedName(e) == XsAppinfoEName || ops.parentOption(e).exists(e => ops.resolvedName(e) == XsAppinfoEName))

    assertResult(18)(appinfoTreeElems.size)

    assertResult(
      appinfoTreeElems.filter(e => ops.parentOption(e).forall(e2 => !appinfoTreeElems.contains(e2))))(appinfoElems)
  }

  @Test def testFindTopmostElems(): Unit = {
    // Non-existing elements

    val bogusElems = ops.findTopmostElems(docElem, e => ops.resolvedName(e) == EName("schema"))

    assertResult(0)(bogusElems.size)

    // xs:schema elements

    val xsSchemaElems = ops.findTopmostElems(docElem, e => ops.resolvedName(e) == XsSchemaEName)

    assertResult(0)(xsSchemaElems.size)

    // xs:appinfo elements

    val appinfoElems =
      ops.findTopmostElems(docElem, e => ops.resolvedName(e) == XsAppinfoEName || ops.parentOption(e).exists(e => ops.resolvedName(e) == XsAppinfoEName))

    assertResult(1)(appinfoElems.size)

    val appinfoTreeElems =
      ops.filterElems(docElem, e => ops.resolvedName(e) == XsAppinfoEName || ops.parentOption(e).exists(e => ops.resolvedName(e) == XsAppinfoEName))

    assertResult(18)(appinfoTreeElems.size)

    assertResult(
      appinfoTreeElems.filter(e => ops.parentOption(e).forall(e2 => !appinfoTreeElems.contains(e2))))(appinfoElems)
  }

  @Test def testFindElemOrSelf(): Unit = {
    // Non-existing elements

    val bogusElems = ops.findElemOrSelf(docElem, e => ops.resolvedName(e) == EName("schema"))

    assertResult(
      ops.filterElemsOrSelf(docElem, e => ops.resolvedName(e) == EName("schema")).headOption)(bogusElems)

    // xs:schema elements

    val xsSchemaElems = ops.findElemOrSelf(docElem, e => ops.resolvedName(e) == XsSchemaEName)

    assertResult(
      ops.filterElemsOrSelf(docElem, e => ops.resolvedName(e) == XsSchemaEName).headOption)(xsSchemaElems)

    // xs:import elements

    val xsImportElems = ops.findElemOrSelf(docElem, e => ops.resolvedName(e) == XsImportEName)

    assertResult(
      ops.filterElemsOrSelf(docElem, e => ops.resolvedName(e) == XsImportEName).headOption)(xsImportElems)

    // link:linkbaseRef elements

    val linkbaseRefElems = ops.findElemOrSelf(docElem, e => ops.resolvedName(e) == LinkLinkbaseRefEName)

    assertResult(
      ops.filterElemsOrSelf(docElem, e => ops.resolvedName(e) == LinkLinkbaseRefEName).headOption)(linkbaseRefElems)
  }

  @Test def testFindElem(): Unit = {
    // Non-existing elements

    val bogusElems = ops.findElem(docElem, e => ops.resolvedName(e) == EName("schema"))

    assertResult(
      ops.filterElems(docElem, e => ops.resolvedName(e) == EName("schema")).headOption)(bogusElems)

    // xs:schema elements

    val xsSchemaElems = ops.findElem(docElem, e => ops.resolvedName(e) == XsSchemaEName)

    assertResult(
      ops.filterElems(docElem, e => ops.resolvedName(e) == XsSchemaEName).headOption)(xsSchemaElems)

    // xs:import elements

    val xsImportElems = ops.findElem(docElem, e => ops.resolvedName(e) == XsImportEName)

    assertResult(
      ops.filterElems(docElem, e => ops.resolvedName(e) == XsImportEName).headOption)(xsImportElems)

    // link:linkbaseRef elements

    val linkbaseRefElems = ops.findElem(docElem, e => ops.resolvedName(e) == LinkLinkbaseRefEName)

    assertResult(
      ops.filterElems(docElem, e => ops.resolvedName(e) == LinkLinkbaseRefEName).headOption)(linkbaseRefElems)
  }

  @Test def testPathNavigation(): Unit = {
    val linkbaseRefElems = ops.filterElemsOrSelf(docElem, e => ops.resolvedName(e) == LinkLinkbaseRefEName)

    val linkbaseRefElemPaths = linkbaseRefElems.map(e => ops.path(e))

    assertResult(17)(linkbaseRefElemPaths.size)

    assertResult((0 to 16).map(i => Path.Entry(LinkLinkbaseRefEName, i)).toSet) {
      linkbaseRefElemPaths.flatMap(_.lastEntryOption).toSet
    }

    val expectedParentPath =
      PathBuilder.from(QName("xs:annotation") -> 0, QName("xs:appinfo") -> 0).build(Scope.from("xs" -> XsNamespace))

    assertResult(Set(expectedParentPath))(linkbaseRefElemPaths.flatMap(_.parentPathOption).toSet)

    val linkbaseRefElems2 = linkbaseRefElems.flatMap(e => ops.findElemOrSelfByPath(ops.rootElem(e), ops.path(e)))

    assertResult(linkbaseRefElems.size)(linkbaseRefElems2.size)
    assertResult(linkbaseRefElems)(linkbaseRefElems2)
  }

  @Test def testAncestry(): Unit = {
    val linkbaseRefElems = ops.filterElemsOrSelf(docElem, e => ops.resolvedName(e) == LinkLinkbaseRefEName)

    assertResult(17)(linkbaseRefElems.size)

    assertResult(Set(List(XsSchemaEName, XsAnnotationEName, XsAppinfoEName))) {
      linkbaseRefElems.map(e => ops.reverseAncestryENames(e)).toSet
    }

    assertResult(Set(List(XsSchemaEName, XsAnnotationEName, XsAppinfoEName, LinkLinkbaseRefEName))) {
      linkbaseRefElems.map(e => ops.reverseAncestryOrSelfENames(e)).toSet
    }

    assertResult(linkbaseRefElems.map(e => ops.ancestors(e).map(e => ops.resolvedName(e)).reverse).toSet) {
      linkbaseRefElems.map(e => ops.reverseAncestryENames(e)).toSet
    }

    assertResult(linkbaseRefElems.map(e => ops.ancestorsOrSelf(e).map(e => ops.resolvedName(e)).reverse).toSet) {
      linkbaseRefElems.map(e => ops.reverseAncestryOrSelfENames(e)).toSet
    }

    assertResult(linkbaseRefElems) {
      linkbaseRefElems.map(e => ops.getChildElemByPathEntry(ops.parent(e), ops.path(e).lastEntry))
    }

    assertResult(linkbaseRefElems.map(e => ops.reverseAncestryOrSelfENames(e)).toSet) {
      linkbaseRefElems.map(e => ops.reverseAncestryOrSelf(e).map(e => ops.resolvedName(e))).toSet
    }

    assertResult(ops.filterElems(docElem, e => ops.resolvedName(e) == XsAppinfoEName).toSet) {
      linkbaseRefElems.map(e => ops.parent(e)).toSet
    }
    assertResult(ops.filterElems(docElem, e => ops.resolvedName(e) == XsAppinfoEName).toSet) {
      linkbaseRefElems.flatMap(e => ops.parentOption(e)).toSet
    }

    assertResult(Set(docElem)) {
      linkbaseRefElems.flatMap(e => ops.findAncestor(e, e2 => ops.resolvedName(e2) == XsSchemaEName)).toSet
    }
    assertResult(Set(docElem)) {
      linkbaseRefElems.flatMap(e => ops.findAncestorOrSelf(e, e2 => ops.resolvedName(e2) == XsSchemaEName)).toSet
    }

    assertResult(0) {
      linkbaseRefElems.flatMap(e => ops.findAncestor(e, e2 => ops.resolvedName(e2) == LinkLinkbaseRefEName)).size
    }
    assertResult(linkbaseRefElems) {
      linkbaseRefElems.flatMap(e => ops.findAncestorOrSelf(e, e2 => ops.resolvedName(e2) == LinkLinkbaseRefEName))
    }
  }

  @Test def testPathConsistency(): Unit = {
    val allElemsOrSelf = ops.findAllElemsOrSelf(docElem)

    assertResult(Set(docElem)) {
      allElemsOrSelf.map(e => ops.rootElem(e)).toSet
    }
    assertResult(allElemsOrSelf) {
      allElemsOrSelf.map(e => ops.getElemOrSelfByPath(ops.rootElem(e), ops.path(e)))
    }

    assertResult(allElemsOrSelf) {
      allElemsOrSelf.map(e => ops.parentOption(e).flatMap(e2 => ops.findChildElemByPathEntry(e2, ops.path(e).lastEntry)).getOrElse(docElem))
    }
  }
}
