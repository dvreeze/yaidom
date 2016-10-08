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

package eu.cdevreeze.yaidom.java8

/**
 * The functional streaming API that can be used in Java 8. This package contains the purely abstract API.
 * It can be implemented for yaidom element implementations, but also for other ones, including element implementations
 * that do not offer a yaidom query API, like Saxon NodeInfo, XOM, or DOM.
 *
 * DO NOT USE THIS PACKAGE WHEN RUNNING ON JAVA BEFORE VERSION 8!
 *
 * @author Chris de Vreeze
 */
package object functionapi
