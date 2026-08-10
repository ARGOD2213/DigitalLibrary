import React, { useState, useEffect } from 'react';
import {
  Container,
  Grid,
  Card,
  CardContent,
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
  CircularProgress,
} from '@mui/material';
import {
  Storefront as VendorIcon,
  MenuBook as BookIcon,
  MonetizationOn as RevenueIcon,
  Publish as UploadIcon,
  Assessment as AnalyticsIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import api from '../../api/axios';
import { useAuth } from '../../context/AuthContext';

export const VendorDashboardPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [loading, setLoading] = useState(true);
  const [books, setBooks] = useState([]);
  const [commissions, setCommissions] = useState([]);
  const [stats, setStats] = useState({
    totalBooks: 0,
    totalEarnings: 0,
    totalSalesCount: 0,
  });

  useEffect(() => {
    const fetchVendorData = async () => {
      setLoading(true);
      try {
        const [booksRes, commRes] = await Promise.all([
          api.get('/vendors/me/books').catch(() => null),
          api.get('/vendors/me/commissions').catch(() => null),
        ]);

        const bookList = booksRes?.data?.data?.content || [];
        const commList = commRes?.data?.data?.content || [];

        setBooks(bookList);
        setCommissions(commList);

        let earnings = 0;
        commList.forEach((c) => {
          earnings += c.vendorEarning || 0;
        });

        setStats({
          totalBooks: bookList.length,
          totalEarnings: earnings.toFixed(2),
          totalSalesCount: commList.length,
        });
      } catch (err) {
        console.error('Failed to load vendor dashboard:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchVendorData();
  }, []);

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" py={12}>
        <CircularProgress size={60} />
      </Box>
    );
  }

  return (
    <Container maxWidth="xl" sx={{ py: 6 }}>
      {/* Vendor Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={5}>
        <Box>
          <Typography variant="h3" fontWeight={800} gutterBottom>
            Vendor Publishing Portal
          </Typography>
          <Typography variant="body1" color="text.secondary">
            Manage your digital library catalog, track sales performance, and view commission revenue payouts.
          </Typography>
        </Box>
        <Box display="flex" gap={2}>
          <Button
            variant="contained"
            color="primary"
            startIcon={<UploadIcon />}
            onClick={() => navigate('/vendor/upload')}
            sx={{ py: 1.5, px: 3, fontWeight: 700 }}
          >
            Upload New Book / Bundle
          </Button>
        </Box>
      </Box>

      {/* Analytics Metric Cards */}
      <Grid container spacing={4} mb={6}>
        <Grid item xs={12} sm={4}>
          <Card variant="outlined" sx={{ p: 3, background: 'linear-gradient(135deg, rgba(99,102,241,0.1) 0%, rgba(99,102,241,0.02) 100%)' }}>
            <Box display="flex" alignItems="center" gap={2} mb={1}>
              <BookIcon color="primary" sx={{ fontSize: 36 }} />
              <Typography variant="h6" color="text.secondary">
                Published Books
              </Typography>
            </Box>
            <Typography variant="h3" fontWeight={800} color="primary.main">
              {stats.totalBooks}
            </Typography>
          </Card>
        </Grid>

        <Grid item xs={12} sm={4}>
          <Card variant="outlined" sx={{ p: 3, background: 'linear-gradient(135deg, rgba(16,185,129,0.1) 0%, rgba(16,185,129,0.02) 100%)' }}>
            <Box display="flex" alignItems="center" gap={2} mb={1}>
              <RevenueIcon sx={{ fontSize: 36, color: '#10b981' }} />
              <Typography variant="h6" color="text.secondary">
                Net Earnings
              </Typography>
            </Box>
            <Typography variant="h3" fontWeight={800} sx={{ color: '#10b981' }}>
              ₹{stats.totalEarnings}
            </Typography>
          </Card>
        </Grid>

        <Grid item xs={12} sm={4}>
          <Card variant="outlined" sx={{ p: 3, background: 'linear-gradient(135deg, rgba(236,72,153,0.1) 0%, rgba(236,72,153,0.02) 100%)' }}>
            <Box display="flex" alignItems="center" gap={2} mb={1}>
              <AnalyticsIcon color="secondary" sx={{ fontSize: 36 }} />
              <Typography variant="h6" color="text.secondary">
                Completed Sales
              </Typography>
            </Box>
            <Typography variant="h3" fontWeight={800} color="secondary.main">
              {stats.totalSalesCount}
            </Typography>
          </Card>
        </Grid>
      </Grid>

      {/* Quick Action Navigation Grid */}
      <Grid container spacing={3} mb={6}>
        <Grid item xs={12} sm={6} md={3}>
          <Card variant="outlined" sx={{ p: 2.5, cursor: 'pointer', '&:hover': { borderColor: 'primary.main' } }} onClick={() => navigate('/vendor/catalog')}>
            <Typography variant="h6" fontWeight={700} gutterBottom>
              Catalog Manager
            </Typography>
            <Typography variant="body2" color="text.secondary">
              View and edit your uploaded books
            </Typography>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card variant="outlined" sx={{ p: 2.5, cursor: 'pointer', '&:hover': { borderColor: 'primary.main' } }} onClick={() => navigate('/vendor/upload')}>
            <Typography variant="h6" fontWeight={700} gutterBottom>
              ZIP Bundle Ingestion
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Upload zip files with automated metadata
            </Typography>
          </Card>
        </Grid>
        <Grid item xs={12} sm={6} md={3}>
          <Card variant="outlined" sx={{ p: 2.5, cursor: 'pointer', '&:hover': { borderColor: 'primary.main' } }} onClick={() => navigate('/vendor/commissions')}>
            <Typography variant="h6" fontWeight={700} gutterBottom>
              Commission Ledger
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Detailed platform vs vendor split reports
            </Typography>
          </Card>
        </Grid>
      </Grid>

      {/* Recent Catalog Preview */}
      <Typography variant="h5" fontWeight={700} mb={3}>
        Recently Published Publications
      </Typography>
      <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 3 }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Title</TableCell>
              <TableCell>Category</TableCell>
              <TableCell>Price</TableCell>
              <TableCell>Total Sales</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Published Date</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {books.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} align="center">
                  No books published yet. Click "Upload New Book" to start!
                </TableCell>
              </TableRow>
            ) : (
              books.slice(0, 5).map((b) => (
                <TableRow key={b.id}>
                  <TableCell><strong>{b.title}</strong></TableCell>
                  <TableCell><Chip label={b.category || 'General'} size="small" /></TableCell>
                  <TableCell>{b.free ? 'Free' : `₹${b.price}`}</TableCell>
                  <TableCell>{b.totalSales || 0}</TableCell>
                  <TableCell>
                    <Chip label={b.status || 'PUBLISHED'} color="success" size="small" />
                  </TableCell>
                  <TableCell>{new Date(b.createdAt).toLocaleDateString()}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Container>
  );
};

export default VendorDashboardPage;
