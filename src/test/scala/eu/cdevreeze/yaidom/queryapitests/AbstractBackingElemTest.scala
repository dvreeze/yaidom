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
import eu.cdevreeze.yaidom.queryapi.BackingElemApi

abstract class AbstractBackingElemTest extends Suite {

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

  // Nice, just the "raw" type without generics. There is a small price, though, and that is that some lambdas need explicit parameter types.
  // That is the case because BackingElemApi "overrides" some super-trait methods, restricting the ThisElem type member to the one of BackingElemApi.
  type E = BackingElemApi

  def docElem: E

  @Test def testResolvedName(): Unit = {
    assertResult(XsSchemaEName)(docElem.resolvedName)
    assertResult(Some(XsNamespace))(docElem.resolvedName.namespaceUriOption)
    assertResult("schema")(docElem.resolvedName.localPart)
  }

  @Test def testQName(): Unit = {
    assertResult(QName("xs:schema"))(docElem.qname)
    assertResult(Some("xs"))(docElem.qname.prefixOption)
    assertResult("schema")(docElem.qname.localPart)
  }

  @Test def testDocUri(): Unit = {
    assertResult("file")(docElem.docUri.getScheme)
    assertResult("some-data.xsd")((new File(docElem.docUri)).getName)
    assertResult(true)((new File(docElem.docUri)).isFile)
    assertResult(Some(docElem.docUri))(docElem.docUriOption)

    assertResult(Set(docElem.docUri)) {
      docElem.findAllElemsOrSelf.map(_.docUri).toSet
    }
    assertResult(Set(Some(docElem.docUri))) {
      docElem.findAllElemsOrSelf.map(_.docUriOption).toSet
    }
  }

  @Test def testDefaultBaseUri(): Unit = {
    assertResult("file")(docElem.baseUri.getScheme)
    assertResult("some-data.xsd")((new File(docElem.baseUri)).getName)
    assertResult(true)((new File(docElem.baseUri)).isFile)
    assertResult(Some(docElem.baseUri))(docElem.baseUriOption)

    assertResult(Set(docElem.baseUri)) {
      docElem.findAllElemsOrSelf.map(_.baseUri).toSet
    }
    assertResult(Set(Some(docElem.baseUri))) {
      docElem.findAllElemsOrSelf.map(_.baseUriOption).toSet
    }
  }

  @Test def testResolvedAttributes(): Unit = {
    assertResult(
      Map(
        TargetNamespaceEName -> "http://www.sometaxonomy/0.1/basis/some2/items/some-data",
        AttributeFormDefaultEName -> "unqualified",
        ElementFormDefaultEName -> "qualified")) {

        docElem.resolvedAttributes.toMap
      }

    val linkbaseRefElems = docElem.filterElems(_.resolvedName == LinkLinkbaseRefEName)

    assertResult(Set(Some("simple"))) {
      linkbaseRefElems.map(_.attributeOption(EName(XLinkNamespace, "type"))).toSet
    }
    assertResult(Set(Some("simple"))) {
      linkbaseRefElems.map(_ \@ EName(XLinkNamespace, "type")).toSet
    }
    assertResult(Set("simple")) {
      linkbaseRefElems.map(_.attribute(EName(XLinkNamespace, "type"))).toSet
    }
  }

  @Test def testQNameAttributes(): Unit = {
    val elementElems = docElem.filterElemsOrSelf(_.resolvedName == XsElementEName)

    assertResult(true)(elementElems.size >= 100)

    assertResult(Set(QName("xbrli:item"))) {
      elementElems.flatMap(_.attributeAsQNameOption(SubstitutionGroupEName)).toSet
    }
    assertResult(Set(QName("xbrli:item"))) {
      elementElems.map(_.attributeAsQName(SubstitutionGroupEName)).toSet
    }

    assertResult(Set(XbrliItemEName)) {
      elementElems.flatMap(_.attributeAsResolvedQNameOption(SubstitutionGroupEName)).toSet
    }
    assertResult(Set(XbrliItemEName)) {
      elementElems.map(_.attributeAsResolvedQName(SubstitutionGroupEName)).toSet
    }
  }

