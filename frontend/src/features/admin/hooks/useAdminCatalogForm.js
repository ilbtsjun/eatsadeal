import { useState } from 'react';
import { getBrand } from '@/api/index.js';

export function useAdminCatalogForm(setMessage) {
    const [form, setForm] = useState(null);
    const openCreate = (type) => {
        if (type !== 'brand' && type !== 'category')
            return;
        setMessage('');
        setForm(type === 'brand'
            ? { type: 'brand', name: '', url: '', img: '', categoryIds: [] }
            : { type: 'category', name: '', img: '', categoryIds: [] }
        );
    };

    const openEdit = async (type, item) => {
        if (!item || (type !== 'brand' && type !== 'category'))
            return;
        setMessage('');

        if (type === 'category') {
            setForm({
                type: 'category',
                id: item.id,
                name: item.name ?? '',
                img: item.img ?? '',
                categoryIds: [],
            });
            return;
        }

        try {
            const detail = await getBrand(item.id);
            const categoryIds = detail.categoryIds
                ?? detail.categories?.map((category) => category.id)
                ?? [];

            setForm({
                type: 'brand',
                id: item.id,
                name: detail.name ?? item.name ?? '',
                url: detail.url ?? '',
                img: detail.img ?? item.img ?? '',
                categoryIds: categoryIds.map(Number).filter(Number.isFinite),
            });
        } catch (error) {
            setMessage(`브랜드 상세 정보를 불러오지 못했습니다: ${error.message}`);
        }
    };

    const toggleCategory = (categoryId) => {
        const id = Number(categoryId);

        setForm((current) => {
            if (!current) return current;

            const categoryIds = current.categoryIds ?? [];
            return {
                ...current,
                categoryIds: categoryIds.includes(id)
                    ? categoryIds.filter((value) => value !== id)
                    : [...categoryIds, id],
            };
        });
    };

    const updateFormField = (field, value) => {
        setForm((current) => current ? { ...current, [field]: value } : current);
    };

    return { form, setForm, openCreate, openEdit, toggleCategory, updateFormField };
}
