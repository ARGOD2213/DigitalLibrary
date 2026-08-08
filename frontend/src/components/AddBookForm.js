import React, { useEffect, useState } from 'react';

const emptyBook = {
  title: '',
  author: '',
  category: '',
  isbn: '',
  availableCopies: 1,
};

function AddBookForm({ editingBook, onCancelEdit, onSubmitBook }) {
  const [formData, setFormData] = useState(emptyBook);

  useEffect(() => {
    if (editingBook) {
      setFormData({
        title: editingBook.title,
        author: editingBook.author,
        category: editingBook.category,
        isbn: editingBook.isbn,
        availableCopies: editingBook.availableCopies,
      });
      return;
    }
    setFormData(emptyBook);
  }, [editingBook]);

  const updateField = (field, value) => {
    setFormData((current) => ({
      ...current,
      [field]: value,
    }));
  };

  const submitBook = async (event) => {
    event.preventDefault();
    await onSubmitBook({
      ...formData,
      availableCopies: Number(formData.availableCopies),
    });

    if (!editingBook) {
      setFormData(emptyBook);
    }
  };

  return (
    <section className="book-form-section">
      <div className="section-header">
        <div>
          <h2>{editingBook ? 'Edit Book' : 'Add Book'}</h2>
          <p>{editingBook ? 'Update the selected book details.' : 'Create a new record in PostgreSQL.'}</p>
        </div>
        {editingBook && (
          <button type="button" className="secondary-button" onClick={onCancelEdit}>
            Cancel
          </button>
        )}
      </div>
      <form onSubmit={submitBook} className="book-form">
        <label>
          Title
          <input value={formData.title} onChange={(e) => updateField('title', e.target.value)} required />
        </label>
        <label>
          Author
          <input value={formData.author} onChange={(e) => updateField('author', e.target.value)} required />
        </label>
        <label>
          Category
          <input value={formData.category} onChange={(e) => updateField('category', e.target.value)} required />
        </label>
        <label>
          ISBN
          <input value={formData.isbn} onChange={(e) => updateField('isbn', e.target.value)} required />
        </label>
        <label>
          Available Copies
          <input
            type="number"
            min="0"
            value={formData.availableCopies}
            onChange={(e) => updateField('availableCopies', e.target.value)}
            required
          />
        </label>
        <button type="submit">{editingBook ? 'Update Book' : 'Add Book'}</button>
      </form>
    </section>
  );
}

export default AddBookForm;
