import { useState } from 'react';
import Modal from '@/common/components/Modal.jsx';
import { setEventHidden, setEventLock, updateEvent } from '@/api/index.js';
import { EVENT_CODE_NAMES } from '@/features/events/utils/eventDisplay.js';
import './EventEditModal.css';

const toInputValue = (value) => (value ? String(value).slice(0, 16) : '');
const toRequestValue = (value) => (value ? `${value}:00` : null);

export default function EventEditModal({ event, onClose, onSaved }) {
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

    return (
        <Modal backdropClassName="event-edit-backdrop" onClose={onClose} locked={saving}>
            <form className="event-edit-modal" onSubmit={handleSubmit}>
                <div className="event-edit-heading">
                    <h2>이벤트 수정</h2>
                    <button type="button" onClick={onClose} disabled={saving}>×</button>
                </div>

                <label>
                    제목
                    <input value={form.title} onChange={setField('title')} required />
                </label>
                <label>
                    내용
                    <textarea rows={3} value={form.description} onChange={setField('description')} />
                </label>
                <label>
                    이벤트 URL
                    <input value={form.url} onChange={setField('url')} required />
                </label>
                <label>
                    이미지 URL
                    <input value={form.img} onChange={setField('img')} required />
                </label>
                <div className="event-edit-row">
                    <label>
                        시작일
                        <input type="datetime-local" value={form.startDate} onChange={setField('startDate')} required />
                    </label>
                    <label>
                        종료일
                        <input type="datetime-local" value={form.endDate} onChange={setField('endDate')} />
                    </label>
                </div>

                <div className="event-edit-codes">
                    <span>이벤트 종류</span>
                    <div>
                        {Object.entries(EVENT_CODE_NAMES).map(([code, name]) => (
                            <button
                                type="button"
                                key={code}
                                className={form.eventCodes.includes(code) ? 'selected' : ''}
                                aria-pressed={form.eventCodes.includes(code)}
                                onClick={() => toggleCode(code)}
                            >
                                {name}
                            </button>
                        ))}
                    </div>
                </div>

                <div className="event-edit-checks">
                    <label>
                        <input type="checkbox" checked={form.isActive} onChange={setCheck('isActive')} />
                        진행 중 (해제하면 종료 처리)
                    </label>
                    <label>
                        <input type="checkbox" checked={form.hidden} onChange={setCheck('hidden')} />
                        사용자 화면에서 숨기기
                    </label>
                    <label>
                        <input type="checkbox" checked={form.adminLocked} onChange={setCheck('adminLocked')} />
                        크롤러가 덮어쓰지 않도록 잠그기
                    </label>
                </div>

                {error && <p className="event-edit-error" role="alert">{error}</p>}

                <button className="event-edit-submit" type="submit" disabled={saving}>
                    {saving ? '저장 중…' : '저장'}
                </button>
            </form>
        </Modal>
    );
}
