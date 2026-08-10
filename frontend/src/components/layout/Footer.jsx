import React from 'react';
import { Box, Container, Typography, Grid, Link, Divider } from '@mui/material';
import { MenuBook as BookIcon } from '@mui/icons-material';

export const Footer = () => {
  return (
    <Box
      component="footer"
      sx={{
        mt: 'auto',
        py: 6,
        backgroundColor: (theme) =>
          theme.palette.mode === 'dark' ? 'rgba(15, 23, 42, 0.95)' : 'rgba(241, 245, 249, 0.95)',
        borderTop: (theme) =>
          theme.palette.mode === 'dark' ? '1px solid rgba(255, 255, 255, 0.08)' : '1px solid rgba(0, 0, 0, 0.06)',
      }}
    >
      <Container maxWidth="xl">
        <Grid container spacing={4}>
          <Grid item xs={12} md={4}>
            <Box display="flex" alignItems="center" mb={2}>
              <BookIcon sx={{ color: 'primary.main', fontSize: 28, mr: 1 }} />
              <Typography variant="h6" fontWeight={700}>
                Digital Library Platform
              </Typography>
            </Box>
            <Typography variant="body2" color="text.secondary">
              Next-generation digital publication ecosystem supporting subscription access, vendor publishing, real-time analytics, and secure document streaming.
            </Typography>
          </Grid>
          <Grid item xs={6} md={2}>
            <Typography variant="subtitle2" fontWeight={700} mb={1.5}>
              Platform
            </Typography>
            <Link href="/catalog" color="text.secondary" display="block" underline="hover" mb={0.8}>
              Book Catalog
            </Link>
            <Link href="/subscriptions" color="text.secondary" display="block" underline="hover" mb={0.8}>
              Subscriptions
            </Link>
            <Link href="/vendor/apply" color="text.secondary" display="block" underline="hover">
              Become a Vendor
            </Link>
          </Grid>
          <Grid item xs={6} md={2}>
            <Typography variant="subtitle2" fontWeight={700} mb={1.5}>
              Account
            </Typography>
            <Link href="/login" color="text.secondary" display="block" underline="hover" mb={0.8}>
              Sign In
            </Link>
            <Link href="/register" color="text.secondary" display="block" underline="hover" mb={0.8}>
              Register
            </Link>
            <Link href="/my-library" color="text.secondary" display="block" underline="hover">
              My Reading List
            </Link>
          </Grid>
          <Grid item xs={12} md={4}>
            <Typography variant="subtitle2" fontWeight={700} mb={1.5}>
              System Status
            </Typography>
            <Typography variant="body2" color="text.secondary" mb={1}>
              Backend: <strong style={{ color: '#10b981' }}>Spring Boot 3.2.5</strong> (Modular Monolith)
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Security: <strong style={{ color: '#6366f1' }}>JWT Dual-Token Rotation & Rate-Limited</strong>
            </Typography>
          </Grid>
        </Grid>
        <Divider sx={{ my: 4 }} />
        <Typography variant="body2" color="text.secondary" align="center">
          © {new Date().getFullYear()} Digital Library Inc. All rights reserved. Powered by Spring Boot & React MUI.
        </Typography>
      </Container>
    </Box>
  );
};
