import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from './context/ThemeContext';
import { AuthProvider } from './context/AuthContext';
import { Navbar } from './components/layout/Navbar';
import { Footer } from './components/layout/Footer';
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { Box, Container, Typography, Button } from '@mui/material';

// Pages
import CatalogPage from './pages/CatalogPage';
import BookDetailPage from './pages/BookDetailPage';
import BookReaderPage from './pages/BookReaderPage';
import UserDashboardPage from './pages/UserDashboardPage';
import SubscriptionsPage from './pages/SubscriptionsPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';

const UnauthorizedPage = () => (
  <Container maxWidth="sm" sx={{ py: 10, textAlign: 'center' }}>
    <Typography variant="h3" color="error" fontWeight={800} gutterBottom>
      403 - Unauthorized Access
    </Typography>
    <Typography variant="body1" color="text.secondary" mb={3}>
      You do not have the required permissions to view this portal.
    </Typography>
    <Button variant="contained" href="/">Return to Catalog</Button>
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
                <Route path="/books/:id" element={<BookDetailPage />} />
                <Route path="/subscriptions" element={<SubscriptionsPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/unauthorized" element={<UnauthorizedPage />} />

                {/* Protected User Routes */}
                <Route element={<ProtectedRoute allowedRoles={['ROLE_USER', 'ROLE_VENDOR', 'ROLE_PARTNER', 'ROLE_ADMIN']} />}>
                  <Route path="/dashboard" element={<UserDashboardPage />} />
                  <Route path="/my-library" element={<UserDashboardPage />} />
                  <Route path="/read/:id" element={<BookReaderPage />} />
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
