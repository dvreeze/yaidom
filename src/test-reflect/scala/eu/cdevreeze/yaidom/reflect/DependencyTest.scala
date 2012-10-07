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
  import scala.reflect.runtime.universe._

  private val yaidomPackageSymbol: Symbol = typeOf[Elem].typeSymbol.owner
  private val yaidomConvertPackageSymbol: Symbol = typeOf[convert.DomConversions.type].typeSymbol.owner
  private val yaidomDomPackageSymbol: Symbol = typeOf[dom.DomElem].typeSymbol.owner
  private val yaidoParsePackageSymbol: Symbol = typeOf[parse.DocumentParser].typeSymbol.owner
  private val yaidomPrintPackageSymbol: Symbol = typeOf[print.DocumentPrinter].typeSymbol.owner
  private val yaidomResolvedPackageSymbol: Symbol = typeOf[resolved.Elem].typeSymbol.owner
  private val yaidomXlinkPackageSymbol: Symbol = typeOf[xlink.XLink].typeSymbol.owner

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
      typeOf[ElemLike[Elem]],
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
      typeOf[IndexedDocument],
      typeOf[Node],
      typeOf[Node.type],
      typeOf[NodeBuilder],
      typeOf[NodeBuilder.type],
      typeOf[ParentElemLike[Elem]],
      typeOf[PathAwareElemLike[Elem]],
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
      typeOf[UpdatableElemLike[Node, Elem]],
      typeOf[XmlStringUtils.type])
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
      "subDeclarationsOf",
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

  private def testDependenciesWithinYaidomTopLevel(tpe: Type, sampleMethodName: String, allowedTypes: Set[Type]) {
    val terms: Iterable[Symbol] = tpe.members filter { _.isTerm }
    val baseClasses: Seq[Symbol] = tpe.baseClasses
    val termTypeSignatures = terms map { _.typeSignatureIn(tpe) }

    assert(terms exists (term => term.isMethod && term.asMethod.name == stringToTermName(sampleMethodName)),
      "Expected method %s to exist in type %s".format(sampleMethodName, tpe))

    val packages: Set[Symbol] = {
      val baseClassResult = baseClasses flatMap { baseClass => findPackageSymbol(baseClass) }
      val termResult = termTypeSignatures flatMap { tpeSig => findPackageSymbol(tpeSig.typeSymbol) }
      val result = baseClassResult ++ termResult
      assert(result forall (_.isPackage))
      result.toSet
    }

    assert(packages forall (pkgSym => isScalaOrJavaPackage(pkgSym) || isYaidomTopLevelPackage(pkgSym)),
      "Expected the packages to be standard Java or Scala package, or the top level yaidom package, and nothing else (for dependencies of %s)".format(tpe))

    val allowedTypeSymbols = allowedTypes map { _.typeSymbol }

    val disallowedYaidomTypeSymbols: Seq[Symbol] = yaidomTypeSymbols filter { sym => !allowedTypeSymbols.contains(sym) }

    for (tpeSym <- disallowedYaidomTypeSymbols) {
      assert(termTypeSignatures forall (tpeSignature => !tpeSignature.contains(tpeSym)),
        "Dependency on type %s not expected in type %s".format(tpeSym, tpe))

      assert(baseClasses forall (baseClass => !(baseClass.asType.toType <:< tpeSym.asType.toType)),
        "Dependency on type %s not expected in any base class of %s".format(tpeSym, tpe))
    }
  }

  private def isJavaPackage(sym: Symbol): Boolean = {
    (sym.isPackage) && ((sym.fullName.startsWith("java.")) || (sym.fullName.startsWith("javax.")))
  }

  private def isScalaPackage(sym: Symbol): Boolean = {
    (sym.isPackage) && ((sym.fullName == "scala") || (sym.fullName.startsWith("scala.")))
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
