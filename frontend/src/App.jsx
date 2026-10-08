import { Routes, Route } from 'react-router';
import LegacyApp from './LegacyApp.jsx'
import LoginRoute from './routes/LoginRoute.jsx';
import SignupRoute from './routes/SignupRoute.jsx';
import LegalOverlay from './routes/LegalOverlay.jsx';

export default function App() {
  return (
      <Routes>
          <Route path="/login" element={<LoginRoute />} />
          <Route path="/signup" element={<SignupRoute />}>
              <Route path="terms" element={<LegalOverlay kind="terms" />} />
              <Route path="privacy" element={<LegalOverlay kind="privacy" />} />
          </Route>
        <Route path="*" element={<LegacyApp />} />
      </Routes>
  );
}