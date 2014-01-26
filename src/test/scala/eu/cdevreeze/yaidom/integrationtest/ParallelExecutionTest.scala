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
package integrationtest

import java.{ util => jutil, io => jio }
import java.util.{ concurrent => juc }
import scala.collection.{ immutable, mutable }
import scala.concurrent._
import scala.concurrent.duration._
import scala.util.{ Try, Success, Failure }
import org.junit.{ Test, Before }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll, Ignore }
import org.scalatest.junit.JUnitRunner
import convert.ScalaXmlConversions._
import parse._
import print._

/**
 * Parallel execution test. It starts 10 "threads of execution", in which elements are serialized, parsed, and transformed.
 * In the end, the resulting elements must be equal to the original. This test also tests the use of thread-local parsers
 * and printers.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ParallelExecutionTest extends Suite with BeforeAndAfterAll {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val threadPool = juc.Executors.newFixedThreadPool(10)

  @Test def testParallelExecutionUsingDom(): Unit = {
    doTestParallelExecution(
      { () => DocumentParserUsingDom.newInstance },
      { () => DocumentPrinterUsingDom.newInstance })
  }

  @Test def testParallelExecutionUsingDomLS(): Unit = {
    doTestParallelExecution(
      { () => DocumentParserUsingDomLS.newInstance },
      { () => DocumentPrinterUsingDomLS.newInstance })
  }

  @Test def testParallelExecutionUsingSax(): Unit = {
    doTestParallelExecution(
      { () => DocumentParserUsingSax.newInstance },
      { () => DocumentPrinterUsingSax.newInstance })
  }

  @Test def testParallelExecutionUsingStax(): Unit = {
    doTestParallelExecution(
      { () => DocumentParserUsingStax.newInstance },
      { () => DocumentPrinterUsingStax.newInstance })
  }

  @Test def testNumberOrRefEqualENamesUsingDefaultENameProvider(): Unit = {
    val enameProvider = new ENameProvider.TrivialENameProvider
    val refEqualGrandChildENameCount = 1

    // No grandChild element ENames are reference-equal
    doTestNumberOrRefEqualENames(enameProvider, refEqualGrandChildENameCount)
  }

  @Test def testNumberOrRefEqualENamesUsingCachingENameProvider(): Unit = {
    val enameProvider = new ENameProvider.SimpleCachingENameProvider
    val refEqualGrandChildENameCount = 250

    // All grandChild element ENames are reference-equal
    doTestNumberOrRefEqualENames(enameProvider, refEqualGrandChildENameCount)
  }

  @Test def testNumberOrRefEqualENamesUsingThreadLocalENameProvider(): Unit = {
    val enameProvider = new ENameProvider.ThreadLocalENameProvider({ () => new ENameProvider.SimpleCachingENameProvider })
    val refEqualGrandChildENameCount = 25

    // Per thread, all grandChild element ENames are reference-equal
    doTestNumberOrRefEqualENames(enameProvider, refEqualGrandChildENameCount)
  }

  @Test def testNumberOrRefEqualENamesUsingSharedCachingENameProvider(): Unit = {
    val sharedENameProvider = new ENameProvider.SimpleCachingENameProvider
    val enameProvider = new ENameProvider.ThreadLocalENameProvider({ () => sharedENameProvider })
    val refEqualGrandChildENameCount = 250

    // All grandChild element ENames are reference-equal
    doTestNumberOrRefEqualENames(enameProvider, refEqualGrandChildENameCount)
  }

  private def doTestParallelExecution(parserCreator: () => DocumentParser, printerCreator: () => DocumentPrinter): Unit = {
    val resolvedRootElem = resolved.Elem(rootElem)

    val docParser = new ThreadLocalDocumentParser(parserCreator)
    val docPrinter = new ThreadLocalDocumentPrinter(printerCreator)

    implicit val execContext = ExecutionContext.fromExecutor(threadPool)

    val encoding = "UTF-8"

    val futures: Vector[Future[Elem]] =
      (1 to 10).toVector map { i =>
        future(docPrinter.print(rootElem)) map { xmlString =>
          docParser.parse(new jio.ByteArrayInputStream(xmlString.getBytes(encoding))).documentElement
        } map { elem =>
          elem transformElems { e =>
            if (e.localName == "grandChild")
              e.withChildren(Vector(Text((e.text.toInt * 2).toString, false)))
            else e
          }
        } map { elem =>
          docPrinter.print(elem)
        } map { xmlString =>
          docParser.parse(new jio.ByteArrayInputStream(xmlString.getBytes(encoding))).documentElement
        } map { elem =>
          elem transformElems { e =>
            if (e.localName == "grandChild")
              e.withChildren(Vector(Text((e.text.toInt / 2).toString, false)))
            else e
          }
        }
      }

    for ((f, idx) <- futures.zipWithIndex) {
      f onComplete {
        case Success(elem) =>
          logger.info(s"Chain $idx successful (thread: ${Thread.currentThread}). Going to check the result ...")

          expectResult(resolvedRootElem) {
            resolved.Elem(elem)
          }
        case Failure(t) =>
          logger.warning(s"Chain $idx failed (thread: ${Thread.currentThread}). Failing ...")
          fail(t.toString)
      }
    }

    for (f <- futures) { Await.ready(f, 5.seconds) }
    expectResult(true) {
      futures forall (f => f.isCompleted)
    }
  }

  private def doTestNumberOrRefEqualENames(enameProvider: ENameProvider, numberOfRefEqualGrandChildENames: Int): Unit = {
    ENameProvider.globalMutableInstance = enameProvider

    val docParser = new ThreadLocalDocumentParser(DocumentParserUsingSax.newInstance)
    val docPrinter = new ThreadLocalDocumentPrinter(DocumentPrinterUsingSax.newInstance)

    // No re-use (or pooling) of threads!
    implicit val execContext = ExecutionContext.fromExecutor(new juc.Executor {
      def execute(command: Runnable): Unit = {
        (new Thread(command)).start()
      }
    })

    val encoding = "UTF-8"

    val futures: Vector[Future[Document]] =
      (1 to 10).toVector map { i =>
        future {
          val xmlString = docPrinter.print(rootElem)
          val doc = docParser.parse(new jio.ByteArrayInputStream(xmlString.getBytes(encoding)))
          doc
        }
      }

    for (f <- futures) { Await.ready(f, 5.seconds) }
    expectResult(true) {
      futures forall (f => f.isCompleted)
    }

    val docs = futures.flatMap(f => f.value.get.toOption)

    val firstGrandChildEName = docs.head.documentElement.findElem(_.localName == "grandChild").get.resolvedName

    val refEqualGrandChildENameCount = {
      val refEqualGrandChildElems =
        docs flatMap { doc =>
          doc.documentElement.filterElemsOrSelf(e => e.resolvedName eq firstGrandChildEName)
        }

      refEqualGrandChildElems.size
    }

    ENameProvider.globalMutableInstance = ENameProvider.defaultInstance

    logger.info(s"Found $refEqualGrandChildENameCount 'grandChild' elements with the same reference-equal resolved name as the first one (expected $numberOfRefEqualGrandChildENames)")

    expectResult(numberOfRefEqualGrandChildENames) {
      refEqualGrandChildENameCount
    }
  }

  private val rootElem: Elem = {
    val xml =
      <root>
        <child>
          <grandChild>0</grandChild>
          <grandChild>1</grandChild>
          <grandChild>2</grandChild>
          <grandChild>3</grandChild>
          <grandChild>4</grandChild>
        </child>
        <child>
          <grandChild>5</grandChild>
          <grandChild>6</grandChild>
          <grandChild>7</grandChild>
          <grandChild>8</grandChild>
          <grandChild>9</grandChild>
        </child>
        <child>
          <grandChild>10</grandChild>
          <grandChild>11</grandChild>
          <grandChild>12</grandChild>
          <grandChild>13</grandChild>
          <grandChild>14</grandChild>
        </child>
        <child>
          <grandChild>15</grandChild>
          <grandChild>16</grandChild>
          <grandChild>17</grandChild>
          <grandChild>18</grandChild>
          <grandChild>19</grandChild>
        </child>
        <child>
          <grandChild>20</grandChild>
          <grandChild>21</grandChild>
          <grandChild>22</grandChild>
          <grandChild>23</grandChild>
          <grandChild>24</grandChild>
        </child>
      </root>

    convertToElem(xml)
  }
}
