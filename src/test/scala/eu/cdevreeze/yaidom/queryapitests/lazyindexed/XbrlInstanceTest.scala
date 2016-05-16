/*
 * Copyright 2011-2014 Chris de Vreeze
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

package eu.cdevreeze.yaidom.queryapitests.lazyindexed

import scala.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.queryapi.ElemWithPath
import eu.cdevreeze.yaidom.queryapi.HasParent
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapitests.AbstractXbrlInstanceTest

/**
 * XBRL instance test case for lazy indexed elements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XbrlInstanceTest extends AbstractXbrlInstanceTest {

  type MyElem = yaidom.indexed.LazyIndexedScopedElem[yaidom.simple.Elem]

  object MyElemFunctionApi extends ElemFunctionApi[MyElem] with ScopedElemLike.FunctionApi[MyElem] with HasParent.FunctionApi[MyElem] {

    // ElemLike gaps

    def findAllChildElems(thisElem: MyElem): immutable.IndexedSeq[MyElem] = {
      thisElem.findAllChildElems
    }

    // HasEName gaps

    def resolvedName(thisElem: MyElem): EName = thisElem.resolvedName

    def resolvedAttributes(thisElem: MyElem): immutable.Iterable[(EName, String)] = {
      thisElem.resolvedAttributes
    }

    // HasText gaps

    def text(thisElem: MyElem): String = thisElem.text

    // HasQNameApi

    def qname(thisElem: MyElem): QName = thisElem.qname

    def attributes(thisElem: MyElem): immutable.Iterable[(QName, String)] = {
      thisElem.attributes
    }

    // HasScopeApi

    def scope(thisElem: MyElem): Scope = thisElem.scope

    // HasParent gaps

    def parentOption(thisElem: MyElem): Option[MyElem] = {
      val path = thisElem.path
      val parentPathOption = path.parentPathOption

      val parentOption =
        parentPathOption.map(pp => yaidom.indexed.LazyIndexedScopedElem(thisElem.rootElem.getElemOrSelfByPath(pp), pp))
      parentOption
    }

    // Other methods

    def path(thisElem: MyElem): Path = {
      thisElem.path
    }

    def toSimpleElem(thisElem: MyElem): yaidom.simple.Elem = {
      thisElem.elem
    }
  }

  final class MyElemWithApi(val elem: MyElem) extends ElemWithApi {

    type E = MyElem

    val functionApi = MyElemFunctionApi

    def withElem(newElem: MyElem): ElemWithApi.Aux[MyElem] = {
      new MyElemWithApi(newElem)
    }
  }

  protected final def elemWithApi: ElemWithApi = {
    val docParser = DocumentParserUsingSax.newInstance()
    val doc = docParser.parse(sampleXbrlInstanceFile)

    val docElem: MyElem = yaidom.indexed.LazyIndexedScopedElem(doc.documentElement)

    new MyElemWithApi(docElem)
  }
}
