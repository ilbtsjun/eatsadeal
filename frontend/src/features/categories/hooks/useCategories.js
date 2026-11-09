import { useState, useEffect, useRef } from 'react';
import { getListBrands, getListCategories } from '@/api/index.js';
import { useFetch } from '@/common/hooks/useFetch.js';

const ALL = { id: 'all', name: '전체' };

const INITIAL_FILTERS = { categories: [ALL], brands: [] };

const toCategories = (data) => [
    ALL,
    ...(Array.isArray(data) ? data : [])
        .filter((c) => c.id != null && c.name)
        .map((c) => ({ id: String(c.id), name: c.name })),
];

const toBrands = (data) => (Array.isArray(data) ? data : [])
    .filter((b) => b.id != null && b.name && b.isActive)
    .map((b) => ({
        id: String(b.id),
        name: b.name,
        categoryIds: (b.categoryIds ?? []).map(String),
    }));

const fetchFilters = () => Promise.all([
    getListCategories().then(toCategories).catch(() => [ALL]),
    getListBrands().then(toBrands).catch(() => []),
]).then(([categories, brands]) => ({ categories, brands }));

export function useCategories({ activeCategory = 'all', onCategoryChange, activeBrand = 'all', onBrandChange } = {}) {
    const { data: { categories, brands } } = useFetch(fetchFilters, { initialData: INITIAL_FILTERS });
    const [collapsedCategory, setCollapsedCategory] = useState(null);
    const brandListRef = useRef(null);
    const [brandScroll, setBrandScroll] = useState({ prev: false, next: false });

    useEffect(() => {
        const list = brandListRef.current;
        if (!list) return undefined;

        const update = () => {
            const maxScroll = list.scrollWidth - list.clientWidth;
            setBrandScroll({ prev: list.scrollLeft > 0, next: list.scrollLeft < maxScroll - 1 });
        };

        const active = list.querySelector('.brand-btn.active');
        if (active) {
            const listRect = list.getBoundingClientRect();
            const activeRect = active.getBoundingClientRect();
            if (activeRect.left < listRect.left || activeRect.right > listRect.right)
                list.scrollLeft += activeRect.left - listRect.left - (list.clientWidth - activeRect.width) / 2;
        } else {
            list.scrollLeft = 0;
        }
        const frame = requestAnimationFrame(update);
        const observer = new ResizeObserver(update);
        observer.observe(list);
        list.addEventListener('scroll', update, { passive: true });

        return () => {
            cancelAnimationFrame(frame);
            observer.disconnect();
            list.removeEventListener('scroll', update);
        };
    }, [activeCategory, activeBrand, brands]);

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