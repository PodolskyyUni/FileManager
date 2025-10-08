const { contextBridge } = require('electron');

contextBridge.exposeInMainWorld('electronAPI', {
  isElectron: true,

  writeFile: (filePath, data) => {
    const fs = require('fs');
    try {
      const uint8Array = new Uint8Array(data);
      fs.writeFileSync(filePath, uint8Array);
      return { success: true };
    } catch (error) {
      return { success: false, error: error.message };
    }
  },

  joinPath: (...paths) => {
    const path = require('path');
    return path.join(...paths);
  },

  getFolderPath: (filePath) => {
    const lastBackslash = filePath.lastIndexOf('\\');
    const lastSlash = filePath.lastIndexOf('/');
    const lastIndex = Math.max(lastBackslash, lastSlash);
    return filePath.substring(0, lastIndex);
  }
});