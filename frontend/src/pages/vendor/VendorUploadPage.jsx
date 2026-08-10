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
  FormControlLabel,
  Switch,
} from '@mui/material';
import { CloudUpload as UploadIcon, FolderZip as ZipIcon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';
import api from '../../api/axios';

export const VendorUploadPage = () => {
  const navigate = useNavigate();
  const [isZipUpload, setIsZipUpload] = useState(false);
  const [file, setFile] = useState(null);
  const [formData, setFormData] = useState({
    title: '',
    author: '',
    category: '',
    isbn: '',
    price: 0,
    free: false,
    description: '',
    previewText: '',
    publisher: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleChange = (e) => {
    const value = e.target.type === 'checkbox' ? e.target.checked : e.target.value;
    setFormData({ ...formData, [e.target.name]: value });
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!file) {
      setError('Please select a file to upload.');
      return;
    }

    setLoading(true);
    setError('');
    setSuccess('');

    try {
      const data = new FormData();
      data.append('file', file);

      if (isZipUpload) {
        // Zip Bundle Upload Endpoint
        const res = await api.post('/partner/upload-zip', data, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });
        setSuccess(`ZIP Bundle extracted successfully! Created book ID: ${res.data.data.id}`);
      } else {
        // Single Book Upload with metadata
        Object.keys(formData).forEach((key) => {
          data.append(key, formData[key]);
        });

        const res = await api.post('/partner/upload', data, {
          headers: { 'Content-Type': 'multipart/form-data' },
        });
        setSuccess(`Book uploaded successfully! ID: ${res.data.data.id}`);
      }

      setTimeout(() => navigate('/vendor/catalog'), 2000);
    } catch (err) {
      setError(err.response?.data?.message || 'Upload failed. Check file format and metadata.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="md" sx={{ py: 6 }}>
      <Card variant="outlined" sx={{ p: 2 }}>
        <CardContent>
          <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
            <Box>
              <Typography variant="h4" fontWeight={800} gutterBottom>
                Upload Digital Publication
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Upload single ebooks (PDF/EPUB) or structured ZIP bundles containing metadata.json.
              </Typography>
            </Box>
            <FormControlLabel
              control={
                <Switch
                  checked={isZipUpload}
                  onChange={(e) => setIsZipUpload(e.target.checked)}
                  color="primary"
                />
              }
              label={
                <Typography variant="subtitle2" fontWeight={700}>
                  {isZipUpload ? 'ZIP Bundle Mode' : 'Single Book Mode'}
                </Typography>
              }
            />
          </Box>

          {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}
          {success && <Alert severity="success" sx={{ mb: 3 }}>{success}</Alert>}

          <Box component="form" onSubmit={handleSubmit} display="flex" flexDirection="column" gap={3}>
            {/* File Drop Area */}
            <Box
              p={4}
              textAlign="center"
              sx={{
                border: '2px dashed #6366f1',
                borderRadius: 3,
                bgcolor: 'rgba(99, 102, 241, 0.04)',
                cursor: 'pointer',
              }}
              onClick={() => document.getElementById('book-file-input').click()}
            >
              <input
                id="book-file-input"
                type="file"
                hidden
                accept={isZipUpload ? '.zip' : '.pdf,.epub,.txt'}
                onChange={handleFileChange}
              />
              {isZipUpload ? <ZipIcon color="primary" sx={{ fontSize: 50 }} /> : <UploadIcon color="primary" sx={{ fontSize: 50 }} />}
              <Typography variant="h6" fontWeight={700} mt={1}>
                {file ? file.name : `Click to select ${isZipUpload ? 'ZIP Bundle' : 'Book File (PDF/EPUB)'}`}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                {isZipUpload
                  ? 'ZIP must contain document file, cover.jpg, and metadata.json'
                  : 'Files are securely ingested into AWS S3'}
              </Typography>
            </Box>

            {/* Metadata Fields (Only shown for Single Book Mode) */}
            {!isZipUpload && (
              <>
                <Grid container spacing={2}>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Book Title"
                      name="title"
                      required
                      fullWidth
                      value={formData.title}
                      onChange={handleChange}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Author Name"
                      name="author"
                      required
                      fullWidth
                      value={formData.author}
                      onChange={handleChange}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Category"
                      name="category"
                      required
                      fullWidth
                      placeholder="Technology, Fiction, History..."
                      value={formData.category}
                      onChange={handleChange}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="ISBN"
                      name="isbn"
                      fullWidth
                      value={formData.isbn}
                      onChange={handleChange}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6}>
                    <TextField
                      label="Price (₹)"
                      name="price"
                      type="number"
                      fullWidth
                      disabled={formData.free}
                      value={formData.price}
                      onChange={handleChange}
                    />
                  </Grid>
                  <Grid item xs={12} sm={6} display="flex" alignItems="center">
                    <FormControlLabel
                      control={
                        <Switch
                          name="free"
                          checked={formData.free}
                          onChange={handleChange}
                          color="success"
                        />
                      }
                      label="Mark as Free Access"
                    />
                  </Grid>
                </Grid>

                <TextField
                  label="Description"
                  name="description"
                  multiline
                  rows={3}
                  fullWidth
                  value={formData.description}
                  onChange={handleChange}
                />
              </>
            )}

            <Button
              type="submit"
              variant="contained"
              size="large"
              disabled={loading}
              sx={{ py: 1.6, fontSize: '1rem', fontWeight: 700 }}
            >
              {loading ? 'Processing & Ingesting...' : isZipUpload ? 'Extract & Ingest ZIP Bundle' : 'Publish Book'}
            </Button>
          </Box>
        </CardContent>
      </Card>
    </Container>
  );
};

export default VendorUploadPage;
