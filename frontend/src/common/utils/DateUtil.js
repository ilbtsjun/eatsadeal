export function calculateDDay(endDate) {
    if (!endDate)
        return '';
    const today = new Date();
    const end = new Date(endDate);
    today.setHours(0, 0, 0, 0);
    end.setHours(0, 0, 0, 0);
    const diffDays = Math.ceil((end.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
    if (diffDays < 0)
        return '종료';
    if (diffDays === 0)
        return 'D-Day';
    if (diffDays > 365)
        return '365+';
    return `D-${diffDays}`;
}

export function formatDate(value) {
    if (!value)
        return '정보 없음';
    const date = new Date(value);
    if (Number.isNaN(date.getTime()))
        return value;
    return date.toLocaleDateString('ko-KR');
}

export function formatDateTime(value) {
    if (!value)
        return '';
    const date = new Date(value);
    if (Number.isNaN(date.getTime()))
        return value;
    return date.toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' });
}