  @Test def testAttributes(): Unit = {
    assertResult(
      Map(
        QName("targetNamespace") -> "http://www.sometaxonomy/0.1/basis/some2/items/some-data",
        QName("attributeFormDefault") -> "unqualified",
        QName("elementFormDefault") -> "qualified")) {

        docElem.attributes.toMap
      }

    val linkbaseRefElems = docElem.filterElems(_.resolvedName == LinkLinkbaseRefEName)

    assertResult(Set(Some("simple"))) {
      linkbaseRefElems.map(_.findAttributeByLocalName("type")).toSet
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

        docElem.scope
      }

    assertResult(Set(docElem.scope)) {
      docElem.findAllElemsOrSelf.map(_.scope).toSet
    }

    assertResult(Declarations.from(docElem.scope.prefixNamespaceMap.toIndexedSeq: _*)) {
      docElem.namespaces
    }
    assertResult(Set(Declarations.from())) {
      docElem.findAllElems.map(_.namespaces).toSet
    }
  }

  @Test def testText(): Unit = {
    assertResult(Set("")) {
      docElem.findAllElemsOrSelf.map(_.text.trim).toSet
    }
    assertResult(true) {
      docElem.findAllElemsOrSelf.map(_.text).filter(_.nonEmpty).nonEmpty
    }
    assertResult(docElem.findAllElemsOrSelf.map(_.trimmedText)) {
      docElem.findAllElemsOrSelf.map(_.text.trim)
    }
    assertResult(docElem.findAllElemsOrSelf.map(_.normalizedText)) {
      docElem.findAllElemsOrSelf.map(_.text.trim)
    }
  }

  @Test def testFilterElemsOrSelf(): Unit = {
    // Non-existing elements

    val bogusElems = docElem.filterElemsOrSelf(_.resolvedName == EName("schema"))

    assertResult(0)(bogusElems.size)
    assertResult(docElem.findAllElemsOrSelf.filter(_.resolvedName == EName("schema")))(bogusElems)

    // xs:schema elements

    val xsSchemaElems = docElem.filterElemsOrSelf(_.resolvedName == XsSchemaEName)

    assertResult(1)(xsSchemaElems.size)
    assertResult(Set(XsSchemaEName)) {
      xsSchemaElems.map(_.resolvedName).toSet
    }
    assertResult(docElem.findAllElemsOrSelf.filter(_.resolvedName == XsSchemaEName)) {
      xsSchemaElems
    }

    // xs:import elements

    val xsImportElems = docElem.filterElemsOrSelf(_.resolvedName == XsImportEName)

    assertResult(7)(xsImportElems.size)
    assertResult(Set(XsImportEName)) {
      xsImportElems.map(_.resolvedName).toSet
    }
    assertResult(docElem.findAllElemsOrSelf.filter(_.resolvedName == XsImportEName)) {
      xsImportElems
    }

    // link:linkbaseRef elements

    val linkbaseRefElems = docElem.filterElemsOrSelf(_.resolvedName == LinkLinkbaseRefEName)

    assertResult(17)(linkbaseRefElems.size)
    assertResult(Set(LinkLinkbaseRefEName)) {
      linkbaseRefElems.map(_.resolvedName).toSet
    }
    assertResult(docElem.findAllElemsOrSelf.filter(_.resolvedName == LinkLinkbaseRefEName)) {
      linkbaseRefElems
    }

    // xs:appinfo child elements

    val appinfoChildElems =
      for {
        annotElem <- docElem.filterElemsOrSelf(_.resolvedName == XsAnnotationEName)
        appinfoElem <- annotElem.filterElemsOrSelf(_.resolvedName == XsAppinfoEName)
        appinfoChildElem <- appinfoElem.filterElemsOrSelf((e: E) => e.parent == appinfoElem)
      } yield appinfoChildElem

    assertResult(17)(appinfoChildElems.size)
    assertResult(Set(LinkLinkbaseRefEName)) {
      appinfoChildElems.map(_.resolvedName).toSet
    }

    val appinfoChildElems2 =
      for {
        annotElem <- docElem.findAllElemsOrSelf.filter(_.resolvedName == XsAnnotationEName)
        appinfoElem <- annotElem.findAllElemsOrSelf.filter(_.resolvedName == XsAppinfoEName)
        appinfoChildElem <- appinfoElem.findAllElemsOrSelf.filter(_.parent == appinfoElem)
      } yield appinfoChildElem

    assertResult(appinfoChildElems2)(appinfoChildElems)
  }

