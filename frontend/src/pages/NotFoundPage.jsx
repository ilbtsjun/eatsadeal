import { Link } from 'react-router';
import Header from '../components/Header';
import { usePageTitle } from '../hooks/usePageTitle.jsx';
import './NotFoundPage.css';

export default function NotFoundPage() {
    usePageTitle('페이지를 찾을 수 없음');
    return (
        <div className="notfound-page">
            <Header />
            <main className="notfound-card">
                <span className="notfound-emoji" aria-hidden="true">🍗</span>
                <h1>페이지를 찾을 수 없어요</h1>
                <p>주소가 잘못되었거나 이동된 페이지입니다.</p>
                <Link to="/" className="notfound-button">메인으로 가기</Link>
            </main>
        </div>
    );
}