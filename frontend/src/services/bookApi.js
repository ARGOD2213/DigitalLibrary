const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8000/api';

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    ...options,
  });

  const body = await response.json().catch(() => null);

  if (!response.ok) {
    const validationDetails = body?.data
      ? Object.values(body.data).join(', ')
      : null;
    throw new Error(validationDetails || body?.message || 'Request failed');
  }

  return body?.data;
}

export function getBooks({ page, size, sortBy, sortDirection }) {
  const params = new URLSearchParams({ page, size, sortBy, sortDirection });
  return request(`/books?${params.toString()}`);
}

export function searchBooks({ keyword, page, size }) {
  const params = new URLSearchParams({ page, size });
  if (keyword) {
    params.set('keyword', keyword);
  }
  return request(`/books/search?${params.toString()}`);
}

export function createBook(book) {
  return request('/books', {
    method: 'POST',
    body: JSON.stringify(book),
  });
}

export function updateBook(id, book) {
  return request(`/books/${id}`, {
    method: 'PUT',
    body: JSON.stringify(book),
  });
}

export function deleteBook(id) {
  return request(`/books/${id}`, {
    method: 'DELETE',
  });
}

export function getHealth() {
  return request('/health');
}