  @Test def testFilterElems(): Unit = {
    // Non-existing elements

    val bogusElems = docElem.filterElems(_.resolvedName == EName("schema"))
    assertResult(docElem.findAllElems.filter(_.resolvedName == EName("schema")))(bogusElems)

    assertResult(0)(bogusElems.size)

    // xs:schema elements

    val xsSchemaElems = docElem.filterElems(_.resolvedName == XsSchemaEName)
    assertResult(docElem.findAllElems.filter(_.resolvedName == XsSchemaEName))(xsSchemaElems)

    assertResult(0)(xsSchemaElems.size)

    // xs:import elements

    val xsImportElems = docElem.filterElems(_.resolvedName == XsImportEName)

    assertResult(7)(xsImportElems.size)
    assertResult(Set(XsImportEName))(xsImportElems.map(_.resolvedName).toSet)
    assertResult(docElem.findAllElems.filter(_.resolvedName == XsImportEName))(xsImportElems)

    // link:linkbaseRef elements

    val linkbaseRefElems = docElem.filterElems(_.resolvedName == LinkLinkbaseRefEName)

    assertResult(17)(linkbaseRefElems.size)
    assertResult(Set(LinkLinkbaseRefEName))(linkbaseRefElems.map(_.resolvedName).toSet)
    assertResult(docElem.findAllElems.filter(_.resolvedName == LinkLinkbaseRefEName))(linkbaseRefElems)

    // xs:appinfo child elements

    val appinfoChildElems =
      for {
        annotElem <- docElem.filterElems(_.resolvedName == XsAnnotationEName)
        appinfoElem <- annotElem.filterElems(_.resolvedName == XsAppinfoEName)
        appinfoChildElem <- appinfoElem.filterElems((e: E) => e.parent == appinfoElem)
      } yield appinfoChildElem

    assertResult(17)(appinfoChildElems.size)
    assertResult(Set(LinkLinkbaseRefEName))(appinfoChildElems.map(_.resolvedName).toSet)

    val appinfoChildElems2 =
      for {
        annotElem <- docElem.findAllElems.filter(_.resolvedName == XsAnnotationEName)
        appinfoElem <- annotElem.findAllElems.filter(_.resolvedName == XsAppinfoEName)
        appinfoChildElem <- appinfoElem.findAllElems.filter(_.parent == appinfoElem)
      } yield appinfoChildElem

    assertResult(appinfoChildElems2)(appinfoChildElems)
  }

  @Test def testFilterChildElems(): Unit = {
    // Non-existing elements

    val bogusElems = docElem.filterChildElems(_.resolvedName == EName("schema"))
    assertResult(docElem.findAllChildElems.filter(_.resolvedName == EName("schema")))(bogusElems)

    assertResult(0)(bogusElems.size)

    // xs:schema elements

    val xsSchemaElems = docElem.filterChildElems(_.resolvedName == XsSchemaEName)
    assertResult(docElem.findAllChildElems.filter(_.resolvedName == XsSchemaEName))(xsSchemaElems)

    assertResult(0)(xsSchemaElems.size)

    // xs:import elements

    val xsImportElems = docElem.filterChildElems(_.resolvedName == XsImportEName)

    assertResult(7)(xsImportElems.size)
    assertResult(Set(XsImportEName))(xsImportElems.map(_.resolvedName).toSet)
    assertResult(docElem.findAllChildElems.filter(_.resolvedName == XsImportEName))(xsImportElems)

    // link:linkbaseRef elements

    val linkbaseRefElems = docElem.filterChildElems(_.resolvedName == LinkLinkbaseRefEName)

    assertResult(0)(linkbaseRefElems.size)

    // xs:appinfo child elements

    val appinfoChildElems =
      for {
        annotElem <- docElem.filterChildElems(_.resolvedName == XsAnnotationEName)
        appinfoElem <- annotElem.filterChildElems(_.resolvedName == XsAppinfoEName)
        appinfoChildElem <- appinfoElem.filterChildElems((e: E) => e.parent == appinfoElem)
      } yield appinfoChildElem

    assertResult(17)(appinfoChildElems.size)
    assertResult(Set(LinkLinkbaseRefEName))(appinfoChildElems.map(_.resolvedName).toSet)

    val appinfoChildElems2 =
      for {
        annotElem <- docElem.findAllChildElems.filter(_.resolvedName == XsAnnotationEName)
        appinfoElem <- annotElem.findAllChildElems.filter(_.resolvedName == XsAppinfoEName)
        appinfoChildElem <- appinfoElem.findAllChildElems.filter(_.parent == appinfoElem)
      } yield appinfoChildElem

    assertResult(appinfoChildElems2)(appinfoChildElems)
  }

