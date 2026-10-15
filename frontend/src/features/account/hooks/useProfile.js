import {useEffect, useState} from "react";
import { getCurrentUser, updateCurrentUser, } from '@/api/index.js';

const toProfileForm = (profile = {}) => ({
    nickname: profile.nickname || '',
    name: profile.name || '',
    phoneNumber: profile.phoneNumber || '',
    birth: profile.birth || '',
});

export function useProfile({user, onUserUpdate}){
    const [profile, setProfile] = useState(user || {});
    const [profileForm, setProfileForm] = useState(() => toProfileForm(user));
    const [editingProfile, setEditingProfile] = useState(false);
    const [profileMessage, setProfileMessage] = useState('');
    const [profileError, setProfileError] = useState('');
    const [profileLoading, setProfileLoading] = useState(true);

    useEffect(() => {
        let cancelled = false;
        const loadProfile = async () => {
            setProfileLoading(true);
            setProfileError('');
            try {
                const data = await getCurrentUser();
                if (cancelled)
                    return;
                setProfile(data);
                setProfileForm(toProfileForm(data));
            } catch (error) {
                if (cancelled)
                    return;
                setProfileError( error.message || '회원정보를 불러오지 못했습니다.' );
            } finally {
                if (!cancelled) {
                    setProfileLoading(false);
                }
            }
        };

        loadProfile();
        return () => { cancelled = true; };
    }, [user]);

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
            setProfileForm(toProfileForm(saved));

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
        profileMessage,
        profileError,
        editingProfile,
        updateForm,
        startEditingProfile,
        cancelEditingProfile,
        updateProfile, };
}