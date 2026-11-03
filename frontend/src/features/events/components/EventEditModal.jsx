import Modal from '@/common/components/Modal.jsx';
import { EVENT_CODE_NAMES } from '@/features/events/utils/eventDisplay.js';
import { useEventEditForm } from '@/features/events/hooks/useEventEditForm.js';
import './EventEditModal.css';

export default function EventEditModal({ event, onClose, onSaved }) {
    const { form, saving, error, setField, setCheck, toggleCode, handleSubmit } = useEventEditForm({ event, onSaved });

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
