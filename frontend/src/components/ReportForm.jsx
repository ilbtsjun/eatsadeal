import { useState } from 'react';
import { apiRequest } from '../api/client';
import './ReportForm.css';

const TITLE_MAX = 100;
const CONTENT_MAX = 2000;

export default function ReportForm({ onCancel, onSubmitted }) {
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [error, setError] = useState('');
    const [done, setDone] = useState(false);

    const handleSubmit = async (event) => {
        event.preventDefault();
        if (submitting) return;

        const trimmedTitle = title.trim();
        const trimmedContent = content.trim();
        if (!trimmedTitle || !trimmedContent) {
            setError('제목과 내용을 모두 입력해주세요.');
            return;
        }

        setSubmitting(true);
        setError('');
        try {
            // 백엔드: POST /api/reports  (body: { title, content })
            await apiRequest('/api/reports', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ title: trimmedTitle, content: trimmedContent }),
            });
            setDone(true);
            onSubmitted?.();
        } catch (requestError) {
            setError(requestError.message || '제보 전송에 실패했습니다. 잠시 후 다시 시도해주세요.');
        } finally {
            setSubmitting(false);
        }
    };

    const handleCancel = () => {
        const hasInput = title.trim() || content.trim();
        if (hasInput && !window.confirm('작성 중인 내용이 사라집니다. 취소할까요?')) return;
        onCancel?.();
    };

    if (done) {
        return (
            <div className="report-container">
                <h2 className="report-heading">제보</h2>
                <p className="report-success">제보가 접수되었습니다. 소중한 의견 감사합니다!</p>
                <div className="report-actions">
                    <button type="button" className="report-btn report-btn-primary" onClick={() => onCancel?.()}>
                        닫기
                    </button>
                </div>
            </div>
        );
    }

    return (
        <form className="report-container" onSubmit={handleSubmit}>
            <h2 className="report-heading">제보</h2>

            <label className="report-label" htmlFor="report-title">제목</label>
            <input
                id="report-title"
                className="report-input"
                type="text"
                value={title}
                maxLength={TITLE_MAX}
                placeholder="제보 제목을 입력해주세요"
                onChange={(event) => setTitle(event.target.value)}
                disabled={submitting}
            />

            <label className="report-label" htmlFor="report-content">내용</label>
            <textarea
                id="report-content"
                className="report-textarea"
                value={content}
                maxLength={CONTENT_MAX}
                rows={8}
                placeholder="새로운 이벤트 정보, 잘못된 정보, 건의사항 등을 자유롭게 적어주세요"
                onChange={(event) => setContent(event.target.value)}
                disabled={submitting}
            />
            <span className="report-counter">{content.length} / {CONTENT_MAX}</span>

            {error && <p className="report-error" role="alert">{error}</p>}

            <div className="report-actions">
                <button type="submit" className="report-btn report-btn-primary" disabled={submitting}>
                    {submitting ? '보내는 중...' : '보내기'}
                </button>
                <button type="button" className="report-btn" onClick={handleCancel} disabled={submitting}>
                    취소
                </button>
            </div>
        </form>
    );
}