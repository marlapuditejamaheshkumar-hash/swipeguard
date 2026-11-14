import { useState, type FormEvent } from 'react';

interface Props {
  title: string;
  onSubmit: (reason: string) => Promise<void>;
  onCancel: () => void;
}

export function ReasonDialog({ title, onSubmit, onCancel }: Props) {
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await onSubmit(reason.trim());
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Something went wrong');
      setBusy(false);
    }
  }

  return (
    <div className="overlay">
      <form className="dialog" onSubmit={handleSubmit}>
        <h2>{title}</h2>
        <label>
          Reason
          <textarea value={reason} onChange={(e) => setReason(e.target.value)} rows={3} maxLength={500} />
        </label>
        {error && <p className="error" role="alert">{error}</p>}
        <div className="dialog-actions">
          <button type="button" onClick={onCancel} disabled={busy}>Cancel</button>
          <button type="submit" disabled={busy || reason.trim() === ''}>Confirm</button>
        </div>
      </form>
    </div>
  );
}