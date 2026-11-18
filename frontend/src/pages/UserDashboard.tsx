import React, { useEffect, useState } from 'react';
import { QueueToken } from '../types';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../hooks/useWebSocket';
import { Link } from 'react-router-dom';
import {
  Clock,
  Users,
  BellRing,
  CheckCircle,
  XCircle,
  AlertTriangle,
  ArrowRight,
  LogOut,
  MapPin,
  Building,
  Radio,
  Sliders,
  X
} from 'lucide-react';

export const UserDashboard: React.FC = () => {
  const { user } = useAuth();
  const [activeToken, setActiveToken] = useState<QueueToken | null>(null);
  const [historyTokens, setHistoryTokens] = useState<QueueToken[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [thresholdModalOpen, setThresholdModalOpen] = useState<boolean>(false);
  const [selectedThreshold, setSelectedThreshold] = useState<number>(10);
  const [updatingThreshold, setUpdatingThreshold] = useState<boolean>(false);

  // Subscribe to user WebSocket topic for live token updates
  const topic = user ? `/topic/user/${user.id}` : null;
  useWebSocket(topic, (message) => {
    if (message.token) {
      if (['WAITING', 'CALLED', 'SERVING'].includes(message.token.status)) {
        setActiveToken(message.token);
      } else {
        setActiveToken(null);
        fetchHistory();
      }
    }
  });

  useEffect(() => {
    fetchActiveToken();
    fetchHistory();
  }, []);

  const fetchActiveToken = async () => {
    try {
      setLoading(true);
      const res = await api.get('/tokens/active');
      if (res.data.success) {
        setActiveToken(res.data.data);
        if (res.data.data) {
          setSelectedThreshold(res.data.data.notifyThreshold || 10);
        }
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const fetchHistory = async () => {
    try {
      const res = await api.get('/tokens/history');
      if (res.data.success) {
        setHistoryTokens(res.data.data.filter((t: QueueToken) => !['WAITING', 'CALLED', 'SERVING'].includes(t.status)));
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleCancelToken = async () => {
    if (!activeToken) return;
    if (!window.confirm('Are you sure you want to leave and cancel this token?')) return;

    try {
      const res = await api.post(`/tokens/${activeToken.id}/cancel`);
      if (res.data.success) {
        setActiveToken(null);
        fetchHistory();
      }
    } catch (e: any) {
      alert(e.response?.data?.message || 'Failed to cancel token');
    }
  };

  const handleSaveThreshold = async () => {
    if (!activeToken) return;
    setUpdatingThreshold(true);
    try {
      const res = await api.put(`/tokens/${activeToken.id}/leave-return`, null, {
        params: { threshold: selectedThreshold },
      });
      if (res.data.success) {
        setActiveToken(res.data.data);
        setThresholdModalOpen(false);
      }
    } catch (e) {
      console.error(e);
    } finally {
      setUpdatingThreshold(false);
    }
  };

  return (
    <div className="space-y-8">
      {/* Welcome Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 dark:text-white">
            Good morning, {user?.fullName?.split(' ')[0]} 👋
          </h1>
          <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
            Monitor your live queue position and manage your digital turn.
          </p>
        </div>

        <Link
          to="/explore"
          className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl text-sm font-semibold text-white bg-brand-600 hover:bg-brand-700 shadow-md shadow-brand-500/20 transition self-start sm:self-auto"
        >
          Join Another Queue <ArrowRight className="w-4 h-4" />
        </Link>
      </div>

      {/* Active Queue Card */}
      {loading ? (
        <div className="h-64 rounded-3xl bg-gray-200 dark:bg-gray-800 animate-pulse" />
      ) : activeToken ? (
        <div className="relative overflow-hidden bg-white dark:bg-gray-900 rounded-3xl p-6 sm:p-8 border border-gray-200 dark:border-gray-800 shadow-xl space-y-6">
          {/* Header */}
          <div className="flex items-start justify-between gap-4 pb-6 border-b border-gray-100 dark:border-gray-800">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wide bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-400">
                  <Radio className="w-3 h-3 animate-pulse" /> Live Active Queue
                </span>
                <span className="text-xs text-gray-400 font-medium">{activeToken.organizationName}</span>
              </div>
              <h2 className="text-2xl font-black text-gray-900 dark:text-white">{activeToken.serviceName}</h2>
              <p className="text-xs text-gray-500 dark:text-gray-400 mt-0.5">{activeToken.departmentName}</p>
            </div>

            <div className="text-right">
              <span className="text-xs font-semibold text-gray-400 uppercase tracking-wider block">Token ID</span>
              <span className="text-4xl sm:text-5xl font-black text-brand-600 dark:text-brand-400 tracking-tight">
                {activeToken.tokenNumber}
              </span>
            </div>
          </div>

          {/* Status highlight / Circular Visualization */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-center">
            {/* Circular Wait Progress */}
            <div className="flex flex-col items-center justify-center p-6 rounded-2xl bg-brand-50/50 dark:bg-brand-950/30 border border-brand-100 dark:border-brand-900/40 text-center">
              <div className="relative w-36 h-36 flex items-center justify-center">
                <svg className="w-full h-full transform -rotate-90">
                  <circle
                    cx="72"
                    cy="72"
                    r="60"
                    className="stroke-gray-200 dark:stroke-gray-800"
                    strokeWidth="10"
                    fill="transparent"
                  />
                  <circle
                    cx="72"
                    cy="72"
                    r="60"
                    className="stroke-brand-600 dark:stroke-brand-400 transition-all duration-1000"
                    strokeWidth="10"
                    strokeDasharray={377}
                    strokeDashoffset={Math.max(0, 377 - (377 * (Math.max(1, 30 - activeToken.peopleAhead) / 30)))}
                    strokeLinecap="round"
                    fill="transparent"
                  />
                </svg>
                <div className="absolute inset-0 flex flex-col items-center justify-center">
                  <span className="text-3xl font-black text-gray-900 dark:text-white">{activeToken.peopleAhead}</span>
                  <span className="text-[10px] font-bold uppercase tracking-wider text-gray-400">Ahead</span>
                </div>
              </div>
              <span className="mt-3 text-xs font-semibold text-brand-700 dark:text-brand-300">
                {activeToken.status === 'CALLED'
                  ? '📢 YOUR TURN! PROCEED TO COUNTER'
                  : activeToken.status === 'SERVING'
                  ? '⚡ CURRENTLY BEING SERVED'
                  : 'Tokens Ahead in Queue'}
              </span>
            </div>

            {/* Metrics */}
            <div className="md:col-span-2 space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/50 border border-gray-100 dark:border-gray-800">
                  <span className="text-xs font-medium text-gray-500 flex items-center gap-1.5 mb-1">
                    <Clock className="w-4 h-4 text-brand-500" /> Estimated Wait
                  </span>
                  <span className="text-2xl font-bold text-gray-900 dark:text-white">
                    ~{activeToken.estimatedWaitMinutes} <span className="text-sm font-normal text-gray-400">mins</span>
                  </span>
                </div>

                <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/50 border border-gray-100 dark:border-gray-800">
                  <span className="text-xs font-medium text-gray-500 flex items-center gap-1.5 mb-1">
                    <Building className="w-4 h-4 text-brand-500" /> Assigned Counter
                  </span>
                  <span className="text-2xl font-bold text-gray-900 dark:text-white">
                    {activeToken.counterNumber || 'Counter --'}
                  </span>
                </div>
              </div>

              {/* Leave & Return Feature card */}
              <div className="p-4 rounded-2xl bg-indigo-50/50 dark:bg-indigo-950/30 border border-indigo-100 dark:border-indigo-900/40 flex items-center justify-between gap-4">
                <div className="flex items-center gap-3">
                  <div className="p-2.5 rounded-xl bg-indigo-600 text-white shadow-md shadow-indigo-500/20">
                    <BellRing className="w-5 h-5" />
                  </div>
                  <div>
                    <h4 className="text-sm font-bold text-gray-900 dark:text-white">Leave & Return Reminders</h4>
                    <p className="text-xs text-gray-500">
                      Alert when <strong className="text-indigo-600 dark:text-indigo-400">{activeToken.notifyThreshold} people</strong> remain.
                    </p>
                  </div>
                </div>

                <button
                  onClick={() => setThresholdModalOpen(true)}
                  className="px-3.5 py-2 rounded-xl text-xs font-bold text-indigo-600 dark:text-indigo-400 bg-white dark:bg-gray-800 border border-indigo-200 dark:border-indigo-800 hover:bg-indigo-50 transition flex items-center gap-1.5"
                >
                  <Sliders className="w-3.5 h-3.5" /> Configure
                </button>
              </div>

              {/* Action buttons */}
              <div className="flex items-center gap-3 pt-2">
                <button
                  onClick={handleCancelToken}
                  className="px-4 py-2.5 rounded-xl text-xs font-bold text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950/40 hover:bg-red-100 transition flex items-center gap-1.5"
                >
                  <XCircle className="w-4 h-4" /> Cancel Token
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="bg-white dark:bg-gray-900 rounded-3xl p-8 sm:p-12 border border-gray-200 dark:border-gray-800 text-center space-y-4">
          <div className="w-16 h-16 rounded-2xl bg-brand-50 dark:bg-brand-950 text-brand-600 dark:text-brand-400 mx-auto flex items-center justify-center">
            <Users className="w-8 h-8" />
          </div>
          <div>
            <h3 className="text-xl font-bold text-gray-900 dark:text-white">No Active Queue Token</h3>
            <p className="text-sm text-gray-500 max-w-md mx-auto mt-1">
              You are not standing in any queue right now. Explore organizations to get your digital queue token.
            </p>
          </div>
          <Link
            to="/explore"
            className="inline-flex items-center gap-2 px-6 py-3 rounded-2xl text-sm font-bold text-white bg-brand-600 hover:bg-brand-700 shadow-lg shadow-brand-500/25 transition"
          >
            Find Organization & Service <ArrowRight className="w-4 h-4" />
          </Link>
        </div>
      )}

      {/* History section */}
      <div className="space-y-4">
        <h2 className="text-xl font-bold text-gray-900 dark:text-white">Recent Queue History</h2>

        <div className="bg-white dark:bg-gray-900 rounded-3xl border border-gray-200 dark:border-gray-800 overflow-hidden shadow-sm">
          {historyTokens.length === 0 ? (
            <div className="p-8 text-center text-gray-400 text-sm">No completed queue history yet.</div>
          ) : (
            <div className="divide-y divide-gray-100 dark:divide-gray-800">
              {historyTokens.map((t) => (
                <div key={t.id} className="p-4 sm:p-5 flex items-center justify-between gap-4">
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <span className="font-mono font-bold text-brand-600 dark:text-brand-400 text-sm">
                        {t.tokenNumber}
                      </span>
                      <h4 className="font-semibold text-gray-900 dark:text-white text-sm">{t.serviceName}</h4>
                    </div>
                    <p className="text-xs text-gray-500">{t.organizationName} • {new Date(t.createdAt).toLocaleDateString()}</p>
                  </div>

                  <div className="flex items-center gap-4 text-xs font-semibold">
                    <span className="text-gray-500">
                      Duration: {t.actualServiceDurationSeconds ? Math.ceil(t.actualServiceDurationSeconds / 60) + 'm' : '5m'}
                    </span>
                    <span
                      className={`px-3 py-1 rounded-full text-[11px] font-bold uppercase tracking-wider ${
                        t.status === 'COMPLETED'
                          ? 'bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-400'
                          : t.status === 'CANCELLED'
                          ? 'bg-red-100 dark:bg-red-950/60 text-red-700 dark:text-red-400'
                          : 'bg-amber-100 dark:bg-amber-950/60 text-amber-700 dark:text-amber-400'
                      }`}
                    >
                      {t.status}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Leave & Return Modal */}
      {thresholdModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-gray-900 rounded-3xl max-w-md w-full p-6 space-y-6 relative border border-gray-200 dark:border-gray-800">
            <button
              onClick={() => setThresholdModalOpen(false)}
              className="absolute top-4 right-4 p-2 text-gray-400 hover:text-gray-600 dark:hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>

            <div className="space-y-2">
              <div className="w-10 h-10 rounded-xl bg-indigo-100 dark:bg-indigo-950 text-indigo-600 dark:text-indigo-400 flex items-center justify-center">
                <BellRing className="w-5 h-5" />
              </div>
              <h3 className="text-xl font-bold text-gray-900 dark:text-white">Leave & Return Reminder</h3>
              <p className="text-xs text-gray-500">
                You can leave the venue! Select how many people ahead trigger an automatic return alert on your device.
              </p>
            </div>

            <div className="space-y-3">
              {[15, 10, 5, 2].map((num) => (
                <label
                  key={num}
                  className={`flex items-center justify-between p-3.5 rounded-2xl border cursor-pointer transition ${
                    selectedThreshold === num
                      ? 'bg-indigo-50/60 dark:bg-indigo-950/40 border-indigo-500 text-indigo-900 dark:text-indigo-200 font-bold'
                      : 'border-gray-200 dark:border-gray-800 text-gray-700 dark:text-gray-300'
                  }`}
                >
                  <span className="text-sm">Notify me when {num} people remain ahead</span>
                  <input
                    type="radio"
                    name="threshold"
                    value={num}
                    checked={selectedThreshold === num}
                    onChange={() => setSelectedThreshold(num)}
                    className="accent-indigo-600"
                  />
                </label>
              ))}
            </div>

            <button
              onClick={handleSaveThreshold}
              disabled={updatingThreshold}
              className="w-full py-3 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm shadow-md shadow-indigo-500/20 transition disabled:opacity-50"
            >
              {updatingThreshold ? 'Saving Preferences...' : 'Save & Active Reminders'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
