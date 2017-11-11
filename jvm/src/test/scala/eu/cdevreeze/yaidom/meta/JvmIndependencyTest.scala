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

package eu.cdevreeze.yaidom.meta

import java.io.File

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import scala.collection.immutable
import scala.meta._

/**
 * Test case checking that the "shared" code base can be used targeting Scala.js, by inspecting import statements.
 *
 * This check is not complete. For example, it only looks at import statements, so misses java.lang classes like
 * java.lang.Runtime (which can not be used in Scala.js). It it also naive in the kinds of import statements expected.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class JvmIndependencyTest extends FunSuite {

  import JvmIndependencyTest.ImportedName

  private val mainScalaDirs: immutable.IndexedSeq[File] = {
    val euDir = new File(classOf[JvmIndependencyTest].getResource("/eu").toURI)
    assert(euDir.isDirectory)
    val testClassDir = euDir.getParentFile
    assert(testClassDir.isDirectory)
    val projDir = getProjectDir(testClassDir).ensuring(_.getName == "jvm")

    val sharedProjectDir = new File(projDir.getParentFile, "shared")
    val sharedResultDir = new File(sharedProjectDir, "src/main/scala")

    val jsProjectDir = new File(projDir.getParentFile, "js")
    val jsResultDir = new File(jsProjectDir, "src/main/scala")

    immutable.IndexedSeq(sharedResultDir, jsResultDir).ensuring(_.forall(_.isDirectory))
  }

  test("testJvmIndependency") {
    val sources = mainScalaDirs.flatMap(d => findSourcesInDirTree(d)).ensuring(_.size >= 50)

    testJvmIndependency(sources)
  }

  private def testJvmIndependency(sources: immutable.IndexedSeq[Source]): Unit = {
    val importers: immutable.IndexedSeq[Importer] = sources.flatMap(source => findImporters(source))

    val javaIoImporters = importers.filter(isJavaIoImporter)
    val disallowedJavaIoImporters = javaIoImporters.filterNot(isAllowedJavaIoImporter)

    assertResult(Nil) {
      disallowedJavaIoImporters
    }

    val javaMathImporters = importers.filter(isJavaMathImporter)
    val disallowedJavaMathImporters = javaMathImporters.filterNot(isAllowedJavaMathImporter)

    assertResult(Nil) {
      disallowedJavaMathImporters
    }

    val javaNetImporters = importers.filter(isJavaNetImporter)
    val disallowedJavaNetImporters = javaNetImporters.filterNot(isAllowedJavaNetImporter)

    assertResult(Nil) {
      disallowedJavaNetImporters
    }

    val javaUtilImporters = importers.filter(isJavaUtilImporter)
    val disallowedJavaUtilImporters = javaUtilImporters.filterNot(isAllowedJavaUtilImporter)

    assertResult(Nil) {
      disallowedJavaUtilImporters
    }

    val javaNioImporters = importers.filter(isJavaNioImporter)
    val disallowedJavaNioImporters = javaNioImporters.filterNot(isAllowedJavaNioImporter)

    assertResult(Nil) {
      disallowedJavaNioImporters
    }

    val javaSecurityImporters = importers.filter(isJavaSecurityImporter)
    val disallowedJavaSecurityImporters = javaSecurityImporters.filterNot(isAllowedJavaSecurityImporter)

    assertResult(Nil) {
      disallowedJavaSecurityImporters
    }

    val nonJavaImporters = importers.filterNot(isJavaImporter)

    val scalaImporters = nonJavaImporters.filter(isScalaImporter)
    val disallowedScalaImporters = scalaImporters.filterNot(isAllowedScalaImporter)

    assertResult(Nil) {
      disallowedScalaImporters
    }

    val otherImporters =
      importers.filterNot(isJavaImporter).filterNot(isScalaImporter).
        filterNot(isScalaJsDomImporter).filterNot(isYaidomImporter)

    assertResult(Nil) {
      otherImporters.filterNot(isOtherAllowedImporter)
    }
  }

  private def findSourcesInDirTree(dir: File): immutable.IndexedSeq[Source] = {
    require(!dir.isFile)

    dir.listFiles.toVector flatMap {
      case f if f.isFile && f.getName.endsWith(".scala") =>
        Vector(f.parse[Source].get)
      case d if d.isDirectory =>
        // Recursive call
        findSourcesInDirTree(d)
      case _ =>
        Vector()
    }
  }

  private def findImporters(source: Source): immutable.Seq[Importer] = {
    source collect { case i: Importer => i }
  }

  private def isScalaImporter(importer: Importer): Boolean = {
    val termNames = importer.ref collect { case tn: Term.Name => tn }

    termNames.take(1).map(_.structure) ==
      List(Term.Name("scala")).map(_.structure)
  }

  private def isAllowedScalaImporter(importer: Importer): Boolean = {
    isScalaImporter(importer) && {
      val termNames = importer.ref collect { case tn: Term.Name => tn }

      !termNames.map(_.structure).contains(Term.Name("parallel").structure) &&
        !termNames.map(_.structure).contains(Term.Name("tools").structure)
    }
  }

  private def isJavaImporter(importer: Importer): Boolean = {
    val termNames = importer.ref collect { case tn: Term.Name => tn }

    termNames.take(1).map(_.structure) ==
      List(Term.Name("java")).map(_.structure)
  }

  private def isJavaIoImporter(importer: Importer): Boolean = {
    val termNames = importer.ref collect { case tn: Term.Name => tn }

    termNames.take(2).map(_.structure) ==
      List(Term.Name("java"), Term.Name("io")).map(_.structure)
  }

  private def isAllowedJavaIoImporter(importer: Importer): Boolean = {
    isJavaIoImporter(importer) && {
      val termNames = importer.ref collect { case tn: Term.Name => tn }
      val importeeNames = importer collect { case n: Name.Indeterminate => n }

      val importedNames: immutable.IndexedSeq[ImportedName] =
        importeeNames.toIndexedSeq.map(n => ImportedName(termNames, n))

      importedNames forall { nm =>
        allowedJavaIoImporters.map(_.names.map(_.structure)).contains(nm.names.map(_.structure))
      }
    }
  }

  private def isJavaMathImporter(importer: Importer): Boolean = {
    val termNames = importer.ref collect { case tn: Term.Name => tn }

    termNames.take(2).map(_.structure) ==
      List(Term.Name("java"), Term.Name("math")).map(_.structure)
  }

  private def isAllowedJavaMathImporter(importer: Importer): Boolean = {
    isJavaMathImporter(importer) && {
      val termNames = importer.ref collect { case tn: Term.Name => tn }
      val importeeNames = importer collect { case n: Name.Indeterminate => n }

      val importedNames: immutable.IndexedSeq[ImportedName] =
        importeeNames.toIndexedSeq.map(n => ImportedName(termNames, n))

      importedNames forall { nm =>
        allowedJavaMathImporters.map(_.names.map(_.structure)).contains(nm.names.map(_.structure))
      }
    }
  }

  private def isJavaNetImporter(importer: Importer): Boolean = {
    val termNames = importer.ref collect { case tn: Term.Name => tn }

    termNames.take(2).map(_.structure) ==
      List(Term.Name("java"), Term.Name("net")).map(_.structure)
  }

  private def isAllowedJavaNetImporter(importer: Importer): Boolean = {
    isJavaNetImporter(importer) && {
      val termNames = importer.ref collect { case tn: Term.Name => tn }
      val importeeNames = importer collect { case n: Name.Indeterminate => n }

      val importedNames: immutable.IndexedSeq[ImportedName] =
        importeeNames.toIndexedSeq.map(n => ImportedName(termNames, n))

      importedNames forall { nm =>
        allowedJavaNetImporters.map(_.names.map(_.structure)).contains(nm.names.map(_.structure))
      }
    }
  }

  private def isJavaUtilImporter(importer: Importer): Boolean = {
    val termNames = importer.ref collect { case tn: Term.Name => tn }

    termNames.take(2).map(_.structure) ==
      List(Term.Name("java"), Term.Name("util")).map(_.structure)
  }

  private def isAllowedJavaUtilImporter(importer: Importer): Boolean = {
    isJavaUtilImporter(importer) && {
      val termNames = importer.ref collect { case tn: Term.Name => tn }
      val importeeNames = importer collect { case n: Name.Indeterminate => n }

      val importedNames: immutable.IndexedSeq[ImportedName] =
        importeeNames.toIndexedSeq.map(n => ImportedName(termNames, n))

      importedNames forall { nm =>
        allowedJavaUtilImporters.map(_.names.map(_.structure)).contains(nm.names.map(_.structure)) ||
          allowedJavaUtilConcurrentImporters.map(_.names.map(_.structure)).contains(nm.names.map(_.structure)) ||
          allowedJavaUtilConcurrentAtomicImporters.map(_.names.map(_.structure)).contains(nm.names.map(_.structure)) ||
          allowedJavaUtilConcurrentLocksImporters.map(_.names.map(_.structure)).contains(nm.names.map(_.structure)) ||
          allowedJavaUtilRegexImporters.map(_.names.map(_.structure)).contains(nm.names.map(_.structure))
      }
    }
  }

  private def isJavaNioImporter(importer: Importer): Boolean = {
    val termNames = importer.ref collect { case tn: Term.Name => tn }

    termNames.take(2).map(_.structure) ==
      List(Term.Name("java"), Term.Name("nio")).map(_.structure)
  }

  private def isAllowedJavaNioImporter(importer: Importer): Boolean = {
    isJavaNioImporter(importer) && {
      val termNames = importer.ref collect { case tn: Term.Name => tn }
      val importeeNames = importer collect { case n: Name.Indeterminate => n }

      val importedNames: immutable.IndexedSeq[ImportedName] =
        importeeNames.toIndexedSeq.map(n => ImportedName(termNames, n))

      importedNames forall { nm =>
        allowedJavaNioImporters.map(_.names.map(_.structure)).contains(nm.names.map(_.structure)) ||
          allowedJavaNioCharsetImporters.map(_.names.map(_.structure)).contains(nm.names.map(_.structure))
      }
    }
  }

  private def isJavaSecurityImporter(importer: Importer): Boolean = {
    val termNames = importer.ref collect { case tn: Term.Name => tn }

    termNames.take(2).map(_.structure) ==
      List(Term.Name("java"), Term.Name("security")).map(_.structure)
  }

  private def isAllowedJavaSecurityImporter(importer: Importer): Boolean = {
    isJavaSecurityImporter(importer) && {
      val termNames = importer.ref collect { case tn: Term.Name => tn }
      val importeeNames = importer collect { case n: Name.Indeterminate => n }

      val importedNames: immutable.IndexedSeq[ImportedName] =
        importeeNames.toIndexedSeq.map(n => ImportedName(termNames, n))

      importedNames forall { nm =>
        allowedJavaSecurityImporters.map(_.names.map(_.structure)).contains(nm.names.map(_.structure))
      }
    }
  }

  private def isScalaJsDomImporter(importer: Importer): Boolean = {
    val termNames = importer.ref collect { case tn: Term.Name => tn }

    termNames.take(3).map(_.structure) ==
      List(Term.Name("org"), Term.Name("scalajs"), Term.Name("dom")).map(_.structure)
  }

  private def isYaidomImporter(importer: Importer): Boolean = {
    val termNames = importer.ref collect { case tn: Term.Name => tn }

    termNames.take(3).map(_.structure) ==
      List(Term.Name("eu"), Term.Name("cdevreeze"), Term.Name("yaidom")).map(_.structure)
  }

  private def isOtherAllowedImporter(importer: Importer): Boolean = {
    val termNames = importer.ref collect { case tn: Term.Name => tn } ensuring (_.nonEmpty)

    // Rather ad-hoc, but all related to yaidom
    val allowedFirstTermNames =
      List("Declarations", "Scope", "scope", "enameProvider", "ElemApi", "NodeBuilder", "XmlSchemas").map(n => Term.Name(n).structure)

    allowedFirstTermNames.contains(termNames.head.structure)
  }

  private def getProjectDir(dir: File): File = {
    require((dir ne null) && dir.isDirectory, s"Expected directory '${dir}' but this is not a directory")

    if ((dir.getName == "yaidom") || (dir.getName == "jvm") || (dir.getName == "shared") || (dir.getName == "js")) {
      dir
    } else {
      val parentDir = dir.getParentFile
      require(parentDir ne null, s"Unexpected directory '${dir}' without parent directory")

      // Recursive call
      getProjectDir(parentDir)
    }
  }

  private val allowedJavaIoImporters: immutable.IndexedSeq[ImportedName] =
    immutable.IndexedSeq[String](
      "BufferedReader",
      "ByteArrayInputStream",
      "ByteArrayOutputStream",
      "Closeable",
      "DataInput",
      "DataInputStream",
      "FilterInputStream",
      "FilterOutputStream",
      "Flushable",
      "InputStream",
      "InputStreamReader",
      "OutputStream",
      "OutputStreamWriter",
      "PrintStream",
      "PrintWriter",
      "Reader",
      "Serializable",
      "StringReader",
      "StringWriter",
      "Throwables",
      "Writer").map(s => ImportedName(List(Term.Name("java"), Term.Name("io")), Name.Indeterminate(s)))

  private val allowedJavaMathImporters: immutable.IndexedSeq[ImportedName] =
    immutable.IndexedSeq[String](
      "BigDecimal",
      "BigInteger",
      "BitLevel",
      "Conversion",
      "Division",
      "Elementary",
      "Logical",
      "MathContext",
      "Multiplication",
      "Primality",
      "RoundingMode").map(s => ImportedName(List(Term.Name("java"), Term.Name("math")), Name.Indeterminate(s)))

  private val allowedJavaNetImporters: immutable.IndexedSeq[ImportedName] =
    immutable.IndexedSeq[String](
      "Throwables",
      "URI",
      "URLDecoder").map(s => ImportedName(List(Term.Name("java"), Term.Name("net")), Name.Indeterminate(s)))

  private val allowedJavaNioImporters: immutable.IndexedSeq[ImportedName] =
    immutable.IndexedSeq[String](
      "Buffer",
      "BufferOverflowException",
      "BufferUnderflowException",
      "ByteArrayBits",
      "ByteBuffer",
      "ByteOrder",
      "CharBuffer",
      "DataViewCharBuffer",
      "DataViewDoubleBuffer",
      "DataViewFloatBuffer",
      "DataViewIntBuffer",
      "DataViewLongBuffer",
      "DataViewShortBuffer",
      "DoubleBuffer",
      "FloatBuffer",
      "GenBuffer",
      "GenDataViewBuffer",
      "GenHeapBuffer",
      "GenHeapBufferView",
      "GenTypedArrayBuffer",
      "HeapByteBuffer",
      "HeapByteBufferCharView",
      "HeapByteBufferDoubleView",
      "HeapByteBufferFloatView",
      "HeapByteBufferIntView",
      "HeapByteBufferLongView",
      "HeapByteBufferShortView",
      "HeapCharBuffer",
      "HeapDoubleBuffer",
      "HeapFloatBuffer",
      "HeapIntBuffer",
      "HeapLongBuffer",
      "HeapShortBuffer",
      "IntBuffer",
      "InvalidMarkException",
      "LongBuffer",
      "ReadOnlyBufferException",
      "ShortBuffer",
      "StringCharBuffer",
      "TypedArrayByteBuffer",
      "TypedArrayCharBuffer",
      "TypedArrayDoubleBuffer",
      "TypedArrayFloatBuffer",
      "TypedArrayIntBuffer",
      "TypedArrayShortBuffer").map(s => ImportedName(List(Term.Name("java"), Term.Name("nio")), Name.Indeterminate(s)))

  private val allowedJavaNioCharsetImporters: immutable.IndexedSeq[ImportedName] =
    immutable.IndexedSeq[String](
      "CharacterCodingException",
      "Charset",
      "CharsetDecoder",
      "CharsetEncoder",
      "CoderMalfunctionError",
      "CoderResult",
      "CodingErrorAction",
      "MalformedInputException",
      "StandardCharsets",
      "UnmappableCharacterException",
      "UnsupportedCharsetException").map(s => ImportedName(List(Term.Name("java"), Term.Name("nio"), Term.Name("charset")), Name.Indeterminate(s)))

  private val allowedJavaSecurityImporters: immutable.IndexedSeq[ImportedName] =
    immutable.IndexedSeq[String](
      "Guard",
      "Permission",
      "Throwables").map(s => ImportedName(List(Term.Name("java"), Term.Name("security")), Name.Indeterminate(s)))

  private val allowedJavaUtilImporters: immutable.IndexedSeq[ImportedName] =
    immutable.IndexedSeq[String](
      "AbstractCollection",
      "AbstractList",
      "AbstractMap",
      "AbstractQueue",
      "AbstractRandomAccessListIterator",
      "AbstractSequentialList",
      "AbstractSet",
      "ArrayDeque",
      "ArrayList",
      "Arrays",
      "Collection",
      "Collections",
      "Comparator",
      "Date",
      "Deque",
      "Dictionary",
      "Enumeration",
      "EventObject",
      "Formattable",
      "FormattableFlags",
      "Formatter",
      "HashMap",
      "HashSet",
      "Hashtable",
      "Iterator",
      "LinkedHashMap",
      "LinkedHashSet",
      "LinkedList",
      "List",
      "ListIterator",
      "Map",
      "NavigableMap",
      "NavigableSet",
      "NavigableView",
      "Objects",
      "Optional",
      "PriorityQueue",
      "Properties",
      "Queue",
      "Random",
      "RandomAccess",
      "Set",
      "SizeChangeEvent",
      "SortedMap",
      "SortedSet",
      "Throwables",
      "Timer",
      "TimerTask",
      "TreeSet",
      "UUID").map(s => ImportedName(List(Term.Name("java"), Term.Name("util")), Name.Indeterminate(s)))

  private val allowedJavaUtilConcurrentImporters: immutable.IndexedSeq[ImportedName] =
    immutable.IndexedSeq[String](
      "Callable",
      "ConcurrentHashMap",
      "ConcurrentLinkedQueue",
      "ConcurrentMap",
      "ConcurrentSkipListSet",
      "CopyOnWriteArrayList",
      "Executor",
      "ThreadFactory",
      "ThreadLocalRandom",
      "Throwables",
      "TimeUnit").map(s => ImportedName(List(Term.Name("java"), Term.Name("util"), Term.Name("concurrent")), Name.Indeterminate(s)))

  private val allowedJavaUtilConcurrentAtomicImporters: immutable.IndexedSeq[ImportedName] =
    immutable.IndexedSeq[String](
      "AtomicBoolean",
      "AtomicInteger",
      "AtomicLong",
      "AtomicLongArray",
      "AtomicReferencce",
      "AtomicReferenceArray").map(s =>
        ImportedName(List(Term.Name("java"), Term.Name("util"), Term.Name("concurrent"), Term.Name("atomic")), Name.Indeterminate(s)))

  private val allowedJavaUtilConcurrentLocksImporters: immutable.IndexedSeq[ImportedName] =
    immutable.IndexedSeq[String](
      "Lock",
      "ReentrantLock").map(s =>
        ImportedName(List(Term.Name("java"), Term.Name("util"), Term.Name("concurrent"), Term.Name("locks")), Name.Indeterminate(s)))

  private val allowedJavaUtilRegexImporters: immutable.IndexedSeq[ImportedName] =
    immutable.IndexedSeq[String](
      "Matcher",
      "MatcherResult",
      "Pattern").map(s => ImportedName(List(Term.Name("java"), Term.Name("util"), Term.Name("regex")), Name.Indeterminate(s)))
}

object JvmIndependencyTest {

  final case class ImportedName(termNames: List[Term.Name], importeeName: Name.Indeterminate) {

    def names: immutable.IndexedSeq[Name] = termNames.toIndexedSeq :+ importeeName
  }
}