  @Test def testFilterElemsOrSelfUsingAlias(): Unit = {
    // Non-existing elements

    val bogusElems = docElem \\ (_.resolvedName == EName("schema"))

    assertResult(0)(bogusElems.size)

    // xs:schema elements

    val xsSchemaElems = docElem \\ (_.resolvedName == XsSchemaEName)

    assertResult(1)(xsSchemaElems.size)
    assertResult(Set(XsSchemaEName))(xsSchemaElems.map(_.resolvedName).toSet)

    // xs:import elements

    val xsImportElems = docElem \\ (_.resolvedName == XsImportEName)

    assertResult(7)(xsImportElems.size)
    assertResult(Set(XsImportEName))(xsImportElems.map(_.resolvedName).toSet)

    // link:linkbaseRef elements

    val linkbaseRefElems = docElem \\ (_.resolvedName == LinkLinkbaseRefEName)

    assertResult(17)(linkbaseRefElems.size)
    assertResult(Set(LinkLinkbaseRefEName))(linkbaseRefElems.map(_.resolvedName).toSet)

    // xs:appinfo child elements

    val appinfoChildElems =
      for {
        annotElem <- (docElem \\ (_.resolvedName == XsAnnotationEName))
        appinfoElem <- (annotElem \\ (_.resolvedName == XsAppinfoEName))
        appinfoChildElem <- appinfoElem \\ ((e: E) => e.parent == appinfoElem)
      } yield appinfoChildElem

    assertResult(17)(appinfoChildElems.size)
    assertResult(Set(LinkLinkbaseRefEName))(appinfoChildElems.map(_.resolvedName).toSet)
  }

  @Test def testFilterChildElemsUsingAlias(): Unit = {
    // Non-existing elements

    val bogusElems = docElem \ (_.resolvedName == EName("schema"))

    assertResult(0)(bogusElems.size)

    // xs:schema elements

    val xsSchemaElems = docElem \ (_.resolvedName == XsSchemaEName)

    assertResult(0)(xsSchemaElems.size)

    // xs:import elements

    val xsImportElems = docElem \ (_.resolvedName == XsImportEName)

    assertResult(7)(xsImportElems.size)
    assertResult(Set(XsImportEName))(xsImportElems.map(_.resolvedName).toSet)

    // link:linkbaseRef elements

    val linkbaseRefElems = docElem \ (_.resolvedName == LinkLinkbaseRefEName)

    assertResult(0)(linkbaseRefElems.size)

    // xs:appinfo child elements

    val appinfoChildElems =
      for {
        annotElem <- (docElem \ (_.resolvedName == XsAnnotationEName))
        appinfoElem <- (annotElem \ (_.resolvedName == XsAppinfoEName))
        appinfoChildElem <- appinfoElem \ ((e: E) => e.parent == appinfoElem)
      } yield appinfoChildElem

    assertResult(17)(appinfoChildElems.size)
    assertResult(Set(LinkLinkbaseRefEName))(appinfoChildElems.map(_.resolvedName).toSet)
  }

  @Test def testFindTopmostElemsOrSelf(): Unit = {
    // Non-existing elements

    val bogusElems = docElem.findTopmostElemsOrSelf(_.resolvedName == EName("schema"))

    assertResult(0)(bogusElems.size)

    // xs:schema elements

    val xsSchemaElems = docElem.findTopmostElemsOrSelf(_.resolvedName == XsSchemaEName)

    assertResult(1)(xsSchemaElems.size)
    assertResult(Set(XsSchemaEName))(xsSchemaElems.map(_.resolvedName).toSet)

    // xs:appinfo elements

    val appinfoElems =
      docElem.findTopmostElemsOrSelf(e => e.resolvedName == XsAppinfoEName || e.parentOption.exists(_.resolvedName == XsAppinfoEName))

    assertResult(1)(appinfoElems.size)

    val appinfoTreeElems =
      docElem.filterElemsOrSelf(e => e.resolvedName == XsAppinfoEName || e.parentOption.exists(_.resolvedName == XsAppinfoEName))

    assertResult(18)(appinfoTreeElems.size)

    assertResult(
      appinfoTreeElems.filter(e => e.parentOption.forall(e2 => !appinfoTreeElems.contains(e2))))(appinfoElems)
  }

