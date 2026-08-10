import React, { useState, useEffect } from 'react';
import {
  Container,
  Box,
  Typography,
  Button,
  Paper,
  IconButton,
  Tooltip,
  LinearProgress,
  CircularProgress,
  AppBar,
  Toolbar,
} from '@mui/material';
import {
  ArrowBack as BackIcon,
  NavigateBefore as PrevIcon,
  NavigateNext as NextIcon,
  ZoomIn as ZoomInIcon,
  ZoomOut as ZoomOutIcon,
  Fullscreen as FullscreenIcon,
} from '@mui/icons-material';
import { useParams, useNavigate } from 'react-router-dom';
import api from '../api/axios';

export const BookReaderPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();

  const [book, setBook] = useState(null);
  const [accessUrl, setAccessUrl] = useState(null);
  const [loading, setLoading] = useState(true);
  const [currentPage, setCurrentPage] = useState(1);
  const [totalPages] = useState(100); // Default pagination scale for Reader
  const [zoom, setZoom] = useState(100);
  const [error, setError] = useState('');

  useEffect(() => {
    const initReader = async () => {
      setLoading(true);
      setError('');
      try {
        const bookRes = await api.get(`/books/${id}`);
        setBook(bookRes.data.data);

        // Fetch pre-signed access URL
        const urlRes = await api.get(`/books/${id}/access-url`).catch(() => null);
        if (urlRes?.data?.data) {
          setAccessUrl(urlRes.data.data);
        }
      } catch (err) {
        setError('Failed to load reader environment or document stream.');
      } finally {
        setLoading(false);
      }
    };

    initReader();
  }, [id]);

  // Sync reading progress to backend on page change
  const syncProgress = async (pageNumber) => {
    try {
      const progressPercent = Math.min(100, Math.round((pageNumber / totalPages) * 100));
      await api.post(`/books/${id}/progress`, null, {
        params: { lastPage: pageNumber, progress: progressPercent },
      });
    } catch (err) {
      console.error('Progress sync error:', err);
    }
  };

  const handleNextPage = () => {
    if (currentPage < totalPages) {
      const next = currentPage + 1;
      setCurrentPage(next);
      syncProgress(next);
    }
  };

  const handlePrevPage = () => {
    if (currentPage > 1) {
      const prev = currentPage - 1;
      setCurrentPage(prev);
      syncProgress(prev);
    }
  };

  if (loading) {
    return (
      <Box display="flex" flexDirection="column" alignItems="center" justifyContent="center" minHeight="80vh">
        <CircularProgress size={60} />
        <Typography variant="body1" color="text.secondary" mt={3}>
          Initializing Secure Stream & Access Grants...
        </Typography>
      </Box>
    );
  }

  if (error || !book) {
    return (
      <Container sx={{ py: 10, textAlign: 'center' }}>
        <Typography variant="h5" color="error" gutterBottom>
          {error || 'Unable to open reader.'}
        </Typography>
        <Button startIcon={<BackIcon />} onClick={() => navigate('/catalog')}>
          Return to Catalog
        </Button>
      </Container>
    );
  }

  const progressPercent = Math.round((currentPage / totalPages) * 100);

  return (
    <Box display="flex" flexDirection="column" height="100vh" bgcolor="background.default">
      {/* Reader Control Header */}
      <AppBar position="static" color="default" elevation={1}>
        <Toolbar>
          <IconButton edge="start" onClick={() => navigate(`/books/${id}`)}>
            <BackIcon />
          </IconButton>

          <Box ml={2} flexGrow={1}>
            <Typography variant="h6" fontWeight={700} noWrap>
              {book.title}
            </Typography>
            <Typography variant="caption" color="text.secondary">
              By {book.author || 'Author'} • Page {currentPage} of {totalPages} ({progressPercent}%)
            </Typography>
          </Box>

          <Box display="flex" alignItems="center" gap={1}>
            <Tooltip title="Zoom Out">
              <IconButton onClick={() => setZoom(Math.max(50, zoom - 10))}>
                <ZoomOutIcon />
              </IconButton>
            </Tooltip>
            <Typography variant="caption" fontWeight={700}>
              {zoom}%
            </Typography>
            <Tooltip title="Zoom In">
              <IconButton onClick={() => setZoom(Math.min(200, zoom + 10))}>
                <ZoomInIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="Toggle Fullscreen">
              <IconButton onClick={() => document.documentElement.requestFullscreen?.()}>
                <FullscreenIcon />
              </IconButton>
            </Tooltip>
          </Box>
        </Toolbar>
        <LinearProgress variant="determinate" value={progressPercent} />
      </AppBar>

      {/* Reader Main Document Canvas */}
      <Box
        flexGrow={1}
        display="flex"
        justifyContent="center"
        alignItems="center"
        p={4}
        sx={{ overflow: 'auto', bgcolor: 'grey.900' }}
      >
        <Paper
          elevation={8}
          sx={{
            width: `${Math.round(800 * (zoom / 100))}px`,
            minHeight: `${Math.round(1000 * (zoom / 100))}px`,
            p: 6,
            bgcolor: '#fafafa',
            color: '#1a1a1a',
            borderRadius: 2,
            transition: 'all 0.2s ease',
          }}
        >
          <Box borderBottom="2px solid #e0e0e0" pb={2} mb={4} display="flex" justifyContent="space-between">
            <Typography variant="overline" color="text.secondary">
              {book.title}
            </Typography>
            <Typography variant="overline" color="text.secondary">
              Section {currentPage}
            </Typography>
          </Box>

          <Typography variant="h4" fontWeight={700} gutterBottom sx={{ color: '#0f172a' }}>
            Chapter {currentPage}: {book.title} Overview
          </Typography>

          <Typography variant="body1" paragraph sx={{ fontSize: `${1.1 * (zoom / 100)}rem`, lineHeight: 1.9 }}>
            {book.previewText ||
              `Welcome to Section ${currentPage} of ${book.title}. Digital Library Streams content dynamically from S3 cloud storage using 48-hour pre-signed access URLs. As you navigate through chapters, your exact page position and completion percentages are automatically synchronized with your user account history.`}
          </Typography>

          <Typography variant="body1" paragraph sx={{ fontSize: `${1.1 * (zoom / 100)}rem`, lineHeight: 1.9 }}>
            This publication is published under the digital category <strong>{book.category}</strong>. You may adjust the text scaling dynamically using the toolbar controls above, or resume reading on any synchronized device.
          </Typography>
        </Paper>
      </Box>

      {/* Reader Page Navigation Footer Bar */}
      <Box p={2} bgcolor="background.paper" borderTop="1px solid rgba(255,255,255,0.1)" display="flex" justifyContent="center" alignItems="center" gap={3}>
        <Button
          variant="contained"
          startIcon={<PrevIcon />}
          disabled={currentPage === 1}
          onClick={handlePrevPage}
        >
          Previous Page
        </Button>

        <Typography variant="body2" fontWeight={700}>
          {currentPage} / {totalPages}
        </Typography>

        <Button
          variant="contained"
          endIcon={<NextIcon />}
          disabled={currentPage === totalPages}
          onClick={handleNextPage}
        >
          Next Page
        </Button>
      </Box>
    </Box>
  );
};

export default BookReaderPage;
