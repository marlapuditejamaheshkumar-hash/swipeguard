export type AlertStatus = 'OPEN' | 'CARD_BLOCKED';
export type StatusFilter = AlertStatus | 'ALL';

export interface Reason {
  rule: string;
  detail: string;
}

export interface Alert {
  id: string;
  transactionId: string;
  cardRef: string;
  maskedPan: string;
  amount: number;
  currency: string;
  mcc: string;
  merchantCountry: string;
  decision: 'APPROVE' | 'REVIEW' | 'DECLINE';
  score: number;
  reasons: Reason[];
  status: AlertStatus;
  createdAt: string;
}

export interface AlertPage {
  content: Alert[];
  page: number;
  size: number;
  totalElements: number;
}

export class ApiError extends Error {}

async function describeError(response: Response): Promise<string> {
  try {
    const body = await response.json();
    const fields: string[] = (body.fields ?? []).map(
      (f: { field: string; message: string }) => `${f.field} ${f.message}`,
    );
    if (fields.length > 0) {
      return fields.join(', ');
    }
  } catch {
    // the error body was not JSON: fall through to the generic message
  }
  return `Request failed (HTTP ${response.status})`;
}

async function request<T>(url: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(url, init);
  } catch {
    throw new ApiError('Cannot reach the server. Is the backend running?');
  }
  if (!response.ok) {
    throw new ApiError(await describeError(response));
  }
  return (await response.json()) as T;
}

export function fetchAlerts(status: StatusFilter, page: number, size: number): Promise<AlertPage> {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  if (status !== 'ALL') {
    params.set('status', status);
  }
  return request<AlertPage>(`/api/v1/alerts?${params.toString()}`);
}

export function changeCard(
  action: 'block' | 'unblock',
  cardRef: string,
  reason: string,
  actor: string,
  alertId: string,
): Promise<{ cardRef: string; state: string }> {
  return request(`/api/v1/cards/${cardRef}/${action}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'X-Actor': actor },
    body: JSON.stringify({ reason, alertId }),
  });
}