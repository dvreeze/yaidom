/*
 * Copyright 2011-2017 Chris de Vreeze
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

package eu.cdevreeze.yaidom.utils

import scala.util.Try

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope

/**
 * TextENameExtractor that treats the text as a QName, to be resolved using the passed Scope.
 */
object SimpleTextENameExtractor extends TextENameExtractor {

  def extractENames(scope: Scope, text: String): Set[EName] = {
    val ename =
      Try(scope.resolveQNameOption(QName(text)).get).toOption.getOrElse(
        sys.error(s"String '${text}' could not be resolved in scope $scope"))

    Set(ename)
  }
}
