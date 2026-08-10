import React, { useState, useEffect } from 'react';
import {
  Container,
  Grid,
  Card,
  Typography,
  Box,
  CircularProgress,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  Button,
  Alert,
} from '@mui/material';
import {
  People as UsersIcon,
  MenuBook as BooksIcon,
  Storefront as VendorIcon,
  MonetizationOn as RevenueIcon,
  Security as AuditIcon,
} from '@mui/icons-material';
import api from '../../api/axios';

export const AdminDashboardPage = () => {
  const [tabValue, setTabValue] = useState(0);
  const [loading, setLoading] = useState(true);
  const [vendorApplications, setVendorApplications] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [payments, setPayments] = useState([]);
  const [stats, setStats] = useState({
    totalVendors: 0,
    pendingApplications: 0,
    totalRevenue: 0,
  });

  useEffect(() => {
    const fetchAdminData = async () => {
      setLoading(true);
      try {
        const [vendorRes, auditRes, payRes] = await Promise.all([
          api.get('/vendors/pending').catch(() => null),
          api.get('/admin/audit-logs').catch(() => null),
          api.get('/admin/payments').catch(() => null),
        ]);

        const vendorList = vendorRes?.data?.data?.content || [];
        const auditList = auditRes?.data?.data?.content || [];
        const payList = payRes?.data?.data?.content || [];

        setVendorApplications(vendorList);
        setAuditLogs(auditList);
        setPayments(payList);

        let revenue = 0;
        payList.forEach((p) => {
          if (p.status === 'SUCCESS') revenue += p.amount || 0;
        });

        setStats({
          totalVendors: 0,
          pendingApplications: vendorList.length,
          totalRevenue: revenue.toFixed(2),
        });
      } catch (err) {
        console.error('Failed to load admin dashboard:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchAdminData();
  }, []);

  const handleVendorDecision = async (vendorId, action) => {
    try {
      await api.patch(`/admin/vendors/${vendorId}/${action}`);
      setVendorApplications((prev) => prev.filter((v) => v.id !== vendorId));
    } catch (err) {
      alert(`Failed to ${action} vendor application.`);
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
      <Box mb={5}>
        <Typography variant="h3" fontWeight={800} gutterBottom>
          Platform Administration Console
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Manage vendor applications, review platform revenue, and monitor security audit trails.
        </Typography>
      </Box>

      {/* Stats Cards */}
      <Grid container spacing={4} mb={6}>
        <Grid item xs={12} sm={4}>
          <Card variant="outlined" sx={{ p: 3, background: 'linear-gradient(135deg, rgba(245,158,11,0.1) 0%, rgba(245,158,11,0.02) 100%)' }}>
            <Box display="flex" alignItems="center" gap={2} mb={1}>
              <VendorIcon sx={{ fontSize: 36, color: '#f59e0b' }} />
              <Typography variant="h6" color="text.secondary">Pending Vendor Applications</Typography>
            </Box>
            <Typography variant="h3" fontWeight={800} sx={{ color: '#f59e0b' }}>
              {stats.pendingApplications}
            </Typography>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card variant="outlined" sx={{ p: 3, background: 'linear-gradient(135deg, rgba(16,185,129,0.1) 0%, rgba(16,185,129,0.02) 100%)' }}>
            <Box display="flex" alignItems="center" gap={2} mb={1}>
              <RevenueIcon sx={{ fontSize: 36, color: '#10b981' }} />
              <Typography variant="h6" color="text.secondary">Platform Revenue (Total)</Typography>
            </Box>
            <Typography variant="h3" fontWeight={800} sx={{ color: '#10b981' }}>
              ₹{stats.totalRevenue}
            </Typography>
          </Card>
        </Grid>
        <Grid item xs={12} sm={4}>
          <Card variant="outlined" sx={{ p: 3, background: 'linear-gradient(135deg, rgba(99,102,241,0.1) 0%, rgba(99,102,241,0.02) 100%)' }}>
            <Box display="flex" alignItems="center" gap={2} mb={1}>
              <AuditIcon color="primary" sx={{ fontSize: 36 }} />
              <Typography variant="h6" color="text.secondary">Recent Audit Events</Typography>
            </Box>
            <Typography variant="h3" fontWeight={800} color="primary.main">
              {auditLogs.length}
            </Typography>
          </Card>
        </Grid>
      </Grid>

      {/* Tabs */}
      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 4 }}>
        <Tabs value={tabValue} onChange={(e, v) => setTabValue(v)}>
          <Tab icon={<VendorIcon />} iconPosition="start" label="Vendor Applications" />
          <Tab icon={<AuditIcon />} iconPosition="start" label="Security Audit Logs" />
          <Tab icon={<RevenueIcon />} iconPosition="start" label="Payment Ledger" />
        </Tabs>
      </Box>

      {/* Tab 0 — Vendor Applications */}
      {tabValue === 0 && (
        <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 3 }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Business Name</TableCell>
                <TableCell>Contact Person</TableCell>
                <TableCell>Business Type</TableCell>
                <TableCell>Applied At</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {vendorApplications.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    No pending vendor applications.
                  </TableCell>
                </TableRow>
              ) : (
                vendorApplications.map((v) => (
                  <TableRow key={v.id}>
                    <TableCell><strong>{v.businessName}</strong></TableCell>
                    <TableCell>{v.contactPerson || v.userName}</TableCell>
                    <TableCell>{v.businessType || 'Publisher'}</TableCell>
                    <TableCell>{new Date(v.appliedAt || v.createdAt).toLocaleDateString()}</TableCell>
                    <TableCell>
                      <Chip label={v.status || 'PENDING'} color="warning" size="small" />
                    </TableCell>
                    <TableCell align="right">
                      <Box display="flex" gap={1} justifyContent="flex-end">
                        <Button
                          variant="contained"
                          color="success"
                          size="small"
                          onClick={() => handleVendorDecision(v.id, 'approve')}
                        >
                          Approve
                        </Button>
                        <Button
                          variant="outlined"
                          color="error"
                          size="small"
                          onClick={() => handleVendorDecision(v.id, 'reject')}
                        >
                          Reject
                        </Button>
                      </Box>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Tab 1 — Audit Logs */}
      {tabValue === 1 && (
        <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 3 }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Action</TableCell>
                <TableCell>User</TableCell>
                <TableCell>Entity</TableCell>
                <TableCell>IP Address</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Timestamp</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {auditLogs.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    No audit events recorded yet.
                  </TableCell>
                </TableRow>
              ) : (
                auditLogs.map((log) => (
                  <TableRow key={log.id}>
                    <TableCell><code>{log.action}</code></TableCell>
                    <TableCell>{log.username || 'Anonymous'}</TableCell>
                    <TableCell>{log.entity || '—'}</TableCell>
                    <TableCell><code>{log.ipAddress}</code></TableCell>
                    <TableCell>
                      <Chip
                        label={log.status}
                        color={log.status === 'SUCCESS' ? 'success' : log.status === 'FAILURE' ? 'error' : 'warning'}
                        size="small"
                      />
                    </TableCell>
                    <TableCell>{new Date(log.createdAt).toLocaleString()}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Tab 2 — Payment Ledger */}
      {tabValue === 2 && (
        <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 3 }}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Transaction ID</TableCell>
                <TableCell>Order Number</TableCell>
                <TableCell>User Email</TableCell>
                <TableCell>Gateway</TableCell>
                <TableCell>Amount</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Date</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {payments.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center">
                    No payment records found.
                  </TableCell>
                </TableRow>
              ) : (
                payments.map((p) => (
                  <TableRow key={p.id}>
                    <TableCell><code>{p.transactionId}</code></TableCell>
                    <TableCell>{p.orderNumber || 'N/A'}</TableCell>
                    <TableCell>{p.userEmail || '—'}</TableCell>
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

export default AdminDashboardPage;
