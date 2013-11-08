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

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalacheck.{ Prop, Gen, Arbitrary }
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.prop.Checkers
import org.scalatest.junit.JUnitRunner

/**
 * Scope properties test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ScopePropTest extends Suite with Checkers {

  import Prop._
  import Gen._
  import Arbitrary.arbitrary

  @Test def testResolveProperty(): Unit = {
    check({ (scope1: Scope, scope2: Scope) =>
      scope1.resolve(scope1.relativize(scope2)) == scope2
    }, minSuccessful(500))
  }

  @Test def testRelativizeProperty(): Unit = {
    check({ (scope: Scope, decls: Declarations) =>
      scope.relativize(scope.resolve(decls)) == scope.minimize(decls)
    }, minSuccessful(500))
  }

  @Test def testMinimizeProperty(): Unit = {
    check({ (scope: Scope, decls: Declarations) =>
      scope.resolve(scope.minimize(decls)) == scope.resolve(decls)
    }, minSuccessful(500))
  }

  // The generators are chosen in such a way, that Scope-pairs and (Scope, Declaration)-pairs are not unlikely to overlap.

  private val genPrefix: Gen[String] = {
    oneOf(Seq("", "x", "y", "z", "longer-prefix"))
  }

  private val genNsUri: Gen[String] = {
    oneOf(Seq(
      "http://www.w3.org/2001/XMLSchema",
      "http://www.xbrl.org/2003/instance",
      "http://www.nltaxonomie.nl/sbi/sbi2008",
      "http://abcdef.org"))
  }

  private val genScope: Gen[Scope] = {
    val genPrefixNsUriPair: Gen[(String, String)] =
      for {
        prefix <- genPrefix
        nsUri <- genNsUri
      } yield (prefix, nsUri)

    val genPrefixNsUriPairs = listOfN(5, genPrefixNsUriPair)

    for {
      pairs <- genPrefixNsUriPairs
      somePairs <- someOf(pairs)
    } yield Scope.from(pairs.toMap)
  }

  private val genDeclarations: Gen[Declarations] = {
    val genPrefixes = someOf(genPrefix, genPrefix)

    for {
      scope <- genScope
      prefixes <- oneOf(value(Seq[String]()), genPrefixes)
      undecls = prefixes.map(pref => (pref -> "")).toMap
    } yield Declarations.from(scope.prefixNamespaceMap ++ undecls)
  }

  private implicit val arbritraryScope: Arbitrary[Scope] = Arbitrary(genScope)

  private implicit val arbritraryDeclarations: Arbitrary[Declarations] = Arbitrary(genDeclarations)
}
