import React, { useEffect, useState } from 'react';
import { Counter, QueueToken, Organization } from '../types';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import { useWebSocket } from '../hooks/useWebSocket';
import {
  Play,
  CheckCircle,
  SkipForward,
  UserX,
  Volume2,
  Pause,
  Clock,
  Users,
  Building,
  Radio,
  RotateCcw,
  Sparkles,
  Building2,
  Check,
  Plus,
  Trash2,
  X
} from 'lucide-react';

export const StaffDashboard: React.FC = () => {
  const { user } = useAuth();
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [selectedOrg, setSelectedOrg] = useState<Organization | null>(null);
  const [counters, setCounters] = useState<Counter[]>([]);
  const [selectedCounter, setSelectedCounter] = useState<Counter | null>(null);
  const [currentToken, setCurrentToken] = useState<QueueToken | null>(null);
  const [waitingTokens, setWaitingTokens] = useState<QueueToken[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [actionLoading, setActionLoading] = useState<boolean>(false);
  const [message, setMessage] = useState<string>('');
  const [showAddCounterModal, setShowAddCounterModal] = useState<boolean>(false);
  const [showDeleteConfirmModal, setShowDeleteConfirmModal] = useState<boolean>(false);
  const [newCounterName, setNewCounterName] = useState<string>('');
  const [newCounterNumber, setNewCounterNumber] = useState<string>('');

  const fetchWaitingTokens = async (counterId: number) => {
    try {
      const res = await api.get(`/staff/counters/${counterId}/waiting-tokens`);
      if (res.data.success) {
        setWaitingTokens(res.data.data);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const topic = selectedCounter ? `/topic/counter/${selectedCounter.id}` : null;
  useWebSocket(topic, (msg) => {
    if (msg.token) {
      setCurrentToken(msg.token);
    }
    if (selectedCounter) {
      fetchWaitingTokens(selectedCounter.id);
    }
  });

  useEffect(() => {
    if (selectedCounter) {
      fetchWaitingTokens(selectedCounter.id);
      const interval = setInterval(() => {
        fetchWaitingTokens(selectedCounter.id);
      }, 4000);
      return () => clearInterval(interval);
    }
  }, [selectedCounter]);

  useEffect(() => {
    fetchOrganizations();
  }, [user]);

  useEffect(() => {
    if (selectedOrg) {
      fetchCounters(selectedOrg.id);
    }
  }, [selectedOrg]);

  const fetchOrganizations = async () => {
    try {
      setLoading(true);
      const res = await api.get('/organizations');
      if (res.data.success) {
        let orgs: Organization[] = res.data.data;
        if ((user?.role === 'STAFF' || user?.role === 'ADMIN') && user?.organizationId) {
          orgs = orgs.filter((o) => o.id === user.organizationId);
        }
        setOrganizations(orgs);
        if (orgs.length > 0) {
          const userOrg = user?.organizationId
            ? orgs.find((o) => o.id === user.organizationId) || orgs[0]
            : orgs[0];
          setSelectedOrg(userOrg);
        }
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const fetchCounters = async (orgId: number) => {
    try {
      setLoading(true);
      const res = await api.get(`/organizations/${orgId}/counters`);
      if (res.data.success) {
        const counterList: Counter[] = res.data.data;
        if (counterList.length > 0) {
          setCounters(counterList);
          const assignedCounter = user?.id
            ? counterList.find((cnt) => cnt.assignedStaffId === user.id)
            : null;
          const targetCounter = assignedCounter || counterList[0];
          setSelectedCounter(targetCounter);
          if (targetCounter.currentToken) {
            setCurrentToken(targetCounter.currentToken as QueueToken);
          } else {
            setCurrentToken(null);
          }
        } else {
          // Attempt auto-initialization of default counter for organization
          try {
            const initRes = await api.post(`/organizations/${orgId}/initialize-counter`);
            if (initRes.data.success && initRes.data.data) {
              const newCnt: Counter = initRes.data.data;
              setCounters([newCnt]);
              setSelectedCounter(newCnt);
              setCurrentToken(null);
              return;
            }
          } catch (initErr) {
            console.error('Auto-initialize counter failed:', initErr);
          }
          setCounters([]);
          setSelectedCounter(null);
          setCurrentToken(null);
        }
      }
    } catch (e) {
      console.error(e);
      setCounters([]);
      setSelectedCounter(null);
      setCurrentToken(null);
    } finally {
      setLoading(false);
    }
  };

  const handleInitializeCounter = async () => {
    if (!selectedOrg) return;
    setActionLoading(true);
    setMessage('');
    try {
      const res = await api.post(`/organizations/${selectedOrg.id}/initialize-counter`);
      if (res.data.success) {
        setMessage('New counter initialized successfully!');
        await fetchCounters(selectedOrg.id);
      }
    } catch (err: any) {
      setMessage(err.response?.data?.message || 'Error initializing counter');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCreateCounterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCounterName.trim() || !selectedOrg) return;
    setActionLoading(true);
    setMessage('');
    try {
      const res = await api.post('/counters', {
        name: newCounterName.trim(),
        counterNumber: newCounterNumber.trim() || undefined,
      });
      if (res.data.success) {
        setMessage(`Counter "${res.data.data.name}" created successfully!`);
        setNewCounterName('');
        setNewCounterNumber('');
        setShowAddCounterModal(false);
        await fetchCounters(selectedOrg.id);
      }
    } catch (err: any) {
      setMessage(err.response?.data?.message || 'Error creating counter');
    } finally {
      setActionLoading(false);
    }
  };

  const handleDeleteCounter = async () => {
    if (!selectedCounter || !selectedOrg) return;
    setActionLoading(true);
    setMessage('');
    try {
      const res = await api.delete(`/counters/${selectedCounter.id}`);
      if (res.data.success) {
        setMessage(`Counter "${selectedCounter.name}" deleted successfully!`);
        setShowDeleteConfirmModal(false);
        setSelectedCounter(null);
        await fetchCounters(selectedOrg.id);
      }
    } catch (err: any) {
      setMessage(err.response?.data?.message || 'Error deleting counter');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCallNext = async () => {
    if (!selectedCounter) return;
    setActionLoading(true);
    setMessage('');
    try {
      const res = await api.post(`/staff/counters/${selectedCounter.id}/call-next`);
      if (res.data.success) {
        setCurrentToken(res.data.data);
        setMessage('Called next token successfully!');
        fetchWaitingTokens(selectedCounter.id);
      }
    } catch (err: any) {
      setMessage(err.response?.data?.message || 'No waiting tokens in queue');
    } finally {
      setActionLoading(false);
    }
  };

  const handleStartService = async () => {
    if (!currentToken) return;
    setActionLoading(true);
    try {
      const res = await api.post(`/staff/tokens/${currentToken.id}/start-service`);
      if (res.data.success) {
        setCurrentToken(res.data.data);
        setMessage('Service started');
        if (selectedCounter) fetchWaitingTokens(selectedCounter.id);
      }
    } catch (err: any) {
      setMessage(err.response?.data?.message || 'Error starting service');
    } finally {
      setActionLoading(false);
    }
  };

  const handleCompleteToken = async () => {
    if (!currentToken) return;
    setActionLoading(true);
    try {
      const res = await api.post(`/staff/tokens/${currentToken.id}/complete`);
      if (res.data.success) {
        setCurrentToken(null);
        setMessage('Token completed! Ready for next customer.');
        if (selectedCounter) fetchWaitingTokens(selectedCounter.id);
      }
    } catch (err: any) {
      setMessage(err.response?.data?.message || 'Error completing token');
    } finally {
      setActionLoading(false);
    }
  };

  const handleSkipToken = async () => {
    if (!currentToken) return;
    setActionLoading(true);
    try {
      const res = await api.post(`/staff/tokens/${currentToken.id}/skip`);
      if (res.data.success) {
        setCurrentToken(null);
        setMessage('Token skipped');
        if (selectedCounter) fetchWaitingTokens(selectedCounter.id);
      }
    } catch (err: any) {
      setMessage(err.response?.data?.message || 'Error skipping token');
    } finally {
      setActionLoading(false);
    }
  };

  const handleNoShowToken = async () => {
    if (!currentToken) return;
    setActionLoading(true);
    try {
      const res = await api.post(`/staff/tokens/${currentToken.id}/no-show`);
      if (res.data.success) {
        setCurrentToken(null);
        setMessage('Marked customer as NO_SHOW');
        if (selectedCounter) fetchWaitingTokens(selectedCounter.id);
      }
    } catch (err: any) {
      setMessage(err.response?.data?.message || 'Error marking no-show');
    } finally {
      setActionLoading(false);
    }
  };

  const handleToggleCounterStatus = async (status: 'OPEN' | 'PAUSED' | 'CLOSED') => {
    if (!selectedCounter) return;
    try {
      const res = await api.patch(`/counters/${selectedCounter.id}/status`, null, {
        params: { status },
      });
      if (res.data.success) {
        setSelectedCounter(res.data.data);
        if (selectedOrg) fetchCounters(selectedOrg.id);
      }
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 dark:text-white flex items-center gap-2">
              Staff Counter Control Panel <Sparkles className="w-6 h-6 text-amber-500" />
            </h1>
          </div>
          <p className="text-sm text-gray-600 dark:text-gray-400">
            Active Organization: <strong className="text-brand-600 dark:text-brand-400">{selectedOrg?.name || 'Loading...'}</strong>
            {selectedOrg?.category && (
              <span className="ml-2 inline-block px-2.5 py-0.5 rounded-full text-xs font-bold bg-brand-100 dark:bg-brand-950 text-brand-700 dark:text-brand-300 border border-brand-200 dark:border-brand-800 uppercase tracking-wide">
                {selectedOrg.category}
              </span>
            )}
          </p>
        </div>

        {/* Organization & Counter Selectors */}
        <div className="flex flex-wrap items-center gap-3">
          {/* Organization Switcher / Locked Pill */}
          <div className="flex items-center gap-2 bg-brand-50/80 dark:bg-brand-950/60 p-2 px-3 rounded-2xl border border-brand-200 dark:border-brand-800 shadow-sm">
            <Building2 className="w-5 h-5 text-brand-600 dark:text-brand-400" />
            {(user?.role === 'STAFF' || user?.role === 'ADMIN') && user?.organizationId ? (
              <span className="text-sm font-extrabold text-brand-900 dark:text-brand-200 pr-1">
                {selectedOrg?.name} ({selectedOrg?.category})
              </span>
            ) : (
              <select
                value={selectedOrg?.id || ''}
                onChange={(e) => {
                  const org = organizations.find((o) => o.id === Number(e.target.value));
                  if (org) setSelectedOrg(org);
                }}
                className="bg-transparent text-sm font-extrabold text-brand-900 dark:text-brand-200 focus:outline-none pr-2 cursor-pointer"
              >
                {organizations.map((org) => (
                  <option key={org.id} value={org.id} className="dark:bg-gray-900 text-gray-900 dark:text-white">
                    {org.name} ({org.category})
                  </option>
                ))}
              </select>
            )}
          </div>

          {/* Counter Switcher */}
          <div className="flex items-center gap-2 bg-white dark:bg-gray-900 p-2 px-3 rounded-2xl border border-gray-200 dark:border-gray-800 shadow-sm">
            <Building className="w-4 h-4 text-gray-400" />
            <select
              value={selectedCounter?.id || ''}
              onChange={(e) => {
                const c = counters.find((cnt) => cnt.id === Number(e.target.value));
                if (c) {
                  setSelectedCounter(c);
                  if (c.currentToken) {
                    setCurrentToken(c.currentToken as QueueToken);
                  } else {
                    setCurrentToken(null);
                  }
                }
              }}
              className="bg-transparent text-sm font-bold text-gray-900 dark:text-white focus:outline-none pr-2"
            >
              {counters.length === 0 ? (
                <option value="">No Counters</option>
              ) : (
                counters.map((cnt) => (
                  <option key={cnt.id} value={cnt.id} className="dark:bg-gray-900">
                    {cnt.name} ({cnt.counterNumber})
                  </option>
                ))
              )}
            </select>
          </div>

          {/* Admin Counter Actions */}
          {(user?.role === 'ADMIN' || user?.role === 'SUPER_ADMIN') && (
            <div className="flex items-center gap-2">
              <button
                onClick={() => setShowAddCounterModal(true)}
                title="Add new counter"
                className="flex items-center gap-1.5 px-3 py-2 rounded-2xl bg-brand-600 hover:bg-brand-700 text-white font-bold text-xs shadow-sm transition cursor-pointer"
              >
                <Plus className="w-4 h-4" /> Add Counter
              </button>

              {selectedCounter && (
                <button
                  onClick={() => setShowDeleteConfirmModal(true)}
                  title="Delete active counter"
                  className="flex items-center gap-1.5 px-3 py-2 rounded-2xl bg-red-50 hover:bg-red-100 dark:bg-red-950/50 dark:hover:bg-red-900/60 text-red-600 dark:text-red-400 font-bold text-xs border border-red-200 dark:border-red-900/50 shadow-sm transition cursor-pointer"
                >
                  <Trash2 className="w-4 h-4" /> Delete Counter
                </button>
              )}
            </div>
          )}
        </div>
      </div>

      {message && (
        <div className="p-4 rounded-xl bg-brand-50 dark:bg-brand-950/50 border border-brand-200 dark:border-brand-800 text-brand-700 dark:text-brand-300 text-sm font-medium">
          {message}
        </div>
      )}

      {selectedCounter ? (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
          {/* Main Control Panel */}
          <div className="lg:col-span-8 bg-white dark:bg-gray-900 rounded-3xl p-6 sm:p-8 border border-gray-200 dark:border-gray-800 shadow-xl space-y-6">
            {/* Status bar */}
            <div className="flex items-center justify-between gap-4 pb-6 border-b border-gray-100 dark:border-gray-800">
              <div>
                <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">Active Counter</span>
                <h2 className="text-2xl font-black text-gray-900 dark:text-white">
                  {selectedCounter.name} <span className="text-brand-600 dark:text-brand-400">({selectedCounter.counterNumber})</span>
                </h2>
              </div>

              <div className="flex items-center gap-2">
                <button
                  onClick={() => handleToggleCounterStatus('OPEN')}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition ${selectedCounter.status === 'OPEN'
                      ? 'bg-emerald-600 text-white shadow-md'
                      : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400'
                    }`}
                >
                  OPEN
                </button>
                <button
                  onClick={() => handleToggleCounterStatus('PAUSED')}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition ${selectedCounter.status === 'PAUSED'
                      ? 'bg-amber-600 text-white shadow-md'
                      : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400'
                    }`}
                >
                  PAUSED
                </button>
                <button
                  onClick={() => handleToggleCounterStatus('CLOSED')}
                  className={`px-3 py-1.5 rounded-xl text-xs font-bold transition ${selectedCounter.status === 'CLOSED'
                      ? 'bg-red-600 text-white shadow-md'
                      : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400'
                    }`}
                >
                  CLOSED
                </button>
              </div>
            </div>

            {/* Current Serving Card */}
            <div className="p-8 rounded-3xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/80 dark:border-gray-800 text-center space-y-4">
              <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">Currently Serving Token</span>

              {currentToken ? (
                <div className="space-y-3">
                  <div className="text-6xl sm:text-7xl font-black text-brand-600 dark:text-brand-400 tracking-tight">
                    {currentToken.tokenNumber}
                  </div>
                  <div className="text-base font-bold text-gray-900 dark:text-white">{currentToken.userName}</div>
                  <span className="inline-block px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-brand-100 dark:bg-brand-950 text-brand-600 dark:text-brand-400">
                    STATUS: {currentToken.status}
                  </span>
                </div>
              ) : (
                <div className="py-6 text-gray-400 text-sm font-medium">
                  No active customer at this counter. Click <strong className="text-brand-600">"Call Next Token"</strong> to serve next.
                </div>
              )}
            </div>

            {/* Actions Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
              <button
                onClick={handleCallNext}
                disabled={actionLoading || selectedCounter.status !== 'OPEN'}
                className="col-span-2 sm:col-span-1 p-4 rounded-2xl bg-brand-600 hover:bg-brand-700 text-white font-bold text-sm shadow-lg shadow-brand-500/25 transition flex flex-col items-center justify-center gap-2 disabled:opacity-50"
              >
                <Volume2 className="w-5 h-5" />
                Call Next Token
              </button>

              <button
                onClick={handleStartService}
                disabled={!currentToken || currentToken.status === 'SERVING' || actionLoading}
                className="p-4 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-sm shadow-lg shadow-emerald-500/25 transition flex flex-col items-center justify-center gap-2 disabled:opacity-50"
              >
                <Play className="w-5 h-5" />
                Start Service
              </button>

              <button
                onClick={handleCompleteToken}
                disabled={!currentToken || actionLoading}
                className="p-4 rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-sm shadow-lg shadow-indigo-500/25 transition flex flex-col items-center justify-center gap-2 disabled:opacity-50"
              >
                <CheckCircle className="w-5 h-5" />
                Complete Service
              </button>

              <button
                onClick={handleSkipToken}
                disabled={!currentToken || actionLoading}
                className="p-4 rounded-2xl bg-amber-600 hover:bg-amber-700 text-white font-bold text-sm shadow-lg shadow-amber-500/25 transition flex flex-col items-center justify-center gap-2 disabled:opacity-50"
              >
                <SkipForward className="w-5 h-5" />
                Skip Customer
              </button>
            </div>

            <div className="pt-2">
              <button
                onClick={handleNoShowToken}
                disabled={!currentToken || actionLoading}
                className="w-full py-3 rounded-2xl border border-red-200 dark:border-red-900/50 text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 font-bold text-sm transition flex items-center justify-center gap-2 disabled:opacity-50"
              >
                <UserX className="w-4 h-4" /> Mark Customer as No-Show
              </button>
            </div>

            {/* Live Waiting Queue List */}
            <div className="pt-6 border-t border-gray-100 dark:border-gray-800 space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-base font-bold text-gray-900 dark:text-white flex items-center gap-2">
                  <Users className="w-5 h-5 text-amber-500" />
                  Live Waiting Queue
                </h3>
                <span className="px-3 py-1 rounded-full text-xs font-black bg-amber-100 dark:bg-amber-950/80 text-amber-600 dark:text-amber-400 border border-amber-200 dark:border-amber-800">
                  {waitingTokens.length} Waiting
                </span>
              </div>

              {waitingTokens.length === 0 ? (
                <div className="p-6 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-800 text-center text-xs font-medium text-gray-400 space-y-1">
                  <p className="font-bold text-gray-500 dark:text-gray-400">No customers currently waiting in line.</p>
                  <p className="text-[11px] text-gray-400">New customer queue tokens will appear here automatically.</p>
                </div>
              ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 max-h-64 overflow-y-auto pr-1">
                  {waitingTokens.map((tok, idx) => (
                    <div
                      key={tok.id}
                      className={`p-4 rounded-2xl border transition flex items-center justify-between shadow-sm ${
                        idx === 0
                          ? 'bg-amber-50/70 dark:bg-amber-950/40 border-amber-300 dark:border-amber-700/60 ring-1 ring-amber-400/30'
                          : 'bg-gray-50 dark:bg-gray-800/40 border-gray-200/60 dark:border-gray-800'
                      }`}
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-black text-lg text-gray-900 dark:text-white">{tok.tokenNumber}</span>
                          {idx === 0 && (
                            <span className="px-2 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wide bg-amber-500 text-white shadow-xs">
                              Next Up
                            </span>
                          )}
                        </div>
                        <p className="text-xs font-bold text-gray-700 dark:text-gray-300">{tok.userName || 'Customer'}</p>
                        <span className="inline-block px-2 py-0.5 rounded text-[10px] font-bold bg-brand-100 dark:bg-brand-950 text-brand-700 dark:text-brand-300">
                          {tok.serviceName}
                        </span>
                      </div>
                      <div className="text-right">
                        <span className="text-[10px] font-bold uppercase tracking-wider text-gray-400 block">Est. Wait</span>
                        <span className="text-sm font-black text-amber-600 dark:text-amber-400">{tok.estimatedWaitMinutes || 5} min</span>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* Statistics Panel */}
          <div className="lg:col-span-4 space-y-4">
            <h3 className="text-lg font-bold text-gray-900 dark:text-white">Counter Live Metrics</h3>

            <div className="bg-white dark:bg-gray-900 rounded-3xl p-6 border border-gray-200 dark:border-gray-800 shadow-sm space-y-4">
              <div className="p-4 rounded-2xl bg-amber-50/80 dark:bg-amber-950/40 border border-amber-200/80 dark:border-amber-800/60 flex items-center justify-between">
                <div>
                  <span className="text-xs font-semibold text-amber-600 dark:text-amber-400 block">Waiting in Queue</span>
                  <span className="text-2xl font-black text-amber-600 dark:text-amber-400">{waitingTokens.length}</span>
                </div>
                <Users className="w-8 h-8 text-amber-500" />
              </div>

              <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/50 border border-gray-100 dark:border-gray-800 flex items-center justify-between">
                <div>
                  <span className="text-xs font-semibold text-gray-400 block">Average Service Duration</span>
                  <span className="text-2xl font-black text-gray-900 dark:text-white">4m 12s</span>
                </div>
                <Clock className="w-8 h-8 text-brand-500" />
              </div>

              <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/50 border border-gray-100 dark:border-gray-800 flex items-center justify-between">
                <div>
                  <span className="text-xs font-semibold text-gray-400 block">Completed Today</span>
                  <span className="text-2xl font-black text-emerald-600 dark:text-emerald-400">86</span>
                </div>
                <CheckCircle className="w-8 h-8 text-emerald-500" />
              </div>

              <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/50 border border-gray-100 dark:border-gray-800 flex items-center justify-between">
                <div>
                  <span className="text-xs font-semibold text-gray-400 block">Counter Status</span>
                  <span className="text-lg font-bold text-gray-900 dark:text-white">{selectedCounter.status}</span>
                </div>
                <Radio className="w-8 h-8 text-indigo-500 animate-pulse" />
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="bg-white dark:bg-gray-900 rounded-3xl p-8 sm:p-12 border border-gray-200 dark:border-gray-800 text-center space-y-4 shadow-xl">
          <div className="w-16 h-16 rounded-2xl bg-amber-50 dark:bg-amber-950 text-amber-600 dark:text-amber-400 mx-auto flex items-center justify-center">
            <Building2 className="w-8 h-8" />
          </div>
          <div>
            <h3 className="text-xl font-bold text-gray-900 dark:text-white">
              No Counter Configured for {selectedOrg?.name || 'this Organization'}
            </h3>
            <p className="text-sm text-gray-500 max-w-md mx-auto mt-1">
              Your organization does not have an active service counter set up yet. Click below to initialize a service counter.
            </p>
          </div>
          <button
            onClick={handleInitializeCounter}
            disabled={actionLoading || !selectedOrg}
            className="inline-flex items-center gap-2 px-6 py-3 rounded-2xl text-sm font-bold text-white bg-brand-600 hover:bg-brand-700 shadow-lg shadow-brand-500/25 transition disabled:opacity-50"
          >
            <Sparkles className="w-4 h-4" /> Initialize Service Counter Now
          </button>
        </div>
      )}

      {/* Add Counter Modal */}
      {showAddCounterModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
          <div className="bg-white dark:bg-gray-900 rounded-3xl p-6 sm:p-8 border border-gray-200 dark:border-gray-800 shadow-2xl max-w-md w-full space-y-6">
            <div className="flex items-center justify-between">
              <h3 className="text-xl font-extrabold text-gray-900 dark:text-white flex items-center gap-2">
                <Plus className="w-5 h-5 text-brand-600" /> Create New Service Counter
              </h3>
              <button
                onClick={() => setShowAddCounterModal(false)}
                className="p-2 rounded-xl text-gray-400 hover:text-gray-600 dark:hover:text-gray-200 transition cursor-pointer"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateCounterSubmit} className="space-y-4">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-gray-700 dark:text-gray-300 mb-1">
                  Counter Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Counter 03, Express Billing"
                  value={newCounterName}
                  onChange={(e) => setNewCounterName(e.target.value)}
                  className="w-full px-4 py-3 rounded-2xl border border-gray-200 dark:border-gray-800 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-brand-500"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-gray-700 dark:text-gray-300 mb-1">
                  Counter Code / Number <span className="text-gray-400 font-normal">(Optional)</span>
                </label>
                <input
                  type="text"
                  placeholder="e.g. COL-03, CNT-03 (Auto-generated if blank)"
                  value={newCounterNumber}
                  onChange={(e) => setNewCounterNumber(e.target.value)}
                  className="w-full px-4 py-3 rounded-2xl border border-gray-200 dark:border-gray-800 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-brand-500"
                />
              </div>

              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowAddCounterModal(false)}
                  className="px-5 py-2.5 rounded-2xl text-xs font-bold text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 transition cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={actionLoading || !newCounterName.trim()}
                  className="px-6 py-2.5 rounded-2xl text-xs font-bold text-white bg-brand-600 hover:bg-brand-700 shadow-md transition disabled:opacity-50 flex items-center gap-1.5 cursor-pointer"
                >
                  <Plus className="w-4 h-4" /> Create Counter
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Counter Confirmation Modal */}
      {showDeleteConfirmModal && selectedCounter && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
          <div className="bg-white dark:bg-gray-900 rounded-3xl p-6 sm:p-8 border border-gray-200 dark:border-gray-800 shadow-2xl max-w-md w-full space-y-6">
            <div className="flex items-center gap-3 text-red-600 dark:text-red-400">
              <div className="p-3 rounded-2xl bg-red-50 dark:bg-red-950/60 border border-red-200 dark:border-red-900/50">
                <Trash2 className="w-6 h-6" />
              </div>
              <div>
                <h3 className="text-lg font-extrabold text-gray-900 dark:text-white">Delete Counter?</h3>
                <p className="text-xs text-gray-500">This action cannot be undone.</p>
              </div>
            </div>

            <p className="text-sm text-gray-600 dark:text-gray-300">
              Are you sure you want to delete counter <strong className="text-gray-900 dark:text-white">{selectedCounter.name} ({selectedCounter.counterNumber})</strong>?
            </p>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setShowDeleteConfirmModal(false)}
                className="px-5 py-2.5 rounded-2xl text-xs font-bold text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 transition cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleDeleteCounter}
                disabled={actionLoading}
                className="px-6 py-2.5 rounded-2xl text-xs font-bold text-white bg-red-600 hover:bg-red-700 shadow-md transition disabled:opacity-50 flex items-center gap-1.5 cursor-pointer"
              >
                <Trash2 className="w-4 h-4" /> Delete Counter
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