  @Test def testFindTopmostElems(): Unit = {
    // Non-existing elements

    val bogusElems = docElem.findTopmostElems(_.resolvedName == EName("schema"))

    assertResult(0)(bogusElems.size)

    // xs:schema elements

    val xsSchemaElems = docElem.findTopmostElems(_.resolvedName == XsSchemaEName)

    assertResult(0)(xsSchemaElems.size)

    // xs:appinfo elements

    val appinfoElems =
      docElem.findTopmostElems(e => e.resolvedName == XsAppinfoEName || e.parentOption.exists(_.resolvedName == XsAppinfoEName))

    assertResult(1)(appinfoElems.size)

    val appinfoTreeElems =
      docElem.filterElems(e => e.resolvedName == XsAppinfoEName || e.parentOption.exists(_.resolvedName == XsAppinfoEName))

    assertResult(18)(appinfoTreeElems.size)

    assertResult(
      appinfoTreeElems.filter(e => e.parentOption.forall(e2 => !appinfoTreeElems.contains(e2))))(appinfoElems)
  }

  @Test def testFindTopmostElemsOrSelfUsingAlias(): Unit = {
    // Non-existing elements

    val bogusElems = docElem \\! (_.resolvedName == EName("schema"))

    assertResult(0)(bogusElems.size)

    // xs:schema elements

    val xsSchemaElems = docElem \\! (_.resolvedName == XsSchemaEName)

    assertResult(1)(xsSchemaElems.size)
    assertResult(Set(XsSchemaEName))(xsSchemaElems.map(_.resolvedName).toSet)

    // xs:appinfo elements

    val appinfoElems =
      docElem \\! (e => e.resolvedName == XsAppinfoEName || e.parentOption.exists(_.resolvedName == XsAppinfoEName))

    assertResult(1)(appinfoElems.size)

    val appinfoTreeElems =
      docElem.filterElemsOrSelf(e => e.resolvedName == XsAppinfoEName || e.parentOption.exists(_.resolvedName == XsAppinfoEName))

    assertResult(18)(appinfoTreeElems.size)

    assertResult(
      appinfoTreeElems.filter(e => e.parentOption.forall(e2 => !appinfoTreeElems.contains(e2))))(appinfoElems)
  }

  @Test def testFindElemOrSelf(): Unit = {
    // Non-existing elements

    val bogusElems = docElem.findElemOrSelf(_.resolvedName == EName("schema"))

    assertResult(
      docElem.filterElemsOrSelf(_.resolvedName == EName("schema")).headOption)(bogusElems)

    // xs:schema elements

    val xsSchemaElems = docElem.findElemOrSelf(_.resolvedName == XsSchemaEName)

    assertResult(
      docElem.filterElemsOrSelf(_.resolvedName == XsSchemaEName).headOption)(xsSchemaElems)

    // xs:import elements

    val xsImportElems = docElem.findElemOrSelf(_.resolvedName == XsImportEName)

    assertResult(
      docElem.filterElemsOrSelf(_.resolvedName == XsImportEName).headOption)(xsImportElems)

    // link:linkbaseRef elements

    val linkbaseRefElems = docElem.findElemOrSelf(_.resolvedName == LinkLinkbaseRefEName)

    assertResult(
      docElem.filterElemsOrSelf(_.resolvedName == LinkLinkbaseRefEName).headOption)(linkbaseRefElems)
  }

  @Test def testFindElem(): Unit = {
    // Non-existing elements

    val bogusElems = docElem.findElem(_.resolvedName == EName("schema"))

    assertResult(
      docElem.filterElems(_.resolvedName == EName("schema")).headOption)(bogusElems)

    // xs:schema elements

    val xsSchemaElems = docElem.findElem(_.resolvedName == XsSchemaEName)

    assertResult(
      docElem.filterElems(_.resolvedName == XsSchemaEName).headOption)(xsSchemaElems)

    // xs:import elements

    val xsImportElems = docElem.findElem(_.resolvedName == XsImportEName)

    assertResult(
      docElem.filterElems(_.resolvedName == XsImportEName).headOption)(xsImportElems)

    // link:linkbaseRef elements

    val linkbaseRefElems = docElem.findElem(_.resolvedName == LinkLinkbaseRefEName)

    assertResult(
      docElem.filterElems(_.resolvedName == LinkLinkbaseRefEName).headOption)(linkbaseRefElems)
  }

