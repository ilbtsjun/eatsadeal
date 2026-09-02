import './AdminTools.css';

export default function AdminTools({ isManaging, onToggleManage, onOpenAdminPage }) {
  return (
    <div className="admin-tools" aria-label="관리자 기능">
      <button
        type="button"
        className={`admin-action-button ${isManaging ? 'active' : ''}`}
        onClick={onOpenAdminPage || onToggleManage}
      >
        관리자 페이지
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
