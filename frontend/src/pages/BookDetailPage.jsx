import React, { useState, useEffect } from 'react';
import {
  Container,
  Grid,
  Box,
  Typography,
  Button,
  Chip,
  Rating,
  Card,
  CardContent,
  Divider,
  TextField,
  Avatar,
  Alert,
  CircularProgress,
} from '@mui/material';
import {
  MenuBook as ReadIcon,
  ShoppingCart as BuyIcon,
  Favorite as FavoriteIcon,
  FavoriteBorder as FavoriteBorderIcon,
  ArrowBack as BackIcon,
} from '@mui/icons-material';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';

export const BookDetailPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, user } = useAuth();

  const [book, setBook] = useState(null);
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isFav, setIsFav] = useState(false);
  const [newRating, setNewRating] = useState(5);
  const [newComment, setNewComment] = useState('');
  const [reviewError, setReviewError] = useState('');
  const [reviewSuccess, setReviewSuccess] = useState('');

  const fetchBookDetails = async () => {
    setLoading(true);
    try {
      const bookRes = await api.get(`/books/${id}`);
      setBook(bookRes.data.data);

      const reviewRes = await api.get(`/books/${id}/reviews`);
      setReviews(reviewRes.data.data.content || []);

      if (isAuthenticated) {
        const favRes = await api.get(`/books/${id}/favorite/status`).catch(() => null);
        if (favRes) setIsFav(favRes.data.data);
      }
    } catch (err) {
      console.error('Failed to load book details:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBookDetails();
  }, [id, isAuthenticated]);

  const handleToggleFavorite = async () => {
    if (!isAuthenticated) return navigate('/login');
    try {
      if (isFav) {
        await api.delete(`/books/${id}/favorite`);
        setIsFav(false);
      } else {
        await api.post(`/books/${id}/favorite`);
        setIsFav(true);
      }
    } catch (err) {
      console.error('Favorite error:', err);
    }
  };

  const handleAddReview = async (e) => {
    e.preventDefault();
    if (!isAuthenticated) return navigate('/login');
    setReviewError('');
    setReviewSuccess('');

    try {
      await api.post(`/books/${id}/reviews`, {
        rating: newRating,
        comment: newComment,
      });
      setReviewSuccess('Review submitted successfully!');
      setNewComment('');
      fetchBookDetails();
    } catch (err) {
      setReviewError(err.response?.data?.message || 'Failed to submit review');
    }
  };

  const handleCheckout = async () => {
    if (!isAuthenticated) return navigate('/login');
    try {
      const res = await api.post('/payments/checkout', {
        bookIds: [parseInt(id, 10)],
        paymentGateway: 'MOCK',
      });
      alert(`Payment successful! Order Number: ${res.data.data.orderNumber}`);
      navigate('/dashboard');
    } catch (err) {
      alert(err.response?.data?.message || 'Checkout failed');
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" py={12}>
        <CircularProgress size={60} />
      </Box>
    );
  }

  if (!book) {
    return (
      <Container sx={{ py: 8, textAlign: 'center' }}>
        <Typography variant="h5" color="error">
          Book not found.
        </Typography>
        <Button startIcon={<BackIcon />} onClick={() => navigate('/catalog')} sx={{ mt: 2 }}>
          Back to Catalog
        </Button>
      </Container>
    );
  }

  return (
    <Container maxWidth="xl" sx={{ py: 6 }}>
      <Button startIcon={<BackIcon />} onClick={() => navigate('/catalog')} sx={{ mb: 4 }}>
        Back to Catalog
      </Button>

      <Grid container spacing={6}>
        {/* Left Column: Cover Image & Actions */}
        <Grid item xs={12} md={4}>
          <Card variant="outlined" sx={{ overflow: 'hidden', borderRadius: 4 }}>
            <Box
              component="img"
              src={book.coverImageUrl || 'https://via.placeholder.com/400x550?text=Book+Cover'}
              alt={book.title}
              sx={{ width: '100%', height: 480, objectFit: 'cover' }}
            />
          </Card>

          <Box display="flex" flexDirection="column" gap={2} mt={3}>
            <Button
              variant="contained"
              size="large"
              startIcon={<ReadIcon />}
              onClick={() => navigate(`/read/${book.id}`)}
              sx={{ py: 1.6, fontSize: '1.1rem', fontWeight: 700 }}
            >
              Read Online
            </Button>

            {!book.free && (
              <Button
                variant="outlined"
                color="secondary"
                size="large"
                startIcon={<BuyIcon />}
                onClick={handleCheckout}
                sx={{ py: 1.5, fontSize: '1rem', fontWeight: 700 }}
              >
                Buy Book — ₹{book.price}
              </Button>
            )}

            <Button
              variant="outlined"
              color={isFav ? 'secondary' : 'inherit'}
              startIcon={isFav ? <FavoriteIcon /> : <FavoriteBorderIcon />}
              onClick={handleToggleFavorite}
            >
              {isFav ? 'Saved in Favorites' : 'Add to Favorites'}
            </Button>
          </Box>
        </Grid>

        {/* Right Column: Metadata & Reviews */}
        <Grid item xs={12} md={8}>
          <Box display="flex" gap={1} mb={2}>
            <Chip label={book.category || 'General'} color="primary" />
            <Chip label={book.free ? 'FREE ACCESS' : `PAID: ₹${book.price}`} color={book.free ? 'success' : 'default'} />
          </Box>

          <Typography variant="h3" fontWeight={800} gutterBottom>
            {book.title}
          </Typography>

          <Typography variant="h6" color="text.secondary" mb={3}>
            By {book.author || 'Unknown Author'} {book.publisher && `• ${book.publisher}`}
          </Typography>

          <Box display="flex" alignItems="center" gap={2} mb={4}>
            <Rating value={book.averageRating || 4.5} precision={0.5} size="large" readOnly />
            <Typography variant="body1" fontWeight={700}>
              {book.averageRating || 4.5} / 5
            </Typography>
            <Typography variant="body2" color="text.secondary">
              ({reviews.length} reviews • {book.viewCount || 0} readers)
            </Typography>
          </Box>

          <Divider sx={{ my: 3 }} />

          <Typography variant="h5" fontWeight={700} gutterBottom>
            Overview & Summary
          </Typography>
          <Typography variant="body1" color="text.secondary" paragraph sx={{ lineHeight: 1.8 }}>
            {book.description || 'No description available for this publication.'}
          </Typography>

          {book.previewText && (
            <Box p={3} sx={{ bgcolor: 'background.paper', borderRadius: 3, borderLeft: '4px solid #6366f1', my: 3 }}>
              <Typography variant="subtitle2" fontWeight={700} gutterBottom color="primary.main">
                Preview Excerpt
              </Typography>
              <Typography variant="body2" color="text.secondary" style={{ whiteSpace: 'pre-line' }}>
                {book.previewText}
              </Typography>
            </Box>
          )}

          <Divider sx={{ my: 4 }} />

          {/* Reviews & Ratings Section */}
          <Typography variant="h5" fontWeight={700} gutterBottom>
            User Reviews & Ratings
          </Typography>

          {/* Add Review Form */}
          {isAuthenticated ? (
            <Card variant="outlined" sx={{ p: 3, mb: 4, mt: 2 }}>
              <Typography variant="h6" fontWeight={700} mb={1}>
                Leave Your Review
              </Typography>

              {reviewError && <Alert severity="error" sx={{ mb: 2 }}>{reviewError}</Alert>}
              {reviewSuccess && <Alert severity="success" sx={{ mb: 2 }}>{reviewSuccess}</Alert>}

              <Box component="form" onSubmit={handleAddReview} display="flex" flexDirection="column" gap={2}>
                <Box display="flex" alignItems="center" gap={2}>
                  <Typography variant="body2">Your Rating:</Typography>
                  <Rating
                    value={newRating}
                    onChange={(e, val) => setNewRating(val)}
                  />
                </Box>

                <TextField
                  multiline
                  rows={3}
                  placeholder="Share your thoughts about this book..."
                  value={newComment}
                  onChange={(e) => setNewComment(e.target.value)}
                  fullWidth
                />

                <Button type="submit" variant="contained" sx={{ alignSelf: 'flex-start' }}>
                  Submit Review
                </Button>
              </Box>
            </Card>
          ) : (
            <Alert severity="info" sx={{ mb: 4 }}>
              Please sign in to write a review.
            </Alert>
          )}

          {/* Review List */}
          {reviews.length === 0 ? (
            <Typography variant="body2" color="text.secondary">
              No reviews yet. Be the first to review this book!
            </Typography>
          ) : (
            <Box display="flex" flexDirection="column" gap={2}>
              {reviews.map((r) => (
                <Card key={r.id} variant="outlined" sx={{ p: 2.5 }}>
                  <Box display="flex" justifyContent="space-between" alignItems="center" mb={1}>
                    <Box display="flex" alignItems="center" gap={1.5}>
                      <Avatar sx={{ width: 32, height: 32, bgcolor: 'secondary.main', fontSize: '0.9rem' }}>
                        {r.userName ? r.userName[0].toUpperCase() : 'U'}
                      </Avatar>
                      <Typography variant="subtitle2" fontWeight={700}>
                        {r.userName || 'Anonymous Reader'}
                      </Typography>
                    </Box>
                    <Rating value={r.rating} size="small" readOnly />
                  </Box>
                  <Typography variant="body2" color="text.secondary">
                    {r.comment}
                  </Typography>
                </Card>
              ))}
            </Box>
          )}
        </Grid>
      </Grid>
    </Container>
  );
};

export default BookDetailPage;
