import React, { useState } from 'react';
import { files } from './api';

const isElectron = () => {
  return typeof window !== 'undefined' && window.electronAPI && window.electronAPI.isElectron;
};

function Sync({ onClose, onSyncComplete }) {
  const [localFiles, setLocalFiles] = useState([]);
  const [syncResult, setSyncResult] = useState(null);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState('');
  const [syncFolderPath, setSyncFolderPath] = useState('');

  const handleFolderSelect = async (e) => {
    const selectedFiles = Array.from(e.target.files);
    const fileNames = selectedFiles.map(f => {
      const fullPath = f.webkitRelativePath || f.name;
      return fullPath.split('/').pop();
    });

    if (isElectron() && selectedFiles.length > 0) {
      const firstFile = selectedFiles[0];
      if (firstFile.path) {
        const folderPath = window.electronAPI.getFolderPath(firstFile.path);
        setSyncFolderPath(folderPath);
      }
    }

    setLocalFiles(selectedFiles);
    setError('');

    try {
      const response = await fetch('http://localhost:8081/api/sync/compare', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({ localFiles: fileNames })
      });

      if (!response.ok) throw new Error('Failed to compare files');

      const result = await response.json();
      setSyncResult(result);
    } catch (err) {
      setError('Failed to analyze sync status');
    }
  };

  const handleSync = async () => {
    if (!syncResult) return;

    setSyncing(true);
    setError('');

    try {
      let uploadedCount = 0;
      let skippedCount = 0;

      for (const fileName of syncResult.toUpload) {
        const originalFile = localFiles.find(f => {
          const name = (f.webkitRelativePath || f.name).split('/').pop();
          return name === fileName;
        });

        if (originalFile) {
          try {
            const cleanFile = new File([originalFile], fileName, { type: originalFile.type });
            await files.upload(cleanFile);
            uploadedCount++;
          } catch (err) {
            skippedCount++;
          }
        }
      }

      let downloadedCount = 0;

      if (isElectron() && syncFolderPath && syncResult.toDownload.length > 0) {
        try {
          const response = await fetch('http://localhost:8081/api/sync/remote-files', {
            headers: {
              'Authorization': `Bearer ${localStorage.getItem('token')}`
            }
          });

          if (!response.ok) throw new Error('Failed to get remote files');

          const userRemoteFiles = await response.json();

          for (const fileName of syncResult.toDownload) {
            const fileToDownload = userRemoteFiles.find(f => f.name === fileName);

            if (fileToDownload) {
              const blob = await files.download(fileToDownload.fileId);
              const buffer = await blob.arrayBuffer();
              const filePath = window.electronAPI.joinPath(syncFolderPath, fileName);

              const result = window.electronAPI.writeFile(filePath, buffer);
              if (result.success) {
                downloadedCount++;
              } else {
                throw new Error(result.error);
              }
            }
          }
        } catch (err) {
          setError('Failed to download files: ' + err.message);
        }
      }

      const message = isElectron()
        ? `Sync complete!\nUploaded: ${uploadedCount}\nDownloaded: ${downloadedCount}\nSkipped: ${skippedCount}`
        : `Sync complete!\nUploaded: ${uploadedCount}\nSkipped: ${skippedCount}\n\n${syncResult.toDownload.length} file(s) available to download (use Download button in file list)`;

      alert(message);
      if (onSyncComplete) onSyncComplete();
      onClose();
    } catch (err) {
      setError('Sync failed: ' + err.message);
    } finally {
      setSyncing(false);
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
        padding: '30px',
        borderRadius: '5px',
        maxWidth: '600px',
        width: '90%',
        maxHeight: '80vh',
        overflow: 'auto'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '20px' }}>
          <h2>Folder Synchronization</h2>
          <button
            onClick={onClose}
            style={{
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
        </div>

        {error && <div style={{ color: 'red', marginBottom: '15px' }}>{error}</div>}

        <div style={{ marginBottom: '20px' }}>
          <label style={{ display: 'block', marginBottom: '10px', fontWeight: 'bold' }}>
            Select Local Folder to Sync:
          </label>
          <input
            type="file"
            webkitdirectory=""
            directory=""
            multiple
            onChange={handleFolderSelect}
            style={{ marginBottom: '10px' }}
          />
          <p style={{ fontSize: '12px', color: '#666' }}>
            Select a folder containing files to synchronize (any file type)
          </p>
        </div>

        {syncResult && (
          <div style={{ marginTop: '20px' }}>
            <h3>Sync Analysis:</h3>

            <div style={{ marginTop: '15px' }}>
              <h4 style={{ color: '#28a745' }}>
                Files to Upload ({syncResult.toUpload.length}):
              </h4>
              {syncResult.toUpload.length > 0 ? (
                <ul style={{ maxHeight: '150px', overflow: 'auto' }}>
                  {syncResult.toUpload.map(file => (
                    <li key={file}>{file}</li>
                  ))}
                </ul>
              ) : (
                <p style={{ color: '#666', fontStyle: 'italic' }}>No files to upload</p>
              )}
            </div>

            <div style={{ marginTop: '15px' }}>
              <h4 style={{ color: '#007bff' }}>
                Files to Download ({syncResult.toDownload.length}):
              </h4>
              {syncResult.toDownload.length > 0 ? (
                <ul style={{ maxHeight: '150px', overflow: 'auto' }}>
                  {syncResult.toDownload.map(file => (
                    <li key={file}>{file}</li>
                  ))}
                </ul>
              ) : (
                <p style={{ color: '#666', fontStyle: 'italic' }}>No files to download</p>
              )}
            </div>

            <div style={{ marginTop: '20px' }}>
              <button
                onClick={handleSync}
                disabled={syncing || (syncResult.toUpload.length === 0 && syncResult.toDownload.length === 0)}
                style={{
                  width: '100%',
                  padding: '12px',
                  background: syncing ? '#ccc' : '#007bff',
                  color: 'white',
                  border: 'none',
                  cursor: syncing ? 'not-allowed' : 'pointer',
                  borderRadius: '5px',
                  fontSize: '16px',
                  fontWeight: 'bold'
                }}
              >
                {syncing ? 'Syncing...' : 'Start Synchronization'}
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default Sync;