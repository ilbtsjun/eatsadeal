import { useState } from 'react';
import { setEventHidden, setEventLock, updateEvent } from '@/api/index.js';

const toInputValue = (value) => (value ? String(value).slice(0, 16) : '');
const toRequestValue = (value) => (value ? `${value}:00` : null);

export function useEventEditForm({ event, onSaved }) {
    const [form, setForm] = useState({
        title: event.title || '',
        description: event.description || '',
        url: event.url || '',
        img: event.img || '',
        startDate: toInputValue(event.startDate),
        endDate: toInputValue(event.endDate),
        isActive: Boolean(event.isActive),
        eventCodes: event.eventCodes ? [...event.eventCodes] : [],
        hidden: Boolean(event.hidden),
        adminLocked: true,
    });
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');

    const setField = (name) => (e) =>
        setForm((current) => ({ ...current, [name]: e.target.value }));
    const setCheck = (name) => (e) =>
        setForm((current) => ({ ...current, [name]: e.target.checked }));
    const toggleCode = (code) =>
        setForm((current) => ({
            ...current,
            eventCodes: current.eventCodes.includes(code)
                ? current.eventCodes.filter((item) => item !== code)
                : [...current.eventCodes, code],
        }));

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (saving) return;
        setSaving(true);
        setError('');
        try {
            await updateEvent(event.id, {
                title: form.title.trim(),
                description: form.description,
                url: form.url.trim(),
                img: form.img.trim(),
                startDate: toRequestValue(form.startDate),
                endDate: toRequestValue(form.endDate),
                isActive: form.isActive,
                eventCodes: form.eventCodes,
            });
            if (form.hidden !== Boolean(event.hidden)) {
                await setEventHidden(event.id, form.hidden);
            }
            if (!form.adminLocked) {
                await setEventLock(event.id, false);
            }
            onSaved();
        } catch (err) {
            setError(err.message || '이벤트를 수정하지 못했습니다.');
            setSaving(false);
        }
    };

    return { form, saving, error, setField, setCheck, toggleCode, handleSubmit };
}
