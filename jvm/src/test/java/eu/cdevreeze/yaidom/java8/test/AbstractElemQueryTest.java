package eu.cdevreeze.yaidom.java8.test;

import static org.junit.Assert.assertEquals;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import eu.cdevreeze.yaidom.core.EName;
import eu.cdevreeze.yaidom.core.ENameProvider;
import eu.cdevreeze.yaidom.core.QName;
import eu.cdevreeze.yaidom.core.Scope;
import eu.cdevreeze.yaidom.core.UnprefixedName;
import eu.cdevreeze.yaidom.java8.queryapi.StreamingClarkElemApi;

public abstract class AbstractElemQueryTest<E extends StreamingClarkElemApi<E>> {

	protected void doTestQueryBookTitles() {
		// XPath: doc("bookstore.xml")/Bookstore/Book/Title

		Stream<E> books = bookstore.filterChildElems(withLocalName("Book"));

		Stream<E> bookTitles = books.map(e -> e.getChildElem(withLocalName("Title")));

		assertEquals(
				bookTitles.map(e -> e.trimmedText()).collect(Collectors.toSet()),
				Stream.of("A First Course in Database Systems", "Database Systems: The Complete Book",
						"Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints").collect(
						Collectors.toSet()));
	}

	protected void doTestQueryBookTitlesUsingENames() {
		// XPath: doc("bookstore.xml")/Bookstore/Book/Title

		String ns = "http://bookstore";
		EName bookEName = EName.apply(ns, "Book");
		EName titleEName = EName.apply(ns, "Title");

		Stream<E> books = bookstore.filterChildElems(withEName(bookEName));

		Stream<E> bookTitles = books.map(e -> e.getChildElem(withEName(titleEName)));

		assertEquals(
				bookTitles.map(e -> e.trimmedText()).collect(Collectors.toSet()),
				Stream.of("A First Course in Database Systems", "Database Systems: The Complete Book",
						"Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints").collect(
						Collectors.toSet()));
	}

	protected void doTestQueryBookOrMagazineTitles() {
		// XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

		Set<String> bookOrMagazineLocalNames = Stream.of("Book", "Magazine").collect(Collectors.toSet());

		Stream<E> booksOrMagazines = bookstore.filterChildElems(book -> bookOrMagazineLocalNames.contains(book
				.localName()));

		Stream<E> bookOrMagazineTitles = booksOrMagazines
				.flatMap(e -> toStream(e.findChildElem(withLocalName("Title"))));

		assertEquals(
				bookOrMagazineTitles.map(e -> e.trimmedText()).collect(Collectors.toSet()),
				Stream.of("A First Course in Database Systems", "Database Systems: The Complete Book",
						"Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints",
						"National Geographic", "Newsweek").collect(Collectors.toSet()));
	}

	protected void doTestQueryCheapBooks() {
		// XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]

		Stream<E> books = bookstore.filterChildElems(withLocalName("Book"));

		EName priceEName = EName.apply("Price");

		Stream<E> cheapBooks = books.filter(e -> toStream(e.attributeOption(priceEName)).anyMatch(
				a -> java.lang.Integer.parseInt(a) < 90));

		Stream<E> cheapBookTitles = cheapBooks.flatMap(e -> toStream(e.findChildElem(withLocalName("Title"))));

		assertEquals(
				cheapBookTitles.map(e -> e.trimmedText()).collect(Collectors.toSet()),
				Stream.of("A First Course in Database Systems", "Hector and Jeff's Database Hints",
						"Jennifer's Economical Database Hints").collect(Collectors.toSet()));
	}

	protected void doTestQueryCheapBooksUsingENames() {
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

		Stream<E> books = bookstore.filterChildElems(withEName(bookEName));

		Stream<E> cheapBooks = books.filter(e -> toStream(e.attributeOption(priceEName)).anyMatch(
				a -> java.lang.Integer.parseInt(a) < 90));

		Stream<E> cheapBookTitles = cheapBooks.flatMap(e -> toStream(e.findChildElem(withEName(titleEName))));

		assertEquals(
				cheapBookTitles.map(e -> e.trimmedText()).collect(Collectors.toSet()),
				Stream.of("A First Course in Database Systems", "Hector and Jeff's Database Hints",
						"Jennifer's Economical Database Hints").collect(Collectors.toSet()));
	}

	protected void doTestFindingChildElems() {
		String ns = "http://bookstore";
		EName bookEName = EName.apply(ns, "Book");
		EName titleEName = EName.apply(ns, "Title");

		java.util.List<E> books = bookstore.filterChildElems(withEName(bookEName)).collect(Collectors.toList());

		Stream<E> bookTitles = books.stream().flatMap(e -> toStream(e.findChildElem(withEName(titleEName))));

		assertEquals(
				bookTitles.map(e -> e.trimmedText()).collect(Collectors.toSet()),
				Stream.of("A First Course in Database Systems", "Database Systems: The Complete Book",
						"Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints").collect(
						Collectors.toSet()));

		assertEquals(books,
				bookstore.filterChildElems(e -> true).filter(withEName(bookEName)).collect(Collectors.toList()));

		assertEquals(books, bookstore.findAllChildElems().filter(withEName(bookEName)).collect(Collectors.toList()));

		assertEquals(books.stream().findFirst().isPresent(), true);

		assertEquals(books.stream().findFirst(), toStream(bookstore.findChildElem(withEName(bookEName))).findFirst());

		assertEquals(bookstore.findAllChildElems().count(), 8);
	}

