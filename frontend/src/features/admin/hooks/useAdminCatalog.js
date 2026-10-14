import { useCallback, useEffect, useState } from 'react';
import { useAdminCatalogForm } from './useAdminCatalogForm.js';
import {
    getListCategories,
    getListBrands,
    activateBrand,
    deactivateBrand,
    deleteCategory,
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
    const [confirmDelete, setConfirmDelete] = useState(null);
    const {
        form,
        setForm,
        openCreate,
        openEdit,
        toggleCategory,
        updateFormField,
    } = useAdminCatalogForm(setMessage);

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
            if (kind === 'activate' && type === 'brand') {
                await activateBrand(id);
            } else if (kind === 'deactivate' && type === 'brand') {
                await deactivateBrand(id);
            } else if (kind === 'delete' && type === 'category') {
                await deleteCategory(id);
            } else {
                throw new Error('지원하지 않는 관리자 작업입니다.');
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