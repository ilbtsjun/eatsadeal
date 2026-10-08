import { useEffect } from 'react';

export function usePageTitle(title) {
    useEffect(() => {
        document.title = title ? `${title} - Eats a Deal` : 'Eats a Deal';
    }, [title]);
}