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

package eu.cdevreeze.yaidom

/**
 * Saxon-based `BackingNodes.Elem` implementation that can be used as underlying element implementation in
 * any "yaidom dialect". If Saxon tiny trees are used under the hood, this implementation is very efficient,
 * in particular in memory footprint.
 *
 * This package depends on the current latest major Saxon version considered stable, like 9.8. If other
 * major Saxon versions must be supported, consider copying and adapting this code, or yaidom itself
 * should provide a separate source tree (with copied and adapted code) from which artifacts are created
 * that target that other Saxon major version(s).
 *
 * The dependency is on Saxon-HE, so features of Saxon-EE are not used here. That does not mean that they
 * are not accessible, of course.
 *
 * @author Chris de Vreeze
 */
package object saxon
