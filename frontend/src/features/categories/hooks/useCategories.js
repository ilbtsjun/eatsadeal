import { useState, useEffect } from 'react';
import { getListBrands, getListCategories } from '@/api/index.js';

const ALL = { id: 'all', name: '전체' };

export function useCategories({ activeCategory = 'all', onCategoryChange } = {}) {
    const [categories, setCategories] = useState([ALL]);
    const [brands, setBrands] = useState([]);
    const [collapsedCategory, setCollapsedCategory] = useState(null);

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

        getListBrands()
            .then((data) => {
                if (cancelled) return;
                setBrands((Array.isArray(data) ? data : [])
                    .filter((b) => b.id != null && b.name && b.isActive)
                    .map((b) => ({
                        id: String(b.id),
                        name: b.name,
                        categoryIds: (b.categoryIds ?? []).map(String),
                    })));
            })
            .catch(() => {
                if (!cancelled) setBrands([]);
            });

        return () => { cancelled = true; };
    }, []);

    const categoryBrands = activeCategory === 'all'
        ? []
        : brands.filter((brand) => brand.categoryIds.includes(String(activeCategory)));
    const brandPanelOpen = categoryBrands.length > 0 && collapsedCategory !== activeCategory;

    const selectCategory = (categoryId) => {
        if (categoryId === activeCategory) {
            setCollapsedCategory((current) => (current === categoryId ? null : categoryId));
            return;
        }
        setCollapsedCategory(null);
        onCategoryChange?.(categoryId);
    };

    return { categories, categoryBrands, brandPanelOpen, selectCategory };
}