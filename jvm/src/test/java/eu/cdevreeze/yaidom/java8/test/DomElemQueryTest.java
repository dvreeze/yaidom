package eu.cdevreeze.yaidom.java8.test;

import java.net.URI;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Test;

import eu.cdevreeze.yaidom.java8.domelem.DomDocument;
import eu.cdevreeze.yaidom.java8.domelem.DomElem;

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
		try {
			URI docUri = DomElemQueryTest.class.getResource("books.xml").toURI();
			DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			return DomDocument.apply(db.parse(docUri.toURL().openStream())).documentElement();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
