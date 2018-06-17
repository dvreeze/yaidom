package eu.cdevreeze.yaidom.java8.test;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import eu.cdevreeze.yaidom.java8.saxonelem.SaxonDocument;
import eu.cdevreeze.yaidom.java8.saxonelem.SaxonElem;
import net.sf.saxon.s9api.DocumentBuilder;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;

public class SaxonElemQueryTest extends AbstractElemQueryTest<SaxonElem> {

	private Processor processor() {
		return new Processor(false);
	}

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

	protected SaxonElem getBookstore() {
		DocumentBuilder docBuilder = processor().newDocumentBuilder();
		URI docUri;
		try {
			docUri = SaxonElemQueryTest.class.getResource("books.xml").toURI();

			SaxonDocument doc = SaxonDocument
					.apply(docBuilder.build(new File(docUri)).getUnderlyingNode().getTreeInfo());
			SaxonElem docElem = doc.documentElement();
			return docElem;
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		} catch (SaxonApiException e) {
			throw new RuntimeException(e);
		}
	}
}
