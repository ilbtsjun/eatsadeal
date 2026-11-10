import { useState } from 'react';
import { useReportForm } from '../hooks/useReportForm.js';
import './ReportForm.css';

const TITLE_MAX = 100;
const CONTENT_MAX = 2000;

export default function ReportForm({ onCancel, onSubmitted }) {
    const {
        title,
        content,
        submitting,
        error,
        done,
        hasInput,
        handleTitleChange,
        handleContentChange,
        handleSubmit,
    } = useReportForm({ onSubmitted });

    const [confirmingCancel, setConfirmingCancel] = useState(false);

    const handleCancel = () => {
        if (hasInput) {
            setConfirmingCancel(true);
            return;
        }
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
                onChange={handleTitleChange}
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
                onChange={handleContentChange}
                disabled={submitting}
            />
            <span className="report-counter">{content.length} / {CONTENT_MAX}</span>

            {error && <p className="report-error" role="alert">{error}</p>}

            {confirmingCancel ? (
                <div className="report-cancel-confirm" role="alert">
                    <p>작성 중인 내용이 사라집니다. 취소할까요?</p>
                    <div className="report-actions">
                        <button type="button" className="report-btn" onClick={() => setConfirmingCancel(false)}>
                            계속 작성
                        </button>
                        <button type="button" className="report-btn report-btn-primary" onClick={() => onCancel?.()}>
                            취소하기
                        </button>
                    </div>
                </div>
            ) : (
                <div className="report-actions">
                    <button type="submit" className="report-btn report-btn-primary" disabled={submitting}>
                        {submitting ? '보내는 중...' : '보내기'}
                    </button>
                    <button type="button" className="report-btn" onClick={handleCancel} disabled={submitting}>
                        취소
                    </button>
                </div>
            )}
        </form>
    );
}