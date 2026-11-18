import React, { useEffect, useState } from 'react';
import { Organization, ServiceItem, Department, Counter } from '../types';
import api from '../api/axios';
import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';
import {
  Search,
  Building2,
  Building,
  Clock,
  Users,
  QrCode,
  ArrowRight,
  Filter,
  CheckCircle2,
  AlertCircle,
  X,
  Sparkles
} from 'lucide-react';

export const Explore: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(true);

  // Selected organization details
  const [selectedOrg, setSelectedOrg] = useState<Organization | null>(null);
  const [services, setServices] = useState<ServiceItem[]>([]);
  const [departments, setDepartments] = useState<Department[]>([]);
  const [counters, setCounters] = useState<Counter[]>([]);

  // QR Modal
  const [qrModalOpen, setQrModalOpen] = useState(false);
  const [qrCodeData, setQrCodeData] = useState<string>('');
  const [issuingTokenId, setIssuingTokenId] = useState<number | null>(null);
  const [errorMessage, setErrorMessage] = useState<string>('');

  const categories = ['ALL', 'College', 'Hospital', 'Government', 'Bank', 'Service Center'];

  useEffect(() => {
    fetchOrganizations();
  }, [selectedCategory, searchQuery]);

  const fetchOrganizations = async () => {
    try {
      setLoading(true);
      const res = await api.get('/organizations', {
        params: {
          category: selectedCategory === 'ALL' ? '' : selectedCategory,
          search: searchQuery,
        },
      });
      if (res.data.success) {
        setOrganizations(res.data.data);
        if (res.data.data.length > 0 && !selectedOrg) {
          handleSelectOrg(res.data.data[0]);
        }
      }
    } catch (e) {
      console.error(e);
    } finally {
      setLoading(false);
    }
  };

  const handleSelectOrg = async (org: Organization) => {
    setSelectedOrg(org);
    try {
      const [servicesRes, deptsRes, countersRes] = await Promise.all([
        api.get(`/organizations/${org.id}/services`),
        api.get(`/organizations/${org.id}/departments`),
        api.get(`/organizations/${org.id}/counters`),
      ]);
      if (servicesRes.data.success) setServices(servicesRes.data.data);
      if (deptsRes.data.success) setDepartments(deptsRes.data.data);
      if (countersRes.data.success) setCounters(countersRes.data.data);
    } catch (e) {
      console.error(e);
    }
  };

  const handleShowQr = async (orgId: number) => {
    try {
      const res = await api.get(`/organizations/${orgId}/qr`);
      if (res.data.success) {
        setQrCodeData(res.data.data);
        setQrModalOpen(true);
      }
    } catch (e) {
      console.error(e);
    }
  };

  const handleTakeToken = async (serviceId: number) => {
    if (!user) {
      navigate('/login');
      return;
    }
    setErrorMessage('');
    setIssuingTokenId(serviceId);
    try {
      const res = await api.post('/tokens', {
        serviceId,
        notifyThreshold: 10,
      });
      if (res.data.success) {
        navigate('/dashboard');
      }
    } catch (err: any) {
      setErrorMessage(err.response?.data?.message || 'Failed to request token');
    } finally {
      setIssuingTokenId(null);
    }
  };

  return (
    <div className="space-y-8">
      {/* Search Header */}
      <div className="bg-gradient-to-r from-brand-600 to-indigo-600 rounded-3xl p-8 sm:p-12 text-white shadow-xl relative overflow-hidden">
        <div className="relative z-10 max-w-2xl space-y-4">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 backdrop-blur-md text-xs font-semibold tracking-wide uppercase text-brand-100">
            <Sparkles className="w-3.5 h-3.5" /> Smart Queue Discovery
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight">
            Join physical queues digitally. Save hours of waiting.
          </h1>
          <p className="text-brand-100 text-sm sm:text-base">
            Select a university, hospital, bank, or government counter and grab your digital queue token right now.
          </p>

          <div className="pt-2 flex flex-col sm:flex-row gap-3">
            <div className="relative flex-1">
              <Search className="w-5 h-5 absolute left-3.5 top-1/2 -translate-y-1/2 text-gray-400" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Search organizations or services (e.g. LPU, Certificate, Token)..."
                className="w-full pl-11 pr-4 py-3 rounded-2xl bg-white text-gray-900 placeholder-gray-400 focus:outline-none focus:ring-4 focus:ring-brand-300 shadow-md text-sm"
              />
            </div>
          </div>
        </div>
      </div>

      {/* Categories Bar */}
      <div className="flex items-center gap-2 overflow-x-auto pb-2 scrollbar-none">
        <Filter className="w-4 h-4 text-gray-400 flex-shrink-0 mr-1" />
        {categories.map((cat) => (
          <button
            key={cat}
            onClick={() => setSelectedCategory(cat)}
            className={`px-4 py-2 rounded-xl text-sm font-semibold transition flex-shrink-0 ${
              selectedCategory === cat
                ? 'bg-brand-600 text-white shadow-md shadow-brand-500/20'
                : 'bg-white dark:bg-gray-900 text-gray-600 dark:text-gray-400 border border-gray-200 dark:border-gray-800 hover:bg-gray-100 dark:hover:bg-gray-800'
            }`}
          >
            {cat}
          </button>
        ))}
      </div>

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Left column: Organization list */}
        <div className="lg:col-span-5 space-y-4">
          <h2 className="text-lg font-bold text-gray-900 dark:text-white flex items-center justify-between">
            Organizations
            <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400">
              {organizations.length} found
            </span>
          </h2>

          {loading ? (
            <div className="space-y-3">
              {[1, 2, 3].map((i) => (
                <div key={i} className="h-28 rounded-2xl bg-gray-200 dark:bg-gray-800 animate-pulse" />
              ))}
            </div>
          ) : organizations.length === 0 ? (
            <div className="p-8 text-center bg-white dark:bg-gray-900 rounded-2xl border border-gray-200 dark:border-gray-800">
              <Building2 className="w-10 h-10 text-gray-400 mx-auto mb-2" />
              <p className="text-gray-500 text-sm">No organizations matching your search.</p>
            </div>
          ) : (
            organizations.map((org) => {
              const isSelected = selectedOrg?.id === org.id;
              return (
                <div
                  key={org.id}
                  onClick={() => handleSelectOrg(org)}
                  className={`p-5 rounded-2xl border transition cursor-pointer relative ${
                    isSelected
                      ? 'bg-white dark:bg-gray-900 border-brand-500 dark:border-brand-500 shadow-md ring-2 ring-brand-500/20'
                      : 'bg-white dark:bg-gray-900 border-gray-200 dark:border-gray-800 hover:border-gray-300 dark:hover:border-gray-700'
                  }`}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <span className="inline-block px-2.5 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wider bg-brand-50 dark:bg-brand-950/60 text-brand-600 dark:text-brand-400 mb-1">
                        {org.category}
                      </span>
                      <h3 className="font-bold text-gray-900 dark:text-white text-base">{org.name}</h3>
                      <p className="text-xs text-gray-500 dark:text-gray-400 mt-1 line-clamp-2">{org.description}</p>
                    </div>
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        handleShowQr(org.id);
                      }}
                      className="p-2 text-gray-400 hover:text-brand-600 dark:hover:text-brand-400 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-xl transition"
                      title="Show Venue QR Code"
                    >
                      <QrCode className="w-5 h-5" />
                    </button>
                  </div>

                  <div className="mt-4 pt-3 border-t border-gray-100 dark:border-gray-800 flex items-center justify-between text-xs text-gray-500">
                    <span className="flex items-center gap-1.5 font-medium text-amber-600 dark:text-amber-400">
                      <Users className="w-4 h-4" /> {org.activeQueueCount || 0} waiting
                    </span>
                    <span className="flex items-center gap-1 font-medium text-gray-600 dark:text-gray-400">
                      <Clock className="w-4 h-4 text-brand-500" /> ~{org.estimatedWaitMinutes || 0} min wait
                    </span>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Right column: Selected Org details & services */}
        <div className="lg:col-span-7">
          {selectedOrg ? (
            <div className="bg-white dark:bg-gray-900 rounded-3xl p-6 sm:p-8 border border-gray-200 dark:border-gray-800 shadow-sm space-y-6">
              <div>
                <span className="px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wider bg-brand-100 dark:bg-brand-950 text-brand-600 dark:text-brand-400">
                  {selectedOrg.category}
                </span>
                <h2 className="text-2xl font-extrabold text-gray-900 dark:text-white mt-2">{selectedOrg.name}</h2>
                <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">{selectedOrg.description}</p>
              </div>

              {errorMessage && (
                <div className="p-4 rounded-xl bg-red-50 dark:bg-red-950/50 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400 text-sm flex items-center gap-2">
                  <AlertCircle className="w-5 h-5 flex-shrink-0" />
                  {errorMessage}
                </div>
              )}

              {/* Departments overview */}
              {departments.length > 0 && (
                <div>
                  <h3 className="text-xs font-bold uppercase tracking-wider text-gray-400 mb-2">Departments</h3>
                  <div className="flex flex-wrap gap-2">
                    {departments.map((d) => (
                      <span key={d.id} className="px-3 py-1.5 rounded-xl bg-gray-100 dark:bg-gray-800 text-xs font-semibold text-gray-700 dark:text-gray-300">
                        {d.name}
                      </span>
                    ))}
                  </div>
                </div>
              )}

              {/* Active Counters Section */}
              <div>
                <div className="flex items-center justify-between mb-3">
                  <h3 className="text-sm font-bold text-gray-900 dark:text-white flex items-center gap-2">
                    <Building className="w-4 h-4 text-brand-500" /> Active Service Counters ({counters.length})
                  </h3>
                </div>

                {counters.length === 0 ? (
                  <div className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/40 border border-gray-200/60 dark:border-gray-800 text-xs text-gray-400 text-center font-medium">
                    No service counters configured for this organization yet.
                  </div>
                ) : (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-6">
                    {counters.map((cnt) => (
                      <div
                        key={cnt.id}
                        className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/50 border border-gray-200/80 dark:border-gray-800 flex items-center justify-between shadow-xs"
                      >
                        <div className="space-y-1">
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-sm text-gray-900 dark:text-white">{cnt.name}</span>
                            <span className="text-xs text-brand-600 dark:text-brand-400 font-semibold">({cnt.counterNumber})</span>
                          </div>
                          <p className="text-[11px] text-gray-500 dark:text-gray-400">
                            Department: <span className="font-semibold text-gray-700 dark:text-gray-300">{cnt.departmentName || 'General'}</span>
                          </p>
                        </div>
                        <div>
                          <span
                            className={`px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider ${
                              cnt.status === 'OPEN'
                                ? 'bg-emerald-100 dark:bg-emerald-950 text-emerald-600 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800'
                                : cnt.status === 'PAUSED'
                                ? 'bg-amber-100 dark:bg-amber-950 text-amber-600 dark:text-amber-400 border border-amber-200 dark:border-amber-800'
                                : 'bg-red-100 dark:bg-red-950 text-red-600 dark:text-red-400 border border-red-200 dark:border-red-800'
                            }`}
                          >
                            {cnt.status}
                          </span>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Services List */}
              <div>
                <h3 className="text-sm font-bold text-gray-900 dark:text-white mb-3">Available Counter Services</h3>
                <div className="space-y-3">
                  {services.map((svc) => (
                    <div
                      key={svc.id}
                      className="p-4 rounded-2xl bg-gray-50 dark:bg-gray-800/50 border border-gray-200/80 dark:border-gray-800 flex flex-col sm:flex-row sm:items-center justify-between gap-4 transition hover:border-brand-500/50"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-brand-500 text-white">
                            Prefix {svc.prefix}
                          </span>
                          <h4 className="font-bold text-gray-900 dark:text-white text-base">{svc.name}</h4>
                        </div>
                        <p className="text-xs text-gray-500 dark:text-gray-400">{svc.description}</p>
                        <div className="flex items-center gap-4 text-xs font-medium text-gray-500 pt-1">
                          <span className="flex items-center gap-1 text-amber-600 dark:text-amber-400">
                            <Users className="w-3.5 h-3.5" /> {svc.currentWaitingCount || 0} waiting
                          </span>
                          <span className="flex items-center gap-1 text-gray-600 dark:text-gray-400">
                            <Clock className="w-3.5 h-3.5 text-brand-500" /> Est: {svc.estimatedWaitMinutes || 5} min
                          </span>
                        </div>
                      </div>

                      <button
                        onClick={() => handleTakeToken(svc.id)}
                        disabled={issuingTokenId === svc.id}
                        className="px-5 py-2.5 rounded-xl font-bold text-sm text-white bg-brand-600 hover:bg-brand-700 shadow-md shadow-brand-500/20 transition flex items-center justify-center gap-2 disabled:opacity-50 flex-shrink-0"
                      >
                        {issuingTokenId === svc.id ? 'Getting Token...' : 'Get Digital Token'}{' '}
                        <ArrowRight className="w-4 h-4" />
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <div className="bg-white dark:bg-gray-900 rounded-3xl p-12 text-center border border-gray-200 dark:border-gray-800 text-gray-400">
              <Building2 className="w-12 h-12 mx-auto mb-3 text-gray-300 dark:text-gray-700" />
              <p className="text-sm font-medium">Select an organization to view available counters and take digital queue tokens.</p>
            </div>
          )}
        </div>
      </div>

      {/* QR Code Modal */}
      {qrModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white dark:bg-gray-900 rounded-3xl max-w-sm w-full p-6 text-center space-y-4 relative border border-gray-200 dark:border-gray-800">
            <button
              onClick={() => setQrModalOpen(false)}
              className="absolute top-4 right-4 p-2 text-gray-400 hover:text-gray-600 dark:hover:text-white"
            >
              <X className="w-5 h-5" />
            </button>
            <div className="inline-flex items-center justify-center w-12 h-12 rounded-2xl bg-brand-100 dark:bg-brand-950 text-brand-600 dark:text-brand-400">
              <QrCode className="w-6 h-6" />
            </div>
            <h3 className="text-lg font-bold text-gray-900 dark:text-white">Venue QR Code</h3>
            <p className="text-xs text-gray-500">Scan this QR code physically at the counter or share with visitors.</p>

            <div className="p-4 bg-white rounded-2xl shadow-inner border border-gray-100 flex justify-center">
              <img src={qrCodeData} alt="Queue QR Code" className="w-48 h-48" />
            </div>

            <button
              onClick={() => setQrModalOpen(false)}
              className="w-full py-2.5 rounded-xl bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-white font-semibold text-sm hover:bg-gray-200 dark:hover:bg-gray-700 transition"
            >
              Close
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
