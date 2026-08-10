import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider } from './context/ThemeContext';
import { AuthProvider } from './context/AuthContext';
import { Navbar } from './components/layout/Navbar';
import { Footer } from './components/layout/Footer';
import { ProtectedRoute } from './components/common/ProtectedRoute';
import { Container, Typography, Button } from '@mui/material';

// Public Pages
import CatalogPage from './pages/CatalogPage';
import BookDetailPage from './pages/BookDetailPage';
import SubscriptionsPage from './pages/SubscriptionsPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';

// Protected User Pages
import BookReaderPage from './pages/BookReaderPage';
import UserDashboardPage from './pages/UserDashboardPage';

// Vendor Pages
import VendorDashboardPage from './pages/vendor/VendorDashboardPage';
import VendorCatalogPage from './pages/vendor/VendorCatalogPage';
import VendorUploadPage from './pages/vendor/VendorUploadPage';
import VendorCommissionsPage from './pages/vendor/VendorCommissionsPage';
import VendorApplicationPage from './pages/vendor/VendorApplicationPage';

// Admin Pages
import AdminDashboardPage from './pages/admin/AdminDashboardPage';

import Box from '@mui/material/Box';

const UnauthorizedPage = () => (
  <Container maxWidth="sm" sx={{ py: 10, textAlign: 'center' }}>
    <Typography variant="h3" color="error" fontWeight={800} gutterBottom>
      403 — Access Denied
    </Typography>
    <Typography variant="body1" color="text.secondary" mb={3}>
      You do not have the required role to access this portal.
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
                {/* ─── Public Routes ───────────────────────────────── */}
                <Route path="/" element={<CatalogPage />} />
                <Route path="/catalog" element={<CatalogPage />} />
                <Route path="/books/:id" element={<BookDetailPage />} />
                <Route path="/subscriptions" element={<SubscriptionsPage />} />
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />
                <Route path="/unauthorized" element={<UnauthorizedPage />} />
                <Route path="/vendor/apply" element={<VendorApplicationPage />} />

                {/* ─── Protected User Routes ───────────────────────── */}
                <Route element={<ProtectedRoute allowedRoles={['ROLE_USER', 'ROLE_VENDOR', 'ROLE_PARTNER', 'ROLE_ADMIN']} />}>
                  <Route path="/dashboard" element={<UserDashboardPage />} />
                  <Route path="/my-library" element={<UserDashboardPage />} />
                  <Route path="/read/:id" element={<BookReaderPage />} />
                </Route>

                {/* ─── Vendor Portal ───────────────────────────────── */}
                <Route element={<ProtectedRoute allowedRoles={['ROLE_VENDOR', 'ROLE_PARTNER', 'ROLE_ADMIN']} />}>
                  <Route path="/vendor/dashboard" element={<VendorDashboardPage />} />
                  <Route path="/vendor/catalog" element={<VendorCatalogPage />} />
                  <Route path="/vendor/upload" element={<VendorUploadPage />} />
                  <Route path="/vendor/commissions" element={<VendorCommissionsPage />} />
                </Route>

                {/* ─── Admin Portal ────────────────────────────────── */}
                <Route element={<ProtectedRoute allowedRoles={['ROLE_ADMIN']} />}>
                  <Route path="/admin/dashboard" element={<AdminDashboardPage />} />
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
