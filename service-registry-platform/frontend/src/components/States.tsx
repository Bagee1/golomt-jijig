interface LoadingStateProps {
  label?: string
}

interface ErrorStateProps {
  message: string
  onRetry?: () => void
}

export function LoadingState({ label = 'Уншиж байна...' }: LoadingStateProps) {
  return (
    <div className="state-card">
      <div className="spinner" aria-hidden="true" />
      <p>{label}</p>
    </div>
  )
}

export function ErrorState({ message, onRetry }: ErrorStateProps) {
  return (
    <div className="state-card state-error">
      <strong>Алдаа гарлаа</strong>
      <p>{message}</p>
      {onRetry ? <button type="button" onClick={onRetry}>Дахин оролдох</button> : null}
    </div>
  )
}

export function EmptyState({ label }: { label: string }) {
  return (
    <div className="state-card">
      <p>{label}</p>
    </div>
  )
}
