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

  val ops: BackingElemFunctionApi.Aux[E]

  // Stable identifier, so we can import all members of ops
  import ops._

  @Test def testResolvedName(): Unit = {
    assertResult(XsSchemaEName)(resolvedName(docElem))
    assertResult(Some(XsNamespace))(resolvedName(docElem).namespaceUriOption)
    assertResult("schema")(resolvedName(docElem).localPart)
  }

  @Test def testQName(): Unit = {
    assertResult(QName("xs:schema"))(qname(docElem))
    assertResult(Some("xs"))(qname(docElem).prefixOption)
    assertResult("schema")(qname(docElem).localPart)
  }

  @Test def testDocUri(): Unit = {
    assertResult("file")(docUri(docElem).getScheme)
    assertResult("some-data.xsd")((new File(docUri(docElem))).getName)
    assertResult(true)((new File(docUri(docElem))).isFile)
    assertResult(Some(docUri(docElem)))(docUriOption(docElem))

    assertResult(Set(docUri(docElem))) {
      findAllElemsOrSelf(docElem).map(e => docUri(e)).toSet
    }
    assertResult(Set(Some(docUri(docElem)))) {
      findAllElemsOrSelf(docElem).map(e => docUriOption(e)).toSet
    }
  }

  @Test def testDefaultBaseUri(): Unit = {
    assertResult("file")(baseUri(docElem).getScheme)
    assertResult("some-data.xsd")((new File(baseUri(docElem))).getName)
    assertResult(true)((new File(baseUri(docElem))).isFile)
    assertResult(Some(baseUri(docElem)))(baseUriOption(docElem))

    assertResult(Set(baseUri(docElem))) {
      findAllElemsOrSelf(docElem).map(e => baseUri(e)).toSet
    }
    assertResult(Set(Some(baseUri(docElem)))) {
      findAllElemsOrSelf(docElem).map(e => baseUriOption(e)).toSet
    }
  }

  @Test def testResolvedAttributes(): Unit = {
    assertResult(
      Map(
        TargetNamespaceEName -> "http://www.sometaxonomy/0.1/basis/some2/items/some-data",
        AttributeFormDefaultEName -> "unqualified",
        ElementFormDefaultEName -> "qualified")) {

        resolvedAttributes(docElem).toMap
      }

    val linkbaseRefElems = filterElems(docElem, e => resolvedName(e) == LinkLinkbaseRefEName)

    assertResult(Set(Some("simple"))) {
      linkbaseRefElems.map(e => attributeOption(e, EName(XLinkNamespace, "type"))).toSet
    }
    assertResult(Set("simple")) {
      linkbaseRefElems.map(e => attribute(e, EName(XLinkNamespace, "type"))).toSet
    }
  }

  @Test def testQNameAttributes(): Unit = {
    val elementElems = filterElemsOrSelf(docElem, e => resolvedName(e) == XsElementEName)

    assertResult(true)(elementElems.size >= 100)

    assertResult(Set(QName("xbrli:item"))) {
      elementElems.flatMap(e => attributeAsQNameOption(e, SubstitutionGroupEName)).toSet
    }
    assertResult(Set(QName("xbrli:item"))) {
      elementElems.map(e => attributeAsQName(e, SubstitutionGroupEName)).toSet
    }

    assertResult(Set(XbrliItemEName)) {
      elementElems.flatMap(e => attributeAsResolvedQNameOption(e, SubstitutionGroupEName)).toSet
    }
    assertResult(Set(XbrliItemEName)) {
      elementElems.map(e => attributeAsResolvedQName(e, SubstitutionGroupEName)).toSet
    }
  }

  @Test def testAttributes(): Unit = {
    assertResult(
      Map(
        QName("targetNamespace") -> "http://www.sometaxonomy/0.1/basis/some2/items/some-data",
        QName("attributeFormDefault") -> "unqualified",
        QName("elementFormDefault") -> "qualified")) {

        attributes(docElem).toMap
      }

    val linkbaseRefElems = filterElems(docElem, e => resolvedName(e) == LinkLinkbaseRefEName)

    assertResult(Set(Some("simple"))) {
      linkbaseRefElems.map(e => findAttributeByLocalName(e, "type")).toSet
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

        scope(docElem)
      }

    assertResult(Set(scope(docElem))) {
      findAllElemsOrSelf(docElem).map(e => scope(e)).toSet
    }

    assertResult(Declarations.from(scope(docElem).prefixNamespaceMap.toIndexedSeq: _*)) {
      namespaces(docElem)
    }
    assertResult(Set(Declarations.from())) {
      findAllElems(docElem).map(e => namespaces(e)).toSet
    }
  }

  @Test def testText(): Unit = {
    assertResult(Set("")) {
      findAllElemsOrSelf(docElem).map(e => text(e).trim).toSet
    }
    assertResult(true) {
      findAllElemsOrSelf(docElem).map(e => text(e)).filter(_.nonEmpty).nonEmpty
    }
    assertResult(findAllElemsOrSelf(docElem).map(e => trimmedText(e))) {
      findAllElemsOrSelf(docElem).map(e => text(e).trim)
    }
    assertResult(findAllElemsOrSelf(docElem).map(e => normalizedText(e))) {
      findAllElemsOrSelf(docElem).map(e => text(e).trim)
    }
  }

  @Test def testFilterElemsOrSelf(): Unit = {
    // Non-existing elements

    val bogusElems = filterElemsOrSelf(docElem, e => resolvedName(e) == EName("schema"))

    assertResult(0)(bogusElems.size)
    assertResult(findAllElemsOrSelf(docElem).filter(e => resolvedName(e) == EName("schema")))(bogusElems)

    // xs:schema elements

    val xsSchemaElems = filterElemsOrSelf(docElem, e => resolvedName(e) == XsSchemaEName)

    assertResult(1)(xsSchemaElems.size)
    assertResult(Set(XsSchemaEName)) {
      xsSchemaElems.map(e => resolvedName(e)).toSet
    }
    assertResult(findAllElemsOrSelf(docElem).filter(e => resolvedName(e) == XsSchemaEName)) {
      xsSchemaElems
    }

    // xs:import elements

    val xsImportElems = filterElemsOrSelf(docElem, e => resolvedName(e) == XsImportEName)

    assertResult(7)(xsImportElems.size)
    assertResult(Set(XsImportEName)) {
      xsImportElems.map(e => resolvedName(e)).toSet
    }
    assertResult(findAllElemsOrSelf(docElem).filter(e => resolvedName(e) == XsImportEName)) {
      xsImportElems
    }

    // link:linkbaseRef elements

    val linkbaseRefElems = filterElemsOrSelf(docElem, e => resolvedName(e) == LinkLinkbaseRefEName)

    assertResult(17)(linkbaseRefElems.size)
    assertResult(Set(LinkLinkbaseRefEName)) {
      linkbaseRefElems.map(e => resolvedName(e)).toSet
    }
    assertResult(findAllElemsOrSelf(docElem).filter(e => resolvedName(e) == LinkLinkbaseRefEName)) {
      linkbaseRefElems
    }

    // xs:appinfo child elements

    val appinfoChildElems =
      for {
        annotElem <- filterElemsOrSelf(docElem, e => resolvedName(e) == XsAnnotationEName)
        appinfoElem <- filterElemsOrSelf(annotElem, e => resolvedName(e) == XsAppinfoEName)
        appinfoChildElem <- filterElemsOrSelf(appinfoElem, e => parent(e) == appinfoElem)
      } yield appinfoChildElem

    assertResult(17)(appinfoChildElems.size)
    assertResult(Set(LinkLinkbaseRefEName)) {
      appinfoChildElems.map(e => resolvedName(e)).toSet
    }

    val appinfoChildElems2 =
      for {
        annotElem <- findAllElemsOrSelf(docElem).filter(e => resolvedName(e) == XsAnnotationEName)
        appinfoElem <- findAllElemsOrSelf(annotElem).filter(e => resolvedName(e) == XsAppinfoEName)
        appinfoChildElem <- findAllElemsOrSelf(appinfoElem).filter(e => parent(e) == appinfoElem)
      } yield appinfoChildElem

    assertResult(appinfoChildElems2)(appinfoChildElems)
  }

  @Test def testFilterElems(): Unit = {
    // Non-existing elements

    val bogusElems = filterElems(docElem, e => resolvedName(e) == EName("schema"))
    assertResult(findAllElems(docElem).filter(e => resolvedName(e) == EName("schema")))(bogusElems)

    assertResult(0)(bogusElems.size)

    // xs:schema elements

    val xsSchemaElems = filterElems(docElem, e => resolvedName(e) == XsSchemaEName)
    assertResult(findAllElems(docElem).filter(e => resolvedName(e) == XsSchemaEName))(xsSchemaElems)

    assertResult(0)(xsSchemaElems.size)

    // xs:import elements

    val xsImportElems = filterElems(docElem, e => resolvedName(e) == XsImportEName)

    assertResult(7)(xsImportElems.size)
    assertResult(Set(XsImportEName))(xsImportElems.map(e => resolvedName(e)).toSet)
    assertResult(findAllElems(docElem).filter(e => resolvedName(e) == XsImportEName))(xsImportElems)

    // link:linkbaseRef elements

    val linkbaseRefElems = filterElems(docElem, e => resolvedName(e) == LinkLinkbaseRefEName)

    assertResult(17)(linkbaseRefElems.size)
    assertResult(Set(LinkLinkbaseRefEName))(linkbaseRefElems.map(e => resolvedName(e)).toSet)
    assertResult(findAllElems(docElem).filter(e => resolvedName(e) == LinkLinkbaseRefEName))(linkbaseRefElems)

    // xs:appinfo child elements

    val appinfoChildElems =
      for {
        annotElem <- filterElems(docElem, e => resolvedName(e) == XsAnnotationEName)
        appinfoElem <- filterElems(annotElem, e => resolvedName(e) == XsAppinfoEName)
        appinfoChildElem <- filterElems(appinfoElem, e => parent(e) == appinfoElem)
      } yield appinfoChildElem

    assertResult(17)(appinfoChildElems.size)
    assertResult(Set(LinkLinkbaseRefEName))(appinfoChildElems.map(e => resolvedName(e)).toSet)

    val appinfoChildElems2 =
      for {
        annotElem <- findAllElems(docElem).filter(e => resolvedName(e) == XsAnnotationEName)
        appinfoElem <- findAllElems(annotElem).filter(e => resolvedName(e) == XsAppinfoEName)
        appinfoChildElem <- findAllElems(appinfoElem).filter(e => parent(e) == appinfoElem)
      } yield appinfoChildElem

    assertResult(appinfoChildElems2)(appinfoChildElems)
  }

  @Test def testFilterChildElems(): Unit = {
    // Non-existing elements

    val bogusElems = filterChildElems(docElem, (e => resolvedName(e) == EName("schema")))
    assertResult(findAllChildElems(docElem).filter(e => resolvedName(e) == EName("schema")))(bogusElems)

    assertResult(0)(bogusElems.size)

    // xs:schema elements

    val xsSchemaElems = filterChildElems(docElem, e => resolvedName(e) == XsSchemaEName)
    assertResult(findAllChildElems(docElem).filter(e => resolvedName(e) == XsSchemaEName))(xsSchemaElems)

    assertResult(0)(xsSchemaElems.size)

    // xs:import elements

    val xsImportElems = filterChildElems(docElem, e => resolvedName(e) == XsImportEName)

    assertResult(7)(xsImportElems.size)
    assertResult(Set(XsImportEName))(xsImportElems.map(e => resolvedName(e)).toSet)
    assertResult(findAllChildElems(docElem).filter(e => resolvedName(e) == XsImportEName))(xsImportElems)

    // link:linkbaseRef elements

    val linkbaseRefElems = filterChildElems(docElem, e => resolvedName(e) == LinkLinkbaseRefEName)

    assertResult(0)(linkbaseRefElems.size)

    // xs:appinfo child elements

    val appinfoChildElems =
      for {
        annotElem <- filterChildElems(docElem, e => resolvedName(e) == XsAnnotationEName)
        appinfoElem <- filterChildElems(annotElem, e => resolvedName(e) == XsAppinfoEName)
        appinfoChildElem <- filterChildElems(appinfoElem, e => parent(e) == appinfoElem)
      } yield appinfoChildElem

    assertResult(17)(appinfoChildElems.size)
    assertResult(Set(LinkLinkbaseRefEName))(appinfoChildElems.map(e => resolvedName(e)).toSet)

    val appinfoChildElems2 =
      for {
        annotElem <- findAllChildElems(docElem).filter(e => resolvedName(e) == XsAnnotationEName)
        appinfoElem <- findAllChildElems(annotElem).filter(e => resolvedName(e) == XsAppinfoEName)
        appinfoChildElem <- findAllChildElems(appinfoElem).filter(e => parent(e) == appinfoElem)
      } yield appinfoChildElem

    assertResult(appinfoChildElems2)(appinfoChildElems)
  }

  @Test def testFindTopmostElemsOrSelf(): Unit = {
    // Non-existing elements

    val bogusElems = findTopmostElemsOrSelf(docElem, e => resolvedName(e) == EName("schema"))

    assertResult(0)(bogusElems.size)

    // xs:schema elements

    val xsSchemaElems = findTopmostElemsOrSelf(docElem, e => resolvedName(e) == XsSchemaEName)

    assertResult(1)(xsSchemaElems.size)
    assertResult(Set(XsSchemaEName))(xsSchemaElems.map(e => resolvedName(e)).toSet)

    // xs:appinfo elements

    val appinfoElems =
      findTopmostElemsOrSelf(docElem, e => resolvedName(e) == XsAppinfoEName || parentOption(e).exists(e => resolvedName(e) == XsAppinfoEName))

    assertResult(1)(appinfoElems.size)

    val appinfoTreeElems =
      filterElemsOrSelf(docElem, e => resolvedName(e) == XsAppinfoEName || parentOption(e).exists(e => resolvedName(e) == XsAppinfoEName))

    assertResult(18)(appinfoTreeElems.size)

    assertResult(
      appinfoTreeElems.filter(e => parentOption(e).forall(e2 => !appinfoTreeElems.contains(e2))))(appinfoElems)
  }

  @Test def testFindTopmostElems(): Unit = {
    // Non-existing elements

    val bogusElems = findTopmostElems(docElem, e => resolvedName(e) == EName("schema"))

    assertResult(0)(bogusElems.size)

    // xs:schema elements

    val xsSchemaElems = findTopmostElems(docElem, e => resolvedName(e) == XsSchemaEName)

    assertResult(0)(xsSchemaElems.size)

    // xs:appinfo elements

    val appinfoElems =
      findTopmostElems(docElem, e => resolvedName(e) == XsAppinfoEName || parentOption(e).exists(e => resolvedName(e) == XsAppinfoEName))

    assertResult(1)(appinfoElems.size)

    val appinfoTreeElems =
      filterElems(docElem, e => resolvedName(e) == XsAppinfoEName || parentOption(e).exists(e => resolvedName(e) == XsAppinfoEName))

    assertResult(18)(appinfoTreeElems.size)

    assertResult(
      appinfoTreeElems.filter(e => parentOption(e).forall(e2 => !appinfoTreeElems.contains(e2))))(appinfoElems)
  }

  @Test def testFindElemOrSelf(): Unit = {
    // Non-existing elements

    val bogusElems = findElemOrSelf(docElem, e => resolvedName(e) == EName("schema"))

    assertResult(
      filterElemsOrSelf(docElem, e => resolvedName(e) == EName("schema")).headOption)(bogusElems)

    // xs:schema elements

    val xsSchemaElems = findElemOrSelf(docElem, e => resolvedName(e) == XsSchemaEName)

    assertResult(
      filterElemsOrSelf(docElem, e => resolvedName(e) == XsSchemaEName).headOption)(xsSchemaElems)

    // xs:import elements

    val xsImportElems = findElemOrSelf(docElem, e => resolvedName(e) == XsImportEName)

    assertResult(
      filterElemsOrSelf(docElem, e => resolvedName(e) == XsImportEName).headOption)(xsImportElems)

    // link:linkbaseRef elements

    val linkbaseRefElems = findElemOrSelf(docElem, e => resolvedName(e) == LinkLinkbaseRefEName)

    assertResult(
      filterElemsOrSelf(docElem, e => resolvedName(e) == LinkLinkbaseRefEName).headOption)(linkbaseRefElems)
  }

  @Test def testFindElem(): Unit = {
    // Non-existing elements

    val bogusElems = findElem(docElem, e => resolvedName(e) == EName("schema"))

    assertResult(
      filterElems(docElem, e => resolvedName(e) == EName("schema")).headOption)(bogusElems)

    // xs:schema elements

    val xsSchemaElems = findElem(docElem, e => resolvedName(e) == XsSchemaEName)

    assertResult(
      filterElems(docElem, e => resolvedName(e) == XsSchemaEName).headOption)(xsSchemaElems)

    // xs:import elements

    val xsImportElems = findElem(docElem, e => resolvedName(e) == XsImportEName)

    assertResult(
      filterElems(docElem, e => resolvedName(e) == XsImportEName).headOption)(xsImportElems)

    // link:linkbaseRef elements

    val linkbaseRefElems = findElem(docElem, e => resolvedName(e) == LinkLinkbaseRefEName)

    assertResult(
      filterElems(docElem, e => resolvedName(e) == LinkLinkbaseRefEName).headOption)(linkbaseRefElems)
  }

  @Test def testPathNavigation(): Unit = {
    val linkbaseRefElems = filterElemsOrSelf(docElem, e => resolvedName(e) == LinkLinkbaseRefEName)

    val linkbaseRefElemPaths = linkbaseRefElems.map(e => path(e))

    assertResult(17)(linkbaseRefElemPaths.size)

    assertResult((0 to 16).map(i => Path.Entry(LinkLinkbaseRefEName, i)).toSet) {
      linkbaseRefElemPaths.flatMap(_.lastEntryOption).toSet
    }

    val expectedParentPath =
      PathBuilder.from(QName("xs:annotation") -> 0, QName("xs:appinfo") -> 0).build(Scope.from("xs" -> XsNamespace))

    assertResult(Set(expectedParentPath))(linkbaseRefElemPaths.flatMap(_.parentPathOption).toSet)

    val linkbaseRefElems2 = linkbaseRefElems.flatMap(e => findElemOrSelfByPath(rootElem(e), path(e)))

    assertResult(linkbaseRefElems.size)(linkbaseRefElems2.size)
    assertResult(linkbaseRefElems)(linkbaseRefElems2)
  }

  @Test def testAncestry(): Unit = {
    val linkbaseRefElems = filterElemsOrSelf(docElem, e => resolvedName(e) == LinkLinkbaseRefEName)

    assertResult(17)(linkbaseRefElems.size)

    assertResult(Set(List(XsSchemaEName, XsAnnotationEName, XsAppinfoEName))) {
      linkbaseRefElems.map(e => reverseAncestryENames(e)).toSet
    }

    assertResult(Set(List(XsSchemaEName, XsAnnotationEName, XsAppinfoEName, LinkLinkbaseRefEName))) {
      linkbaseRefElems.map(e => reverseAncestryOrSelfENames(e)).toSet
    }

    assertResult(linkbaseRefElems.map(e => ancestors(e).map(e => resolvedName(e)).reverse).toSet) {
      linkbaseRefElems.map(e => reverseAncestryENames(e)).toSet
    }

    assertResult(linkbaseRefElems.map(e => ancestorsOrSelf(e).map(e => resolvedName(e)).reverse).toSet) {
      linkbaseRefElems.map(e => reverseAncestryOrSelfENames(e)).toSet
    }

    assertResult(linkbaseRefElems) {
      linkbaseRefElems.map(e => getChildElemByPathEntry(parent(e), path(e).lastEntry))
    }

    assertResult(linkbaseRefElems.map(e => reverseAncestryOrSelfENames(e)).toSet) {
      linkbaseRefElems.map(e => reverseAncestryOrSelf(e).map(e => resolvedName(e))).toSet
    }

    assertResult(filterElems(docElem, e => resolvedName(e) == XsAppinfoEName).toSet) {
      linkbaseRefElems.map(e => parent(e)).toSet
    }
    assertResult(filterElems(docElem, e => resolvedName(e) == XsAppinfoEName).toSet) {
      linkbaseRefElems.flatMap(e => parentOption(e)).toSet
    }

    assertResult(Set(docElem)) {
      linkbaseRefElems.flatMap(e => findAncestor(e, e2 => resolvedName(e2) == XsSchemaEName)).toSet
    }
    assertResult(Set(docElem)) {
      linkbaseRefElems.flatMap(e => findAncestorOrSelf(e, e2 => resolvedName(e2) == XsSchemaEName)).toSet
    }

    assertResult(0) {
      linkbaseRefElems.flatMap(e => findAncestor(e, e2 => resolvedName(e2) == LinkLinkbaseRefEName)).size
    }
    assertResult(linkbaseRefElems) {
      linkbaseRefElems.flatMap(e => findAncestorOrSelf(e, e2 => resolvedName(e2) == LinkLinkbaseRefEName))
    }
  }

  @Test def testPathConsistency(): Unit = {
    val allElemsOrSelf = findAllElemsOrSelf(docElem)

    assertResult(Set(docElem)) {
      allElemsOrSelf.map(e => rootElem(e)).toSet
    }
    assertResult(allElemsOrSelf) {
      allElemsOrSelf.map(e => getElemOrSelfByPath(rootElem(e), path(e)))
    }

    assertResult(allElemsOrSelf) {
      allElemsOrSelf.map(e => parentOption(e).flatMap(e2 => findChildElemByPathEntry(e2, path(e).lastEntry)).getOrElse(docElem))
    }
  }
}
