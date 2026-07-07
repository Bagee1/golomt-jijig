import { ChevronLeft, ChevronRight } from 'lucide-react'

interface PaginationControlsProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
}

export function PaginationControls({ page, totalPages, onPageChange }: PaginationControlsProps) {
  if (totalPages <= 1) {
    return null
  }

  return (
    <div className="pagination-bar">
      <button
        type="button"
        className="page-btn"
        disabled={page <= 0}
        onClick={() => onPageChange(page - 1)}
      >
        <ChevronLeft size={15} />
        Өмнөх
      </button>
      <span className="page-info">{page + 1} / {totalPages}</span>
      <button
        type="button"
        className="page-btn"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        Дараах
        <ChevronRight size={15} />
      </button>
    </div>
  )
}
