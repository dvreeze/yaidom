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
 * Coding practices test case, testing for example that traits have no val and var members.
 *
 * Note that we are in "Java reflection territory" here, so we cannot expect to find all possible dependencies of
 * the analyzed yaidom classes, such as those hidden inside method implementations.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class CodingPracticesTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.reflect")

  // Importing the members of the api.JavaUniverse cake
  val ru = scala.reflect.runtime.universe
  import ru._

  private val selectedTypes: Seq[Type] = Vector(
    typeOf[DocBuilder],
    typeOf[convert.DomConversions.type],
    typeOf[convert.StaxConversions.type],
    typeOf[dom.DomDocument],
    typeOf[indexed.Document],
    typeOf[resolved.Elem],
    typeOf[parse.DocumentParserUsingSax],
    typeOf[print.DocumentPrinterUsingSax],
    typeOf[xlink.ExtendedLink],
    typeOf[xlink.LabeledXLink])

  @Test def testNoValVarInTraits() {
    val tpes: Set[Type] = {
      def p(tpe: Type): Boolean = if (tpe.typeSymbol == NoSymbol) false else isYaidomPackage(tpe.typeSymbol.owner)

      val result = selectedTypes flatMap { tpe => collectTypes(tpe, p _) }
      val erasedResult = result map { _.erasure }
      erasedResult.toSet
    }

    val expectedTraits = Set(
      typeOf[ElemApi[_]].erasure,
      typeOf[ElemLike[_]].erasure,
      typeOf[QName],
      typeOf[ConverterToDocument[_]].erasure)

    assert(expectedTraits.subsetOf(tpes))

    val traitTypeSymbols: Set[ClassSymbol] =
      tpes filter { tpe => tpe.typeSymbol.isClass } map { tpe => tpe.typeSymbol.asClass } filter { sym => sym.isTrait }

    assert(traitTypeSymbols.size >= expectedTraits.size)

    val traitValMembers = traitTypeSymbols flatMap { sym =>
      sym.toType.members filter { mem => mem.isTerm && mem.asTerm.isVal }
    }
    val traitVarMembers = traitTypeSymbols flatMap { sym =>
      sym.toType.members filter { mem => mem.isTerm && mem.asTerm.isVar }
    }
    val traitGetterMembers = traitTypeSymbols flatMap { sym =>
      sym.toType.members filter { mem => mem.isTerm && mem.asTerm.isGetter }
    }
    val traitSetterMembers = traitTypeSymbols flatMap { sym =>
      sym.toType.members filter { mem => mem.isTerm && mem.asTerm.isSetter }
    }

    expectResult(Set(), "No var trait members expected") {
      traitVarMembers
    }
    expectResult(Set(), "No val trait members expected") {
      traitValMembers
    }
    expectResult(Set(), "No getter trait members expected") {
      traitGetterMembers
    }
    expectResult(Set(), "No setter trait members expected") {
      traitSetterMembers
    }
  }

  private def collectTypes(tpe: Type, p: Type => Boolean, remainingDepth: Int = 10): Seq[Type] = {
    val terms: Iterable[Symbol] = tpe.members filter { _.isTerm }
    val baseClasses: Seq[Symbol] = tpe.baseClasses
    val baseClassTypes = baseClasses map { sym => sym.asType.toType }

    val termTypeSignatures = terms map { _.typeSignatureIn(tpe) }

    val termTypes: Set[Type] = termTypeSignatures.toSet flatMap { (tpe: Type) =>
      tpe match {
        case methodType: MethodType => methodSignatureTypes(methodType)
        case tpe: Type => Set(tpe)
      }
    }

    val currTypes = (baseClassTypes ++ termTypes :+ tpe).distinct.filter(p)
    val result =
      if (remainingDepth >= 1) currTypes
      else currTypes ++ currTypes.flatMap(tpe => collectTypes(tpe, p, remainingDepth - 1))
    result.distinct
  }

  private def methodSignatureTypes(methodType: MethodType): Set[Type] = {
    val resultType = methodType.resultType
    val paramTypes = methodType.params map { par => par.typeSignature }
    paramTypes.toSet + resultType
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
