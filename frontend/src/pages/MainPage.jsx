import Header from '../components/Header';
import CategoryFilter from '../components/CategoryFilter';
import HighlightBanner from '../components/HighlightBanner';
import EventCardList from '../components/EventCardList'; // 카드 리스트 컴포넌트
import FloatingButton from '../components/FloatingButton';

export default function MainPage() {
    return (
        <div className="main-page">
            {/* 1. 헤더 */}
            <Header />

            {/*/!* 2. 카테고리와 필터바 *!/*/}
            <CategoryFilter />

            {/*/!* 3. 하이라이트 배너 *!/*/}
            <HighlightBanner />

            {/*/!* 4. 이벤트 카드 리스트 *!/*/}
            <EventCardList />

            {/*/!* 5. 추가제보 버튼 (우측 하단 고정 형태 등) *!/*/}
            <FloatingButton />
        </div>
    );
}