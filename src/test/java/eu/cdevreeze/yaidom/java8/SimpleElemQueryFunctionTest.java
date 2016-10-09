package eu.cdevreeze.yaidom.java8;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import eu.cdevreeze.yaidom.core.EName;
import eu.cdevreeze.yaidom.core.ENameProvider;
import eu.cdevreeze.yaidom.core.QName;
import eu.cdevreeze.yaidom.core.Scope;
import eu.cdevreeze.yaidom.core.UnprefixedName;
import static eu.cdevreeze.yaidom.java8.functionapi.JavaStreamUtil.toStream;
import static eu.cdevreeze.yaidom.java8.elems.SimpleElemOps.*;
import eu.cdevreeze.yaidom.parse.DocumentParser;
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax;
import eu.cdevreeze.yaidom.simple.Document;
import eu.cdevreeze.yaidom.simple.Elem;

public class SimpleElemQueryFunctionTest {

	@Test
	public void testQueryBookTitles() {
		// XPath: doc("bookstore.xml")/Bookstore/Book/Title

		Stream<Elem> books = filterChildElems(bookstore, withLocalName("Book"));

		Stream<Elem> bookTitles = books.map(e -> getChildElem(e, withLocalName("Title")));

		assertEquals(
				bookTitles.map(e -> trimmedText(e)).collect(Collectors.toSet()),
				Stream.of("A First Course in Database Systems", "Database Systems: The Complete Book",
						"Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints").collect(
						Collectors.toSet()));
	}

	@Test
	public void testQueryBookTitlesUsingENames() {
		// XPath: doc("bookstore.xml")/Bookstore/Book/Title

		String ns = "http://bookstore";
		EName bookEName = EName.apply(ns, "Book");
		EName titleEName = EName.apply(ns, "Title");

		Stream<Elem> books = filterChildElems(bookstore, withEName(bookEName));

		Stream<Elem> bookTitles = books.map(e -> getChildElem(e, withEName(titleEName)));

		assertEquals(
				bookTitles.map(e -> trimmedText(e)).collect(Collectors.toSet()),
				Stream.of("A First Course in Database Systems", "Database Systems: The Complete Book",
						"Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints").collect(
						Collectors.toSet()));
	}

	@Test
	public void testQueryBookOrMagazineTitles() {
		// XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

		Set<String> bookOrMagazineLocalNames = Stream.of("Book", "Magazine").collect(Collectors.toSet());

		Stream<Elem> booksOrMagazines = filterChildElems(bookstore,
				book -> bookOrMagazineLocalNames.contains(localName(book)));

		Stream<Elem> bookOrMagazineTitles = booksOrMagazines.flatMap(e -> toStream(findChildElem(e,
				withLocalName("Title"))));

		assertEquals(
				bookOrMagazineTitles.map(e -> trimmedText(e)).collect(Collectors.toSet()),
				Stream.of("A First Course in Database Systems", "Database Systems: The Complete Book",
						"Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints",
						"National Geographic", "Newsweek").collect(Collectors.toSet()));
	}

	@Test
	public void testQueryCheapBooks() {
		// XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]

		Stream<Elem> books = filterChildElems(bookstore, withLocalName("Book"));

		EName priceEName = EName.apply("Price");

		Stream<Elem> cheapBooks = books.filter(e -> toStream(attributeOption(e, priceEName)).anyMatch(
				a -> java.lang.Integer.parseInt(a) < 90));

		Stream<Elem> cheapBookTitles = cheapBooks.flatMap(e -> toStream(findChildElem(e, withLocalName("Title"))));

		assertEquals(
				cheapBookTitles.map(e -> trimmedText(e)).collect(Collectors.toSet()),
				Stream.of("A First Course in Database Systems", "Hector and Jeff's Database Hints",
						"Jennifer's Economical Database Hints").collect(Collectors.toSet()));
	}

	@Test
	public void testQueryCheapBooksUsingENames() {
		// XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]

		String ns = "http://bookstore";
		Scope scope = Scope.Empty().append("", ns).append("books", ns);

		QName bookQName = UnprefixedName.apply("Book");
		QName priceQName = UnprefixedName.apply("Price");
		QName titleQName = UnprefixedName.apply("Title");

		ENameProvider enameProvider = new ENameProvider.TrivialENameProvider();

		EName bookEName = scope.resolveQNameOption(bookQName, enameProvider).get();
		EName priceEName = scope.withoutDefaultNamespace().resolveQNameOption(priceQName, enameProvider).get();
		EName titleEName = scope.resolveQNameOption(titleQName, enameProvider).get();

		Stream<Elem> books = filterChildElems(bookstore, withEName(bookEName));

		Stream<Elem> cheapBooks = books.filter(e -> toStream(attributeOption(e, priceEName)).anyMatch(
				a -> java.lang.Integer.parseInt(a) < 90));

		Stream<Elem> cheapBookTitles = cheapBooks.flatMap(e -> toStream(findChildElem(e, withEName(titleEName))));

		assertEquals(
				cheapBookTitles.map(e -> trimmedText(e)).collect(Collectors.toSet()),
				Stream.of("A First Course in Database Systems", "Hector and Jeff's Database Hints",
						"Jennifer's Economical Database Hints").collect(Collectors.toSet()));
	}

	private Predicate<Elem> withLocalName(String localName) {
		return (Elem e) -> localName.equals(e.localName());
	}

	private Predicate<Elem> withEName(EName ename) {
		return (Elem e) -> ename.equals(e.resolvedName());
	}

	private Elem getBookstore() {
		DocumentParser docParser = DocumentParserUsingSax.newInstance();
		URI docUri;
		try {
			docUri = SimpleElemQueryFunctionTest.class.getResource("books.xml").toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		Document doc = docParser.parse(docUri);
		return doc.documentElement();
	}

	private Elem bookstore = getBookstore();
}
