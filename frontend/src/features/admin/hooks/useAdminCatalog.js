import { useCallback, useEffect, useState } from 'react';
import {
    getListCategories,
    getListBrands,
    activateBrand,
    deactivateBrand,
    deleteCategory,
    getBrand,
    createBrand,
    updateBrand,
    createCategory,
    updateCategory } from '@/api/index.js'

const CONFIRM_TEXT = {
    delete: {
        title: '삭제 확인',
        question: '을(를) 삭제하시겠습니까?',
        note: '삭제한 정보는 복구할 수 없습니다.',
        button: '삭제하기',
    },
    deactivate: {
        title: '브랜드 정지',
        question: ' 브랜드를 정지하시겠습니까?',
        note: '정지한 브랜드는 언제든 다시 활성화할 수 있습니다.',
        button: '정지하기',
    },
    activate: {
        title: '브랜드 활성화',
        question: ' 브랜드를 다시 활성화하시겠습니까?',
        note: '',
        button: '활성화하기',
    },
};

const isBrandActive = (brand) =>
    Boolean(brand?.isActive ?? brand?.active ?? true);

export function useAdminCatalog() {
    const [brands, setBrands] = useState([]);
    const [categories, setCategories] = useState([]);
    const [loading, setLoading] = useState(false);
    const [message, setMessage] = useState('');
    const [form, setForm] = useState(null);
    const [confirmDelete, setConfirmDelete] = useState(null);

    const loadLists = useCallback(async () => {
        setLoading(true);
        setMessage('');

        try {
            const [brandData, categoryData] = await Promise.all([
                getListBrands(),
                getListCategories(),
            ]);

            setBrands(Array.isArray(brandData) ? brandData : []);
            setCategories(Array.isArray(categoryData) ? categoryData : []);
        } catch (error) {
            setMessage(error.message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadLists();
    }, [loadLists]);

    const deleteItem = (type, id, name) => {
        setConfirmDelete({ kind: 'delete', type, id, name });
    };

    const toggleBrandActive = (brand) => {
        setConfirmDelete({
            kind: isBrandActive(brand) ? 'deactivate' : 'activate',
            type: 'brand',
            id: brand.id,
            name: brand.name,
        });
    };

    const confirmDeleteItem = async () => {
        if (!confirmDelete) return;

        const { kind, type, id, name } = confirmDelete;
        setConfirmDelete(null);

        try {
            if (kind === 'activate') {
                await activateBrand(id);
            } else if (kind === 'deactivate') {
                await deactivateBrand(id);
            } else if (type === 'brand') {
                await deactivateBrand(id);
            } else {
                await deleteCategory(id);
            }

            await loadLists();

            if (kind === 'activate') {
                setMessage(`${name ?? '브랜드'}가 활성화되었습니다.`);
            } else if (kind === 'deactivate') {
                setMessage(`${name ?? '브랜드'}가 정지되었습니다.`);
            } else {
                setMessage(`${type === 'brand' ? '브랜드' : '카테고리'}가 삭제되었습니다.`);
            }
        } catch (error) {
            setMessage(error.message);
        }
    };

    const openCreate = (type) => {
        if (type !== 'brand' && type !== 'category') return;

        setMessage('');

        if (type === 'brand') {
            setForm({
                type: 'brand',
                name: '',
                url: '',
                img: '',
                categoryIds: [],
            });
        } else {
            setForm({
                type: 'category',
                name: '',
                img: '',
                categoryIds: [],
            });
        }
    };

    const openEdit = async (type, item) => {
        if (!item || (type !== 'brand' && type !== 'category')) return;

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
            const ids = detail.categoryIds
                ?? detail.categories?.map((category) => category.id)
                ?? [];

            setForm({
                type: 'brand',
                id: item.id,
                name: detail.name ?? item.name ?? '',
                url: detail.url ?? '',
                img: detail.img ?? item.img ?? '',
                categoryIds: ids.map(Number).filter(Number.isFinite),
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
        setForm((current) =>
            current ? { ...current, [field]: value } : current
        );
    };

    const submitForm = async (event) => {
        event.preventDefault();

        if (!form) return;

        const name = form.name.trim();
        const img = form.img.trim();

        if (!name) {
            setMessage('이름을 입력해주세요.');
            return;
        }

        if (!img) {
            setMessage(
                form.type === 'category'
                    ? '카테고리 이미지 URL을 입력해주세요.'
                    : '브랜드 이미지 URL을 입력해주세요.'
            );
            return;
        }

        try {
            if (form.type === 'brand') {
                const categoryIds = form.categoryIds;

                if (!categoryIds.length) {
                    setMessage('카테고리를 하나 이상 선택해주세요.');
                    return;
                }

                const body = { name, url: form.url.trim(), img, categoryIds };
                if(form.id)
                    await updateBrand(form.id, body);
                else
                    await createBrand(body);
            } else {
                const body = form.id ? { img } : { name, img };
                if(form.id)
                    await updateCategory(form.id, body);
                else
                    await createCategory(body);
            }

            setForm(null);
            await loadLists();
            setMessage('저장되었습니다.');
        } catch (error) {
            setMessage(error.message);
        }
    };

    return {
        brands,
        categories,
        loading,
        message,
        setMessage,
        loadLists,
        form,
        setForm,
        confirmDelete,
        setConfirmDelete,
        deleteItem,
        toggleBrandActive,
        confirmDeleteItem,
        openCreate,
        openEdit,
        toggleCategory,
        updateFormField,
        submitForm,
        confirmTextMap: CONFIRM_TEXT
    };
}