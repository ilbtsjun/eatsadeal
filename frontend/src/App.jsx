import { Routes, Route } from 'react-router';
import LegacyApp from './LegacyApp.jsx'

export default function App() {
  return (
      <Routes>
        <Route path="*" element={<LegacyApp />} />
      </Routes>
  );
}