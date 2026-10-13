import Modal from '@/common/components/Modal.jsx';

export default function BrandCategoryFormModal({
                                                   form,
                                                   categories,
                                                   onClose,
                                                   onSubmit,
                                                   onToggleCategory,
                                                   onNameChange,
                                                   onUrlChange,
                                                   onImageChange,
                                               }) {
    if (!form) return null;

    return (
        <Modal backdropClassName="admin-modal-backdrop" onClose={onClose}>
            <form className="admin-modal" onSubmit={onSubmit}>
                <div className="modal-heading">
                    <h2>{form.id ? '정보 수정' : '정보 생성'}</h2>
                    <button type="button" onClick={onClose}>×</button>
                </div>
                <label>
                    이름
                    <input value={form.name} onChange={onNameChange} required />
                </label>
                {form.type === 'brand' && (
                    <>
                        <label>
                            브랜드 URL
                            <input value={form.url} onChange={onUrlChange} required={!form.id} />
                        </label>
                        <div className="category-picker">
                            <span className="category-picker-label">
                                카테고리<em>{form.categoryIds.length}개 선택</em>
                            </span>
                            {categories.length === 0 ? (
                                <p className="category-picker-empty">
                                    등록된 카테고리가 없습니다. 카테고리를 먼저 생성해주세요.
                                </p>
                            ) : (
                                <div className="category-chip-list">
                                    {categories.map((category) => {
                                        const selected = form.categoryIds.includes(Number(category.id));
                                        return (
                                            <button
                                                type="button"
                                                key={category.id}
                                                className={`category-chip ${selected ? 'selected' : ''}`}
                                                aria-pressed={selected}
                                                onClick={() => onToggleCategory(category.id)}
                                            >
                                                {category.name}
                                            </button>
                                        );
                                    })}
                                </div>
                            )}
                        </div>
                    </>
                )}
                <label>
                    {form.type === 'category' ? '카테고리 링크(URL)' : '이미지 URL'}
                    <input value={form.img} onChange={onImageChange} required />
                </label>
                <button className="modal-submit" type="submit">저장</button>
            </form>
        </Modal>
    );
}
