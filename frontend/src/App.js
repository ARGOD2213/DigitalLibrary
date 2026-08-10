import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from './context/ThemeContext';
import { AuthProvider } from './context/AuthContext';
import { Navbar } from './components/layout/Navbar';
import { Footer } from './components/layout/Footer';
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { Box, Container, Typography, Card, CardContent, Button } from '@mui/material';

// Temporary placeholder page components until Tasks 7.2, 7.3, 7.4 build detailed views
const CatalogPage = () => (
  <Container maxWidth="xl" sx={{ py: 6 }}>
    <Typography variant="h3" fontWeight={800} gutterBottom>
      Book Catalog & Marketplace
    </Typography>
    <Typography variant="body1" color="text.secondary">
      Browse our extensive collection of digital books, research papers, and technical guides.
    </Typography>
  </Container>
);

const SubscriptionsPage = () => (
  <Container maxWidth="lg" sx={{ py: 6 }}>
    <Typography variant="h3" fontWeight={800} align="center" gutterBottom>
      Choose Your Subscription Plan
    </Typography>
    <Typography variant="body1" color="text.secondary" align="center" sx={{ mb: 4 }}>
      Unlock unlimited reading and downloads with our flexible pricing options.
    </Typography>
  </Container>
);

const LoginPage = () => (
  <Container maxWidth="sm" sx={{ py: 8 }}>
    <Card variant="outlined">
      <CardContent sx={{ p: 4 }}>
        <Typography variant="h4" fontWeight={700} align="center" gutterBottom>
          Welcome Back
        </Typography>
        <Typography variant="body2" color="text.secondary" align="center" mb={3}>
          Sign in to your Digital Library account
        </Typography>
      </CardContent>
    </Card>
  </Container>
);

const RegisterPage = () => (
  <Container maxWidth="sm" sx={{ py: 8 }}>
    <Card variant="outlined">
      <CardContent sx={{ p: 4 }}>
        <Typography variant="h4" fontWeight={700} align="center" gutterBottom>
          Create Account
        </Typography>
        <Typography variant="body2" color="text.secondary" align="center" mb={3}>
          Join thousands of readers and publishers on Digital Library
        </Typography>
      </CardContent>
    </Card>
  </Container>
);

const UnauthorizedPage = () => (
  <Container maxWidth="sm" sx={{ py: 8, textAlign: 'center' }}>
    <Typography variant="h3" color="error" fontWeight={800} gutterBottom>
      403 - Unauthorized Access
    </Typography>
    <Typography variant="body1" color="text.secondary" mb={3}>
      You do not have the required permissions to view this portal.
    </Typography>
    <Button variant="contained" href="/">Return to Home</Button>
  </Container>
);

function App() {
  return (
    <Router>
      <AuthProvider>
        <ThemeProvider>
          <Box display="flex" flexDirection="column" minHeight="100vh">
            <Navbar />
            <Box component="main" flexGrow={1}>
              <Routes>
                <Route path="/" element={<CatalogPage />} />
                <Route path="/catalog" element={<CatalogPage />} />
                <Route path="/subscriptions" element={<SubscriptionsPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/unauthorized" element={<UnauthorizedPage />} />

                {/* Protected User Routes */}
                <Route element={<ProtectedRoute allowedRoles={['ROLE_USER', 'ROLE_VENDOR', 'ROLE_ADMIN']} />}>
                  <Route path="/dashboard" element={<CatalogPage />} />
                  <Route path="/my-library" element={<CatalogPage />} />
                </Route>

                {/* Vendor Portal */}
                <Route element={<ProtectedRoute allowedRoles={['ROLE_VENDOR', 'ROLE_PARTNER', 'ROLE_ADMIN']} />}>
                  <Route path="/vendor/dashboard" element={<CatalogPage />} />
                </Route>

                {/* Admin Portal */}
                <Route element={<ProtectedRoute allowedRoles={['ROLE_ADMIN']} />}>
                  <Route path="/admin/dashboard" element={<CatalogPage />} />
                </Route>

                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </Box>
            <Footer />
          </Box>
        </ThemeProvider>
      </AuthProvider>
    </Router>
  );
}

export default App;
