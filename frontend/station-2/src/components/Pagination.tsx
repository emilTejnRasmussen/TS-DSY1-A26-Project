interface PaginationProps {
    page: number;
    totalPages: number;
    totalElements: number;
    onPageChange: (page: number) => void;
    isLoading?: boolean;
    label?: string;
}

export default function Pagination({ page, totalPages, totalElements, onPageChange,
    isLoading = false, label = 'results' }: PaginationProps) {
    if (totalPages === 0 && page === 0) return null;

    return (
        <div className="pagination">
            <span className="pagination-info">
                {totalElements} {label} · Page {page + 1} of {Math.max(totalPages, 1)}
            </span>
            <div className="pagination-actions">
                <button type="button" className="button-secondary" disabled={isLoading || page === 0}
                    onClick={() => onPageChange(page - 1)}>Previous</button>
                <button type="button" className="button-secondary"
                    disabled={isLoading || page >= totalPages - 1}
                    onClick={() => onPageChange(page + 1)}>Next</button>
            </div>
        </div>
    );
}
