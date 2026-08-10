import React, { useState, useEffect } from 'react';
import {
  Container,
  Grid,
  Card,
  CardContent,
  Typography,
  Button,
  Box,
  Chip,
  List,
  ListItem,
  ListItemIcon,
  ListItemText,
  CircularProgress,
} from '@mui/material';
import { Check as CheckIcon, Star as StarIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';

export const SubscriptionsPage = () => {
  const [plans, setPlans] = useState([]);
  const [loading, setLoading] = useState(true);
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    const fetchPlans = async () => {
      setLoading(true);
      try {
        const res = await api.get('/subscriptions/plans');
        setPlans(res.data.data || []);
      } catch (err) {
        console.error('Failed to load plans:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchPlans();
  }, []);

  const handleSubscribe = async (planId) => {
    if (!isAuthenticated) return navigate('/login');
    try {
      const res = await api.post(`/subscriptions/subscribe/${planId}`);
      alert(`Subscribed successfully to ${res.data.data.plan.name}!`);
      navigate('/dashboard');
    } catch (err) {
      alert(err.response?.data?.message || 'Subscription failed');
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 8 }}>
      <Box textAlign="center" mb={6}>
        <Typography variant="h2" fontWeight={800} gutterBottom>
          Flexible Pricing for Every Reader
        </Typography>
        <Typography variant="h6" color="text.secondary" maxWidth="600px" mx="auto">
          Choose a plan that fits your reading goals. Cancel or switch tiers at any time.
        </Typography>
      </Box>

      {loading ? (
        <Box display="flex" justifyContent="center" py={8}>
          <CircularProgress size={60} />
        </Box>
      ) : (
        <Grid container spacing={4} justifyContent="center">
          {plans.map((plan) => {
            const isPopular = plan.planType === 'MONTHLY' || plan.planType === 'YEARLY';
            return (
              <Grid item key={plan.id} xs={12} md={4}>
                <Card
                  variant="outlined"
                  sx={{
                    height: '100%',
                    display: 'flex',
                    flexDirection: 'column',
                    p: 2,
                    position: 'relative',
                    borderColor: isPopular ? 'primary.main' : 'divider',
                    borderWidth: isPopular ? 2 : 1,
                  }}
                >
                  {isPopular && (
                    <Chip
                      icon={<StarIcon sx={{ color: '#fff !important' }} />}
                      label="MOST POPULAR"
                      color="primary"
                      size="small"
                      sx={{ position: 'absolute', top: -14, right: 24, fontWeight: 800 }}
                    />
                  )}

                  <CardContent sx={{ flexGrow: 1, display: 'flex', flexDirection: 'column' }}>
                    <Typography variant="h5" fontWeight={800} gutterBottom>
                      {plan.name}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" mb={3}>
                      {plan.durationDays} Days Unlimited Access
                    </Typography>

                    <Box display="flex" alignItems="baseline" mb={3}>
                      <Typography variant="h3" fontWeight={800} color="primary.main">
                        ₹{plan.price}
                      </Typography>
                      <Typography variant="subtitle1" color="text.secondary" ml={1}>
                        / {plan.durationDays} days
                      </Typography>
                    </Box>

                    <List sx={{ mb: 4, flexGrow: 1 }}>
                      <ListItem disableGutters>
                        <ListItemIcon sx={{ minWidth: 32 }}>
                          <CheckIcon color="primary" />
                        </ListItemIcon>
                        <ListItemText primary={`Up to ${plan.maxDownloadsPerMonth} downloads / mo`} />
                      </ListItem>
                      <ListItem disableGutters>
                        <ListItemIcon sx={{ minWidth: 32 }}>
                          <CheckIcon color="primary" />
                        </ListItemIcon>
                        <ListItemText primary="Unlimited online web reader" />
                      </ListItem>
                      <ListItem disableGutters>
                        <ListItemIcon sx={{ minWidth: 32 }}>
                          <CheckIcon color="primary" />
                        </ListItemIcon>
                        <ListItemText primary="Reading progress synchronization" />
                      </ListItem>
                    </List>

                    <Button
                      variant={isPopular ? 'contained' : 'outlined'}
                      size="large"
                      fullWidth
                      onClick={() => handleSubscribe(plan.id)}
                      sx={{ py: 1.5, fontWeight: 700 }}
                    >
                      Subscribe Now
                    </Button>
                  </CardContent>
                </Card>
              </Grid>
            );
          })}
        </Grid>
      )}
    </Container>
  );
};

export default SubscriptionsPage;
