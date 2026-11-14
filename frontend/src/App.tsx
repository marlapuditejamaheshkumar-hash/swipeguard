import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import App from './App';
import type { Alert } from './api';

const alert: Alert = {
  id: '11111111-1111-1111-1111-111111111111',
  transactionId: 'tx-1',
  cardRef: 'a'.repeat(64),
  maskedPan: '411111******1111',
  amount: 75000,
  currency: 'INR',
  mcc: '7995',
  merchantCountry: 'US',
  decision: 'DECLINE',
  score: 100,
  reasons: [
    { rule: 'HIGH_AMOUNT', detail: 'amount 75000 exceeds 50000' },
    { rule: 'RISKY_MCC', detail: 'merchant category 7995 is high risk' },
  ],
  status: 'OPEN',
  createdAt: '2026-11-14T06:00:00Z',
};

function jsonResponse(body: unknown): Response {
  return { ok: true, status: 200, json: async () => body } as Response;
}

describe('alert queue', () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    fetchMock.mockImplementation(async (_url: string, init?: RequestInit) => {
      if (init?.method === 'POST') {
        return jsonResponse({ cardRef: alert.cardRef, state: 'BLOCKED' });
      }
      return jsonResponse({ content: [alert], page: 0, size: 10, totalElements: 1 });
    });
    vi.stubGlobal('fetch', fetchMock);
  });

  it('renders the masked PAN and the rule reasons, never a full PAN', async () => {
    render(<App />);

    expect(await screen.findByText('411111******1111')).toBeInTheDocument();
    expect(screen.getByText('HIGH_AMOUNT')).toBeInTheDocument();
    expect(screen.getByText('RISKY_MCC')).toBeInTheDocument();
    expect(document.body.textContent).not.toMatch(/\d{16}/);
  });

  it('sends the reason and X-Actor when blocking a card', async () => {
    const user = userEvent.setup();
    render(<App />);

    await user.click(await screen.findByRole('button', { name: /^block card$/i }));
    await user.type(screen.getByLabelText(/reason/i), 'Customer reported stolen');
    await user.click(screen.getByRole('button', { name: /^confirm$/i }));

    await waitFor(() => {
      const post = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST');
      expect(post).toBeDefined();
      const [url, init] = post!;
      expect(url).toBe(`/api/v1/cards/${alert.cardRef}/block`);
      expect((init.headers as Record<string, string>)['X-Actor']).toBe('analyst1');
      expect(JSON.parse(init.body as string)).toEqual({ reason: 'Customer reported stolen', alertId: alert.id });
    });
  });

  it('shows an error message when the API is down', async () => {
    fetchMock.mockRejectedValue(new TypeError('network down'));
    render(<App />);

    expect(await screen.findByText(/cannot reach the server/i)).toBeInTheDocument();
  });
});