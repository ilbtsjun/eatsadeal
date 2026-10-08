import { Routes, Route } from 'react-router';
import LegacyApp from './LegacyApp.jsx'
import { LoginRoute, SignupRoute, LegalOverlay, EventDetailRoute } from './routes';

export default function App() {
  return (
      <Routes>
          <Route path="/login" element={<LoginRoute />} />
          <Route path="/signup" element={<SignupRoute />}>
              <Route path="terms" element={<LegalOverlay kind="terms" />} />
              <Route path="privacy" element={<LegalOverlay kind="privacy" />} />
          </Route>
          <Route path="/events/:eventId" element={<EventDetailRoute />} />
        <Route path="*" element={<LegacyApp />} />
      </Routes>
  );
}