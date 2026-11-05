import { useEffect, useMemo, useState } from 'react';
import {
  Area,
  AreaChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts';

const SAMPLE_USER_ID = 'user_samsung_watch9_twin';
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '')
  .replace(/\/api\/?$/, '')
  .replace(/\/$/, '');

const metricConfig = [
  { key: 'steps', label: 'Steps', unit: 'steps', color: '#4facfe' },
  { key: 'calories', label: 'Calories', unit: 'kcal', color: '#ff8a65' },
  { key: 'distance', label: 'Distance', unit: 'km', color: '#6ce0a3' },
  { key: 'spo2', label: 'SpO₂', unit: '%', color: '#7a6cff' },
  { key: 'temperature', label: 'Temperature', unit: '°C', color: '#f7c948' },
  { key: 'syncCount', label: 'Sync', unit: 'count', color: '#58d1c9' }
];

const signalGroups = [
  {
    title: 'Activity',
    source: 'Samsung Health → Health Connect',
    color: '#4facfe',
    signals: [
      { label: 'Steps', detail: 'Daily total', status: 'received' },
      { label: 'Distance', detail: 'Walking and running distance', status: 'received' },
      { label: 'Active calories', detail: 'Energy burned during activity', status: 'payload' },
      { label: 'Exercise sessions', detail: 'Workout type, duration, distance and calories', status: 'pending' }
    ]
  },
  {
    title: 'Heart & vitals',
    source: 'Watch sensors / Samsung Health',
    color: '#e85d5d',
    signals: [
      { label: 'Heart rate', detail: 'Spot readings and exercise series', status: 'pending' },
      { label: 'Resting heart rate', detail: 'Daily resting baseline', status: 'pending' },
      { label: 'Heart-rate variability', detail: 'Where supported by the source', status: 'pending' },
      { label: 'Blood oxygen (SpO₂)', detail: 'Oxygen saturation samples', status: 'payload' },
      { label: 'Skin temperature', detail: 'Sensor-derived skin temperature', status: 'pending' },
      { label: 'Body temperature', detail: 'Samsung Health temperature record', status: 'pending' },
      { label: 'Irregular rhythm notification', detail: 'Notification record; not a raw ECG waveform', status: 'pending' },
      { label: 'ECG waveform', detail: 'Not part of this standard collector contract', status: 'unsupported' }
    ]
  },
  {
    title: 'Sleep & recovery',
    source: 'Samsung Health → Health Connect',
    color: '#7a6cff',
    signals: [
      { label: 'Sleep duration', detail: 'Total sleep interval', status: 'pending' },
      { label: 'Sleep stages', detail: 'Awake, light, deep and REM where available', status: 'pending' },
      { label: 'Sleep score', detail: 'Samsung Health-derived score', status: 'pending' },
      { label: 'Sleep apnea', detail: 'Where recorded and permitted', status: 'pending' },
      { label: 'Sleep goal', detail: 'Configured target', status: 'pending' },
      { label: 'Stress / energy score', detail: 'Samsung Health-derived wellness metrics', status: 'pending' }
    ]
  },
  {
    title: 'Body measurements',
    source: 'Samsung Health / Health Connect permissions',
    color: '#f7c948',
    signals: [
      { label: 'Body composition', detail: 'BIA-derived body fat, muscle and water', status: 'pending' },
      { label: 'Blood pressure', detail: 'Requires supported device, calibration and permissions', status: 'pending' },
      { label: 'Blood glucose', detail: 'Usually external/manual source, not a watch sensor', status: 'pending' },
      { label: 'Weight and height', detail: 'Profile or connected scale data', status: 'pending' },
      { label: 'Floors climbed', detail: 'Barometer-derived activity metric', status: 'pending' }
    ]
  },
  {
    title: 'Nutrition & goals',
    source: 'Samsung Health records',
    color: '#58d1c9',
    signals: [
      { label: 'Nutrition', detail: 'Food and nutrient records', status: 'pending' },
      { label: 'Water intake', detail: 'Logged hydration records', status: 'pending' },
      { label: 'Activity and step goals', detail: 'Configured Samsung Health targets', status: 'pending' },
      { label: 'Active time goals', detail: 'Configured Samsung Health target', status: 'pending' }
    ]
  }
];

const signalStatus = {
  received: { label: 'Received', className: 'received' },
  payload: { label: 'Payload field', className: 'payload' },
  pending: { label: 'Collector needed', className: 'pending' },
  unsupported: { label: 'Not exposed here', className: 'unsupported' }
};

function formatTimestamp(ms) {
  if (!ms) return '—';
  return new Date(ms).toLocaleString();
}

