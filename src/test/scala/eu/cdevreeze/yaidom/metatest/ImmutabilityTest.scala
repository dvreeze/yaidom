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
package metatest

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import NodeBuilder._
import Scope._

/**
 * Test case, naively checking some classes for probable immutability (in the absence of an effect system), using Scala 2.10 reflection.
 * Disclaimer: my knowledge of Scala reflection is almost non-existent, so these are baby steps in that regard.
 *
 * For one of the introductions to Scala reflection, see http://dcsobral.blogspot.nl/2012/07/json-serialization-with-reflection-in.html.
 * Note that Scala 2.10 reflection still seems to be in flux, so the actual incantations may be different than found there.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ImmutabilityTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.metatest")

  // Remember: universe is a lazy val, not a package! It is a scala.reflect.api.JavaUniverse
  // As a result of the import, all members of the JavaUniverse (slices of the) cake are in scope, such as method typeOf and the many path-dependent types
  import scala.reflect.runtime.universe._

  @Test def testPrintTree() {
    val expr: Expr[Unit] = reify {
      final class Person(val name: String, val children: Vector[String]) extends Immutable {

        def numberOfChildren: Int = children.size
      }
    }

    val rawTreeString = showRaw(expr.tree)
    logger.info("Raw tree:%n%s".format(rawTreeString))

    val treeString = show(expr.tree)
    logger.info("Tree:%n%s".format(treeString))
  }

  @Test def testPrintAnotherTree() {
    val expr: Expr[Unit] = reify {
      final case class Person(name: String, children: Vector[String]) extends Immutable {

        def numberOfChildren: Int = children.size
      }
    }

    val rawTreeString = showRaw(expr.tree)
    logger.info("Raw tree:%n%s".format(rawTreeString))

    val treeString = show(expr.tree)
    logger.info("Tree:%n%s".format(treeString))
  }

  @Test def testSomeKnownTypesForImmutability() {
    expect(true) {
      isLikelyImmutableType(typeOf[String])
    }

    expect(false) {
      isLikelyImmutableType(typeOf[java.util.Date])
    }

    expect(false) {
      isLikelyImmutableType(typeOf[Vector[Any]])
    }
    expect(true) {
      isLikelyImmutableType(typeOf[Vector[String]])
    }
    expect(false) {
      isLikelyImmutableType(typeOf[Vector[Option[java.util.Date]]])
    }
    expect(true) {
      isLikelyImmutableType(typeOf[Vector[Option[String]]])
    }

    expect(false) {
      isLikelyImmutableType(typeOf[scala.collection.mutable.ArrayBuffer[String]])
    }
  }

  @Test def testImmutabilityOfPrefixedName() {
    val tpe: Type = typeOf[PrefixedName]

    checkImmutabilityOfType(tpe)
  }

  @Test def testImmutabilityOfUnprefixedName() {
    val tpe: Type = typeOf[UnprefixedName]

    checkImmutabilityOfType(tpe)
  }

  @Test def testImmutabilityOfEName() {
    val tpe: Type = typeOf[EName]

    checkImmutabilityOfType(tpe)
  }

  @Test def testImmutabilityOfScope() {
    val tpe: Type = typeOf[eu.cdevreeze.yaidom.Scope]

    val getters: Iterable[TermSymbol] = tpe.members collect { case m if m.isTerm => m.asTermSymbol } filter { _.isGetter }

    checkImmutabilityOfType(tpe)
  }

  @Test def testImmutabilityOfScopeDeclarations() {
    val tpe: Type = typeOf[eu.cdevreeze.yaidom.Scope.Declarations]

    checkImmutabilityOfType(tpe)
  }

  @Test def testImmutabilityOfElem() {
    val tpe: Type = typeOf[eu.cdevreeze.yaidom.Elem]

    checkImmutabilityOfType(tpe)
  }

  @Test def testImmutabilityOfDocument() {
    val tpe: Type = typeOf[eu.cdevreeze.yaidom.Document]

    checkImmutabilityOfType(tpe)
  }

  @Test def testImmutabilityOfElemBuilder() {
    val tpe: Type = typeOf[eu.cdevreeze.yaidom.ElemBuilder]

    checkImmutabilityOfType(tpe)
  }

  @Test def testImmutabilityOfDocBuilder() {
    val tpe: Type = typeOf[eu.cdevreeze.yaidom.DocBuilder]

    checkImmutabilityOfType(tpe)
  }

  private def checkImmutabilityOfType(tpe: Type) {
    val getters: Iterable[TermSymbol] = tpe.members collect { case m if m.isTerm => m.asTermSymbol } filter { _.isGetter }
    val setters: Iterable[TermSymbol] = tpe.members collect { case m if m.isTerm => m.asTermSymbol } filter { _.isSetter }

    expect(true) {
      !getters.isEmpty
    }
    expect(Nil) {
      setters.toList
    }

    val getterMethods: Iterable[MethodSymbol] = getters map { m => m.asMethodSymbol }
    val getterResultTypes: Iterable[Type] = getterMethods map { m => m.typeSignature.resultType }

    expect(true) {
      getterResultTypes forall { t => isLikelyImmutableType(t) }
    }
  }

  private def isLikelyImmutableType(tpe: Type): Boolean = {
    // Very naive incomplete attempt
    tpe match {
      case t if knownToBeImmutable(t) => true
      case t if !t.typeArguments.isEmpty => {
        val rawTypeImmutable =
          declaredImmutable(t) || (t.typeConstructor =:= typeOf[Option[Any]].typeConstructor)
        val typeArgsImmutable = t.typeArguments forall { t => isLikelyImmutableType(t) }
        rawTypeImmutable && typeArgsImmutable
      }
      case _ => declaredImmutable(tpe)
    }
  }

  private def declaredImmutable(tpe: Type): Boolean = {
    val baseClasses = tpe.baseClasses map { sym => sym.asTypeSymbol.asType }
    val isImmutable = baseClasses exists { t => t =:= typeOf[Immutable] }
    val isMutable = baseClasses exists { t => t =:= typeOf[Mutable] }
    isImmutable && !isMutable
  }

  private def knownToBeImmutable(tpe: Type): Boolean = {
    // Extremely naive and incomplete
    List(
      typeOf[String],
      typeOf[Int],
      typeOf[Long],
      typeOf[java.lang.Integer],
      typeOf[java.lang.Long],
      typeOf[java.net.URI],
      typeOf[java.rmi.server.UID]) exists { t => t =:= tpe }
  }
}
