import { useState } from 'react';
import { login, getCurrentUser } from '@/api/index.js';
import { formatDateTime } from '@/utils/DateUtil.js';
import { loginUser } from '@/auth/AuthUtil.js';

function getLoginErrorMessage(error) {
    const data = error.response?.data ?? error.data ?? {};
    const code = data.code ?? data.errorCode;
    const message = data.message ?? data.msg ?? error.message;

    switch (code) {
        case 'AUTH-004':
            return message || '이메일 또는 비밀번호를 확인해주세요.';

        case 'AUTH-005':
            return message || '너무 많은 요청을 했습니다. 잠시 후 다시 시도해주세요.';

        case 'AUTH-007':
            return formatSuspendedMessage(message);

        case 'AUTH-008':
            return message || '이미 탈퇴한 사용자입니다.';

        default:
            return message || '로그인에 실패했습니다.';
    }
}

function formatSuspendedMessage(message = '') {
    if (!message) {
        return '이용이 제한된 계정입니다.';
    }

    const [dateTime, ...rest] = message.split('까지');

    if (!rest.length) {
        return message;
    }

    return `${formatDateTime(dateTime)}까지${rest.join('까지')}`;
}

export function useLogin({ onLogin }) {
    const [id, setId] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async (event) => {
        event.preventDefault();

        if (loading) return;

        setError('');
        setLoading(true);

        try {
            const result = await login({
                id: id.trim(),
                password,
            });

            if (String(result.status) !== '200' || !result.token) {
                throw new Error(
                    result.msg || '이메일 또는 비밀번호를 확인해주세요.',
                );
            }

            loginUser(result.token);

            const user = await getCurrentUser();

            onLogin({
                ...user,
                token: result.token,
            });
        } catch (requestError) {
            console.error('로그인 요청 실패:', requestError);
            setError(getLoginErrorMessage(requestError));
        } finally {
            setLoading(false);
        }
    };

    return {
        id,
        setId,
        password,
        setPassword,
        error,
        loading,
        handleSubmit,
    };
}