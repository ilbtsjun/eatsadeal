import {apiRequest} from "../../api/client.js";

export const toggleFavorite = async (user, event, clickEvent, setError, setEvents) => {
    clickEvent.stopPropagation();
    if (!user) {
        window.alert('찜하기 기능을 사용하려면 로그인해주세요.');
        return;
    }

    const nextFavorite = !event.isFavorite;
    try {
        await apiRequest(`/api/events/${event.id}/favorite`, {
            method: nextFavorite ? 'POST' : 'DELETE',
        });
        setEvents((current) => current.map((item) => item.id === event.id
            ? { ...item, isFavorite: nextFavorite }
            : item));
    } catch (requestError) {
        setError(requestError.message);
    }
};