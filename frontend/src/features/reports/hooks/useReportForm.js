import { useState } from 'react';
import { createReport } from '@/api/index.js';
import { usePending } from '@/common/hooks/usePending.js';

export function useReportForm({ onSubmitted }) {
    const [title, setTitle] = useState('');
    const [content, setContent] = useState('');
    const [submitting, withSubmitting] = usePending();
    const [error, setError] = useState('');
    const [done, setDone] = useState(false);

    const hasInput = Boolean(title.trim() || content.trim());

    const handleTitleChange = (event) => setTitle(event.target.value);
    const handleContentChange = (event) => setContent(event.target.value);

    const handleSubmit = async (event) => {
        event.preventDefault();
        const trimmedTitle = title.trim();
        const trimmedContent = content.trim();
        if (!trimmedTitle || !trimmedContent) {
            setError('제목과 내용을 모두 입력해주세요.');
            return;
        }

        await withSubmitting(async () => {
            setError('');
            try {
                await createReport({ title: trimmedTitle, content: trimmedContent });
                setDone(true);
                onSubmitted?.();
            } catch (requestError) {
                setError(requestError.message || '제보 전송에 실패했습니다. 잠시 후 다시 시도해주세요.');
            }
        });
    };

    return {
        title,
        content,
        submitting,
        error,
        done,
        hasInput,
        handleTitleChange,
        handleContentChange,
        handleSubmit,
    };
}
