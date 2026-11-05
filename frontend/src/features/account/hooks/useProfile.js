import { useMemo, useState } from "react";
import { getCurrentUser, updateCurrentUser, } from '@/api/index.js';
import { useFetch } from '@/common/hooks/useFetch.js';

const toProfileForm = (profile = {}) => ({
    nickname: profile.nickname || '',
    name: profile.name || '',
    phoneNumber: profile.phoneNumber || '',
    birth: profile.birth || '',
});

export function useProfile({user, onUserUpdate}){
    const request = useMemo(() => (user ? () => getCurrentUser() : null), [user]);
    const {
        data: profile,
        setData: setProfile,
        loading: profileLoading,
        loadError,
        reload,
    } = useFetch(request, { initialData: user || {}, fallbackMessage: '회원정보를 불러오지 못했습니다.' });
    const [profileForm, setProfileForm] = useState(() => toProfileForm(profile));
    const [formSource, setFormSource] = useState(profile);
    const [editingProfile, setEditingProfile] = useState(false);
    const [profileMessage, setProfileMessage] = useState('');
    const [profileError, setProfileError] = useState('');

    if (formSource !== profile) {
        setFormSource(profile);
        setProfileForm(toProfileForm(profile));
    }

    const updateForm = (event) => {
        const { name, value } = event.target;
        setProfileForm((current) => ({ ...current, [name]: value, }));
        setProfileError('');
        setProfileMessage('');
    };

    const startEditingProfile = () => {
        setEditingProfile(true);
        setProfileMessage('정보를 수정한 뒤 완료를 눌러 저장하세요.');
        setProfileError('');
    };

    const cancelEditingProfile = () => {
        setEditingProfile(false);
        setProfileForm(toProfileForm(profile));
        setProfileError('');
        setProfileMessage('');
    };

    const updateProfile = async (event) => {
        event.preventDefault();
        setProfileError('');
        setProfileMessage('');

        try {
            await updateCurrentUser({
                nickname: profileForm.nickname.trim(),
                name: profileForm.name.trim(),
                phoneNumber: profileForm.phoneNumber.trim(),
                birth: profileForm.birth || null,
            });

            const saved = await getCurrentUser();
            setProfile(saved);

            if (saved.nickname && saved.nickname !== user?.nickname) {
                onUserUpdate?.({ nickname: saved.nickname });
            }

            setEditingProfile(false);
            setProfileMessage('회원정보가 수정되었습니다.');
        } catch (requestError) {
            setProfileError(requestError.message || '회원정보 수정에 실패했습니다.');
        }
    };
    return { profile,
        profileForm,
        profileLoading,
        loadError,
        reload,
        profileMessage,
        profileError,
        editingProfile,
        updateForm,
        startEditingProfile,
        cancelEditingProfile,
        updateProfile, };
}