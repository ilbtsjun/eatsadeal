import { useProfile } from '../hooks/useProfile.js';

export default function Profile({ user, onUserUpdate }) {
    const {
        profile,
        profileForm,
        profileLoading,
        profileMessage,
        profileError,
        editingProfile,
        updateForm,
        startEditingProfile,
        cancelEditingProfile,
        updateProfile,
    } = useProfile({ user, onUserUpdate });

    if (profileLoading) {
        return (
            <section className="mypage-panel">
                <h2>내 정보</h2>
                <p role="status">회원정보를 불러오는 중입니다.</p>
            </section>
        );
    }

    return (
        <section className="mypage-panel">
            <h2>내 정보</h2>

            {profileError && (
                <p className="mypage-error" role="alert">{profileError}</p>
            )}
            {profileMessage && (
                <p className="mypage-message" role="status">{profileMessage}</p>
            )}

            {!editingProfile ? (
                <>
                    <dl className="profile-info-list">
                        <div className="profile-info-item">
                            <dt>닉네임</dt>
                            <dd>{profile.nickname || '-'}</dd>
                        </div>
                        <div className="profile-info-item">
                            <dt>이름</dt>
                            <dd>{profile.name || '-'}</dd>
                        </div>
                        <div className="profile-info-item">
                            <dt>전화번호</dt>
                            <dd>{profile.phoneNumber || '-'}</dd>
                        </div>
                        <div className="profile-info-item">
                            <dt>생년월일</dt>
                            <dd>{profile.birth || '-'}</dd>
                        </div>
                    </dl>
                    <div className="profile-actions">
                        <button type="button" onClick={startEditingProfile}>
                            정보 수정
                        </button>
                    </div>
                </>
            ) : (
                <form className="mypage-form" onSubmit={updateProfile}>
                    <label>
                        닉네임
                        <input
                            name="nickname"
                            value={profileForm.nickname}
                            onChange={updateForm}
                            required
                        />
                    </label>
                    <label>
                        이름
                        <input
                            name="name"
                            value={profileForm.name}
                            onChange={updateForm}
                            required
                        />
                    </label>
                    <label>
                        전화번호
                        <input
                            name="phoneNumber"
                            value={profileForm.phoneNumber}
                            onChange={updateForm}
                            required
                        />
                    </label>
                    <label>
                        생년월일
                        <input
                            type="date"
                            name="birth"
                            value={profileForm.birth}
                            onChange={updateForm}
                        />
                    </label>

                    <div className="mypage-form-actions">
                        <button type="submit">완료</button>
                        <button
                            type="button"
                            className="secondary"
                            onClick={cancelEditingProfile}
                        >
                            취소
                        </button>
                    </div>
                </form>
            )}
        </section>
    );
}