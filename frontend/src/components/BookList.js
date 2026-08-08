import React from 'react';

function BookList({ books, pageInfo, loading, onEdit, onDelete, onPageChange }) {
  if (loading) {
    return (
      <section className="book-list-section">
        <h2>Books</h2>
        <p>Loading books...</p>
      </section>
    );
  }

  return (
    <section className="book-list-section">
      <div className="section-header">
        <div>
          <h2>Books</h2>
          <p>{pageInfo.totalElements} total book{pageInfo.totalElements === 1 ? '' : 's'}</p>
        </div>
      </div>
      {books.length === 0 ? (
        <p>No books found. Add one to get started.</p>
      ) : (
        <div className="book-grid">
          {books.map((book) => (
            <div key={book.id} className="book-card">
              <h3>{book.title}</h3>
              <p><strong>Author:</strong> {book.author}</p>
              <p><strong>Category:</strong> {book.category}</p>
              <p><strong>ISBN:</strong> {book.isbn}</p>
              <p><strong>Available Copies:</strong> {book.availableCopies}</p>
              <div className="card-actions">
                <button type="button" className="secondary-button" onClick={() => onEdit(book)}>
                  Edit
                </button>
                <button type="button" className="danger-button" onClick={() => onDelete(book.id)}>
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}

      <div className="pagination-bar">
        <button
          type="button"
          className="secondary-button"
          disabled={pageInfo.first}
          onClick={() => onPageChange(pageInfo.page - 1)}
        >
          Previous
        </button>
        <span>
          Page {pageInfo.totalPages === 0 ? 0 : pageInfo.page + 1} of {pageInfo.totalPages}
        </span>
        <button
          type="button"
          className="secondary-button"
          disabled={pageInfo.last || pageInfo.totalPages === 0}
          onClick={() => onPageChange(pageInfo.page + 1)}
        >
          Next
        </button>
      </div>
    </section>
  );
}

export default BookList;
