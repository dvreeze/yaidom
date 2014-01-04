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

import org.scalatest.Tag

/**
 * ScalaTest tags. For example, the tag PerformanceTest, which is used for performance tests which are not run in a normal
 * build, and which require some environment to be set up (such as a root directory under which XML test data files can be
 * found).
 *
 * To exclude performance tests tagged PerformanceTest, while in an sbt session, with performance tests on the test classpath,
 * enter the following:
 * {{{
 * test-only * -- -l eu.cdevreeze.yaidom.testtag.PerformanceTest
 * }}}
 *
 * On the other hand, the performance tests are in their own source tree, which by default is not added to the test classpath,
 * so these tests are not run by default.
 *
 * @author Chris de Vreeze
 */
package object testtag {

  object PerformanceTest extends Tag("eu.cdevreeze.yaidom.testtag.PerformanceTest")
}
