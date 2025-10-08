import React, { useState, useEffect } from 'react';
import { files } from './api';

function FileViewer({ file, onClose }) {
  const [content, setContent] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadFileContent();
  }, [file]);

  const loadFileContent = async () => {
    try {
      const response = await files.download(file.fileId);

      if (file.type === 'kt') {
        const text = await response.text();
        setContent({ type: 'text', data: text });
      } else if (file.type === 'jpg' || file.type === 'jpeg') {
        const url = URL.createObjectURL(response);
        setContent({ type: 'image', data: url });
      } else {
        setContent({ type: 'unsupported', data: response });
      }

      setLoading(false);
    } catch (err) {
      console.error('Failed to load file content', err);
      setLoading(false);
    }
  };

  const handleDownload = () => {
    if (content && content.data) {
      const url = content.type === 'image'
        ? content.data
        : URL.createObjectURL(content.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = file.name;
      link.click();
    }
  };

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      right: 0,
      bottom: 0,
      background: 'rgba(0,0,0,0.7)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      zIndex: 1000
    }}>
      <div style={{
        background: 'white',
        padding: '20px',
        borderRadius: '5px',
        maxWidth: '800px',
        maxHeight: '80vh',
        overflow: 'auto',
        position: 'relative',
        width: '90%'
      }}>
        <button
          onClick={onClose}
          style={{
            position: 'absolute',
            top: '10px',
            right: '10px',
            padding: '5px 10px',
            background: '#dc3545',
            color: 'white',
            border: 'none',
            cursor: 'pointer',
            borderRadius: '3px'
          }}
        >
          Close
        </button>

        <h3>{file.name}</h3>

        {loading && <p>Loading...</p>}

        {!loading && content && content.type === 'text' && (
          <pre style={{
            background: '#f4f4f4',
            padding: '15px',
            borderRadius: '5px',
            overflow: 'auto',
            maxHeight: '500px',
            fontFamily: 'monospace'
          }}>
            {content.data}
          </pre>
        )}

        {!loading && content && content.type === 'image' && (
          <img
            src={content.data}
            alt={file.name}
            style={{ maxWidth: '100%', height: 'auto' }}
          />
        )}

        {!loading && content && content.type === 'unsupported' && (
          <div style={{ textAlign: 'center', padding: '40px' }}>
            <p style={{ marginBottom: '20px', color: '#666' }}>
              Preview not available for .{file.type} files
            </p>
            <button
              onClick={handleDownload}
              style={{
                padding: '10px 20px',
                background: '#007bff',
                color: 'white',
                border: 'none',
                cursor: 'pointer',
                borderRadius: '5px',
                fontSize: '16px'
              }}
            >
              Download File
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default FileViewer;