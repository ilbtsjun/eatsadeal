import Header from '@/common/components/Header';
import UserProfile from '@/features/account/components/UserProfile.jsx';
import PasswordChange from '@/features/account/components/PasswordChange.jsx';
import UserComments from '@/features/account/components/UserComments.jsx';
import UserQuit from '@/features/account/components/UserQuit.jsx';
import './MyPage.css';

export default function MyPage({ user, onLogout, onBack, onOpenEvent, onUserUpdate }) {
  return (
      <div className="mypage">
        <Header />
        <main className="mypage-container">
          <button type="button" className="mypage-back" onClick={onBack}>← 메인으로</button>
          <h1>마이페이지</h1>
          <p className="mypage-intro">내 정보와 활동을 관리할 수 있습니다.</p>
          <UserProfile user={user} onUserUpdate={onUserUpdate}/>
          <PasswordChange />
          <UserComments user={user} onOpenEvent={onOpenEvent}/>
          <UserQuit onLogout={onLogout} onBack={onBack} />
        </main>
      </div>
  );
}