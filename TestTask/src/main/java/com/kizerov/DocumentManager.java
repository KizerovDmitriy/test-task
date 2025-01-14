package com.kizerov;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.Data;

/**
 * For implement this task focus on clear code, and make this solution as simple readable as possible
 * Don't worry about performance, concurrency, etc
 * You can use in Memory collection for sore data
 * <p>
 * Please, don't change class name, and signature for methods save, search, findById
 * Implementations should be in a single class
 * This class could be auto tested
 */
public class DocumentManager {
	private final Map<String, Document> documentsDB = new HashMap<>();

	/**
	 * Implementation of this method should upsert the document to your storage
	 * And generate unique id if it does not exist, don't change [created] field
	 *
	 * @param document - document content and author data
	 * @return saved document
	 */
	public Document save(Document document) {
		String documentId = ensureDocumentId(document);

		findById(documentId).ifPresentOrElse(existingDocument -> {
			updateDocument(existingDocument, document);
			documentsDB.put(documentId, existingDocument);
		}, () -> documentsDB.put(documentId, document));

		return documentsDB.get(documentId);
	}

	/**
	 * Returns the document ID from the given document, or generates a new one
	 * if it is null or empty.
	 *
	 * @param document - the document to ensure an ID for
	 * @return the ID of the document, never null
	 */
	private String ensureDocumentId(Document document) {
		if (Objects.isNull(document.getId()) || document.getId().isEmpty()) {
			document.setId(UUID.randomUUID().toString());
		}
		return document.getId();
	}

	/**
	 * Update existing document with new data
	 *
	 * @param existingDocument - document from storage
	 * @param document         - new document data
	 */
	private void updateDocument(Document existingDocument, Document document) {
		existingDocument.setTitle(document.getTitle());
		existingDocument.setContent(document.getContent());
		existingDocument.setAuthor(document.getAuthor());
	}

	/**
	 * Implementation this method should find documents which match with request
	 *
	 * @param request - search request, each field could be null
	 * @return list matched documents
	 */
	public List<Document> search(SearchRequest request) {
		Set<Document> searchResults = new HashSet<>();

		addMatchesByTitlePrefixes(request, searchResults);
		addMatchesByContent(request, searchResults);
		addMatchesByAuthorIds(request, searchResults);
		addMatchesByCreatedDateRange(request, searchResults);

		return new ArrayList<>(searchResults);
	}

	/**
	 * Adds documents to the {@code searchResults} set which match the
	 * title prefixes from the given {@code request}.
	 *
	 * @param request       the search request containing title prefixes to filter by
	 * @param searchResults the set to add matching documents to
	 */
	private void addMatchesByTitlePrefixes(SearchRequest request, Set<Document> searchResults) {
		if (request.getTitlePrefixes() != null) {
			request.getTitlePrefixes().forEach(titlePrefix ->
				addMatches(request, searchResults,
					document -> document.getTitle().startsWith(titlePrefix)));
		}
	}

	/**
	 * Adds documents to the {@code searchResults} set which match the
	 * contains content from the given {@code request}.
	 *
	 * @param request       the search request to filter by
	 * @param searchResults the set to add matching documents to
	 */
	private void addMatchesByContent(SearchRequest request, Set<Document> searchResults) {
		if (request.getContainsContents() != null) {
			request.getContainsContents().forEach(content ->
				addMatches(request, searchResults,
					document -> document.getContent().contains(content)));
		}
	}

	/**
	 * Adds documents to the {@code searchResults} set which match the
	 * given predicate from the {@code documentsDB}.
	 *
	 * @param request       the search request to filter by
	 * @param searchResults the set to add matching documents to
	 * @param predicate     the condition to filter documents
	 */
	private void addMatches(SearchRequest request, Set<Document> searchResults,
							Predicate<Document> predicate) {
		documentsDB.values().stream()
			.filter(predicate)
			.forEach(searchResults::add);
	}

	/**
	 * Adds documents to the {@code searchResults} set which match the
	 * given author IDs from the {@code request}.
	 *
	 * @param request       the search request to filter by
	 * @param searchResults the set to add matching documents to
	 */
	private void addMatchesByAuthorIds(SearchRequest request, Set<Document> searchResults) {
		if (request.getAuthorIds() != null) {
			request.getAuthorIds().forEach(authorId ->
				addMatches(request, searchResults,
					document -> document.getAuthor().getId().equals(authorId)));
		}
	}

	/**
	 * Adds documents to the {@code searchResults} set which match the
	 * creation date range from the given {@code request}.
	 *
	 * @param request       the search request to filter by
	 * @param searchResults the set to add matching documents to
	 */
	private void addMatchesByCreatedDateRange(SearchRequest request, Set<Document> searchResults) {
		if (request.getCreatedFrom() != null) {
			documentsDB.values().stream()
				.filter(document -> document.getCreated().isAfter(request.getCreatedFrom()))
				.forEach(searchResults::add);
		}

		if (request.getCreatedTo() != null) {
			documentsDB.values().stream()
				.filter(document -> document.getCreated().isBefore(request.getCreatedTo()))
				.forEach(searchResults::add);
		}
	}

	/**
	 * Implementation this method should find document by id
	 *
	 * @param id - document id
	 * @return optional document
	 */
	public Optional<Document> findById(String id) {

		return Optional.ofNullable(documentsDB.get(id));
	}

	@Data
	@Builder
	public static class SearchRequest {
		private List<String> titlePrefixes;
		private List<String> containsContents;
		private List<String> authorIds;
		private Instant createdFrom;
		private Instant createdTo;
	}

	@Data
	@Builder
	public static class Document {
		private String id;
		private String title;
		private String content;
		private Author author;
		private Instant created;
	}

	@Data
	@Builder
	public static class Author {
		private String id;
		private String name;
	}
}