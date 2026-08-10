import React, { useState, useEffect } from 'react';
import {
  Container,
  Grid,
  Card,
  CardContent,
  Typography,
  Box,
  LinearProgress,
  Button,
  Chip,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  CircularProgress,
  Avatar,
  Tab,
  Tabs,
} from '@mui/material';
import {
  Bookmark as BookmarkIcon,
  History as HistoryIcon,
  Payment as PaymentIcon,
  CardMembership as SubIcon,
  MenuBook as ReadIcon,
} from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';

export const UserDashboardPage = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [tabValue, setTabValue] = useState(0);
  const [loading, setLoading] = useState(true);
  const [subscription, setSubscription] = useState(null);
  const [readingHistory, setReadingHistory] = useState([]);
  const [favorites, setFavorites] = useState([]);
  const [payments, setPayments] = useState([]);

  useEffect(() => {
    const fetchDashboardData = async () => {
      setLoading(true);
      try {
        const [subRes, historyRes, favRes, payRes] = await Promise.all([
          api.get('/subscriptions/me').catch(() => null),
          api.get('/users/me/reading-history').catch(() => null),
          api.get('/users/me/favorites').catch(() => null),
          api.get('/payments/me').catch(() => null),
        ]);

        if (subRes?.data?.data) setSubscription(subRes.data.data);
        if (historyRes?.data?.data?.content) setReadingHistory(historyRes.data.data.content);
        if (favRes?.data?.data?.content) setFavorites(favRes.data.data.content);
        if (payRes?.data?.data?.content) setPayments(payRes.data.data.content);
      } catch (err) {
        console.error('Dashboard fetch error:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardData();
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
      {/* Profile Welcome Header */}
      <Card variant="outlined" sx={{ p: 4, mb: 5, background: 'linear-gradient(135deg, rgba(99,102,241,0.1) 0%, rgba(236,72,153,0.1) 100%)' }}>
        <Box display="flex" alignItems="center" gap={3}>
          <Avatar sx={{ width: 72, height: 72, bgcolor: 'primary.main', fontSize: '2rem', fontWeight: 800 }}>
            {user?.fullName ? user.fullName[0].toUpperCase() : 'U'}
          </Avatar>
          <Box flexGrow={1}>
            <Typography variant="h4" fontWeight={800}>
              Welcome back, {user?.fullName || 'Reader'}!
            </Typography>
            <Typography variant="body1" color="text.secondary">
              {user?.email} • Account Status: <strong style={{ color: '#10b981' }}>Active</strong>
            </Typography>
          </Box>
          <Chip
            label={user?.role?.replace('ROLE_', '')}
            color="primary"
            sx={{ fontWeight: 700, px: 2, py: 2.5, fontSize: '0.9rem' }}
          />
        </Box>
      </Card>

      {/* Subscription Summary Banner */}
      <Grid container spacing={4} mb={5}>
        <Grid item xs={12} md={6}>
          <Card variant="outlined" sx={{ height: '100%', p: 3 }}>
            <Box display="flex" alignItems="center" gap={2} mb={2}>
              <SubIcon color="primary" sx={{ fontSize: 32 }} />
              <Typography variant="h6" fontWeight={700}>
                Active Membership Plan
              </Typography>
            </Box>
            {subscription ? (
              <Box>
                <Typography variant="h5" color="primary.main" fontWeight={800} gutterBottom>
                  {subscription.plan?.name || 'Standard Subscription'}
                </Typography>
                <Typography variant="body2" color="text.secondary" mb={2}>
                  Valid through: <strong>{new Date(subscription.endDate).toLocaleDateString()}</strong>
                </Typography>
                <Chip label={subscription.status} color="success" size="small" />
              </Box>
            ) : (
              <Box>
                <Typography variant="body2" color="text.secondary" mb={2}>
                  You are currently on the Free Basic Tier. Upgrade to access premium publications.
                </Typography>
                <Button variant="contained" onClick={() => navigate('/subscriptions')}>
                  Upgrade Membership
                </Button>
              </Box>
            )}
          </Card>
        </Grid>

        <Grid item xs={12} md={6}>
          <Card variant="outlined" sx={{ height: '100%', p: 3 }}>
            <Box display="flex" alignItems="center" gap={2} mb={2}>
              <HistoryIcon color="secondary" sx={{ fontSize: 32 }} />
              <Typography variant="h6" fontWeight={700}>
                Reading Activity Overview
              </Typography>
            </Box>
            <Typography variant="h4" fontWeight={800} color="secondary.main" gutterBottom>
              {readingHistory.length} Publications
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Tracked across your current library session. Continue reading anytime from where you left off.
            </Typography>
          </Card>
        </Grid>
      </Grid>

      {/* Tabs Section */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 4 }}>
        <Tabs value={tabValue} onChange={(e, val) => setTabValue(val)}>
          <Tab icon={<HistoryIcon />} iconPosition="start" label="Reading History" />
          <Tab icon={<BookmarkIcon />} iconPosition="start" label="Saved Favorites" />
          <Tab icon={<PaymentIcon />} iconPosition="start" label="Payment Records" />
        </Tabs>
      </Box>

      {/* Tab 0: Reading History */}
      {tabValue === 0 && (
        <Grid container spacing={3}>
          {readingHistory.length === 0 ? (
            <Grid item xs={12}>
              <Typography variant="body1" color="text.secondary" align="center" py={4}>
                No reading history tracked yet. Browse the catalog to start reading!
              </Typography>
            </Grid>
          ) : (
            readingHistory.map((item) => (
              <Grid item key={item.id} xs={12} md={6}>
                <Card variant="outlined" sx={{ p: 2.5, display: 'flex', gap: 2, alignItems: 'center' }}>
                  <Box
                    component="img"
                    src={item.coverImageUrl || 'https://via.placeholder.com/100x140'}
                    alt={item.bookTitle}
                    sx={{ width: 80, height: 110, borderRadius: 2, objectFit: 'cover' }}
                  />
                  <Box flexGrow={1}>
                    <Typography variant="h6" fontWeight={700} gutterBottom>
                      {item.bookTitle}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" mb={1}>
                      Last Read Page: <strong>{item.lastPageRead}</strong>
                    </Typography>

                    <Box display="flex" alignItems="center" gap={2} mb={1}>
                      <LinearProgress
                        variant="determinate"
                        value={item.progressPercentage || 0}
                        sx={{ flexGrow: 1, height: 8, borderRadius: 4 }}
                      />
                      <Typography variant="caption" fontWeight={700}>
                        {item.progressPercentage}%
                      </Typography>
                    </Box>

                    <Button
                      size="small"
                      variant="contained"
                      startIcon={<ReadIcon />}
                      onClick={() => navigate(`/read/${item.bookId}`)}
                    >
                      Resume Reading
                    </Button>
                  </Box>
                </Card>
              </Grid>
            ))
          )}
        </Grid>
      )}

      {/* Tab 1: Saved Favorites */}
      {tabValue === 1 && (
        <Grid container spacing={3}>
          {favorites.length === 0 ? (
            <Grid item xs={12}>
              <Typography variant="body1" color="text.secondary" align="center" py={4}>
                You haven't saved any favorite books yet.
              </Typography>
            </Grid>
          ) : (
            favorites.map((book) => (
              <Grid item key={book.id} xs={12} sm={6} md={4}>
                <Card variant="outlined" sx={{ p: 2, display: 'flex', flexDirection: 'column' }}>
                  <Typography variant="h6" fontWeight={700} gutterBottom>
                    {book.title}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" mb={2}>
                    By {book.author || 'Unknown Author'}
                  </Typography>
                  <Button variant="contained" onClick={() => navigate(`/books/${book.id}`)}>
                    View Details
                  </Button>
                </Card>
              </Grid>
            ))
          )}
        </Grid>
      )}

      {/* Tab 2: Payment Records */}
      {tabValue === 2 && (
        <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 3 }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Transaction ID</TableCell>
                <TableCell>Order Number</TableCell>
                <TableCell>Gateway</TableCell>
                <TableCell>Amount</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Date</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {payments.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    No payment history recorded.
                  </TableCell>
                </TableRow>
              ) : (
                payments.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell><code>{p.transactionId}</code></TableCell>
                    <TableCell>{p.orderNumber || 'N/A'}</TableCell>
                    <TableCell>{p.paymentGateway}</TableCell>
                    <TableCell><strong>₹{p.amount}</strong></TableCell>
                    <TableCell>
                      <Chip label={p.status} color={p.status === 'SUCCESS' ? 'success' : 'warning'} size="small" />
                    </TableCell>
                    <TableCell>{new Date(p.createdAt).toLocaleDateString()}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Container>
  );
};

export default UserDashboardPage;
