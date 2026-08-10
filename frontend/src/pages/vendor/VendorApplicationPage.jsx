import React, { useState } from 'react';
import {
  Container,
  Card,
  CardContent,
  Typography,
  TextField,
  Button,
  Box,
  Alert,
  Grid,
} from '@mui/material';
import { HowToReg as ApplyIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import api from '../../api/axios';

export const VendorApplicationPage = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    businessName: '',
    contactPerson: '',
    email: '',
    phone: '',
    businessType: '',
    description: '',
    website: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await api.post('/vendors/apply', formData);
      setSuccess(true);
      setTimeout(() => navigate('/'), 3000);
    } catch (err) {
      setError(err.response?.data?.message || 'Application submission failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="md" sx={{ py: 8 }}>
      <Card variant="outlined">
        <CardContent sx={{ p: 4 }}>
          <Box textAlign="center" mb={4}>
            <ApplyIcon sx={{ fontSize: 56, color: 'secondary.main' }} />
            <Typography variant="h3" fontWeight={800} gutterBottom>
              Become a Vendor Publisher
            </Typography>
            <Typography variant="body1" color="text.secondary" maxWidth="560px" mx="auto">
              Join our digital publishing ecosystem. Publish and monetize your books with our commission-based model. Admin review typically takes 24–48 hours.
            </Typography>
          </Box>

          {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
          {success && (
            <Alert severity="success" sx={{ mb: 3 }}>
              Application submitted! Our team will review your request within 24–48 hours. Redirecting to home...
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit} display="flex" flexDirection="column" gap={3}>
            <Grid container spacing={2}>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Business / Publisher Name"
                  name="businessName"
                  required
                  fullWidth
                  value={formData.businessName}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Contact Person"
                  name="contactPerson"
                  required
                  fullWidth
                  value={formData.contactPerson}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Business Email"
                  name="email"
                  type="email"
                  required
                  fullWidth
                  value={formData.email}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Phone Number"
                  name="phone"
                  fullWidth
                  value={formData.phone}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Business Type"
                  name="businessType"
                  required
                  fullWidth
                  placeholder="Publisher, Author, Research Institute..."
                  value={formData.businessType}
                  onChange={handleChange}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <TextField
                  label="Website / Portfolio URL"
                  name="website"
                  fullWidth
                  value={formData.website}
                  onChange={handleChange}
                />
              </Grid>
            </Grid>

            <TextField
              label="About Your Publishing Business"
              name="description"
              multiline
              rows={4}
              required
              fullWidth
              placeholder="Describe the type of content you plan to publish and your target audience..."
              value={formData.description}
              onChange={handleChange}
            />

            <Button
              type="submit"
              variant="contained"
              color="secondary"
              size="large"
              disabled={loading || success}
              startIcon={<ApplyIcon />}
              sx={{ py: 1.6, fontSize: '1rem', fontWeight: 700 }}
            >
              {loading ? 'Submitting Application...' : 'Submit Vendor Application'}
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Container>
  );
};

export default VendorApplicationPage;
