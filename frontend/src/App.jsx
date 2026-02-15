import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import './App.css';

// Check if user is logged in
const PrivateRoute = ({ children }) => {
  const token = localStorage.getItem('token'); // Check for token in localStorage
  return token ? children : <Navigate to="/login" />; // If no token, redirect to login
};

function App() { 
  return (
    <BrowserRouter> 
      <Routes>
        <Route path="/login" element={<Login />} /> 
        <Route path="/register" element={<Register />} />
        <Route
          path="/dashboard"
          element={
            <PrivateRoute>
              <Dashboard />
            </PrivateRoute>
          }
        />
        <Route path="/" element={<Navigate to="/dashboard" />} /> 
      </Routes>
    </BrowserRouter>
  );
}

export default App;
