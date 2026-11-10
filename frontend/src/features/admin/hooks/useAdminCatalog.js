import { useState } from 'react';
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
import { usePending } from '@/common/hooks/usePending.js';
import { useFetch } from '@/common/hooks/useFetch.js';

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

const EMPTY_CATALOG = { brands: [], categories: [] };

const fetchCatalog = () =>
    Promise.all([getListBrands(), getListCategories()])
        .then(([brands, categories]) => ({
            brands: Array.isArray(brands) ? brands : [],
            categories: Array.isArray(categories) ? categories : [],
        }));

export function useAdminCatalog() {
    const {
        data: { brands, categories },
        loading,
        loadError: listError,
        reload,
        refresh,
    } = useFetch(fetchCatalog, { initialData: EMPTY_CATALOG, fallbackMessage: '목록을 불러오지 못했습니다.' });
    const [message, setMessage] = useState('');
    const [confirmDelete, setConfirmDelete] = useState(null);
    const [processing, withProcessing] = usePending();
    const {
        form,
        setForm,
        formError,
        setFormError,
        openCreate,
        openEdit,
        toggleCategory,
        updateFormField,
    } = useAdminCatalogForm(setMessage);

    const loadLists = () => {
        setMessage('');
        reload();
    };

    const refreshAfterChange = async (successMessage) => {
        try {
            await refresh();
            setMessage(successMessage);
        } catch {
            setMessage(`${successMessage} 목록을 새로고침하지 못했습니다.`);
        }
    };

    const deleteItem = (type, id, name) => {
        setConfirmDelete({ kind: 'delete', type, id, name });
    };

    const toggleBrandActive = (brand) => {
        setConfirmDelete({
            kind: brand.isActive ? 'deactivate' : 'activate',
            type: 'brand',
            id: brand.id,
            name: brand.name,
        });
    };

    const closeForm = () => {
        if (!processing) setForm(null);
    };

    const closeConfirm = () => {
        if (!processing) setConfirmDelete(null);
    };

    const confirmDeleteItem = async () => {
        if (!confirmDelete) return;

        const { kind, type, id, name } = confirmDelete;
        await withProcessing(async () => {
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

                if (kind === 'activate') {
                    await refreshAfterChange(`${name ?? '브랜드'}가 활성화되었습니다.`);
                } else if (kind === 'deactivate') {
                    await refreshAfterChange(`${name ?? '브랜드'}가 정지되었습니다.`);
                } else {
                    await refreshAfterChange(`${type === 'brand' ? '브랜드' : '카테고리'}가 삭제되었습니다.`);
                }
            } catch (error) {
                setMessage(error.message);
            } finally {
                setConfirmDelete(null);
            }
        });
    };

    const submitForm = async (event) => {
        event.preventDefault();

        if (!form) return;

        const name = form.name.trim();
        const img = form.img.trim();

        if (!name) {
            setFormError('이름을 입력해주세요.');
            return;
        }

        if (!img) {
            setFormError(
                form.type === 'category'
                    ? '카테고리 이미지 URL을 입력해주세요.'
                    : '브랜드 이미지 URL을 입력해주세요.'
            );
            return;
        }

        if (form.type === 'brand' && !form.categoryIds.length) {
            setFormError('카테고리를 하나 이상 선택해주세요.');
            return;
        }

        await withProcessing(async () => {
            try {
                if (form.type === 'brand') {
                    const body = { name, url: form.url.trim(), img, categoryIds: form.categoryIds };
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
                await refreshAfterChange('저장되었습니다.');
            } catch (error) {
                setFormError(error.message);
            }
        });
    };

    return {
        brands,
        categories,
        loading,
        listError,
        message,
        setMessage,
        loadLists,
        form,
        formError,
        confirmDelete,
        deleteItem,
        toggleBrandActive,
        confirmDeleteItem,
        openCreate,
        openEdit,
        toggleCategory,
        updateFormField,
        submitForm,
        processing,
        closeForm,
        closeConfirm,
        confirmTextMap: CONFIRM_TEXT
    };
}