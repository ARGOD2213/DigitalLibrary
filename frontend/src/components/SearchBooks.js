import React from 'react';

function SearchBooks({ keyword, onKeywordChange, onSearch, onClear, sortBy, sortDirection, onSortChange }) {
  const handleSearch = (event) => {
    event.preventDefault();
    onSearch();
  };

  const handleClear = () => {
    onClear();
  };

  return (
    <section className="search-section">
      <div className="section-header">
        <div>
          <h2>Search And Sort</h2>
          <p>Search by title or author, then page through the backend result.</p>
        </div>
      </div>
      <form onSubmit={handleSearch} className="search-form">
        <input
          value={keyword}
          onChange={(e) => onKeywordChange(e.target.value)}
          placeholder="Search by title or author"
        />
        <select value={sortBy} onChange={(e) => onSortChange(e.target.value, sortDirection)}>
          <option value="title">Sort by title</option>
          <option value="author">Sort by author</option>
          <option value="category">Sort by category</option>
          <option value="availableCopies">Sort by copies</option>
        </select>
        <select value={sortDirection} onChange={(e) => onSortChange(sortBy, e.target.value)}>
          <option value="asc">Ascending</option>
          <option value="desc">Descending</option>
        </select>
        <button type="submit">Search</button>
        <button type="button" className="secondary-button" onClick={handleClear}>Show All</button>
      </form>
    </section>
  );
}

export default SearchBooks;
