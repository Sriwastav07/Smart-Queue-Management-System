import React, { useEffect, useState } from 'react';
import { AdminAnalytics, QueuePrediction, Organization } from '../types';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import {
  Users,
  Clock,
  CheckCircle2,
  XCircle,
  Building,
  TrendingUp,
  Sparkles,
  Award,
  AlertTriangle,
  Building2
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
  Legend
} from 'recharts';

export const AdminDashboard: React.FC = () => {
  const { user } = useAuth();
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [selectedOrgId, setSelectedOrgId] = useState<number | null>(null);
  const [analytics, setAnalytics] = useState<AdminAnalytics | null>(null);
  const [prediction, setPrediction] = useState<QueuePrediction | null>(null);
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    fetchOrganizations();
  }, [user]);

  useEffect(() => {
    if (selectedOrgId) {
      fetchAnalytics(selectedOrgId);
    }
  }, [selectedOrgId]);

  const fetchOrganizations = async () => {
    try {
      setLoading(true);
      const res = await api.get('/organizations');
      if (res.data.success) {
        let orgs: Organization[] = res.data.data;
        if (user?.role === 'ADMIN' && user?.organizationId) {
          orgs = orgs.filter((o) => o.id === user.organizationId);
        }
        setOrganizations(orgs);
        if (orgs.length > 0) {
          const defaultId = user?.organizationId || orgs[0].id;
          setSelectedOrgId(defaultId);
        }
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const fetchAnalytics = async (orgId: number) => {
    try {
      setLoading(true);
      const [analyticsRes, predictionRes] = await Promise.all([
        api.get(`/admin/analytics/organizations/${orgId}`),
        api.get('/services/1/prediction'),
      ]);

      if (analyticsRes.data.success) setAnalytics(analyticsRes.data.data);
      if (predictionRes.data.success) setPrediction(predictionRes.data.data);
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  if (loading || !analytics) {
    return (
      <div className="py-20 text-center space-y-4">
        <div className="w-12 h-12 rounded-full border-4 border-brand-600 border-t-transparent animate-spin mx-auto" />
        <p className="text-gray-500 font-medium">Loading organizational analytics & crowd forecasts...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-gray-900 dark:text-white flex items-center gap-2">
            Organizational Queue Analytics <Award className="w-6 h-6 text-brand-500" />
          </h1>
          <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">
            Real-time crowd monitoring, peak hour forecasting, and staff performance for <strong className="text-brand-600 dark:text-brand-400">{analytics.organizationName}</strong>.
          </p>
        </div>

        {/* Organization Switcher / Locked Pill */}
        <div className="flex items-center gap-2 bg-white dark:bg-gray-900 p-2.5 px-4 rounded-2xl border border-gray-200 dark:border-gray-800 shadow-sm">
          <Building2 className="w-5 h-5 text-brand-600 dark:text-brand-400" />
          {user?.role === 'ADMIN' ? (
            <span className="text-sm font-extrabold text-gray-900 dark:text-white pr-1">
              {analytics.organizationName}
            </span>
          ) : (
            <select
              value={selectedOrgId || ''}
              onChange={(e) => setSelectedOrgId(Number(e.target.value))}
              className="bg-transparent text-sm font-extrabold text-gray-900 dark:text-white focus:outline-none cursor-pointer pr-2"
            >
              {organizations.map((org) => (
                <option key={org.id} value={org.id} className="dark:bg-gray-900">
                  {org.name} ({org.category})
                </option>
              ))}
            </select>
          )}
        </div>
      </div>

      {/* Metric Cards Grid */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-4">
        <div className="p-5 rounded-2xl bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 shadow-sm">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block mb-1">Total Customers</span>
          <span className="text-2xl font-black text-gray-900 dark:text-white">{analytics.totalCustomersToday}</span>
        </div>

        <div className="p-5 rounded-2xl bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 shadow-sm">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block mb-1">Currently Waiting</span>
          <span className="text-2xl font-black text-amber-600 dark:text-amber-400">{analytics.currentlyWaiting}</span>
        </div>

        <div className="p-5 rounded-2xl bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 shadow-sm">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block mb-1">Average Wait</span>
          <span className="text-2xl font-black text-brand-600 dark:text-brand-400">{analytics.averageWaitMinutes} <span className="text-xs font-normal">min</span></span>
        </div>

        <div className="p-5 rounded-2xl bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 shadow-sm">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block mb-1">Completed</span>
          <span className="text-2xl font-black text-emerald-600 dark:text-emerald-400">{analytics.completedToday}</span>
        </div>

        <div className="p-5 rounded-2xl bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 shadow-sm">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block mb-1">No-Shows / Cancelled</span>
          <span className="text-2xl font-black text-red-600 dark:text-red-400">{analytics.noShowsToday + analytics.cancelledToday}</span>
        </div>

        <div className="p-5 rounded-2xl bg-white dark:bg-gray-900 border border-gray-200 dark:border-gray-800 shadow-sm">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block mb-1">Active Counters</span>
          <span className="text-2xl font-black text-indigo-600 dark:text-indigo-400">{analytics.activeCountersCount} / {analytics.totalCountersCount}</span>
        </div>
      </div>

      {/* Statistical Queue Prediction Engine Banner */}
      {prediction && (
        <div className="p-6 rounded-3xl bg-gradient-to-r from-indigo-900 to-purple-900 text-white shadow-xl space-y-3">
          <div className="flex items-center gap-2 text-indigo-300 text-xs font-bold uppercase tracking-wider">
            <Sparkles className="w-4 h-4 text-amber-400" /> Statistical Queue AI Prediction Insights
          </div>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <h3 className="text-xl font-extrabold">{prediction.serviceName} Peak Hour Forecast</h3>
              <p className="text-sm text-indigo-200 mt-1">{prediction.recommendation}</p>
            </div>
            <div className="px-4 py-2 rounded-2xl bg-white/10 backdrop-blur-md border border-white/10 text-center flex-shrink-0">
              <span className="text-xs text-indigo-200 block">Current Crowd Level</span>
              <span className="text-lg font-black text-amber-300">MODERATE (40-60%)</span>
            </div>
          </div>
        </div>
      )}

      {/* Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Hourly Traffic Chart */}
        <div className="p-6 bg-white dark:bg-gray-900 rounded-3xl border border-gray-200 dark:border-gray-800 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-gray-900 dark:text-white text-base">Hourly Queue Flow Today</h3>
            <span className="text-xs font-semibold text-gray-400">Joined vs Served</span>
          </div>

          <div className="h-72 w-full pt-4">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={analytics.hourlyQueueStats}>
                <defs>
                  <linearGradient id="colorJoined" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#0c93e7" stopOpacity={0.8} />
                    <stop offset="95%" stopColor="#0c93e7" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="colorServed" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.8} />
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" opacity={0.1} />
                <XAxis dataKey="hourLabel" stroke="#888888" fontSize={12} />
                <YAxis stroke="#888888" fontSize={12} />
                <Tooltip />
                <Area type="monotone" dataKey="joinedCount" stroke="#0c93e7" fillOpacity={1} fill="url(#colorJoined)" name="Joined" />
                <Area type="monotone" dataKey="servedCount" stroke="#10b981" fillOpacity={1} fill="url(#colorServed)" name="Served" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Counter Performance Bar Chart */}
        <div className="p-6 bg-white dark:bg-gray-900 rounded-3xl border border-gray-200 dark:border-gray-800 shadow-sm space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-bold text-gray-900 dark:text-white text-base">Counter Performance & Tokens Served</h3>
            <span className="text-xs font-semibold text-gray-400">Tokens per Counter</span>
          </div>

          <div className="h-72 w-full pt-4">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={analytics.counterPerformance}>
                <CartesianGrid strokeDasharray="3 3" opacity={0.1} />
                <XAxis dataKey="counterName" stroke="#888888" fontSize={12} />
                <YAxis stroke="#888888" fontSize={12} />
                <Tooltip />
                <Bar dataKey="tokensServed" fill="#6366f1" radius={[8, 8, 0, 0]} name="Completed Tokens" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>
    </div>
  );
};
