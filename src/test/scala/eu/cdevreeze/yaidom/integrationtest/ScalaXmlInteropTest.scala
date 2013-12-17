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
import scala.collection.immutable
import org.junit.{ Test, Before }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll, Ignore }
import org.scalatest.junit.JUnitRunner
import NodeBuilder._
import convert.ScalaXmlConversions._

/**
 * Scala XML interoperability test case.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ScalaXmlInteropTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  @Test def testConvert() {
    // 1. Convert XML to Elem

    val root: Elem = convertToElem(bookstore)

    expectResult(Set("Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root.findAllElems map (e => e.localName)).toSet
    }
    expectResult(Set("Bookstore", "Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expectResult(8) {
      root.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expectResult(3) {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 2. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root)(Scope.Empty).build()

    expectResult((root.findAllElems map (e => e.localName)).toSet) {
      (root3.findAllElems map (e => e.localName)).toSet
    }
    expectResult((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root3.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expectResult(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root3.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expectResult {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root3 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 3. Convert to resolved.Elem, and check again

    val root5: resolved.Elem = resolved.Elem(root)

    expectResult((root.findAllElems map (e => e.localName)).toSet) {
      (root5.findAllElems map (e => e.localName)).toSet
    }
    expectResult((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root5.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expectResult(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root5.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expectResult {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root5 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 4. Convert to Scala XML and back

    val root6 = convertToElem(convertElem(root))

    expectResult(resolved.Elem(root)) {
      resolved.Elem(root6)
    }
  }

  @Test def testConvertStrangeXml() {
    // 1. Convert XML to Elem

    val root: Elem = convertToElem(strangeXml)

    // This test works in spite of of bug: SI 6939: Namespace binding (xmlns) is duplicated if a child redefines a prefix.
    // (see https://issues.scala-lang.org/browse/SI-6939 and https://github.com/scala/scala/pull/1858).
    // See method ScalaXmlToYaidomConversions.extractScope for the reason why. That method works around the bug.

    expectResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root)(Scope.Empty).build()

    expectResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 3. Print to tree representation String and parse back that DSL String, and check again

    val treeRepr: String = root.toString

    assert(treeRepr.trim.startsWith("elem("), "Expected the tree representation to start with 'elem('")

    val root4: Elem = {
      import TreeReprParsers._

      val parseResult = parseAll(TreeReprParsers.element, treeRepr)
      parseResult.get.build()
    }

    expectResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root4.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 4. Convert to Scala XML and back

    val root5 = convertToElem(convertElem(root))

    expectResult(resolved.Elem(root)) {
      resolved.Elem(root5)
    }
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/79 */
  @Test def testConvertDefaultNamespaceXml() {
    // 1. Convert XML to Elem

    val root: Elem = convertToElem(trivialXml)

    expectResult(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expectResult(Set(QName("root"), QName("child"))) {
      val result = root.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expectResult("Trivial XML") {
      val result = root.findAllElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }

    // 2. Convert to NodeBuilder and back, and check again

    val document3: eu.cdevreeze.yaidom.Document = DocBuilder.fromDocument(Document(root)).build()
    val root3: Elem = document3.documentElement

    expectResult(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expectResult(Set(QName("root"), QName("child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expectResult("Trivial XML") {
      val result = root3.findAllElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }

    // 3. Print to tree representation String and parse back that DSL String, and check again

    val treeRepr: String = Document(root).toString

    assert(treeRepr.trim.startsWith("document("), "Expected the tree representation to start with 'document('")

    val document4: Document = {
      import TreeReprParsers._

      val parseResult = parseAll(TreeReprParsers.document, treeRepr)
      parseResult.get.build()
    }

    val root4 = document4.documentElement

    expectResult(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root4.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expectResult(Set(QName("root"), QName("child"))) {
      val result = root4.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expectResult("Trivial XML") {
      val result = root4.findAllElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }

    // 4. Convert to Scala XML and back

    val root5 = convertToElem(convertElem(root))

    expectResult(resolved.Elem(root)) {
      resolved.Elem(root5)
    }
  }

  @Test def testConvertXmlWithNonExpandedEntityRef() {
    // 1. Convert XML to Elem

    val root: Elem = convertToElem(trivialXmlWithEntityRef)

    val ns = "urn:foo:bar"

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildTextAndEntityRef(rootElm: Elem): Unit = {
      val childOption = rootElm.findElem(EName(ns, "child"))
      expectResult(true) {
        childOption.isDefined
      }
      expectResult(2) {
        childOption.get.textChildren.size
      }
      expectResult(1) {
        val result = childOption.get.children collect { case er: EntityRef => er }
        result.size
      }
      expectResult(EntityRef("hello")) {
        val entityRefs = childOption.get.children collect { case er: EntityRef => er }
        val entityRef: EntityRef = entityRefs.head
        entityRef
      }
      val s = "This text contains an entity reference, viz."
      expectResult(s) {
        childOption.get.trimmedText.take(s.length)
      }
    }

    checkChildTextAndEntityRef(root)

    // 2. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root)(Scope.Empty).build()

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildTextAndEntityRef(root3)

    // 3. Print to tree representation String and parse back that DSL String, and check again

    val treeRepr: String = root.toString

    assert(treeRepr.trim.startsWith("elem("), "Expected the tree representation to start with 'elem('")

    val root4: Elem = {
      import TreeReprParsers._

      val parseResult = parseAll(TreeReprParsers.element, treeRepr)
      parseResult.get.build()
    }

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root4.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildTextAndEntityRef(root4)

    // 4. Convert to Scala XML and back

    val root5 = convertToElem(convertElem(root))

    expectResult(resolved.Elem(root)) {
      resolved.Elem(root5)
    }
  }

  @Test def testConvertXmlWithNamespaceUndeclarations() {
    // 1. Convert XML to Elem

    val root: Elem = convertToElem(trivialXmlWithNSUndeclarations)

    val ns = "urn:foo:bar"

    expectResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root)(Scope.Empty).build()

    expectResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 3. Print to tree representation String and parse back that DSL String, and check again

    val treeRepr: String = root.toString

    assert(treeRepr.trim.startsWith("elem("), "Expected the tree representation to start with 'elem('")

    val root4: Elem = {
      import TreeReprParsers._

      val parseResult = parseAll(TreeReprParsers.element, treeRepr)
      parseResult.get.build()
    }

    expectResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root4.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 4. Convert to Scala XML and back

    val root5 = convertToElem(convertElem(root))

    expectResult(resolved.Elem(root)) {
      resolved.Elem(root5)
    }
  }

  /**
   * Currently this test is ignored because of bug: SI 3368: Preserve CDATA sections, don't convert them to generic Text nodes.
   * (see https://issues.scala-lang.org/browse/SI-3368).
   */
  @Ignore @Test def testConvertXmlWithEscapedChars() {
    // 1. Convert XML to Elem

    val root: Elem = convertToElem(trivialXmlWithEscapedChars)

    val ns = "urn:foo:bar"

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: Elem): Unit = {
      val childElms = rootElm.findTopmostElems(EName(ns, "child"))
      expectResult(2) {
        childElms.size
      }

      val text = "Jansen & co"

      // Remember: we set the parser to coalescing!
      expectResult(Set(text)) {
        val result = childElms map { e => e.trimmedText }
        result.toSet
      }

      expectResult(Set(text)) {
        val result = childElms map { e => e.attributeOption(EName("about")).getOrElse("Missing text") }
        result.toSet
      }

      expectResult(Set(text)) {
        val result = rootElm.commentChildren map { c => c.text.trim }
        result.toSet
      }
    }

    doChecks(root)

    // 2. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root)(Scope.Empty).build()

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root3)

    // 3. Print to tree representation String and parse back that DSL String, and check again

    val treeRepr: String = root.toString

    assert(treeRepr.trim.startsWith("elem("), "Expected the tree representation to start with 'elem('")

    val root4: Elem = {
      import TreeReprParsers._

      val parseResult = parseAll(TreeReprParsers.element, treeRepr)
      parseResult.get.build()
    }

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root4.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root4)
  }

  @Test def testConvertXmlWithSpecialChars() {
    // 1. Convert XML to Elem

    val root: Elem = convertToElem(trivialXmlWithEuro)

    val ns = "urn:foo:bar"

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: Elem): Unit = {
      val childElms = rootElm.findTopmostElems(EName(ns, "child"))
      expectResult(2) {
        childElms.size
      }

      val text = "\u20AC 200"

      expectResult(Set(text)) {
        val result = childElms map { e => e.trimmedText }
        result.toSet
      }
    }

    doChecks(root)

    // 2. Convert to NodeBuilder and back, and check again

    val root2: Elem = NodeBuilder.fromElem(root)(Scope.Empty).build()

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root2)

    // 3. Print to tree representation String and parse back that DSL String, and check again

    val treeRepr: String = root.toString

    assert(treeRepr.trim.startsWith("elem("), "Expected the tree representation to start with 'elem('")

    val root3: Elem = {
      import TreeReprParsers._

      val parseResult = parseAll(TreeReprParsers.element, treeRepr)
      parseResult.get.build()
    }

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root3)

    // 4. Convert to Scala XML and back

    val root5 = convertToElem(convertElem(root))

    expectResult(resolved.Elem(root)) {
      resolved.Elem(root5)
    }
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  @Test def testConvertGroovyXmlExample() {
    val doc = Document(convertToElem(cars))

    expectResult("records") {
      doc.documentElement.localName
    }

    val recordsElm = doc.documentElement

    expectResult(3) {
      (recordsElm \ (_.localName == "car")).size
    }

    expectResult(10) {
      recordsElm.findAllElemsOrSelf.size
    }

    val firstRecordElm = (recordsElm \ (_.localName == "car"))(0)

    expectResult("car") {
      firstRecordElm.localName
    }

    expectResult("Holden") {
      firstRecordElm.attribute(EName("make"))
    }

    expectResult("Australia") {
      firstRecordElm.getChildElem(_.localName == "country").trimmedText
    }

    expectResult(2) {
      val carElms = recordsElm \ (_.localName == "car")
      val result = carElms filter { e => e.attributeOption(EName("make")).getOrElse("").contains('e') }
      result.size
    }

    expectResult(Set("Holden", "Peel")) {
      val carElms = recordsElm \ (_.localName == "car")
      val pattern = ".*s.*a.*".r.pattern

      val resultElms = carElms filter { e =>
        val s = e.getChildElem(_.localName == "country").trimmedText
        pattern.matcher(s).matches
      }

      (resultElms map (e => e.attribute(EName("make")))).toSet
    }

    expectResult(Set("speed", "size", "price")) {
      val result = recordsElm.findAllElemsOrSelf collect { case e if e.attributeOption(EName("type")).isDefined => e.attribute(EName("type")) }
      result.toSet
    }

    import NodeBuilder._

    val countryPath = PathBuilder.from(QName("car") -> 0, QName("country") -> 0).build(Scope.Empty)
    val updatedCountryElm = textElem(QName("country"), "New Zealand").build()
    val updatedDoc = doc.updated(countryPath, updatedCountryElm)

    expectResult("New Zealand") {
      updatedDoc.documentElement.filterChildElems(_.localName == "car")(0).getChildElem(_.localName == "country").trimmedText
    }

    expectResult(List("Royale", "P50", "HSV Maloo")) {
      val carElms = recordsElm \ (_.localName == "car")
      val resultElms = carElms sortBy { e => e.attributeOption(EName("year")).getOrElse("0").toInt }
      resultElms map { e => e.attribute(EName("name")) }
    }

    // Convert to Scala XML and back

    val newRoot = convertToElem(convertElem(doc.documentElement))

    expectResult(resolved.Elem(doc.documentElement)) {
      resolved.Elem(newRoot)
    }
  }

  private val bookstore: scala.xml.Elem =
    <books:Bookstore xmlns="http://bookstore" xmlns:books="http://bookstore">
      <Book ISBN="ISBN-0-13-713526-2" Price="85" Edition="3rd">
        <Title>A First Course in Database Systems</Title>
        <Authors>
          <Author>
            <First_Name>Jeffrey</First_Name>
            <Last_Name>Ullman</Last_Name>
          </Author>
          <Author>
            <First_Name>Jennifer</First_Name>
            <Last_Name>Widom</Last_Name>
          </Author>
        </Authors>
      </Book>
      <Book ISBN="ISBN-0-13-815504-6" Price="100">
        <Title>Database Systems: The Complete Book</Title>
        <Authors>
          <Author>
            <First_Name>Hector</First_Name>
            <Last_Name>Garcia-Molina</Last_Name>
          </Author>
          <Author>
            <First_Name>Jeffrey</First_Name>
            <Last_Name>Ullman</Last_Name>
          </Author>
          <Author>
            <First_Name>Jennifer</First_Name>
            <Last_Name>Widom</Last_Name>
          </Author>
        </Authors>
        <Remark>
          Buy this book bundled with "A First Course" - a great deal!
        </Remark>
      </Book>
      <Book ISBN="ISBN-0-11-222222-3" Price="50">
        <Title>Hector and Jeff's Database Hints</Title>
        <Authors>
          <Author>
            <First_Name>Jeffrey</First_Name>
            <Last_Name>Ullman</Last_Name>
          </Author>
          <Author>
            <First_Name>Hector</First_Name>
            <Last_Name>Garcia-Molina</Last_Name>
          </Author>
        </Authors>
        <Remark>An indispensable companion to your textbook</Remark>
      </Book>
      <Book ISBN="ISBN-9-88-777777-6" Price="25">
        <Title>Jennifer's Economical Database Hints</Title>
        <Authors>
          <Author>
            <First_Name>Jennifer</First_Name>
            <Last_Name>Widom</Last_Name>
          </Author>
        </Authors>
      </Book>
      <Magazine Month="January" Year="2009">
        <Title>National Geographic</Title>
      </Magazine>
      <Magazine Month="February" Year="2009">
        <Title>National Geographic</Title>
      </Magazine>
      <Magazine Month="February" Year="2009">
        <Title>Newsweek</Title>
      </Magazine>
      <Magazine Month="March" Year="2009">
        <Title>Hector and Jeff's Database Hints</Title>
      </Magazine>
    </books:Bookstore>

  private val strangeXml: scala.xml.Elem =
    <bar xmlns:ns="http://www.yahoo.com">
      <ns:foo xmlns:ns="http://www.google.com"/>
    </bar>

  private val trivialXml: scala.xml.Elem =
    <root xmlns="urn:foo:bar">
      <!-- Trivial XML -->
      <child/>
    </root>

  private val trivialXmlWithEntityRef: scala.xml.Elem =
    <root xmlns="urn:foo:bar">
      <child>
        This text contains an entity reference, viz. &hello;.
      </child>
    </root>

  private val trivialXmlWithNSUndeclarations: scala.xml.Elem =
    <root xmlns="urn:foo:bar">
      <a>
        <b xmlns="">
          <c>
            <d xmlns="urn:foo:bar">text</d>
          </c>
        </b>
      </a>
    </root>

  private val trivialXmlWithEscapedChars: scala.xml.Elem =
    <root xmlns="urn:foo:bar">
      <!-- Jansen & co -->
      <child about="Jansen &amp; co">
        Jansen &amp; co
      </child>
      <child about="Jansen &amp; co">
        <![CDATA[Jansen & co]]>
      </child>
    </root>

  private val trivialXmlWithEuro: scala.xml.Elem =
    <root xmlns="urn:foo:bar">
      <child>€ 200</child>
      <child>&#x20AC; 200</child>
    </root>

  private val cars: scala.xml.Elem =
    <records>
      <car name='HSV Maloo' make='Holden' year='2006'>
        <country>Australia</country>
        <record type='speed'>
          Production Pickup Truck with speed of 271kph
        </record>
      </car>
      <car name='P50' make='Peel' year='1962'>
        <country>Isle of Man</country>
        <record type='size'>
          Smallest Street-Legal Car at 99cm wide and 59 kg
			in weight
        </record>
      </car>
      <car name='Royale' make='Bugatti' year='1931'>
        <country>France</country>
        <record type='price'>Most Valuable Car at $15 million</record>
      </car>
    </records>
}
