import './FloatingButton.css';

export default function FloatingButton() {
    const handleReportClick = () => {
        alert('할인 제보 페이지로 이동합니다! (추후 구현 예정)');
    };

    return (
        <button className="floating-report-btn" onClick={handleReportClick}>
            ➕ 할인 제보하기
        </button>
    );
}