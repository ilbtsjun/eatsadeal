import { useState, useEffect, useRef } from 'react';
import { getListBrands, getListCategories } from '@/api/index.js';

const ALL = { id: 'all', name: '전체' };

export function useCategories({ activeCategory = 'all', onCategoryChange, activeBrand = 'all', onBrandChange } = {}) {
    const [categories, setCategories] = useState([ALL]);
    const [brands, setBrands] = useState([]);
    const [collapsedCategory, setCollapsedCategory] = useState(null);
    const brandListRef = useRef(null);
    const [brandScroll, setBrandScroll] = useState({ prev: false, next: false });

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

    useEffect(() => {
        const list = brandListRef.current;
        if (!list) return undefined;

        const update = () => {
            const maxScroll = list.scrollWidth - list.clientWidth;
            setBrandScroll({ prev: list.scrollLeft > 0, next: list.scrollLeft < maxScroll - 1 });
        };

        list.scrollLeft = 0;
        const frame = requestAnimationFrame(update);
        const observer = new ResizeObserver(update);
        observer.observe(list);
        list.addEventListener('scroll', update, { passive: true });

        return () => {
            cancelAnimationFrame(frame);
            observer.disconnect();
            list.removeEventListener('scroll', update);
        };
    }, [activeCategory, brands]);

    const categoryBrands = activeCategory === 'all'
        ? brands
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

    const selectBrand = (brandId) => {
        onBrandChange?.(brandId === activeBrand ? 'all' : brandId);
    };

    const scrollBrands = (direction) => {
        const list = brandListRef.current;
        list?.scrollBy({ left: direction * list.clientWidth * 0.8, behavior: 'smooth' });
    };

    return {
        categories,
        categoryBrands,
        brandPanelOpen,
        selectCategory,
        selectBrand,
        brandListRef,
        brandScroll,
        scrollBrands,
    };
}