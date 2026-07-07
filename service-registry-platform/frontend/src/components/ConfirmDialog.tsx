interface ConfirmDialogProps {
  title: string
  message: string
  confirmLabel?: string
  isBusy?: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({
  title,
  message,
  confirmLabel = 'Тийм',
  isBusy = false,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  return (
    <div className="modal-overlay" role="dialog" aria-modal="true" aria-label={title}>
      <div className="modal-card">
        <h3>{title}</h3>
        <p>{message}</p>
        <div className="modal-actions">
          <button type="button" className="ghost-button" onClick={onCancel} disabled={isBusy}>
            Болих
          </button>
          <button type="button" className="danger-button" onClick={onConfirm} disabled={isBusy}>
            {isBusy ? 'Түр хүлээнэ үү...' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
