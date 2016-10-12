package eu.cdevreeze.yaidom.java8;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
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
import eu.cdevreeze.yaidom.java8.simpleelem.SimpleElem;
import eu.cdevreeze.yaidom.parse.DocumentParser;
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax;
import eu.cdevreeze.yaidom.simple.Document;

public class SimpleElemQueryTest {

	@Test
	public void testQueryBookTitles() {
		// XPath: doc("bookstore.xml")/Bookstore/Book/Title

		Stream<SimpleElem> books = bookstore.filterChildElems(withLocalName("Book"));

		Stream<SimpleElem> bookTitles = books.map(e -> e.getChildElem(withLocalName("Title")));

		assertEquals(
				bookTitles.map(e -> e.trimmedText()).collect(Collectors.toSet()),
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

		Stream<SimpleElem> books = bookstore.filterChildElems(withEName(bookEName));

		Stream<SimpleElem> bookTitles = books.map(e -> e.getChildElem(withEName(titleEName)));

		assertEquals(
				bookTitles.map(e -> e.trimmedText()).collect(Collectors.toSet()),
				Stream.of("A First Course in Database Systems", "Database Systems: The Complete Book",
						"Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints").collect(
						Collectors.toSet()));
	}

	@Test
	public void testQueryBookOrMagazineTitles() {
		// XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

		Set<String> bookOrMagazineLocalNames = Stream.of("Book", "Magazine").collect(Collectors.toSet());

		Stream<SimpleElem> booksOrMagazines = bookstore.filterChildElems(book -> bookOrMagazineLocalNames.contains(book
				.localName()));

		Stream<SimpleElem> bookOrMagazineTitles = booksOrMagazines.flatMap(e -> toStream(e
				.findChildElem(withLocalName("Title"))));

		assertEquals(
				bookOrMagazineTitles.map(e -> e.trimmedText()).collect(Collectors.toSet()),
				Stream.of("A First Course in Database Systems", "Database Systems: The Complete Book",
						"Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints",
						"National Geographic", "Newsweek").collect(Collectors.toSet()));
	}

	@Test
	public void testQueryCheapBooks() {
		// XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]

		Stream<SimpleElem> books = bookstore.filterChildElems(withLocalName("Book"));

		EName priceEName = EName.apply("Price");

		Stream<SimpleElem> cheapBooks = books.filter(e -> toStream(e.attributeOption(priceEName)).anyMatch(
				a -> java.lang.Integer.parseInt(a) < 90));

		Stream<SimpleElem> cheapBookTitles = cheapBooks.flatMap(e -> toStream(e.findChildElem(withLocalName("Title"))));

		assertEquals(
				cheapBookTitles.map(e -> e.trimmedText()).collect(Collectors.toSet()),
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

		Stream<SimpleElem> books = bookstore.filterChildElems(withEName(bookEName));

		Stream<SimpleElem> cheapBooks = books.filter(e -> toStream(e.attributeOption(priceEName)).anyMatch(
				a -> java.lang.Integer.parseInt(a) < 90));

		Stream<SimpleElem> cheapBookTitles = cheapBooks.flatMap(e -> toStream(e.findChildElem(withEName(titleEName))));

		assertEquals(
				cheapBookTitles.map(e -> e.trimmedText()).collect(Collectors.toSet()),
				Stream.of("A First Course in Database Systems", "Hector and Jeff's Database Hints",
						"Jennifer's Economical Database Hints").collect(Collectors.toSet()));
	}

	private Predicate<SimpleElem> withLocalName(String localName) {
		return (SimpleElem e) -> localName.equals(e.localName());
	}

	private Predicate<SimpleElem> withEName(EName ename) {
		return (SimpleElem e) -> ename.equals(e.resolvedName());
	}

	private <A> Stream<A> toStream(Optional<A> optVal) {
		if (optVal.isPresent()) {
			return Stream.of(optVal.get());
		} else {
			return Stream.empty();
		}
	}

	private SimpleElem getBookstore() {
		DocumentParser docParser = DocumentParserUsingSax.newInstance();
		URI docUri;
		try {
			docUri = SimpleElemQueryTest.class.getResource("books.xml").toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
		Document doc = docParser.parse(docUri);
		return new SimpleElem(doc.documentElement());
	}

	private SimpleElem bookstore = getBookstore();
}
