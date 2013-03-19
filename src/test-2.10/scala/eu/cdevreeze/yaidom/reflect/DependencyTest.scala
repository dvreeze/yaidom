/*
 * Copyright 2011 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.yaidom
package reflect

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll, Ignore }
import org.scalatest.junit.JUnitRunner

/**
 * Dependency test case.
 *
 * For one of the introductions to Scala reflection, see
 * http://dcsobral.blogspot.nl/2012/07/json-serialization-with-reflection-in.html.
 *
 * Note that we are in "Java reflection territory" here, so we cannot expect to find all possible dependencies of
 * the analyzed yaidom classes, such as those hidden inside method implementations.
 *
 * Also note that it is quite conceivable that we can use the Scala Reflection API as alternative to Java reflection,
 * although I have not tried yet.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class DependencyTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.reflect")

  // Importing the members of the api.JavaUniverse cake
  val ru = scala.reflect.runtime.universe
  import ru._

  private val yaidomPackageSymbol: Symbol = typeOf[Elem].typeSymbol.owner
  private val yaidomConvertPackageSymbol: Symbol = typeOf[convert.DomConversions.type].typeSymbol.owner
  private val yaidomDomPackageSymbol: Symbol = typeOf[dom.DomElem].typeSymbol.owner
  private val yaidomParsePackageSymbol: Symbol = typeOf[parse.DocumentParser].typeSymbol.owner
  private val yaidomPrintPackageSymbol: Symbol = typeOf[print.DocumentPrinter].typeSymbol.owner
  private val yaidomResolvedPackageSymbol: Symbol = typeOf[resolved.Elem].typeSymbol.owner
  private val yaidomXLinkPackageSymbol: Symbol = typeOf[xlink.XLink].typeSymbol.owner

  private val thisPackage = typeOf[DependencyTest].typeSymbol.owner

  // Of course, I could scan the classpath instead...

  private val yaidomTypes: Seq[Type] = {
    List(
      typeOf[Comment],
      typeOf[CommentBuilder],
      typeOf[ConverterToDocument[AnyRef]],
      typeOf[ConverterToElem[AnyRef]],
      typeOf[Declarations],
      typeOf[Declarations.type],
      typeOf[DocBuilder],
      typeOf[DocBuilder.type],
      typeOf[eu.cdevreeze.yaidom.Document],
      typeOf[eu.cdevreeze.yaidom.Document.type],
      typeOf[DocumentConverter[AnyRef]],
      typeOf[Elem],
      typeOf[Elem.type],
      typeOf[ElemBuilder],
      typeOf[ElemConverter[AnyRef]],
      typeOf[ElemLike[_]],
      typeOf[ElemPath],
      typeOf[ElemPath.type],
      typeOf[ElemPath.Entry],
      typeOf[ElemPath.Entry.type],
      typeOf[ElemPathBuilder],
      typeOf[ElemPathBuilder.type],
      typeOf[EName],
      typeOf[EName.type],
      typeOf[EntityRef],
      typeOf[EntityRefBuilder],
      typeOf[HasText],
      typeOf[Node],
      typeOf[Node.type],
      typeOf[NodeBuilder],
      typeOf[NodeBuilder.type],
      typeOf[ParentElemLike[_]],
      typeOf[PathAwareElemLike[_]],
      typeOf[PrefixedName],
      typeOf[ProcessingInstruction],
      typeOf[ProcessingInstructionBuilder],
      typeOf[QName],
      typeOf[QName.type],
      typeOf[eu.cdevreeze.yaidom.Scope],
      typeOf[eu.cdevreeze.yaidom.Scope.type],
      typeOf[eu.cdevreeze.yaidom.Text],
      typeOf[eu.cdevreeze.yaidom.TextBuilder],
      typeOf[TreeReprParsers.type],
      typeOf[UnprefixedName],
      typeOf[UpdatableElemLike[_, _]],
      typeOf[XmlStringUtils.type])
  }

  private val yaidomConvertTypes: Seq[Type] = {
    List(
      typeOf[convert.DomConversions.type],
      typeOf[convert.DomToYaidomConversions],
      typeOf[convert.StaxConversions.type],
      typeOf[convert.StaxEventsToYaidomConversions],
      typeOf[convert.YaidomToDomConversions],
      typeOf[convert.YaidomToDomConversions.type],
      typeOf[convert.YaidomToStaxEventsConversions],
      typeOf[convert.YaidomToStaxEventsConversions.type])
  }

  private val yaidomDomTypes: Seq[Type] = {
    List(
      typeOf[dom.DomComment],
      typeOf[dom.DomDocument],
      typeOf[dom.DomElem],
      typeOf[dom.DomEntityRef],
      typeOf[dom.DomNode],
      typeOf[dom.DomNode.type],
      typeOf[dom.DomParentNode],
      typeOf[dom.DomProcessingInstruction],
      typeOf[dom.DomText])
  }

  private val yaidomParseTypes: Seq[Type] = {
    List(
      typeOf[parse.DefaultElemProducingSaxHandler],
      typeOf[parse.DefaultElemProducingSaxHandler.type],
      typeOf[parse.DocumentParser],
      typeOf[parse.DocumentParserUsingDom],
      typeOf[parse.DocumentParserUsingDom.type],
      typeOf[parse.DocumentParserUsingDomLS],
      typeOf[parse.DocumentParserUsingDomLS.type],
      typeOf[parse.DocumentParserUsingSax],
      typeOf[parse.DocumentParserUsingSax.type],
      typeOf[parse.DocumentParserUsingStax],
      typeOf[parse.DocumentParserUsingStax.type],
      typeOf[parse.ElemProducingSaxHandler],
      typeOf[parse.SaxHandlerWithLocator])
  }

  private val yaidomPrintTypes: Seq[Type] = {
    List(
      typeOf[print.DocumentPrinter],
      typeOf[print.DocumentPrinterUsingDom],
      typeOf[print.DocumentPrinterUsingDom.type],
      typeOf[print.DocumentPrinterUsingDomLS],
      typeOf[print.DocumentPrinterUsingDomLS.type],
      typeOf[print.DocumentPrinterUsingSax],
      typeOf[print.DocumentPrinterUsingSax.type],
      typeOf[print.DocumentPrinterUsingStax],
      typeOf[print.DocumentPrinterUsingStax.type])
  }

  private val yaidomResolvedTypes: Seq[Type] = {
    List(
      typeOf[resolved.Node],
      typeOf[resolved.Node.type],
      typeOf[resolved.Elem],
      typeOf[resolved.Elem.type],
      typeOf[resolved.Text],
      typeOf[resolved.Text.type])
  }

  private val yaidomXLinkTypes: Seq[Type] = {
    List(
      typeOf[xlink.XLink],
      typeOf[xlink.XLink.type],
      typeOf[xlink.Link],
      typeOf[xlink.Link.type],
      typeOf[xlink.SimpleLink],
      typeOf[xlink.SimpleLink.type],
      typeOf[xlink.ExtendedLink],
      typeOf[xlink.ExtendedLink.type],
      typeOf[xlink.Arc],
      typeOf[xlink.Arc.type],
      typeOf[xlink.Locator],
      typeOf[xlink.Locator.type],
      typeOf[xlink.Resource],
      typeOf[xlink.Resource.type],
      typeOf[xlink.Title],
      typeOf[xlink.Title.type])
  }

  private val yaidomTypeSymbols: Seq[Symbol] = {
    val result = yaidomTypes map { _.typeSymbol }
    assert(result forall (sym => isYaidomTopLevelPackage(findPackageSymbol(sym).get)))
    result
  }

  private val expectedDependenciesOfQName: Set[Type] = Set(
    typeOf[QName], typeOf[QName.type], typeOf[PrefixedName], typeOf[UnprefixedName])

  private val expectedDependenciesOfEName: Set[Type] =
    expectedDependenciesOfQName ++ Set(typeOf[EName], typeOf[EName.type])

  private val expectedDependenciesOfDeclarations: Set[Type] =
    Set(typeOf[Declarations], typeOf[Declarations.type])

  private val expectedDependenciesOfScope: Set[Type] =
    expectedDependenciesOfEName ++
      expectedDependenciesOfDeclarations ++
      Set(typeOf[eu.cdevreeze.yaidom.Scope], typeOf[eu.cdevreeze.yaidom.Scope.type])

  private val expectedDependenciesOfElemPath: Set[Type] =
    expectedDependenciesOfScope ++
      Set(typeOf[ElemPath], typeOf[ElemPath.type], typeOf[ElemPathBuilder], typeOf[ElemPathBuilder.type],
        typeOf[ElemPath.Entry], typeOf[ElemPath.Entry.type], typeOf[ElemPathBuilder.Entry])

  private val expectedDependenciesOfElemLike: Set[Type] =
    expectedDependenciesOfEName ++ Set(typeOf[ParentElemLike[_]], typeOf[ElemLike[_]])

  private val expectedDependenciesOfPathAwareElemLike: Set[Type] =
    expectedDependenciesOfElemLike ++ expectedDependenciesOfElemPath ++ Set(typeOf[PathAwareElemLike[_]])

  private val expectedDependenciesOfUpdatableElemLike: Set[Type] =
    expectedDependenciesOfPathAwareElemLike ++ Set(typeOf[UpdatableElemLike[_, _]])

  private val expectedDependenciesOfElem: Set[Type] =
    expectedDependenciesOfUpdatableElemLike ++
      (yaidomTypes filter (tpe => tpe <:< typeOf[eu.cdevreeze.yaidom.Node])) ++
      Set(typeOf[HasText], typeOf[eu.cdevreeze.yaidom.Node.type], typeOf[eu.cdevreeze.yaidom.Elem.type], typeOf[eu.cdevreeze.yaidom.Document], typeOf[eu.cdevreeze.yaidom.Document.type])

  @Test def testDependenciesOfQName() {
    // Depends on XmlStringUtils too, hidden inside statements, so not found in a Java reflection universe
    testDependenciesWithinYaidomTopLevel(
      typeOf[QName],
      "localPart",
      expectedDependenciesOfQName)
  }

  @Test def testDependenciesOfQNameObject() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[QName.type],
      "apply",
      expectedDependenciesOfQName)
  }

  @Test def testDependenciesOfPrefixedName() {
    // Depends on XmlStringUtils too, hidden inside statements, so not found in a Java reflection universe
    testDependenciesWithinYaidomTopLevel(
      typeOf[PrefixedName],
      "localPart",
      expectedDependenciesOfQName)
  }

  @Test def testDependenciesOfUnprefixedName() {
    // Depends on XmlStringUtils too, hidden inside statements, so not found in a Java reflection universe
    testDependenciesWithinYaidomTopLevel(
      typeOf[UnprefixedName],
      "localPart",
      expectedDependenciesOfQName)
  }

  @Test def testDependenciesOfEName() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[EName],
      "namespaceUriOption",
      expectedDependenciesOfEName)
  }

  @Test def testDependenciesOfENameObject() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[EName.type],
      "apply",
      expectedDependenciesOfEName)
  }

  @Test def testDependenciesOfDeclarations() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[Declarations],
      "withoutUndeclarations",
      expectedDependenciesOfDeclarations)
  }

  @Test def testDependenciesOfDeclarationsObject() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[Declarations.type],
      "from",
      expectedDependenciesOfDeclarations)
  }

  @Test def testDependenciesOfScope() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.Scope],
      "subScopeOf",
      expectedDependenciesOfScope)
  }

  @Test def testDependenciesOfScopeObject() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.Scope.type],
      "from",
      expectedDependenciesOfScope)
  }

  @Test def testDependenciesOfElemPath() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.ElemPath],
      "parentPathOption",
      expectedDependenciesOfElemPath)
  }

  @Test def testDependenciesOfElemPathObject() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.ElemPath.type],
      "from",
      expectedDependenciesOfElemPath)
  }

  @Test def testDependenciesOfElemPathBuilder() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.ElemPathBuilder],
      "prepend",
      expectedDependenciesOfElemPath)
  }

  @Test def testDependenciesOfElemPathBuilderObject() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.ElemPathBuilder.type],
      "from",
      expectedDependenciesOfElemPath)
  }

  @Test def testDependenciesOfElemPathEntry() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.ElemPath.Entry],
      "localName",
      expectedDependenciesOfElemPath)
  }

  @Test def testDependenciesOfElemPathEntryObject() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.ElemPath.Entry.type],
      "apply",
      expectedDependenciesOfElemPath)
  }

  @Test def testDependenciesOfElemPathBuilderEntry() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.ElemPathBuilder.Entry],
      "localName",
      expectedDependenciesOfElemPath)
  }

  @Test def testDependenciesOfParentElemLike() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.ParentElemLike[_]],
      "findAllElemsOrSelf",
      Set(typeOf[ParentElemLike[_]]))
  }

  @Test def testDependenciesOfElemLike() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.ElemLike[_]],
      "attributeOption",
      expectedDependenciesOfElemLike)
  }

  @Test def testDependenciesOfPathAwareElemLike() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.PathAwareElemLike[_]],
      "allChildElemPaths",
      expectedDependenciesOfPathAwareElemLike)
  }

  @Test def testDependenciesOfUpdatableElemLike() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.UpdatableElemLike[_, _]],
      "updated",
      expectedDependenciesOfUpdatableElemLike)
  }

  @Test def testDependenciesOfHasText() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.HasText],
      "trimmedText",
      Set(typeOf[HasText]))
  }

  @Test def testDependenciesOfElem() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.Elem],
      "findAllElemsOrSelf",
      expectedDependenciesOfElem)
  }

  @Test def testDependenciesOfElemObject() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.Elem.type],
      "apply",
      expectedDependenciesOfElem)
  }

  @Test def testDependenciesOfNode() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.Node],
      "toTreeRepr",
      expectedDependenciesOfElem)
  }

  @Test def testDependenciesOfNodeObject() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.Node.type],
      "elem",
      expectedDependenciesOfElem)
  }

  @Test def testDependenciesOfComment() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.Comment],
      "toTreeRepr",
      expectedDependenciesOfElem)
  }

  @Test def testDependenciesOfEntityRef() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.EntityRef],
      "toTreeRepr",
      expectedDependenciesOfElem)
  }

  @Test def testDependenciesOfProcessingInstruction() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.ProcessingInstruction],
      "toTreeRepr",
      expectedDependenciesOfElem)
  }

  @Test def testDependenciesOfText() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.Text],
      "normalizedText",
      expectedDependenciesOfElem)
  }

  @Test def testDependenciesOfDocument() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.Document],
      "updated",
      expectedDependenciesOfElem)
  }

  @Test def testDependenciesOfDocumentObject() {
    testDependenciesWithinYaidomTopLevel(
      typeOf[eu.cdevreeze.yaidom.Document.type],
      "apply",
      expectedDependenciesOfElem)
  }

  @Test def testDependenciesOfYaidomPackage() {
    val tpes = yaidomTypes

    val packageDependencies: Set[Symbol] = getPackageDependencies(tpes)

    expectResult(Set(yaidomPackageSymbol)) {
      packageDependencies filter { sym => isYaidomPackage(sym) && (sym != thisPackage) }
    }
  }

  @Test def testDependenciesOfYaidomConvertPackage() {
    val tpes = yaidomConvertTypes

    val packageDependencies: Set[Symbol] = getPackageDependencies(tpes)

    expectResult(Set(yaidomPackageSymbol, yaidomConvertPackageSymbol)) {
      packageDependencies filter { sym => isYaidomPackage(sym) && (sym != thisPackage) }
    }
  }

  @Test def testDependenciesOfYaidomParsePackage() {
    val tpes = yaidomParseTypes

    val packageDependencies: Set[Symbol] = getPackageDependencies(tpes)

    expectResult(Set(yaidomPackageSymbol, yaidomParsePackageSymbol)) {
      packageDependencies filter { sym => isYaidomPackage(sym) && (sym != thisPackage) }
    }
  }

  @Test def testDependenciesOfYaidomPrintPackage() {
    val tpes = yaidomPrintTypes

    val packageDependencies: Set[Symbol] = getPackageDependencies(tpes)

    expectResult(Set(yaidomPackageSymbol, yaidomPrintPackageSymbol)) {
      packageDependencies filter { sym => isYaidomPackage(sym) && (sym != thisPackage) }
    }
  }

  @Test def testDependenciesOfYaidomDomPackage() {
    val tpes = yaidomDomTypes

    val packageDependencies: Set[Symbol] = getPackageDependencies(tpes)

    expectResult(Set(yaidomPackageSymbol, yaidomDomPackageSymbol)) {
      packageDependencies filter { sym => isYaidomPackage(sym) && (sym != thisPackage) }
    }
  }

  @Test def testDependenciesOfYaidomResolvedPackage() {
    val tpes = yaidomResolvedTypes

    val packageDependencies: Set[Symbol] = getPackageDependencies(tpes)

    expectResult(Set(yaidomPackageSymbol, yaidomResolvedPackageSymbol)) {
      packageDependencies filter { sym => isYaidomPackage(sym) && (sym != thisPackage) }
    }
  }

  @Test def testDependenciesOfYaidomXLinkPackage() {
    val tpes = yaidomXLinkTypes

    val packageDependencies: Set[Symbol] = getPackageDependencies(tpes)

    expectResult(Set(yaidomPackageSymbol, yaidomXLinkPackageSymbol)) {
      packageDependencies filter { sym => isYaidomPackage(sym) && (sym != thisPackage) }
    }
  }

  private def testDependenciesWithinYaidomTopLevel(tpe: Type, sampleMethodName: String, allowedTypes: Set[Type]) {
    val terms: Iterable[Symbol] = tpe.members filter { _.isTerm }
    val baseClasses: Seq[Symbol] = tpe.baseClasses
    val termTypeSignatures = terms map { _.typeSignatureIn(tpe) }

    assert(terms exists (term => term.isMethod && term.asMethod.name == stringToTermName(sampleMethodName)),
      "Expected method %s to exist in type %s".format(sampleMethodName, tpe))

    val packages: Set[Symbol] = getPackageDependencies(tpe)

    val unexpectedPackages: Set[Symbol] =
      packages filterNot { pkgSym => isScalaOrJavaPackage(pkgSym) || isYaidomTopLevelPackage(pkgSym) || (pkgSym == thisPackage) }

    expectResult(Set()) {
      unexpectedPackages
    }

    val allowedTypeSymbols = allowedTypes map { _.typeSymbol }

    val disallowedYaidomTypeSymbols: Seq[Symbol] = yaidomTypeSymbols filter { sym => !allowedTypeSymbols.contains(sym) }

    for (tpeSym <- disallowedYaidomTypeSymbols) {
      assert(termTypeSignatures forall (tpeSignature => !tpeSignature.contains(tpeSym)),
        "Dependency on type %s not expected in type %s".format(tpeSym, tpe))

      assert(baseClasses forall (baseClass => !(baseClass.asType.toType <:< tpeSym.asType.toType)),
        "Dependency on type %s not expected in any base class of %s".format(tpeSym, tpe))
    }
  }

  private def getPackageDependencies(tpe: Type): Set[Symbol] = {
    val terms: Iterable[Symbol] = tpe.members filter { _.isTerm }
    val baseClasses: Seq[Symbol] = tpe.baseClasses

    val termTypeSignatures = terms map { _.typeSignatureIn(tpe) }

    val termTypes: Set[Type] = termTypeSignatures.toSet flatMap { (tpe: Type) =>
      tpe match {
        case methodType: MethodType => methodSignatureTypes(methodType)
        case tpe: Type => Set(tpe)
      }
    }

    val packages: Set[Symbol] = {
      val baseClassResult = baseClasses flatMap { baseClass => findPackageSymbol(baseClass) }
      val termResult = termTypes flatMap { tpeSig => findPackageSymbol(tpeSig.typeSymbol) }
      val result = baseClassResult ++ termResult
      assert(result forall (_.isPackage))
      result.toSet
    }
    packages
  }

  private def methodSignatureTypes(methodType: MethodType): Set[Type] = {
    val resultType = methodType.resultType
    val paramTypes = methodType.params map { par => par.typeSignature }
    paramTypes.toSet + resultType
  }

  private def getPackageDependencies(tpes: Iterable[Type]): Set[Symbol] = {
    tpes.toSet flatMap { (tpe: Type) => getPackageDependencies(tpe) }
  }

  private def isJavaPackage(sym: Symbol): Boolean = {
    sym.isPackage && {
      sym.fullName.startsWith("java.") ||
        sym.fullName.startsWith("javax.") ||
        sym.fullName.startsWith("org.w3c") ||
        sym.fullName.startsWith("org.xml")
    }
  }

  private def isScalaPackage(sym: Symbol): Boolean = {
    sym.isPackage && {
      sym.fullName == "scala" ||
        sym.fullName.startsWith("scala.")
    }
  }

  private def isScalaOrJavaPackage(sym: Symbol): Boolean = isScalaPackage(sym) || isJavaPackage(sym)

  private def isYaidomTopLevelPackage(sym: Symbol): Boolean = {
    (sym.isPackage) && (sym.fullName == "eu.cdevreeze.yaidom")
  }

  private def isYaidomPackage(sym: Symbol): Boolean = {
    (sym.isPackage) && ((sym.fullName == "eu.cdevreeze.yaidom") || (sym.fullName.startsWith("eu.cdevreeze.yaidom.")))
  }

  private def findPackageSymbol(sym: Symbol): Option[Symbol] = {
    if (sym == NoSymbol) None
    else if (sym.isPackage) Some(sym)
    else findPackageSymbol(sym.owner)
  }
}
