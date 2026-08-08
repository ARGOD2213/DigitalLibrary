import React, { useEffect, useState } from 'react';
import AddBookForm from './components/AddBookForm';
import BookList from './components/BookList';
import SearchBooks from './components/SearchBooks';
import { createBook, deleteBook, getBooks, searchBooks, updateBook } from './services/bookApi';
import './App.css';

const defaultPageInfo = {
  content: [],
  page: 0,
  size: 6,
  totalElements: 0,
  totalPages: 0,
  first: true,
  last: true,
};

function App() {
  const [books, setBooks] = useState([]);
  const [pageInfo, setPageInfo] = useState(defaultPageInfo);
  const [keyword, setKeyword] = useState('');
  const [sortBy, setSortBy] = useState('title');
  const [sortDirection, setSortDirection] = useState('asc');
  const [editingBook, setEditingBook] = useState(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const showMessage = (text) => {
    setMessage(text);
    setTimeout(() => setMessage(''), 3000);
  };

  const applyPage = (pageData) => {
    setBooks(pageData.content);
    setPageInfo(pageData);
  };

  const loadBooks = async (page = 0) => {
    setError('');
    setLoading(true);
    try {
      const pageData = keyword
        ? await searchBooks({ keyword, page, size: pageInfo.size })
        : await getBooks({ page, size: pageInfo.size, sortBy, sortDirection });
      applyPage(pageData);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBooks(0);
  }, [sortBy, sortDirection]);

  const handleSubmitBook = async (book) => {
    setError('');
    try {
      if (editingBook) {
        await updateBook(editingBook.id, book);
        setEditingBook(null);
        showMessage('Book updated successfully');
      } else {
        await createBook(book);
        showMessage('Book added successfully');
      }
      await loadBooks(0);
    } catch (err) {
      setError(err.message);
    }
  };

  const handleSearch = async () => {
    setError('');
    setLoading(true);
    try {
      const pageData = await searchBooks({ keyword, page: 0, size: pageInfo.size });
      applyPage(pageData);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleClear = async () => {
    setKeyword('');
    setEditingBook(null);
    setError('');
    setLoading(true);
    try {
      const pageData = await getBooks({ page: 0, size: pageInfo.size, sortBy, sortDirection });
      applyPage(pageData);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (id) => {
    setError('');
    try {
      await deleteBook(id);
      showMessage('Book deleted successfully');
      await loadBooks(Math.max(pageInfo.page, 0));
    } catch (err) {
      setError(err.message);
    }
  };

  const handleSortChange = (nextSortBy, nextSortDirection) => {
    setSortBy(nextSortBy);
    setSortDirection(nextSortDirection);
  };

  return (
    <div className="app-container">
      <header>
        <h1>Digital Library</h1>
        <p>Local Spring Boot, React, and PostgreSQL app for learning production patterns step by step.</p>
      </header>

      <div className="notification-bar">
        {message && <div className="success-message">{message}</div>}
        {error && <div className="error-message">{error}</div>}
      </div>

      <SearchBooks
        keyword={keyword}
        onKeywordChange={setKeyword}
        onSearch={handleSearch}
        onClear={handleClear}
        sortBy={sortBy}
        sortDirection={sortDirection}
        onSortChange={handleSortChange}
      />
      <AddBookForm
        editingBook={editingBook}
        onCancelEdit={() => setEditingBook(null)}
        onSubmitBook={handleSubmitBook}
      />
      <BookList
        books={books}
        pageInfo={pageInfo}
        loading={loading}
        onEdit={setEditingBook}
        onDelete={handleDelete}
        onPageChange={loadBooks}
      />
    </div>
  );
}

export default App;
