import React, { useState, useEffect } from 'react';
import {
  Container,
  Grid,
  Card,
  CardMedia,
  CardContent,
  Typography,
  Box,
  TextField,
  Button,
  Chip,
  Rating,
  Pagination,
  InputAdornment,
  MenuItem,
  CircularProgress,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  Search as SearchIcon,
  Favorite as FavoriteIcon,
  FavoriteBorder as FavoriteBorderIcon,
  MenuBook as ReadIcon,
  ShoppingCart as BuyIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';

export const CatalogPage = () => {
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');
  const [sortBy, setSortBy] = useState('title');
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [favorites, setFavorites] = useState(new Set());
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const fetchBooks = async () => {
    setLoading(true);
    try {
      let response;
      if (keyword.trim()) {
        response = await api.get('/books/search', {
          params: { keyword, page: page - 1, size: 9 },
        });
      } else {
        response = await api.get('/books', {
          params: { page: page - 1, size: 9, sortBy, sortDirection: 'desc' },
        });
      }

      const data = response.data.data;
      setBooks(data.content || []);
      setTotalPages(data.totalPages || 1);
    } catch (err) {
      console.error('Failed to load books:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBooks();
  }, [page, sortBy]);

  const handleSearchSubmit = (e) => {
    e.preventDefault();
    setPage(1);
    fetchBooks();
  };

  const toggleFavorite = async (bookId) => {
    if (!isAuthenticated) {
      navigate('/login');
      return;
    }
    try {
      if (favorites.has(bookId)) {
        await api.delete(`/books/${bookId}/favorite`);
        setFavorites((prev) => {
          const next = new Set(prev);
          next.delete(bookId);
          return next;
        });
      } else {
        await api.post(`/books/${bookId}/favorite`);
        setFavorites((prev) => new Set(prev).add(bookId));
      }
    } catch (err) {
      console.error('Favorite toggle error:', err);
    }
  };

  return (
    <Container maxWidth="xl" sx={{ py: 6 }}>
      {/* Header Banner */}
      <Box mb={6} textAlign="center">
        <Typography variant="h2" fontWeight={800} gutterBottom>
          Discover Digital Books & Publications
        </Typography>
        <Typography variant="h6" color="text.secondary" maxWidth="700px" mx="auto">
          Explore thousands of ebooks, technical papers, and articles available for instant online reading or direct purchase.
        </Typography>
      </Box>

      {/* Search & Filter Toolbar */}
      <Box component="form" onSubmit={handleSearchSubmit} mb={5}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} md={7}>
            <TextField
              fullWidth
              placeholder="Search by title, author, category, or ISBN..."
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon color="primary" />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <Button type="submit" variant="contained">
                      Search
                    </Button>
                  </InputAdornment>
                ),
              }}
            />
          </Grid>
          <Grid item xs={6} md={3}>
            <TextField
              select
              fullWidth
              label="Sort By"
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
            >
              <option value="title">Title (A-Z)</option>
              <option value="createdAt">Newest Releases</option>
              <option value="totalSales">Most Popular</option>
              <option value="price">Price</option>
            </TextField>
          </Grid>
          <Grid item xs={6} md={2}>
            <Button
              fullWidth
              variant="outlined"
              sx={{ py: 1.8 }}
              onClick={() => {
                setKeyword('');
                setSortBy('title');
                setPage(1);
              }}
            >
              Reset Filters
            </Button>
          </Grid>
        </Grid>
      </Box>

      {/* Book Grid */}
      {loading ? (
        <Box display="flex" justifyContent="center" py={10}>
          <CircularProgress size={60} />
        </Box>
      ) : books.length === 0 ? (
        <Box textAlign="center" py={10}>
          <Typography variant="h5" color="text.secondary">
            No books found matching your criteria.
          </Typography>
        </Box>
      ) : (
        <Grid container spacing={4}>
          {books.map((book) => (
            <Grid item key={book.id} xs={12} sm={6} md={4}>
              <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column', position: 'relative' }}>
                {book.free && (
                  <Chip
                    label="FREE"
                    color="success"
                    size="small"
                    sx={{ position: 'absolute', top: 16, left: 16, zIndex: 1, fontWeight: 700 }}
                  />
                )}

                <Tooltip title={favorites.has(book.id) ? 'Remove Favorite' : 'Add to Favorites'}>
                  <IconButton
                    onClick={() => toggleFavorite(book.id)}
                    sx={{ position: 'absolute', top: 12, right: 12, zIndex: 1, bgcolor: 'background.paper' }}
                  >
                    {favorites.has(book.id) ? (
                      <FavoriteIcon sx={{ color: 'secondary.main' }} />
                    ) : (
                      <FavoriteBorderIcon />
                    )}
                  </IconButton>
                </Tooltip>

                <CardMedia
                  component="img"
                  height="260"
                  image={book.coverImageUrl || 'https://via.placeholder.com/300x400?text=Digital+Book'}
                  alt={book.title}
                  sx={{ objectFit: 'cover', cursor: 'pointer' }}
                  onClick={() => navigate(`/books/${book.id}`)}
                />

                <CardContent sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column' }}>
                  <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                    <Chip label={book.category || 'General'} size="small" variant="outlined" />
                    <Typography variant="h6" color="primary.main" fontWeight={800}>
                      {book.free ? 'Free' : `₹${book.price}`}
                    </Typography>
                  </Box>

                  <Typography
                    variant="h6"
                    fontWeight={700}
                    gutterBottom
                    sx={{
                      cursor: 'pointer',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      display: '-webkit-box',
                      WebkitLineClamp: 2,
                      WebkitBoxOrient: 'vertical',
                    }}
                    onClick={() => navigate(`/books/${book.id}`)}
                  >
                    {book.title}
                  </Typography>

                  <Typography variant="body2" color="text.secondary" mb={2}>
                    By {book.author || 'Unknown Author'}
                  </Typography>

                  <Box display="flex" alignItems="center" gap={1} mb={2} mt="auto">
                    <Rating value={book.averageRating || 4.5} precision={0.5} size="small" readOnly />
                    <Typography variant="caption" color="text.secondary">
                      ({book.viewCount || 0} views)
                    </Typography>
                  </Box>

                  <Box display="flex" gap={1} mt={1}>
                    <Button
                      variant="contained"
                      fullWidth
                      startIcon={<ReadIcon />}
                      onClick={() => navigate(`/read/${book.id}`)}
                    >
                      Read Now
                    </Button>
                    <Button
                      variant="outlined"
                      color="secondary"
                      onClick={() => navigate(`/books/${book.id}`)}
                    >
                      Details
                    </Button>
                  </Box>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <Box display="flex" justifyContent="center" mt={6}>
          <Pagination
            count={totalPages}
            page={page}
            onChange={(e, value) => setPage(value)}
            color="primary"
            size="large"
          />
        </Box>
      )}
    </Container>
  );
};

export default CatalogPage;