function formatDateTime(value) {
  if (!value) return '—';
  const date = new Date(value);
  return isNaN(date) ? '—' : date.toLocaleString();
}

function getTrend(current, previous) {
  if (current == null || previous == null || previous === 0) return null;
  const delta = current - previous;
  const pct = (delta / previous) * 100;
  return { delta, pct };
}

async function fetchJson(url) {
  const response = await fetch(`${API_BASE_URL}${url}`, { credentials: 'include' });
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }
  if (response.status === 204) return null;
  return response.json();
}

export default function App() {
  const [latestRecord, setLatestRecord] = useState(null);
  const [records, setRecords] = useState([]);
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [range, setRange] = useState(24);
  const [status, setStatus] = useState('demo');

  const refreshData = async () => {
    try {
      setError('');
      const [latest, history, summaryData] = await Promise.all([
        fetchJson(`/api/health-records/latest?userId=${SAMPLE_USER_ID}`),
        fetchJson(`/api/health-records/history?userId=${SAMPLE_USER_ID}&hours=${range}`),
        fetchJson(`/api/health-records/summary?userId=${SAMPLE_USER_ID}`)
      ]);

      setLatestRecord(latest || null);
      setRecords(Array.isArray(history) ? history : []);
      setSummary(summaryData || null);
      setStatus(latest ? 'live' : 'demo');
      setLoading(false);
    } catch (err) {
      setError(err.message || 'Unable to load health data');
      setStatus('disconnected');
      setLoading(false);
    }
  };

  useEffect(() => {
    refreshData();
    const intervalId = setInterval(refreshData, 5000);
    return () => clearInterval(intervalId);
  }, [range]);

  useEffect(() => {
    const source = new EventSource(`${API_BASE_URL}/health-records/events`);
    source.onmessage = (event) => {
      try {
        const record = JSON.parse(event.data);
        setLatestRecord(record);
        setStatus('live');
        setRecords((current) => [record, ...current.filter((item) => item.id !== record.id)].slice(0, 20));
      } catch {
        setStatus('live');
      }
    };
    source.onerror = () => {
      setStatus('stale');
    };
    return () => source.close();
  }, []);

  const metricCards = useMemo(() => {
    if (!latestRecord) return metricConfig.map((metric) => ({ ...metric, value: null, previous: null }));
    const current = latestRecord;
    const previous = records[1] || null;
    return metricConfig.map((metric) => {
      const keys = { steps: 'steps', calories: 'calories', distance: 'distance', spo2: 'spo2', temperature: 'temperature', syncCount: 'syncCount' };
      const currentValue = current[keys[metric.key]];
      const previousValue = previous ? previous[keys[metric.key]] : null;
      return {
        ...metric,
        value: currentValue,
        previous: previousValue,
        trend: getTrend(currentValue, previousValue)
      };
    });
  }, [latestRecord, records]);

  const chartData = useMemo(() => {
    return [...records].reverse().map((row) => ({
      time: formatDateTime(row.eventTime),
      steps: row.steps,
      calories: row.calories,
      distance: row.distance / 1000,
      spo2: row.spo2,
      temperature: row.temperature,
      syncCount: row.syncCount
    }));
  }, [records]);

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <p className="eyebrow">Health Digital Twin</p>
          <h1>Samsung Galaxy Watch 9 • Digital Twin Dashboard</h1>
        </div>
        <div className="status-cluster">
          <div className={`status-badge ${status}`}>
            {status === 'live' ? 'Live' : status === 'stale' ? 'Stale' : status === 'disconnected' ? 'Disconnected' : 'Demo'}
          </div>
          <div className="tiny-meta">Connected device: Samsung Galaxy Watch 9</div>
          <div className="tiny-meta">Data source: Galaxy Phone</div>
        </div>
      </header>

      <main className="dashboard">
        <section className="summary-row">
          <div className="summary-card"><span>Backend/cloud</span><strong>{status === 'live' ? 'Available' : status === 'demo' ? 'Demo mode' : 'Unavailable'}</strong></div>
          <div className="summary-card"><span>Last successful sync</span><strong>{latestRecord ? formatTimestamp(latestRecord.eventTimestamp) : 'No data yet'}</strong></div>
          <div className="summary-card"><span>Last received reading</span><strong>{latestRecord ? formatTimestamp(latestRecord.eventTimestamp) : '—'}</strong></div>
        </section>

        <section className="metrics-grid">
          {metricCards.map((metric) => (
            <article key={metric.key} className="metric-card">
              <div className="metric-head">
                <span>{metric.label}</span>
                <span className="metric-icon" style={{ background: metric.color }}>{metric.label.slice(0, 1)}</span>
              </div>
              <div className="metric-value">
                {metric.value == null ? '—' : metric.value.toFixed(metric.key === 'distance' || metric.key === 'temperature' ? 1 : 0)}
                <small>{metric.unit}</small>
              </div>
              <div className="trend-row">
                {(metric.trend && metric.trend.delta !== 0) ? (
                  <span className={metric.trend.delta >= 0 ? 'up' : 'down'}>
                    {metric.trend.delta >= 0 ? '▲' : '▼'} {Math.abs(metric.trend.delta).toFixed(metric.key === 'distance' || metric.key === 'temperature' ? 1 : 0)}
                  </span>
                ) : <span className="neutral">No recent trend</span>}
                <span className="muted">updated {latestRecord ? formatDateTime(latestRecord.eventTime) : '—'}</span>
              </div>
              <svg viewBox="0 0 120 28" className="sparkline" preserveAspectRatio="none">
                <path d="M0,18 L20,16 L40,15 L60,12 L80,14 L100,8 L120,10" stroke={metric.color} strokeWidth="2" fill="none"/>
              </svg>
            </article>
          ))}
        </section>

        <section className="main-layout">
          <div className="panel twin-panel">
            <div className="panel-header">
              <h2>Human Digital Twin</h2>
              <span className="panel-tag">Conceptual visualization • not clinical diagnosis</span>
            </div>
            <div className="digital-twin">
              <svg viewBox="0 0 240 280" aria-label="Human digital twin illustration">
                <circle cx="120" cy="44" r="30" fill="#5ac0d8" opacity="0.9"/>
                <path d="M120 78 L95 124 L90 185 L120 210 L150 185 L145 124 Z" fill="#88d6f5" opacity="0.8"/>
                <path d="M120 78 L82 95 L70 135 L95 150 L120 125 Z" fill="#7cbaf0" opacity="0.8"/>
                <path d="M120 78 L158 95 L170 135 L145 150 L120 125 Z" fill="#7cbaf0" opacity="0.8"/>
                <line x1="120" y1="205" x2="92" y2="250" stroke="#88d6f5" strokeWidth="8"/>
                <line x1="120" y1="205" x2="148" y2="250" stroke="#88d6f5" strokeWidth="8"/>
                <line x1="95" y1="124" x2="62" y2="152" stroke="#88d6f5" strokeWidth="8"/>
                <line x1="145" y1="124" x2="178" y2="152" stroke="#88d6f5" strokeWidth="8"/>
              </svg>
            </div>
            <div className="twin-metrics">
              <div><span>SpO₂</span><strong>{latestRecord ? `${latestRecord.spo2}%` : '—'}</strong></div>
              <div><span>Temperature</span><strong>{latestRecord ? `${latestRecord.temperature.toFixed(1)}°C` : '—'}</strong></div>
              <div><span>Sync count</span><strong>{latestRecord ? latestRecord.syncCount : '—'}</strong></div>
            </div>
          </div>

          <div className="panel charts-panel">
            <div className="panel-header">
              <h2>Health trends</h2>
              <div className="range-selector">
                {[1, 6, 24, 168].map((hours) => (
                  <button key={hours} className={hours === range ? 'active' : ''} onClick={() => setRange(hours)}>
                    {hours === 168 ? '7d' : hours === 24 ? '24h' : hours === 6 ? '6h' : '1h'}
                  </button>
                ))}
              </div>
            </div>
            <div className="chart-wrap">
              <ResponsiveContainer width="100%" height={240}>
                <AreaChart data={chartData}>
                  <defs>
                    <linearGradient id="stepsFill" x1="0" x2="0" y1="0" y2="1">
                      <stop offset="5%" stopColor="#4facfe" stopOpacity={0.7} />
                      <stop offset="95%" stopColor="#4facfe" stopOpacity={0.1} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e9eef7" />
                  <XAxis dataKey="time" tick={{ fontSize: 10 }} interval={0} angle={-30} textAnchor="end" height={50} />
                  <YAxis />
                  <Tooltip />
                  <Legend />
                  <Area type="monotone" dataKey="steps" stroke="#4facfe" fill="url(#stepsFill)" name="Steps" />
                </AreaChart>
              </ResponsiveContainer>
            </div>
          </div>
        </section>

        <section className="panel ecg-panel">
          <div className="panel-header">
            <div>
              <h2>ECG Signal</h2>
              <p className="panel-subtitle">Conceptual arrhythmia waveform</p>
            </div>
            <span className="availability-badge">Conceptual only</span>
          </div>
          <div className="ecg-card">
            <div className="ecg-grid" aria-hidden="true">
              <svg viewBox="0 0 1000 180" preserveAspectRatio="none" role="img" aria-label="Conceptual ECG waveform">
                <path d="M0 90 H80 L90 90 L100 90 L120 90 L135 50 L145 130 L155 90 L175 90 L200 90 L215 90 L240 50 L260 130 L280 90 L310 90 L345 90 L360 90 L395 50 L420 130 L440 90 L455 90 L500 90 L520 90 L540 80 L555 120 L570 90 L590 90 L625 90 L660 50 L680 130 L705 90 L730 90 L750 90 L790 60 L810 120 L840 90 L900 90 L1000 90" className="ecg-wave" />
                <path d="M0 90 H1000" className="ecg-baseline" />
              </svg>
            </div>
            <div className="ecg-copy">
              <strong>Waveform placeholder for future ECG instrumentation.</strong>
              <span>No ECG signal is currently present in the phone payload, so this panel is a design placeholder and not a clinical reading.</span>
            </div>
          </div>
        </section>

        <section className="panel signal-catalog-panel">
          <div className="panel-header">
            <div>
              <h2>Samsung Health signal catalog</h2>
              <p className="panel-subtitle">What the watch ecosystem can expose versus what this collector currently sends</p>
            </div>
            <span className="panel-tag">Permission and device support vary</span>
          </div>
          <div className="signal-groups">
            {signalGroups.map((group) => (
              <article className="signal-group" key={group.title}>
                <div className="signal-group-header">
                  <span className="signal-group-dot" style={{ background: group.color }} />
                  <div>
                    <h3>{group.title}</h3>
                    <span>{group.source}</span>
                  </div>
                </div>
                <div className="signal-list">
                  {group.signals.map((signal) => {
                    const state = signalStatus[signal.status];
                    return (
                      <div className="signal-row" key={signal.label}>
                        <div>
                          <strong>{signal.label}</strong>
                          <span>{signal.detail}</span>
                        </div>
                        <span className={`signal-status ${state.className}`}>{state.label}</span>
                      </div>
                    );
                  })}
                </div>
              </article>
            ))}
          </div>
          <p className="catalog-note">
            “Collector needed” means the dashboard does not claim the value until the phone app adds it to the POST contract.
            Samsung Health is the source of truth for Samsung-derived metrics; Health Connect availability depends on the
            Samsung Health sync setting and granted permissions.
          </p>
        </section>

        <section className="table-panel panel">
          <div className="panel-header">
            <h2>Recent measurements</h2>
          </div>
          {loading ? <div className="state">Loading records…</div> : error ? <div className="state error">{error}</div> : records.length === 0 ? <div className="state">No measurements yet</div> : <div className="table-wrap"><table>
            <thead>
              <tr>
                <th>Timestamp</th>
                <th>User ID</th>
                <th>Steps</th>
                <th>Calories</th>
                <th>Distance</th>
                <th>SpO₂</th>
                <th>Temp</th>
                <th>Sync</th>
              </tr>
            </thead>
            <tbody>
              {records.slice(0, 8).map((record) => (
                <tr key={record.id || record.eventTimestamp}>
                  <td>{formatDateTime(record.eventTime)}</td>
                  <td>{record.userId}</td>
                  <td>{record.steps}</td>
                  <td>{record.calories}</td>
                  <td>{(record.distance / 1000).toFixed(2)} km</td>
                  <td>{record.spo2}%</td>
                  <td>{record.temperature.toFixed(1)}°C</td>
                  <td>{record.syncCount}</td>
                </tr>
              ))}
            </tbody>
          </table></div> }
        </section>

        <section className="pipeline panel">
          <div className="panel-header"><h2>Connection & Data Flow</h2></div>
          <div className="pipeline-row">
            <span>Watch</span><span>→</span><span>Phone</span><span>→</span><span>REST API</span><span>→</span><span>Database</span><span>→</span><span>Dashboard</span>
          </div>
          <div className="pipeline-status">
            <div><small>Last successful POST</small><strong>{latestRecord ? formatTimestamp(latestRecord.eventTimestamp) : 'Pending'}</strong></div>
            <div><small>API</small><strong>{status === 'live' ? 'Receiving data' : status === 'stale' ? 'Stale response' : status === 'disconnected' ? 'Disconnected' : 'Demo mode'}</strong></div>
            <div><small>DB</small><strong>{latestRecord ? 'Persisted' : 'No data on file'}</strong></div>
          </div>
        </section>
      </main>
    </div>
  );
}
