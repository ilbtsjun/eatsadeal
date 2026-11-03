import './StatusMessage.css';

export default function StatusMessage({ type = 'loading', title, message, onRetry, size = 'page' }) {
    const isError = type === 'error';

    return (
        <div className={`status-message status-message--${size}`} role={isError ? 'alert' : 'status'}>
            {isError
                ? <span className="status-icon" aria-hidden="true">😵</span>
                : <span className="status-spinner" aria-hidden="true" />}
            {title && <h2 className="status-title">{title}</h2>}
            {message && <p className="status-text">{message}</p>}
            {isError && onRetry && (
                <button type="button" className="status-retry" onClick={onRetry}>
                    다시 시도
                </button>
            )}
        </div>
    );
}
