import { Routes, Route } from 'react-router';
import LoginRoute from './routes/LoginRoute.jsx';
import SignupRoute from './routes/SignupRoute.jsx';
import LegalOverlay from './routes/LegalOverlay.jsx';
import EventDetailRoute from './routes/EventDetailRoute.jsx';
import LegalRoute from './routes/LegalRoute.jsx'
import RequireAuth from './routes/RequireAuth.jsx'
import { MyPageRoute, FavoritesRoute, AdminRoute } from './routes/AccountRoute.jsx'
import MainRoute from './routes/MainRoute.jsx'
import NotFoundPage from './pages/NotFoundPage.jsx'

export default function App() {
  return (
      <Routes>
          <Route path="/" element={<MainRoute />} />
          <Route path="/login" element={<LoginRoute />} />
          <Route path="/signup" element={<SignupRoute />}>
              <Route path="terms" element={<LegalOverlay kind="terms" />} />
              <Route path="privacy" element={<LegalOverlay kind="privacy" />} />
          </Route>
          <Route path="/events/:eventId" element={<EventDetailRoute />} />
          <Route path="/terms" element={<LegalRoute kind="terms" />} />
          <Route path="/privacy" element={<LegalRoute kind="privacy" />} />

          <Route element={<RequireAuth />}>
              <Route path="/mypage" element={<MyPageRoute />} />
              <Route path="/favorites" element={<FavoritesRoute />} />
              <Route path="/admin" element={<AdminRoute />} />
          </Route>

        <Route path="*" element={<NotFoundPage />} />
      </Routes>
  );
}