export const COMMENT_MAX = 300;

export default function CommentForm({
                                        className,
                                        value,
                                        onChange,
                                        onSubmit,
                                        placeholder,
                                        submitLabel,
                                        submitting,
                                        onCancel,
                                        autoFocus = false,
                                    }) {
    const submitButton = <button type="submit" disabled={submitting}>{submitLabel}</button>;

    return (
        <form className={className} onSubmit={onSubmit}>
            <textarea
                value={value}
                onChange={(event) => onChange(event.target.value)}
                placeholder={placeholder}
                maxLength={COMMENT_MAX}
                required
                autoFocus={autoFocus}
            />
            <div className="comment-form-footer">
                <span>{value.length}/{COMMENT_MAX}</span>
                {onCancel ? (
                    <div className="comment-inline-buttons">
                        <button type="button" className="comment-action-button" onClick={onCancel}>
                            취소
                        </button>
                        {submitButton}
                    </div>
                ) : submitButton}
            </div>
        </form>
    );
}
