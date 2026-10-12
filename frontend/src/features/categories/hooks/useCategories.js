import { useState, useEffect } from 'react';
import { getListCategories } from '@/api/categories.js';

const ALL = { id: 'all', name: '전체' };

export function useCategories() {
    const [categories, setCategories] = useState([ALL]);

    useEffect(() => {
        let cancelled = false;

        getListCategories()
            .then((data) => {
                if (cancelled) return;
                const list = (Array.isArray(data) ? data : [])
                    .filter((c) => c.id != null && c.name)
                    .map((c) => ({ id: String(c.id), name: c.name }));
                setCategories([ALL, ...list]);
            })
            .catch(() => {
                if (!cancelled) setCategories([ALL]);
            });

        return () => { cancelled = true; };
    }, []);

    return { categories };
}