import { useState } from 'react';
import './FloatingButton.css';
import ReportForm from './ReportForm.jsx';

export default function FloatingButton() {
    const [open, setOpen] = useState(false);

    return (
        <>
            <button
                type="button"
                className="floating-report-btn"
                onClick={() => setOpen(true)}
            >
                ➕ 할인 제보하기
            </button>

            {open && (
                <div
                    className="report-modal-overlay"
                    onClick={() => setOpen(false)}
                >
                    <div
                        className="report-modal"
                        role="dialog"
                        aria-modal="true"
                        aria-label="제보하기"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <ReportForm onCancel={() => setOpen(false)} />
                    </div>
                </div>
            )}
        </>
    );
}