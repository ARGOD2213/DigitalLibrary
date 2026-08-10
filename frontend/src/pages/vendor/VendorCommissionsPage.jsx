import React, { useState, useEffect } from 'react';
import {
  Container,
  Typography,
  Box,
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
import api from '../../api/axios';

export const VendorCommissionsPage = () => {
  const [commissions, setCommissions] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchCommissions = async () => {
      setLoading(true);
      try {
        const res = await api.get('/vendors/me/commissions');
        setCommissions(res.data.data.content || []);
      } catch (err) {
        console.error('Failed to load commissions:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchCommissions();
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
      <Box mb={5}>
        <Typography variant="h3" fontWeight={800} gutterBottom>
          Vendor Revenue & Commission Split Ledger
        </Typography>
        <Typography variant="body1" color="text.secondary">
          Track sales transaction breakdowns, platform fees, and net vendor payouts automatically calculated by CommissionService.
        </Typography>
      </Box>

      <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 3 }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Commission ID</TableCell>
              <TableCell>Book Title</TableCell>
              <TableCell>Sale Price</TableCell>
              <TableCell>Platform Fee Rate</TableCell>
              <TableCell>Platform Cut</TableCell>
              <TableCell>Net Vendor Earning</TableCell>
              <TableCell>Payout Status</TableCell>
              <TableCell>Sale Date</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {commissions.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} align="center">
                  No commission earnings recorded yet.
                </TableCell>
              </TableRow>
            ) : (
              commissions.map((c) => (
                <TableRow key={c.id}>
                  <TableCell><code>#COMM-{c.id}</code></TableCell>
                  <TableCell><strong>{c.bookTitle || 'Publication'}</strong></TableCell>
                  <TableCell>₹{c.salePrice}</TableCell>
                  <TableCell>{c.commissionRate}%</TableCell>
                  <TableCell style={{ color: '#ef4444' }}>-₹{c.platformCut}</TableCell>
                  <TableCell style={{ color: '#10b981', fontWeight: 700 }}>+₹{c.vendorEarning}</TableCell>
                  <TableCell>
                    <Chip label={c.payoutStatus || 'PENDING'} color={c.payoutStatus === 'PAID' ? 'success' : 'warning'} size="small" />
                  </TableCell>
                  <TableCell>{new Date(c.createdAt).toLocaleDateString()}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Container>
  );
};

export default VendorCommissionsPage;
