import UserComments from '@/features/admin/components/UserComments.jsx';

const USER_STATUS_LABELS = { ACTIVE: '정상', SUSPEND: '정지', WITHDRAWN: '탈퇴' };

export default function UserManagement({
                                           userNickname,
                                           onNicknameChange,
                                           onSearch,
                                           selectedUser,
                                           userMessage,
                                           userComments,
                                           commentsLoading,
                                           onSuspend,
                                           onRelease,
}){
    const getUserStatus = (target) => String(target?.userStatus ?? target?.status ?? '').toUpperCase();
    const selectedUserStatus = getUserStatus(selectedUser);
    const selectedUserWithdrawn = selectedUserStatus === 'WITHDRAWN';
    const selectedUserSuspended = selectedUserStatus === 'SUSPEND' || selectedUserStatus === 'SUSPENDED';

    return (
        <section className="admin-panel">
            <h2>회원 정보 조회</h2>
            <p className="panel-description">회원 ID 또는 닉네임으로 검색합니다.</p>
            <form className="user-search-form" onSubmit={onSearch}>
                <input value={userNickname}
                       onChange={(event) =>
                           onNicknameChange(event.target.value)}
                       placeholder="회원 ID 또는 닉네임 입력"
                       required />
                <button type="submit">조회</button>
            </form>
            {userMessage && <p className="admin-message">{userMessage}</p>}
            {selectedUser && (
                <div className="user-info-card">
                    <div><span>회원 ID</span><strong>{selectedUser.id}</strong></div>
                    <div><span>닉네임</span><strong>{selectedUser.nickname || '-'}</strong></div>
                    <div><span>이메일</span><strong>{selectedUser.email || '-'}</strong></div>
                    <div><span>권한</span><strong>{String(selectedUser.role || 'USER')}</strong></div>
                    <div><span>상태</span><strong>{
                        USER_STATUS_LABELS[selectedUserStatus] || selectedUserStatus || '-'}</strong></div>
                    <div className="user-actions">
                        {selectedUserWithdrawn && <p className="user-withdrawn-notice">이미 탈퇴한 회원은 정지하거나 해제할 수 없습니다.</p>}
                        <button type="button" disabled={selectedUserWithdrawn || selectedUserSuspended}
                                onClick={onSuspend}>회원 정지</button>
                        <button type="button" className="secondary" disabled={selectedUserWithdrawn || !selectedUserSuspended}
                                onClick={onRelease}>정지 해제</button>
                    </div>
                    <UserComments
                        comments={userComments}
                        loading={commentsLoading}
                    />
                </div>
            )}
        </section>
    )
}