const PAGE_GROUP_SIZE = 5;

export default function Pagination({ page, totalPages, onPageChange }) {
    if (totalPages <= 1) return null;

    const groupStart = Math.floor(page / PAGE_GROUP_SIZE) * PAGE_GROUP_SIZE;
    const groupEnd = Math.min(groupStart + PAGE_GROUP_SIZE, totalPages);
    const pageNumbers = Array.from({ length: groupEnd - groupStart }, (_, i) => groupStart + i);

    return (
        <nav className="pagination" aria-label="이벤트 페이지 이동">
            <button
                type="button"
                className="page-btn"
                onClick={() => onPageChange(0)}
                disabled={page === 0}
                aria-label="첫 페이지"
            >
                &laquo;
            </button>
            <button
                type="button"
                className="page-btn"
                onClick={() => onPageChange(Math.max(page - 1, 0))}
                disabled={page === 0}
                aria-label="이전 페이지"
            >
                &lsaquo;
            </button>

            {pageNumbers.map((pageIndex) => (
                <button
                    key={pageIndex}
                    type="button"
                    className={`page-btn ${pageIndex === page ? 'is-active' : ''}`}
                    onClick={() => onPageChange(pageIndex)}
                    aria-current={pageIndex === page ? 'page' : undefined}
                >
                    {pageIndex + 1}
                </button>
            ))}

            <button
                type="button"
                className="page-btn"
                onClick={() => onPageChange(Math.min(page + 1, totalPages - 1))}
                disabled={page >= totalPages - 1}
                aria-label="다음 페이지"
            >
                &rsaquo;
            </button>
            <button
                type="button"
                className="page-btn"
                onClick={() => onPageChange(totalPages - 1)}
                disabled={page >= totalPages - 1}
                aria-label="마지막 페이지"
            >
                &raquo;
            </button>
        </nav>
    );
}