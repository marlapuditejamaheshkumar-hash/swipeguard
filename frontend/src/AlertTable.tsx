import type { Alert } from './api';

interface Props {
  alerts: Alert[];
  onAction: (alert: Alert, action: 'block' | 'unblock') => void;
}

export function AlertTable({ alerts, onAction }: Props) {
  return (
    <table>
      <thead>
        <tr>
          <th>Time</th>
          <th>Card</th>
          <th>Amount</th>
          <th>MCC</th>
          <th>Country</th>
          <th>Decision</th>
          <th>Reasons</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        {alerts.map((alert) => (
          <tr key={alert.id}>
            <td>{new Date(alert.createdAt).toLocaleString()}</td>
            <td>{alert.maskedPan}</td>
            <td>{alert.amount.toLocaleString('en-IN')} {alert.currency}</td>
            <td>{alert.mcc}</td>
            <td>{alert.merchantCountry}</td>
            <td className={`decision-${alert.decision}`}>{alert.decision}</td>
            <td>
              <ul>
                {alert.reasons.map((reason) => (
                  <li key={reason.rule}><strong>{reason.rule}</strong>: {reason.detail}</li>
                ))}
              </ul>
            </td>
            <td>{alert.status}</td>
            <td>
              {alert.status === 'CARD_BLOCKED' ? (
                <button onClick={() => onAction(alert, 'unblock')}>Unblock card</button>
              ) : (
                <button onClick={() => onAction(alert, 'block')}>Block card</button>
              )}
            </td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}