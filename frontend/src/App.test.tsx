import { useEffect, useState } from 'react';
import { AlertTable } from './AlertTable';
import { ReasonDialog } from './ReasonDialog';
import { changeCard, fetchAlerts, type Alert, type AlertPage, type StatusFilter } from './api';
import './App.css';

const PAGE_SIZE = 10;

interface PendingAction {
  alert: Alert;
  action: 'block' | 'unblock';
}

export default function App() {
  const [status, setStatus] = useState<StatusFilter>('ALL');
  const [page, setPage] = useState(0);
  const [actor, setActor] = useState('analyst1');
  const [data, setData] = useState<AlertPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [pending, setPending] = useState<PendingAction | null>(null);

  // Loads whenever the filter, the page or reloadKey changes. "cancelled" ignores a stale response.
  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    fetchAlerts(status, page, PAGE_SIZE)
      .then((result) => { if (!cancelled) setData(result); })
      .catch((err: Error) => { if (!cancelled) setError(err.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [status, page, reloadKey]);

  async function submitReason(reason: string) {
    if (!pending) return;
    await changeCard(pending.action, pending.alert.cardRef, reason, actor, pending.alert.id);
    setPending(null);
    setReloadKey((key) => key + 1); // refetch so the row shows its new status
  }

  const totalPages = data ? Math.max(1, Math.ceil(data.totalElements / data.size)) : 1;

  return (
    <main>
      <h1>Swipeguard: alert queue</h1>

      <div className="toolbar">
        <label>
          Status
          <select value={status} onChange={(e) => { setStatus(e.target.value as StatusFilter); setPage(0); }}>
            <option value="ALL">All</option>
            <option value="OPEN">Open</option>
            <option value="CARD_BLOCKED">Card blocked</option>
          </select>
        </label>
        <label>
          Your name (X-Actor)
          <input value={actor} onChange={(e) => setActor(e.target.value)} />
        </label>
      </div>

      {loading && <p>Loading alerts…</p>}
      {error && (
        <p className="error" role="alert">
          {error} <button onClick={() => setReloadKey((key) => key + 1)}>Retry</button>
        </p>
      )}
      {!loading && !error && data && data.content.length === 0 && <p>No alerts found for this filter.</p>}
      {!loading && !error && data && data.content.length > 0 && (
        <>
          <AlertTable alerts={data.content} onAction={(alert, action) => setPending({ alert, action })} />
          <div className="pager">
            <button onClick={() => setPage((p) => p - 1)} disabled={page === 0}>Previous</button>
            <span>Page {page + 1} of {totalPages}</span>
            <button onClick={() => setPage((p) => p + 1)} disabled={page + 1 >= totalPages}>Next</button>
          </div>
        </>
      )}

      {pending && (
        <ReasonDialog
          title={`${pending.action === 'block' ? 'Block' : 'Unblock'} card ${pending.alert.maskedPan}`}
          onSubmit={submitReason}
          onCancel={() => setPending(null)}
        />
      )}
    </main>
  );
}