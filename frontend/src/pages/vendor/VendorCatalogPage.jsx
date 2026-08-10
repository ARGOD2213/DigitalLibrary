import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Box,
  Button,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  CircularProgress,
  Tooltip,
} from '@mui/material';
import {
  Publish as UploadIcon,
  Visibility as ViewIcon,
  Delete as DeleteIcon,
  Block as UnpublishIcon,
  CheckCircle as PublishIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import api from '../../api/axios';

export const VendorCatalogPage = () => {
  const navigate = useNavigate();
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchVendorBooks = async () => {
    setLoading(true);
    try {
      const res = await api.get('/vendors/me/books');
      setBooks(res.data.data.content || []);
    } catch (err) {
      console.error('Failed to load vendor books:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchVendorBooks();
  }, []);

  const handleTogglePublish = async (bookId, currentStatus) => {
    try {
      const endpoint = currentStatus ? `/books/${bookId}/unpublish` : `/books/${bookId}/publish`;
      await api.patch(endpoint);
      fetchVendorBooks();
    } catch (err) {
      alert('Failed to update publish status');
    }
  };

  const handleDeleteBook = async (bookId) => {
    if (!window.confirm('Are you sure you want to soft-delete this publication?')) return;
    try {
      await api.delete(`/books/${bookId}`);
      fetchVendorBooks();
    } catch (err) {
      alert('Failed to delete book');
    }
  };

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" py={12}>
        <CircularProgress size={60} />
      </Box>
    );
  }

  return (
    <Container maxWidth="xl" sx={{ py: 6 }}>
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={5}>
        <Box>
          <Typography variant="h3" fontWeight={800} gutterBottom>
            My Published Catalog
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Manage your uploaded ebooks, control publication status, and update details.
          </Typography>
        </Box>
        <Button
          variant="contained"
          startIcon={<UploadIcon />}
          onClick={() => navigate('/vendor/upload')}
          sx={{ py: 1.5, px: 3, fontWeight: 700 }}
        >
          Upload New Book
        </Button>
      </Box>

      <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 3 }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Title</TableCell>
              <TableCell>ISBN</TableCell>
              <TableCell>Category</TableCell>
              <TableCell>Price</TableCell>
              <TableCell>Sales Count</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {books.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  You haven't uploaded any books yet.
                </TableCell>
              </TableRow>
            ) : (
              books.map((b) => (
                <TableRow key={b.id}>
                  <TableCell><strong>{b.title}</strong></TableCell>
                  <TableCell><code>{b.isbn || 'N/A'}</code></TableCell>
                  <TableCell><Chip label={b.category || 'General'} size="small" /></TableCell>
                  <TableCell>{b.free ? 'Free' : `₹${b.price}`}</TableCell>
                  <TableCell>{b.totalSales || 0}</TableCell>
                  <TableCell>
                    <Chip
                      label={b.published ? 'PUBLISHED' : 'DRAFT'}
                      color={b.published ? 'success' : 'default'}
                      size="small"
                    />
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="View Details">
                      <IconButton onClick={() => navigate(`/books/${b.id}`)}>
                        <ViewIcon />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title={b.published ? 'Unpublish' : 'Publish'}>
                      <IconButton onClick={() => handleTogglePublish(b.id, b.published)}>
                        {b.published ? <UnpublishIcon color="warning" /> : <PublishIcon color="success" />}
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Delete">
                      <IconButton onClick={() => handleDeleteBook(b.id)}>
                        <DeleteIcon color="error" />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Container>
  );
};

export default VendorCatalogPage;
