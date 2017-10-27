package eu.cdevreeze.yaidom.java8.test;

import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;

import eu.cdevreeze.yaidom.convert.DomConversions;
import eu.cdevreeze.yaidom.core.Scope;
import eu.cdevreeze.yaidom.java8.domelem.DomElem;
import eu.cdevreeze.yaidom.parse.DocumentParser;
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax;
import eu.cdevreeze.yaidom.simple.Document;

public class DomElemQueryTest extends AbstractElemQueryTest<DomElem> {

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

	protected DomElem getBookstore() {
		DocumentParser docParser = DocumentParserUsingSax.newInstance();
		URI docUri;
		try {
			docUri = DomElemQueryTest.class.getResource("books.xml").toURI();
			Document doc = docParser.parse(docUri);
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			return new DomElem(DomConversions.convertElem(doc.documentElement(), db.newDocument(), Scope.Empty()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
