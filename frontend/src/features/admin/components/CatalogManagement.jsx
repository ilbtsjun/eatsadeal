import { getFallbackImage } from '@/common/utils/BrandUtil.js';

export default function CatalogManagement({
                                              tab,
                                              brands,
                                              categories,
                                              onCreate,
                                              onEdit,
                                              onToggleBrandActive,
                                              onDelete,
                                          }) {
    const isBrand = tab === 'brand';
    const items = isBrand ? brands : categories;

    const isBrandActive = (brand) =>
        Boolean(brand?.isActive ?? brand?.active ?? true);

    return (
        <section className="admin-panel">
            <div className="panel-heading">
                <div>
                    <h2>{isBrand ? '브랜드 목록' : '카테고리 목록'}</h2>
                    <p className="panel-description">
                        현재 등록된 {isBrand ? '브랜드' : '카테고리'} 정보입니다.
                    </p>
                </div>

                <button
                    type="button"
                    className="create-placeholder"
                    onClick={() => onCreate(tab)}
                >
                    생성
                </button>
            </div>

            <div className="management-list">
                {items.length === 0 ? (
                    <p className="admin-empty">등록된 정보가 없습니다.</p>
                ) : (
                    items.map((item) => {
                        const active = !isBrand || isBrandActive(item);

                        return (
                            <div
                                className={`management-row ${active ? '' : 'is-inactive'}`}
                                key={item.id}
                            >
                                <div className="item-main">
                                    {item.img && (
                                        <img
                                            src={item.img}
                                            alt=""
                                            onError={(event) => {
                                                event.currentTarget.onerror = null;
                                                event.currentTarget.src =
                                                    getFallbackImage(item.name);
                                            }}
                                        />
                                    )}

                                    <div className="item-title">
                                        <strong>{item.name}</strong>

                                        {isBrand && (
                                            <span
                                                className={`brand-status ${
                                                    active ? 'active' : 'inactive'
                                                }`}
                                            >
                        {active ? '활성' : '정지'}
                      </span>
                                        )}
                                    </div>
                                </div>

                                <div className="row-actions">
                                    <button
                                        type="button"
                                        onClick={() => onEdit(tab, item)}
                                    >
                                        수정
                                    </button>

                                    {isBrand ? (
                                        <button
                                            type="button"
                                            className={active ? 'delete' : 'activate'}
                                            onClick={() => onToggleBrandActive(item)}
                                        >
                                            {active ? '정지' : '활성화'}
                                        </button>
                                    ) : (
                                        <button
                                            type="button"
                                            className="delete"
                                            onClick={() =>
                                                onDelete(tab, item.id, item.name)
                                            }
                                        >
                                            삭제
                                        </button>
                                    )}
                                </div>
                            </div>
                        );
                    })
                )}
            </div>
        </section>
    );
}