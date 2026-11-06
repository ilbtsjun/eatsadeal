import { useState } from 'react';
import { checkEmailAvailability, checkNicknameAvailability, signup, verifyEmail,} from '@/api/index.js';

const INITIAL_FORM = {
    nickname: '',
    email: '',
    password: '',
    passwordConfirm: '',
    name: '',
    phoneNumber: '',
    userGender: 'UNSPECIFIED',
    birth: '',
};

const DUPLICATE_CODE = 'COMMON-003';

export function useSignup({ onSignupSuccess }) {
    const [step, setStep] = useState(1);
    const [form, setForm] = useState(INITIAL_FORM);
    const [emailChecked, setEmailChecked] = useState(false);
    const [nicknameChecked, setNicknameChecked] = useState(false);
    const [emailMessage, setEmailMessage] = useState('');
    const [showLoginPrompt, setShowLoginPrompt] = useState(false);
    const [nicknameMessage, setNicknameMessage] = useState('');
    const [error, setError] = useState('');
    const [notice, setNotice] = useState('');
    const [loading, setLoading] = useState(false);
    const [authCode, setAuthCode] = useState('');
    const [agreeTerms, setAgreeTerms] = useState(false);
    const [agreePrivacy, setAgreePrivacy] = useState(false);

    const allAgreed = agreeTerms && agreePrivacy;

    const updateField = (event) => {
        const { name, value } = event.target;
        setForm((current) => ({ ...current, [name]: value }));

        if (name === 'email') {
            setEmailChecked(false);
            setEmailMessage('');
            setShowLoginPrompt(false);
        }

        if (name === 'nickname') {
            setNicknameChecked(false);
            setNicknameMessage('');
        }

        setError('');
    };

    const handleAgreeAll = (event) => {
        const checked = event.target.checked;
        setAgreeTerms(checked);
        setAgreePrivacy(checked);
        setError('');
    };

    const handleEmailCheck = async () => {
        const email = form.email.trim();

        if (!email) {
            setEmailChecked(false);
            setShowLoginPrompt(false);
            setEmailMessage('이메일을 입력해주세요.');
            return;
        }

        try {
            const exists = await checkEmailAvailability(email);
            setEmailChecked(!exists);
            setShowLoginPrompt(exists);
            setEmailMessage(
                exists ? '이미 가입된 이메일입니다.' : '사용 가능한 이메일입니다.'
            );
        } catch (requestError) {
            setEmailChecked(false);
            setShowLoginPrompt(false);
            setEmailMessage(requestError.message);
        }
    };

    const handleNicknameCheck = async () => {
        const nickname = form.nickname.trim();

        if (!nickname) {
            setNicknameChecked(false);
            setNicknameMessage('닉네임을 입력해주세요.');
            return;
        }

        try {
            const exists = await checkNicknameAvailability(nickname);
            setNicknameChecked(!exists);
            setNicknameMessage(
                exists ? '이미 사용 중인 닉네임입니다.' : '사용 가능한 닉네임입니다.'
            );
        } catch (requestError) {
            setNicknameChecked(false);
            setNicknameMessage(requestError.message);
        }
    };

    const handleNext = (event) => {
        event.preventDefault();
        setError('');

        if (!nicknameChecked || !emailChecked) {
            setError('닉네임과 이메일 중복확인을 완료해주세요.');
            return;
        }

        if (form.password !== form.passwordConfirm) {
            setError('비밀번호가 일치하지 않습니다.');
            return;
        }

        if (!agreeTerms || !agreePrivacy) {
            setError('이용약관과 개인정보처리방침에 모두 동의해주세요.');
            return;
        }

        setStep(2);
    };

    const recheckDuplicates = async () => {
        setStep(1);

        try {
            const [emailTaken, nicknameTaken] = await Promise.all([
                checkEmailAvailability(form.email.trim()),
                checkNicknameAvailability(form.nickname.trim()),
            ]);

            setEmailChecked(!emailTaken);
            setShowLoginPrompt(emailTaken);
            setEmailMessage(emailTaken ? '이미 가입된 이메일입니다.' : '사용 가능한 이메일입니다.');
            setNicknameChecked(!nicknameTaken);
            setNicknameMessage(nicknameTaken ? '이미 사용 중인 닉네임입니다.' : '사용 가능한 닉네임입니다.');
            setError(emailTaken
                ? '이미 가입된 이메일입니다.'
                : nicknameTaken
                    ? '이미 사용 중인 닉네임입니다.'
                    : '이메일 또는 닉네임이 이미 사용 중입니다. 다시 시도해주세요.');
        } catch {
            setEmailChecked(false);
            setNicknameChecked(false);
            setEmailMessage('');
            setNicknameMessage('');
            setShowLoginPrompt(false);
            setError('이메일 또는 닉네임이 이미 사용 중입니다. 중복확인을 다시 해주세요.');
        }
    };

    const handleSubmit = async (event) => {
        event.preventDefault();
        setError('');
        setNotice('');
        setLoading(true);

        try {
            await signup({
                name: form.name.trim(),
                email: form.email.trim(),
                password: form.password,
                nickname: form.nickname.trim(),
                phoneNumber: form.phoneNumber.trim() || null,
                userGender: form.userGender || 'UNSPECIFIED',
                birth: form.birth || null,
            });

            setStep(3);
            setNotice('인증번호가 이메일로 발송되었습니다. 인증번호를 입력해주세요.');
        } catch (requestError) {
            if (requestError.data?.code === DUPLICATE_CODE) {
                await recheckDuplicates();
                return;
            }
            setError(requestError.message || '회원가입에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    const handleVerify = async (event) => {
        event.preventDefault();
        setError('');
        setLoading(true);

        try {
            await verifyEmail({
                email: form.email.trim(),
                inputCode: authCode.trim(),
            });
            onSignupSuccess?.();
        } catch (requestError) {
            setError(requestError.message || '이메일 인증에 실패했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return {
        step,
        setStep,
        form,
        emailChecked,
        nicknameChecked,
        emailMessage,
        showLoginPrompt,
        nicknameMessage,
        error,
        notice,
        loading,
        authCode,
        setAuthCode,
        agreeTerms,
        setError,
        setAgreeTerms,
        agreePrivacy,
        setAgreePrivacy,
        allAgreed,
        updateField,
        handleAgreeAll,
        handleEmailCheck,
        handleNicknameCheck,
        handleNext,
        handleSubmit,
        handleVerify,
    };
}