import React, { useState, useEffect } from 'react';
import { files } from './api';
import FileViewer from './FileViewer';
import Sync from './Sync';

const isElectron = () => {
  return typeof window !== 'undefined' && window.electronAPI && window.electronAPI.isElectron;
};

function FileList({ username, onLogout }) {
  const [fileList, setFileList] = useState([]);
  const [sortOrder, setSortOrder] = useState(null);
  const [filterTypes, setFilterTypes] = useState([]);
  const [visibleColumns, setVisibleColumns] = useState({
    name: true,
    created: true,
    modified: true,
    uploader: true,
    editor: true,
  });
  const [selectedFile, setSelectedFile] = useState(null);
  const [dragging, setDragging] = useState(false);
  const [error, setError] = useState('');
  const [showSync, setShowSync] = useState(false);
  const [replacingFileId, setReplacingFileId] = useState(null);

  useEffect(() => {
    loadFiles();
  }, [sortOrder, filterTypes]);

  const loadFiles = async () => {
    try {
      const response = await files.list(sortOrder, filterTypes.length > 0 ? filterTypes : null);
      setFileList(response);
    } catch (err) {
      setError('Failed to load files');
    }
  };

  const handleFileUpload = async (file) => {
    try {
      await files.upload(file);
      loadFiles();
      setError('');
    } catch (err) {
      setError(err.response?.data || 'Upload failed');
    }
  };

  const handleFileReplace = async (fileId, file) => {
    try {
      await files.update(fileId, file);
      loadFiles();
      setReplacingFileId(null);
      setError('');
    } catch (err) {
      setError(err.response?.data || 'Update failed');
    }
  };

  const handleFileSelect = (e) => {
    const file = e.target.files[0];
    if (file) {
      if (replacingFileId) {
        handleFileReplace(replacingFileId, file);
      } else {
        handleFileUpload(file);
      }
    }
  };

  const triggerReplace = (fileId) => {
    setReplacingFileId(fileId);
    document.getElementById('file-input-hidden').click();
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragging(true);
  };

  const handleDragLeave = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.currentTarget === e.target) {
      setDragging(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragging(false);

    const file = e.dataTransfer.files[0];
    if (file) handleFileUpload(file);
  };

  const handleDelete = async (fileId) => {
    if (window.confirm('Delete this file?')) {
      try {
        await files.delete(fileId);
        loadFiles();
      } catch (err) {
        setError('Delete failed');
      }
    }
  };

  const handleDownload = async (file) => {
    try {
      const response = await files.download(file.fileId);
      const url = window.URL.createObjectURL(new Blob([response]));
      const link = document.createElement('a');
      link.href = url;
      link.download = file.name;
      link.click();
    } catch (err) {
      setError('Download failed');
    }
  };

  const toggleFilter = (type) => {
    setFilterTypes(prev =>
      prev.includes(type) ? prev.filter(t => t !== type) : [...prev, type]
    );
  };

  const formatDate = (dateStr) => {
    return new Date(dateStr).toLocaleString();
  };

  return (
    <div style={{ padding: '20px', position: 'relative' }}>
      {/* Hidden file input for replace functionality */}
      <input
        id="file-input-hidden"
        type="file"
        onChange={handleFileSelect}
        style={{ display: 'none' }}
      />

      {/* Fullscreen drag-drop overlay for Electron */}
      <input
        type="file"
        onChange={handleFileSelect}
        style={{
          position: 'fixed',
          top: 0,
          left: 0,
          width: '100vw',
          height: '100vh',
          opacity: 0,
          zIndex: dragging ? 9999 : -1,
          cursor: 'copy'
        }}
        onDragEnter={() => setDragging(true)}
        onDragLeave={() => setDragging(false)}
        onDrop={() => setDragging(false)}
      />

      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
        <h2>File Manager - {username}</h2>
        <div>
          {isElectron() && (
            <button
              onClick={() => setShowSync(true)}
              style={{
                padding: '8px 15px',
                marginRight: '10px',
                background: '#28a745',
                color: 'white',
                border: 'none',
                cursor: 'pointer',
                borderRadius: '3px'
              }}
            >
              Sync Folder
            </button>
          )}
          <button onClick={onLogout} style={{ padding: '8px 15px' }}>Logout</button>
        </div>
      </div>

      {error && <div style={{ color: 'red', marginBottom: '10px' }}>{error}</div>}

      <div
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        style={{
          border: dragging ? '3px dashed #007bff' : '2px dashed #ccc',
          padding: '30px',
          textAlign: 'center',
          marginBottom: '20px',
          background: dragging ? '#e7f3ff' : '#f9f9f9',
          borderRadius: '5px'
        }}
      >
        <p>Drag & Drop files here or</p>
        <input type="file" onChange={handleFileSelect} />
        <p style={{ fontSize: '12px', color: '#666', marginTop: '10px' }}>Any file type accepted</p>
      </div>

      <div style={{ marginBottom: '20px', display: 'flex', gap: '15px', alignItems: 'center' }}>
        <div>
          <label>Sort by Modified Date: </label>
          <button onClick={() => setSortOrder(true)} style={{ marginLeft: '5px', padding: '5px 10px', background: sortOrder === true ? '#007bff' : '#fff', color: sortOrder === true ? '#fff' : '#000' }}>Ascending</button>
          <button onClick={() => setSortOrder(false)} style={{ marginLeft: '5px', padding: '5px 10px', background: sortOrder === false ? '#007bff' : '#fff', color: sortOrder === false ? '#fff' : '#000' }}>Descending</button>
          <button onClick={() => setSortOrder(null)} style={{ marginLeft: '5px', padding: '5px 10px' }}>Clear</button>
        </div>

        <div>
          <label>Filter: </label>
          <button onClick={() => toggleFilter('kt')} style={{ marginLeft: '5px', padding: '5px 10px', background: filterTypes.includes('kt') ? '#28a745' : '#fff', color: filterTypes.includes('kt') ? '#fff' : '#000' }}>.kt</button>
          <button onClick={() => toggleFilter('jpg')} style={{ marginLeft: '5px', padding: '5px 10px', background: filterTypes.includes('jpg') ? '#28a745' : '#fff', color: filterTypes.includes('jpg') ? '#fff' : '#000' }}>.jpg</button>
        </div>
      </div>

      <div style={{ marginBottom: '10px' }}>
        <label>Toggle Columns: </label>
        {Object.keys(visibleColumns).filter(k => k !== 'name').map(col => (
          <label key={col} style={{ marginLeft: '10px' }}>
            <input
              type="checkbox"
              checked={visibleColumns[col]}
              onChange={() => setVisibleColumns(prev => ({ ...prev, [col]: !prev[col] }))}
            />
            {col}
          </label>
        ))}
      </div>

      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ background: '#f0f0f0' }}>
            <th style={{ border: '1px solid #ddd', padding: '10px' }}>Name</th>
            {visibleColumns.created && <th style={{ border: '1px solid #ddd', padding: '10px' }}>Created</th>}
            {visibleColumns.modified && <th style={{ border: '1px solid #ddd', padding: '10px' }}>Modified</th>}
            {visibleColumns.uploader && <th style={{ border: '1px solid #ddd', padding: '10px' }}>Uploader</th>}
            {visibleColumns.editor && <th style={{ border: '1px solid #ddd', padding: '10px' }}>Editor</th>}
            <th style={{ border: '1px solid #ddd', padding: '10px' }}>Actions</th>
          </tr>
        </thead>
        <tbody>
          {fileList.map(file => (
            <tr key={file.fileId}>
              <td style={{ border: '1px solid #ddd', padding: '10px' }}>{file.name}</td>
              {visibleColumns.created && <td style={{ border: '1px solid #ddd', padding: '10px' }}>{formatDate(file.createdDate)}</td>}
              {visibleColumns.modified && <td style={{ border: '1px solid #ddd', padding: '10px' }}>{formatDate(file.modifiedDate)}</td>}
              {visibleColumns.uploader && <td style={{ border: '1px solid #ddd', padding: '10px' }}>{file.uploaderName}</td>}
              {visibleColumns.editor && <td style={{ border: '1px solid #ddd', padding: '10px' }}>{file.editorName}</td>}
              <td style={{ border: '1px solid #ddd', padding: '10px' }}>
                {(file.type === 'kt' || file.type === 'jpg') && (
                  <button onClick={() => setSelectedFile(file)} style={{ marginRight: '5px', padding: '5px 10px' }}>View</button>
                )}
                <button onClick={() => handleDownload(file)} style={{ marginRight: '5px', padding: '5px 10px' }}>Download</button>
                <button onClick={() => triggerReplace(file.fileId)}>Replace</button>
                {file.uploaderName === username && (
                  <button onClick={() => handleDelete(file.fileId)}>Delete</button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {selectedFile && (
        <FileViewer file={selectedFile} onClose={() => setSelectedFile(null)} />
      )}

      {showSync && (
        <Sync
          onClose={() => setShowSync(false)}
          onSyncComplete={loadFiles}
        />
      )}
    </div>
  );
}

export default FileList;