	protected void doTestFindingDescendantElems() {
		String ns = "http://bookstore";
		EName titleEName = EName.apply(ns, "Title");

		java.util.List<E> titles = bookstore.filterElems(withEName(titleEName)).collect(Collectors.toList());

		assertEquals(
				titles.stream().map(e -> e.trimmedText()).collect(Collectors.toSet()),
				Stream.of("A First Course in Database Systems", "Database Systems: The Complete Book",
						"Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints",
						"National Geographic", "Newsweek").collect(Collectors.toSet()));

		assertEquals(titles, bookstore.filterElems(e -> true).filter(withEName(titleEName))
				.collect(Collectors.toList()));

		assertEquals(titles, bookstore.findAllElems().filter(withEName(titleEName)).collect(Collectors.toList()));

		assertEquals(titles.stream().findFirst().isPresent(), true);

		assertEquals(titles.stream().findFirst(), toStream(bookstore.findElem(withEName(titleEName))).findFirst());

		assertEquals(bookstore.findAllElems().count(), 46);
	}

	protected void doTestFindingDescendantOrSelfElems() {
		String ns = "http://bookstore";
		EName titleEName = EName.apply(ns, "Title");

		java.util.List<E> titles = bookstore.filterElemsOrSelf(withEName(titleEName)).collect(Collectors.toList());

		assertEquals(
				titles.stream().map(e -> e.trimmedText()).collect(Collectors.toSet()),
				Stream.of("A First Course in Database Systems", "Database Systems: The Complete Book",
						"Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints",
						"National Geographic", "Newsweek").collect(Collectors.toSet()));

		assertEquals(titles,
				bookstore.filterElemsOrSelf(e -> true).filter(withEName(titleEName)).collect(Collectors.toList()));

		assertEquals(titles, bookstore.findAllElemsOrSelf().filter(withEName(titleEName)).collect(Collectors.toList()));

		assertEquals(titles.stream().findFirst().isPresent(), true);

		assertEquals(titles.stream().findFirst(), toStream(bookstore.findElemOrSelf(withEName(titleEName))).findFirst());

		assertEquals(bookstore.findAllElemsOrSelf().count(), 1 + bookstore.findAllElems().count());
	}

	protected void doTestFindingTopmostElems() {
		Predicate<E> isBookOrMagazineOrTitle = e -> Stream.of("Book", "Title", "Magazine").collect(Collectors.toSet())
				.contains(e.localName());

		java.util.List<E> booksOrMagazines = bookstore.findTopmostElems(isBookOrMagazineOrTitle).collect(
				Collectors.toList());

		assertEquals(booksOrMagazines.stream().map(e -> e.localName()).collect(Collectors.toSet()),
				Stream.of("Book", "Magazine").collect(Collectors.toSet()));

		assertEquals(booksOrMagazines.stream().findFirst().isPresent(), true);

		assertEquals(booksOrMagazines.stream().findFirst(), toStream(bookstore.findElem(isBookOrMagazineOrTitle))
				.findFirst());

		assertEquals(bookstore.findTopmostElems(isBookOrMagazineOrTitle).count(), 8);

		assertEquals(bookstore.findTopmostElems(e -> "Bookstore".equals(e.localName())).count(), 0);
	}

	protected void doTestFindingTopmostOrSelfElems() {
		Predicate<E> isBookOrMagazineOrTitle = e -> Stream.of("Book", "Title", "Magazine").collect(Collectors.toSet())
				.contains(e.localName());

		java.util.List<E> booksOrMagazines = bookstore.findTopmostElemsOrSelf(isBookOrMagazineOrTitle).collect(
				Collectors.toList());

		assertEquals(booksOrMagazines.stream().map(e -> e.localName()).collect(Collectors.toSet()),
				Stream.of("Book", "Magazine").collect(Collectors.toSet()));

		assertEquals(booksOrMagazines.stream().findFirst().isPresent(), true);

		assertEquals(booksOrMagazines.stream().findFirst(), toStream(bookstore.findElemOrSelf(isBookOrMagazineOrTitle))
				.findFirst());

		assertEquals(bookstore.findTopmostElemsOrSelf(isBookOrMagazineOrTitle).count(), 8);

		assertEquals(bookstore.findTopmostElemsOrSelf(e -> "Bookstore".equals(e.localName())).count(), 1);
	}

	protected void doTestFindingAttributes() {
		String ns = "http://bookstore";
		EName bookEName = EName.apply(ns, "Book");
		EName isbnEName = EName.apply("ISBN");

		java.util.List<E> books = bookstore.filterChildElems(withEName(bookEName)).collect(Collectors.toList());

		assertEquals(
				books.stream().anyMatch(
						e -> toStream(e.attributeOption(isbnEName)).anyMatch(a -> "ISBN-0-11-222222-3".equals(a))),
				true);

		assertEquals(
				books.stream().anyMatch(
						e -> toStream(e.attributeOption(isbnEName)).anyMatch(a -> "ISBN-0-11-234567-3".equals(a))),
				false);

		assertEquals(
				books.stream().anyMatch(
						e -> toStream(e.findAttributeByLocalName(isbnEName.localPart())).anyMatch(
								a -> "ISBN-0-11-222222-3".equals(a))), true);

		assertEquals(
				books.stream().anyMatch(
						e -> toStream(e.findAttributeByLocalName(isbnEName.localPart())).anyMatch(
								a -> "ISBN-0-11-234567-3".equals(a))), false);
	}

	private Predicate<E> withLocalName(String localName) {
		return (E e) -> localName.equals(e.localName());
	}

	private Predicate<E> withEName(EName ename) {
		return (E e) -> ename.equals(e.resolvedName());
	}

	private <A> Stream<A> toStream(Optional<A> optVal) {
		if (optVal.isPresent()) {
			return Stream.of(optVal.get());
		} else {
			return Stream.empty();
		}
	}

	protected abstract E getBookstore();

	private E bookstore = getBookstore();
}
