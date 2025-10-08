import React, { useState, useEffect } from 'react';
import Login from './Login';
import Register from './Register';
import FileList from './FileList';

function App() {
  const [user, setUser] = useState(null);
  const [showRegister, setShowRegister] = useState(false);

  useEffect(() => {
    const storedUsername = localStorage.getItem('username');
    const token = localStorage.getItem('token');
    if (storedUsername && token) {
      setUser(storedUsername);
    }
  }, []);

  const handleLogin = (username) => {
    setUser(username);
  };

  const handleRegister = (username) => {
    setUser(username);
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('username');
    setUser(null);
  };

  if (user) {
    return <FileList username={user} onLogout={handleLogout} />;
  }

  if (showRegister) {
    return (
      <Register 
        onRegister={handleRegister}
        onSwitchToLogin={() => setShowRegister(false)}
      />
    );
  }

  return (
    <Login 
      onLogin={handleLogin}
      onSwitchToRegister={() => setShowRegister(true)}
    />
  );
}

export default App;