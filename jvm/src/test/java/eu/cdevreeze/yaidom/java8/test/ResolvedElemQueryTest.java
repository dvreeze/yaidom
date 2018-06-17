package eu.cdevreeze.yaidom.java8.test;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import eu.cdevreeze.yaidom.java8.resolvedelem.ResolvedElem;
import eu.cdevreeze.yaidom.parse.DocumentParser;
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax;
import eu.cdevreeze.yaidom.simple.Document;

public class ResolvedElemQueryTest extends AbstractElemQueryTest<ResolvedElem> {

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

	protected ResolvedElem getBookstore() {
		DocumentParser docParser = DocumentParserUsingSax.newInstance();
		URI docUri;
		try {
			docUri = ResolvedElemQueryTest.class.getResource("books.xml").toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		Document doc = docParser.parse(docUri);
		return ResolvedElem.from(doc.documentElement());
	}
}
