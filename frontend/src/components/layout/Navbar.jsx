import React, { useState } from 'react';
import {
  AppBar,
  Toolbar,
  Typography,
  Button,
  IconButton,
  Box,
  Menu,
  MenuItem,
  Avatar,
  Tooltip,
  Container,
  Chip,
} from '@mui/material';
import {
  MenuBook as BookIcon,
  Brightness4 as DarkIcon,
  Brightness7 as LightIcon,
  AccountCircle,
  Dashboard as DashboardIcon,
  Storefront as VendorIcon,
  AdminPanelSettings as AdminIcon,
  Logout as LogoutIcon,
} from '@mui/icons-material';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { useAppTheme } from '../../context/ThemeContext';

export const Navbar = () => {
  const { user, isAuthenticated, logout, isAdmin, isVendor } = useAuth();
  const { mode, toggleTheme } = useAppTheme();
  const navigate = useNavigate();
  const [anchorEl, setAnchorEl] = useState(null);

  const handleOpenMenu = (event) => setAnchorEl(event.currentTarget);
  const handleCloseMenu = () => setAnchorEl(null);

  const handleLogout = async () => {
    handleCloseMenu();
    await logout();
    navigate('/login');
  };

  return (
    <AppBar position="sticky">
      <Container maxWidth="xl">
        <Toolbar disableGutters justifycontent="space-between">
          {/* Logo & Brand */}
          <Box display="flex" alignItems="center" component={RouterLink} to="/" sx={{ textDecoration: 'none', color: 'inherit', mr: 4 }}>
            <BookIcon sx={{ color: 'primary.main', fontSize: 32, mr: 1 }} />
            <Typography variant="h5" fontWeight={800} sx={{ background: 'linear-gradient(45deg, #6366f1, #ec4899)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
              Digital Library
            </Typography>
          </Box>

          {/* Navigation Links */}
          <Box display="flex" alignItems="center" gap={1} sx={{ flexGrow: 1 }}>
            <Button component={RouterLink} to="/catalog" color="inherit">
              Explore Books
            </Button>
            <Button component={RouterLink} to="/subscriptions" color="inherit">
              Subscription Plans
            </Button>
            {isAuthenticated && (
              <Button component={RouterLink} to="/my-library" color="inherit">
                My Library
              </Button>
            )}
            {isVendor && (
              <Button component={RouterLink} to="/vendor/dashboard" startIcon={<VendorIcon />} sx={{ color: 'secondary.main', fontWeight: 600 }}>
                Vendor Hub
              </Button>
            )}
            {isAdmin && (
              <Button component={RouterLink} to="/admin/dashboard" startIcon={<AdminIcon />} sx={{ color: 'accent.gold', fontWeight: 600 }}>
                Admin Portal
              </Button>
            )}
          </Box>

          {/* Right Action Icons */}
          <Box display="flex" alignItems="center" gap={2}>
            <Tooltip title={`Switch to ${mode === 'dark' ? 'Light' : 'Dark'} mode`}>
              <IconButton onClick={toggleTheme} color="inherit">
                {mode === 'dark' ? <LightIcon sx={{ color: 'accent.gold' }} /> : <DarkIcon />}
              </IconButton>
            </Tooltip>

            {isAuthenticated ? (
              <>
                <Chip
                  label={user?.role?.replace('ROLE_', '') || 'USER'}
                  size="small"
                  color={isAdmin ? 'warning' : isVendor ? 'secondary' : 'primary'}
                  variant="outlined"
                />
                <IconButton onClick={handleOpenMenu} color="inherit">
                  <Avatar sx={{ bgcolor: 'primary.main', width: 36, height: 36 }}>
                    {user?.fullName ? user.fullName[0].toUpperCase() : 'U'}
                  </Avatar>
                </IconButton>
                <Menu
                  anchorEl={anchorEl}
                  open={Boolean(anchorEl)}
                  onClose={handleCloseMenu}
                  PaperProps={{ sx: { borderRadius: 3, minWidth: 180, mt: 1 } }}
                >
                  <MenuItem onClick={() => { handleCloseMenu(); navigate('/dashboard'); }}>
                    <DashboardIcon sx={{ mr: 1.5, fontSize: 20 }} /> Dashboard
                  </MenuItem>
                  <MenuItem onClick={handleLogout}>
                    <LogoutIcon sx={{ mr: 1.5, fontSize: 20, color: 'error.main' }} /> Logout
                  </MenuItem>
                </Menu>
              </>
            ) : (
              <Box display="flex" gap={1}>
                <Button component={RouterLink} to="/login" variant="outlined" size="small">
                  Sign In
                </Button>
                <Button component={RouterLink} to="/register" variant="contained" size="small">
                  Get Started
                </Button>
              </Box>
            )}
          </Box>
        </Toolbar>
      </Container>
    </AppBar>
  );
};
