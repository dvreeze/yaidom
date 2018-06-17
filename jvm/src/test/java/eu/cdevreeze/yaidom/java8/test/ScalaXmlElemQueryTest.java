package eu.cdevreeze.yaidom.java8.test;

import java.net.URI;

import org.junit.Test;

import eu.cdevreeze.yaidom.convert.ScalaXmlConversions;
import eu.cdevreeze.yaidom.java8.scalaxmlelem.ScalaXmlElem;
import eu.cdevreeze.yaidom.parse.DocumentParser;
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax;
import eu.cdevreeze.yaidom.simple.Document;

public class ScalaXmlElemQueryTest extends AbstractElemQueryTest<ScalaXmlElem> {

	@Test
	public void testQueryBookTitles() {
		doTestQueryBookTitles();
	}

	@Test
	public void testQueryBookTitlesUsingENames() {
		doTestQueryBookTitlesUsingENames();
	}

	@Test
	public void testQueryBookOrMagazineTitles() {
		doTestQueryBookOrMagazineTitles();
	}

	@Test
	public void testQueryCheapBooks() {
		doTestQueryCheapBooks();
	}

	@Test
	public void testQueryCheapBooksUsingENames() {
		doTestQueryCheapBooksUsingENames();
	}

	@Test
	public void testFindingChildElems() {
		doTestFindingChildElems();
	}

	@Test
	public void testFindingDescendantElems() {
		doTestFindingDescendantElems();
	}

	@Test
	public void testFindingDescendantOrSelfElems() {
		doTestFindingDescendantOrSelfElems();
	}

	@Test
	public void testFindingTopmostElems() {
		doTestFindingTopmostElems();
	}

	@Test
	public void testFindingTopmostOrSelfElems() {
		doTestFindingTopmostOrSelfElems();
	}

	@Test
	public void testFindingAttributes() {
		doTestFindingAttributes();
	}

	protected ScalaXmlElem getBookstore() {
		DocumentParser docParser = DocumentParserUsingSax.newInstance();
		URI docUri;
		try {
			docUri = ScalaXmlElemQueryTest.class.getResource("books.xml").toURI();
			Document doc = docParser.parse(docUri);
			return ScalaXmlElem.apply(ScalaXmlConversions.convertElem(doc.documentElement()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
