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
 * Also note that we can use the Scala Reflection API as alternative to Java reflection, for example to do dependency
 * analysis normally delegated to tools such as for example Macker (with the limitations that Java reflection impose upon us).
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class DependencyTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.reflect")

  // Importing the members of the api.JavaUniverse cake
  import scala.reflect.runtime.universe._

  private val yaidomPackageSymbol: Symbol = typeOf[Elem].typeSymbol.owner
  private val yaidomConvertPackageSymbol: Symbol = typeOf[convert.DomConversions$].typeSymbol.owner
  private val yaidomDomPackageSymbol: Symbol = typeOf[dom.DomElem].typeSymbol.owner
  private val yaidoParsePackageSymbol: Symbol = typeOf[parse.DocumentParser].typeSymbol.owner
  private val yaidomPrintPackageSymbol: Symbol = typeOf[print.DocumentPrinter].typeSymbol.owner
  private val yaidomResolvedPackageSymbol: Symbol = typeOf[resolved.Elem].typeSymbol.owner
  private val yaidomXlinkPackageSymbol: Symbol = typeOf[xlink.XLink].typeSymbol.owner

  private val yaidomTypeSymbols: Seq[Symbol] = {
    val types = List(
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

    val result = types map { _.typeSymbol }
    assert(result forall (sym => isYaidomTopLevelPackage(findPackageSymbol(sym).get)))
    result
  }

  @Test def testDependenciesOfQName() {
    // Depends on XmlStringUtils too, hidden inside statements, so not found in a Java reflection universe
    testDependencies(
      typeOf[QName],
      "localPart",
      List(typeOf[QName], typeOf[QName.type], typeOf[PrefixedName], typeOf[UnprefixedName]))
  }

  @Test def testDependenciesOfQNameObject() {
    testDependencies(
      typeOf[QName.type],
      "apply",
      List(typeOf[QName], typeOf[QName.type], typeOf[PrefixedName], typeOf[UnprefixedName]))
  }

  @Test def testDependenciesOfPrefixedName() {
    // Depends on XmlStringUtils too, hidden inside statements, so not found in a Java reflection universe
    testDependencies(
      typeOf[PrefixedName],
      "localPart",
      List(typeOf[QName], typeOf[QName.type], typeOf[PrefixedName], typeOf[UnprefixedName]))
  }

  @Test def testDependenciesOfUnprefixedName() {
    // Depends on XmlStringUtils too, hidden inside statements, so not found in a Java reflection universe
    testDependencies(
      typeOf[UnprefixedName],
      "localPart",
      List(typeOf[QName], typeOf[QName.type], typeOf[PrefixedName], typeOf[UnprefixedName]))
  }

  @Test def testDependenciesOfEName() {
    val allowedTypes = List(
      typeOf[QName], typeOf[QName.type],
      typeOf[PrefixedName], typeOf[UnprefixedName],
      typeOf[EName], typeOf[EName.type])

    testDependencies(
      typeOf[EName],
      "namespaceUriOption",
      allowedTypes)
  }

  @Test def testDependenciesOfENameObject() {
    val allowedTypes = List(
      typeOf[QName], typeOf[QName.type],
      typeOf[PrefixedName], typeOf[UnprefixedName],
      typeOf[EName], typeOf[EName.type])

    testDependencies(
      typeOf[EName.type],
      "apply",
      allowedTypes)
  }

  @Test def testDependenciesOfDeclarations() {
    testDependencies(typeOf[Declarations], "subDeclarationsOf", List(typeOf[Declarations], typeOf[Declarations.type]))
  }

  @Test def testDependenciesOfDeclarationsObject() {
    testDependencies(typeOf[Declarations.type], "from", List(typeOf[Declarations], typeOf[Declarations.type]))
  }

  @Test def testDependenciesOfScope() {
    val allowedTypes = List(
      typeOf[Declarations], typeOf[Declarations.type],
      typeOf[eu.cdevreeze.yaidom.Scope], typeOf[eu.cdevreeze.yaidom.Scope.type],
      typeOf[QName], typeOf[QName.type],
      typeOf[EName], typeOf[EName.type])

    testDependencies(
      typeOf[eu.cdevreeze.yaidom.Scope],
      "subScopeOf",
      allowedTypes)
  }

  @Test def testDependenciesOfScopeObject() {
    val allowedTypes = List(
      typeOf[Declarations], typeOf[Declarations.type],
      typeOf[eu.cdevreeze.yaidom.Scope], typeOf[eu.cdevreeze.yaidom.Scope.type],
      typeOf[QName], typeOf[QName.type],
      typeOf[EName], typeOf[EName.type])

    testDependencies(
      typeOf[eu.cdevreeze.yaidom.Scope.type],
      "from",
      allowedTypes)
  }

  private def testDependencies(tpe: Type, sampleMethodName: String, allowedTypes: Seq[Type]) {
    val terms: Iterable[Symbol] = tpe.members filter { _.isTerm }
    val termTypeSignatures = terms map { _.typeSignatureIn(tpe) }

    assert(terms exists (term => term.isMethod && term.asMethod.name == stringToTermName(sampleMethodName)),
      "Expected method %s to exist".format(sampleMethodName))

    val packages: Set[Symbol] = {
      val result = termTypeSignatures flatMap { tpeSig => findPackageSymbol(tpeSig.typeSymbol) }
      assert(result forall (_.isPackage))
      result.toSet
    }

    assert(packages forall (pkgSym => isScalaOrJavaPackage(pkgSym) || isYaidomTopLevelPackage(pkgSym)),
      "Expected the packages to be standard Java or Scala package, or the top level yaidom package, and nothing else")

    val disallowedYaidomTypeSymbols: Seq[Symbol] = yaidomTypeSymbols filter { sym =>
      val allowedTypeSymbols = allowedTypes map { _.typeSymbol }

      !allowedTypeSymbols.contains(sym)
    }

    for (tpeSym <- disallowedYaidomTypeSymbols) {
      assert(termTypeSignatures forall (tpeSignature => !tpeSignature.contains(tpeSym)),
        "Dependency on type %s not expected".format(tpeSym))
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
