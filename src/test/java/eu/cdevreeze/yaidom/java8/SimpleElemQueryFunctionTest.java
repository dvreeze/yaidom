package eu.cdevreeze.yaidom.java8;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import eu.cdevreeze.yaidom.core.EName;
import eu.cdevreeze.yaidom.java8.elems.SimpleElems;
import eu.cdevreeze.yaidom.java8.functionapi.ScopedElemFunctionApi;
import eu.cdevreeze.yaidom.java8.functionapi.JavaStreamUtil;
import eu.cdevreeze.yaidom.parse.DocumentParser;
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax;
import eu.cdevreeze.yaidom.simple.Document;
import eu.cdevreeze.yaidom.simple.Elem;

public class SimpleElemQueryFunctionTest {

	@Test
	public void testQueryBookTitles() {
		// XPath: doc("bookstore.xml")/Bookstore/Book/Title

		Stream<Elem> bookTitles = ops.filterChildElems(bookstore,
				book -> "Book".equals(ops.localName(book))).map(
				e -> ops.getChildElem(e,
						e2 -> "Title".equals(ops.localName(e2))));

		assertEquals(
				bookTitles.map(e -> ops.trimmedText(e)).collect(
						Collectors.toSet()),
				Stream.of("A First Course in Database Systems",
						"Database Systems: The Complete Book",
						"Hector and Jeff's Database Hints",
						"Jennifer's Economical Database Hints").collect(
						Collectors.toSet()));
	}

	@Test
	public void testQueryBookTitlesUsingENames() {
		// XPath: doc("bookstore.xml")/Bookstore/Book/Title

		String ns = "http://bookstore";
		EName bookEName = EName.apply(ns, "Book");
		EName titleEName = EName.apply(ns, "Title");

		Stream<Elem> bookTitles = ops.filterChildElems(bookstore,
				book -> bookEName.equals(ops.resolvedName(book))).map(
				e -> ops.getChildElem(e,
						e2 -> titleEName.equals(ops.resolvedName(e2))));

		assertEquals(
				bookTitles.map(e -> ops.trimmedText(e)).collect(
						Collectors.toSet()),
				Stream.of("A First Course in Database Systems",
						"Database Systems: The Complete Book",
						"Hector and Jeff's Database Hints",
						"Jennifer's Economical Database Hints").collect(
						Collectors.toSet()));
	}

	@Test
	public void testQueryBookOrMagazineTitles() {
		// XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

		Set<String> bookOrMagazineLocalNames = Stream.of("Book", "Magazine")
				.collect(Collectors.toSet());

		Stream<Elem> booksOrMagazines = ops.filterChildElems(bookstore,
				book -> bookOrMagazineLocalNames.contains(ops.localName(book)));

		Stream<Elem> bookOrMagazineTitles = booksOrMagazines
				.flatMap(e -> JavaStreamUtil.toStream(ops.findChildElem(e,
						e2 -> "Title".equals(ops.localName(e2)))));

		assertEquals(
				bookOrMagazineTitles.map(e -> ops.trimmedText(e)).collect(
						Collectors.toSet()),
				Stream.of("A First Course in Database Systems",
						"Database Systems: The Complete Book",
						"Hector and Jeff's Database Hints",
						"Jennifer's Economical Database Hints",
						"National Geographic", "Newsweek").collect(
						Collectors.toSet()));
	}

	private Elem getBookstore() {
		DocumentParser docParser = DocumentParserUsingSax.newInstance();
		URI docUri;
		try {
			docUri = SimpleElemQueryFunctionTest.class.getResource("books.xml")
					.toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		Document doc = docParser.parse(docUri);
		return doc.documentElement();
	}

	private Elem bookstore = getBookstore();

	private ScopedElemFunctionApi<Elem> ops = SimpleElems.getInstance();
}