  @Test def testPathNavigation(): Unit = {
    val linkbaseRefElems = docElem.filterElemsOrSelf(_.resolvedName == LinkLinkbaseRefEName)

    val linkbaseRefElemPaths = linkbaseRefElems.map(_.path)

    assertResult(17)(linkbaseRefElemPaths.size)

    assertResult((0 to 16).map(i => Path.Entry(LinkLinkbaseRefEName, i)).toSet) {
      linkbaseRefElemPaths.flatMap(_.lastEntryOption).toSet
    }

    val expectedParentPath =
      PathBuilder.from(QName("xs:annotation") -> 0, QName("xs:appinfo") -> 0).build(Scope.from("xs" -> XsNamespace))

    assertResult(Set(expectedParentPath))(linkbaseRefElemPaths.flatMap(_.parentPathOption).toSet)

    val linkbaseRefElems2 = linkbaseRefElems.flatMap(e => e.rootElem.findElemOrSelfByPath(e.path))

    assertResult(linkbaseRefElems.size)(linkbaseRefElems2.size)
    assertResult(linkbaseRefElems)(linkbaseRefElems2)
  }

  @Test def testAncestry(): Unit = {
    val linkbaseRefElems = docElem.filterElemsOrSelf(_.resolvedName == LinkLinkbaseRefEName)

    assertResult(17)(linkbaseRefElems.size)

    assertResult(Set(List(XsSchemaEName, XsAnnotationEName, XsAppinfoEName))) {
      linkbaseRefElems.map(_.reverseAncestryENames).toSet
    }

    assertResult(Set(List(XsSchemaEName, XsAnnotationEName, XsAppinfoEName, LinkLinkbaseRefEName))) {
      linkbaseRefElems.map(_.reverseAncestryOrSelfENames).toSet
    }

    assertResult(linkbaseRefElems.map(_.ancestors.map(_.resolvedName).reverse).toSet) {
      linkbaseRefElems.map(_.reverseAncestryENames).toSet
    }

    assertResult(linkbaseRefElems.map(_.ancestorsOrSelf.map(_.resolvedName).reverse).toSet) {
      linkbaseRefElems.map(_.reverseAncestryOrSelfENames).toSet
    }

    assertResult(linkbaseRefElems) {
      linkbaseRefElems.map(e => e.parent.getChildElemByPathEntry(e.path.lastEntry))
    }

    assertResult(linkbaseRefElems.map(_.reverseAncestryOrSelfENames).toSet) {
      linkbaseRefElems.map(_.reverseAncestryOrSelf.map(_.resolvedName)).toSet
    }

    assertResult(docElem.filterElems(_.resolvedName == XsAppinfoEName).toSet) {
      linkbaseRefElems.map(_.parent).toSet
    }
    assertResult(docElem.filterElems(_.resolvedName == XsAppinfoEName).toSet) {
      linkbaseRefElems.flatMap(_.parentOption).toSet
    }

    assertResult(Set(docElem)) {
      linkbaseRefElems.flatMap(_.findAncestor((e: E) => e.resolvedName == XsSchemaEName)).toSet
    }
    assertResult(Set(docElem)) {
      linkbaseRefElems.flatMap(_.findAncestorOrSelf((e: E) => e.resolvedName == XsSchemaEName)).toSet
    }

    assertResult(0) {
      linkbaseRefElems.flatMap(_.findAncestor((e: E) => e.resolvedName == LinkLinkbaseRefEName)).size
    }
    assertResult(linkbaseRefElems) {
      linkbaseRefElems.flatMap(_.findAncestorOrSelf((e: E) => e.resolvedName == LinkLinkbaseRefEName))
    }
  }

  @Test def testPathConsistency(): Unit = {
    val allElemsOrSelf = docElem.findAllElemsOrSelf

    assertResult(Set(docElem)) {
      allElemsOrSelf.map(_.rootElem).toSet
    }
    assertResult(allElemsOrSelf) {
      allElemsOrSelf.map(e => e.rootElem.getElemOrSelfByPath(e.path))
    }

    assertResult(allElemsOrSelf) {
      allElemsOrSelf.map(e => e.parentOption.flatMap(_.findChildElemByPathEntry(e.path.lastEntry)).getOrElse(docElem))
    }
  }
}
