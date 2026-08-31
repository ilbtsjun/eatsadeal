import './AdminTools.css';

export default function AdminTools({ isManaging, onToggleManage }) {
  return (
    <div className="admin-tools" aria-label="관리자 기능">
      <button
        type="button"
        className={`admin-action-button ${isManaging ? 'active' : ''}`}
        onClick={onToggleManage}
      >
        {isManaging ? '관리 닫기' : '관리'}
      </button>
      <button
        type="button"
        className="admin-action-button create-button"
        onClick={() => {}}
      >
        생성
      </button>
    </div>
  );
}
