import { useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { AlertTriangle, ClipboardList, Download, Filter, RefreshCw, Search, ShieldCheck } from 'lucide-react';
import toast from 'react-hot-toast';
import useAuthStore from '../store/authStore';
import { authService, ticketService } from '../services/apiService';

const statusStyles = {
  NEW: 'bg-blue-100 text-blue-700',
  IN_PROGRESS: 'bg-amber-100 text-amber-700',
  RESOLVED: 'bg-green-100 text-green-700',
  CLOSED: 'bg-gray-200 text-gray-700',
};

const priorityStyles = {
  LOW: 'bg-gray-100 text-gray-600',
  NORMAL: 'bg-indigo-100 text-indigo-700',
  HIGH: 'bg-orange-100 text-orange-700',
  URGENT: 'bg-red-100 text-red-700',
};

const AdminTickets = () => {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [slaFilter, setSlaFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [sortKey, setSortKey] = useState('CREATED_DESC');
  const [reportCategory, setReportCategory] = useState('ALL');
  const [reportRestaurantId, setReportRestaurantId] = useState('');
  const [statusCards, setStatusCards] = useState({
    session: 'Checking',
    ticketsApi: 'Checking',
    auditApi: 'Checking',
    lastChecked: null,
  });

  const checkRuntimeStatus = async () => {
    const nextStatus = {
      session: 'Checking',
      ticketsApi: 'Checking',
      auditApi: 'Checking',
      lastChecked: new Date(),
    };
    try {
      await authService.testSession();
      nextStatus.session = 'Healthy';
    } catch (error) {
      nextStatus.session = 'Down';
    }

    try {
      await ticketService.getAll();
      nextStatus.ticketsApi = 'Healthy';
    } catch (error) {
      nextStatus.ticketsApi = 'Down';
    }

    try {
      await ticketService.getAllActionLogs();
      nextStatus.auditApi = 'Healthy';
    } catch (error) {
      nextStatus.auditApi = 'Down';
    }

    setStatusCards(nextStatus);
  };

  const loadTickets = async () => {
    try {
      setLoading(true);
      const data = await ticketService.getAll();
      setTickets(data || []);
    } catch (error) {
      console.error('Failed to load tickets:', error);
      toast.error(error.response?.data?.message || 'Failed to load tickets');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (user?.role !== 'ADMIN') {
      toast.error('Only admins can access the ticket board');
      navigate('/');
      return;
    }
    loadTickets();
    checkRuntimeStatus();
  }, [user?.role]);

  const filteredTickets = useMemo(() => {
    const normalized = search.trim().toLowerCase();
    const base = statusFilter === 'ALL'
      ? tickets
      : tickets.filter((ticket) => ticket.status === statusFilter);

    const slaFiltered = base.filter((ticket) => {
      if (slaFilter === 'ALL') return true;
      if (!ticket.slaDeadline) return false;
      const deadline = new Date(ticket.slaDeadline);
      const overdue = ticket.slaOverdue || deadline < new Date();
      return slaFilter === 'OVERDUE' ? overdue : !overdue;
    });

    const searched = normalized
      ? slaFiltered.filter((ticket) => {
        const haystack = [
          ticket.title,
          ticket.description,
          ticket.category,
          ticket.priority,
          ticket.status,
          ticket.assignedRole,
          ticket.assignedTo,
          ticket.createdBy,
          ticket.restaurantId?.toString(),
        ]
          .filter(Boolean)
          .join(' ')
          .toLowerCase();
        return haystack.includes(normalized);
      })
      : slaFiltered;

    const sorted = [...searched];
    sorted.sort((a, b) => {
      if (sortKey === 'CREATED_ASC') {
        return new Date(a.createdAt || 0) - new Date(b.createdAt || 0);
      }
      if (sortKey === 'PRIORITY') {
        const order = { URGENT: 4, HIGH: 3, NORMAL: 2, LOW: 1 };
        return (order[b.priority] || 0) - (order[a.priority] || 0);
      }
      if (sortKey === 'STATUS') {
        const order = { NEW: 1, IN_PROGRESS: 2, RESOLVED: 3, CLOSED: 4 };
        return (order[a.status] || 99) - (order[b.status] || 99);
      }
      return new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
    });

    return sorted;
  }, [tickets, statusFilter, slaFilter, search, sortKey]);

  const filteredReportTickets = useMemo(() => {
    const normalizedRestaurant = reportRestaurantId.trim();
    return tickets.filter((ticket) => {
      if (reportCategory !== 'ALL' && ticket.category !== reportCategory) {
        return false;
      }
      if (normalizedRestaurant && String(ticket.restaurantId || '') !== normalizedRestaurant) {
        return false;
      }
      return true;
    });
  }, [tickets, reportCategory, reportRestaurantId]);

  const slaReport = useMemo(() => {
    const now = new Date();
    const buildReport = (days) => {
      const start = new Date(now.getTime() - days * 24 * 60 * 60 * 1000);
      const scoped = filteredReportTickets.filter((ticket) => new Date(ticket.createdAt || 0) >= start);
      const overdueCount = scoped.filter((ticket) => {
        if (!ticket.slaDeadline) return false;
        return new Date(ticket.slaDeadline) < now;
      }).length;
      const resolvedCount = scoped.filter((ticket) => ticket.status === 'RESOLVED').length;
      return {
        total: scoped.length,
        overdue: overdueCount,
        resolved: resolvedCount,
      };
    };
    return {
      sevenDays: buildReport(7),
      thirtyDays: buildReport(30),
    };
  }, [filteredReportTickets]);

  const handleExportSlaReport = () => {
    const now = new Date();
    const buildRows = (label, days) => {
      const start = new Date(now.getTime() - days * 24 * 60 * 60 * 1000);
      const scoped = filteredReportTickets.filter((ticket) => new Date(ticket.createdAt || 0) >= start);
      const overdueCount = scoped.filter((ticket) => {
        if (!ticket.slaDeadline) return false;
        return new Date(ticket.slaDeadline) < now;
      }).length;
      const resolvedCount = scoped.filter((ticket) => ticket.status === 'RESOLVED').length;
      return {
        period: label,
        total: scoped.length,
        overdue: overdueCount,
        resolved: resolvedCount,
        category: reportCategory,
        restaurantId: reportRestaurantId || 'ALL',
      };
    };
    const rows = [
      buildRows('7d', 7),
      buildRows('30d', 30),
    ];
    const headers = ['period', 'total', 'overdue', 'resolved', 'category', 'restaurantId'];
    const csv = [
      headers.join(','),
      ...rows.map((row) => headers.map((key) => `"${String(row[key] ?? '').replace(/"/g, '""')}"`).join(',')),
    ].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `sla-report-${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  };

  const stats = useMemo(() => {
    const total = tickets.length;
    const byStatus = tickets.reduce((acc, ticket) => {
      acc[ticket.status] = (acc[ticket.status] || 0) + 1;
      return acc;
    }, {});
    return {
      total,
      newCount: byStatus.NEW || 0,
      inProgress: byStatus.IN_PROGRESS || 0,
      resolved: byStatus.RESOLVED || 0,
    };
  }, [tickets]);

  const handleExport = () => {
    const headers = [
      'id',
      'title',
      'status',
      'priority',
      'category',
      'assignedRole',
      'assignedTo',
      'restaurantId',
      'createdBy',
      'createdAt',
    ];
    const rows = filteredTickets.map((ticket) => headers.map((key) => {
      const value = ticket[key];
      return value === null || value === undefined
        ? ''
        : String(value).replace(/"/g, '""');
    }));
    const csv = [headers.join(','), ...rows.map((row) => row.map((cell) => `"${cell}"`).join(','))].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `tickets-${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  };

  const parseEvidence = (ticket) => {
    if (!ticket?.evidenceJson) return null;
    try {
      return JSON.parse(ticket.evidenceJson);
    } catch (error) {
      return null;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        <div className="flex flex-col gap-6">
          <div className="flex items-start justify-between">
            <div>
              <div className="flex items-center gap-3">
                <ClipboardList className="w-7 h-7 text-indigo-600" />
                <h1 className="text-3xl font-bold text-gray-900">Ticket center</h1>
              </div>
              <p className="text-gray-600 mt-2">Review anomaly tickets and agent suggestions in one place.</p>
            </div>
            <button
              type="button"
              onClick={loadTickets}
              className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-200 rounded-lg shadow-sm hover:bg-gray-50"
            >
              <RefreshCw className="w-4 h-4" />
              Refresh
            </button>
          </div>

          <div className="flex flex-wrap items-center gap-3 bg-white rounded-xl shadow-sm p-4 border border-gray-100">
            <div className="flex items-center gap-2 text-gray-600">
              <Filter className="w-4 h-4" />
              Status filter
            </div>
            {['ALL', 'NEW', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'].map((status) => (
              <button
                key={status}
                type="button"
                onClick={() => setStatusFilter(status)}
                className={`px-3 py-1 rounded-full text-sm font-medium border ${
                  statusFilter === status
                    ? 'bg-indigo-600 text-white border-indigo-600'
                    : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50'
                }`}
              >
                {status === 'ALL' ? 'All' : status}
              </button>
            ))}
            <div className="flex items-center gap-2 text-gray-600">
              <Filter className="w-4 h-4" />
              SLA filter
            </div>
            {['ALL', 'OVERDUE', 'ON_TIME'].map((value) => (
              <button
                key={value}
                type="button"
                onClick={() => setSlaFilter(value)}
                className={`px-3 py-1 rounded-full text-sm font-medium border ${
                  slaFilter === value
                    ? 'bg-emerald-600 text-white border-emerald-600'
                    : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-50'
                }`}
              >
                {value === 'ALL' ? 'All' : value === 'OVERDUE' ? 'Overdue' : 'On time'}
              </button>
            ))}
            <div className="flex-1 flex flex-wrap items-center justify-end gap-3">
              <div className="relative w-full sm:w-64">
                <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  value={search}
                  onChange={(event) => setSearch(event.target.value)}
                  placeholder="Search title/description/assignee/restaurant"
                  className="w-full border border-gray-200 rounded-lg pl-9 pr-3 py-2 text-sm"
                />
              </div>
              <select
                value={sortKey}
                onChange={(event) => setSortKey(event.target.value)}
                className="border border-gray-200 rounded-lg px-3 py-2 text-sm"
              >
                <option value="CREATED_DESC">Newest first</option>
                <option value="CREATED_ASC">Oldest first</option>
                <option value="PRIORITY">Sort by priority</option>
                <option value="STATUS">Sort by status</option>
              </select>
              <button
                type="button"
                onClick={handleExport}
                className="inline-flex items-center gap-2 px-3 py-2 border border-gray-200 rounded-lg text-sm text-gray-700 hover:bg-gray-50"
              >
                <Download className="w-4 h-4" />
                Export CSV
              </button>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm p-4 border border-gray-100">
            <div className="flex flex-wrap items-center gap-3">
              <div className="text-xs text-gray-500">Report filters</div>
              <select
                value={reportCategory}
                onChange={(event) => setReportCategory(event.target.value)}
                className="border border-gray-200 rounded-lg px-3 py-2 text-sm"
              >
                <option value="ALL">All categories</option>
                {Array.from(new Set(tickets.map((ticket) => ticket.category).filter(Boolean)))
                  .map((category) => (
                    <option key={category} value={category}>{category}</option>
                  ))}
              </select>
              <input
                value={reportRestaurantId}
                onChange={(event) => setReportRestaurantId(event.target.value)}
                className="border border-gray-200 rounded-lg px-3 py-2 text-sm w-40"
                placeholder="Restaurant ID"
              />
              <button
                type="button"
                onClick={handleExportSlaReport}
                className="inline-flex items-center gap-2 px-3 py-2 border border-gray-200 rounded-lg text-sm text-gray-700 hover:bg-gray-50"
              >
                <Download className="w-4 h-4" />
                Export report
              </button>
            </div>
          </div>

          <div className="grid md:grid-cols-2 gap-4">
            <div className="bg-white rounded-xl shadow-sm p-4 border border-gray-100">
              <p className="text-xs text-gray-500">SLA report (last 7 days)</p>
              <div className="mt-2 flex flex-wrap gap-2 text-xs">
                <span className="px-2 py-1 rounded-full bg-slate-100">Total {slaReport.sevenDays.total}</span>
                <span className="px-2 py-1 rounded-full bg-red-100 text-red-700">Overdue {slaReport.sevenDays.overdue}</span>
                <span className="px-2 py-1 rounded-full bg-emerald-100 text-emerald-700">Resolved {slaReport.sevenDays.resolved}</span>
              </div>
            </div>
            <div className="bg-white rounded-xl shadow-sm p-4 border border-gray-100">
              <p className="text-xs text-gray-500">SLA report (last 30 days)</p>
              <div className="mt-2 flex flex-wrap gap-2 text-xs">
                <span className="px-2 py-1 rounded-full bg-slate-100">Total {slaReport.thirtyDays.total}</span>
                <span className="px-2 py-1 rounded-full bg-red-100 text-red-700">Overdue {slaReport.thirtyDays.overdue}</span>
                <span className="px-2 py-1 rounded-full bg-emerald-100 text-emerald-700">Resolved {slaReport.thirtyDays.resolved}</span>
              </div>
            </div>
          </div>

          <div className="grid md:grid-cols-4 gap-4">
            <div className="bg-white rounded-xl shadow-sm p-4 border border-gray-100">
              <p className="text-xs text-gray-500">Total tickets</p>
              <p className="text-2xl font-semibold text-gray-900">{stats.total}</p>
            </div>
            <div className="bg-white rounded-xl shadow-sm p-4 border border-gray-100">
              <p className="text-xs text-gray-500">New</p>
              <p className="text-2xl font-semibold text-blue-600">{stats.newCount}</p>
            </div>
            <div className="bg-white rounded-xl shadow-sm p-4 border border-gray-100">
              <p className="text-xs text-gray-500">In progress</p>
              <p className="text-2xl font-semibold text-amber-600">{stats.inProgress}</p>
            </div>
            <div className="bg-white rounded-xl shadow-sm p-4 border border-gray-100">
              <p className="text-xs text-gray-500">Resolved</p>
              <p className="text-2xl font-semibold text-green-600">{stats.resolved}</p>
            </div>
          </div>

          <div className="bg-white rounded-xl shadow-sm p-4 border border-gray-100">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-5 h-5 text-indigo-600" />
                <h2 className="text-sm font-semibold text-gray-800">Runtime status</h2>
              </div>
              <button
                type="button"
                onClick={checkRuntimeStatus}
                className="text-xs px-3 py-1 border border-gray-200 rounded-full text-gray-600 hover:bg-gray-50"
              >
                Recheck
              </button>
            </div>
            <div className="mt-3 grid md:grid-cols-4 gap-3 text-xs">
              <div className="p-3 rounded-lg bg-gray-50 border border-gray-100">
                <p className="text-gray-500">Session status</p>
                <p className={`mt-1 font-semibold ${statusCards.session === 'Healthy' ? 'text-green-600' : 'text-red-600'}`}>
                  {statusCards.session}
                </p>
              </div>
              <div className="p-3 rounded-lg bg-gray-50 border border-gray-100">
                <p className="text-gray-500">Tickets API</p>
                <p className={`mt-1 font-semibold ${statusCards.ticketsApi === 'Healthy' ? 'text-green-600' : 'text-red-600'}`}>
                  {statusCards.ticketsApi}
                </p>
              </div>
              <div className="p-3 rounded-lg bg-gray-50 border border-gray-100">
                <p className="text-gray-500">Audit API</p>
                <p className={`mt-1 font-semibold ${statusCards.auditApi === 'Healthy' ? 'text-green-600' : 'text-red-600'}`}>
                  {statusCards.auditApi}
                </p>
              </div>
              <div className="p-3 rounded-lg bg-gray-50 border border-gray-100">
                <p className="text-gray-500">Last checked</p>
                <p className="mt-1 font-semibold text-gray-700">
                  {statusCards.lastChecked ? statusCards.lastChecked.toLocaleTimeString() : '--'}
                </p>
              </div>
            </div>
          </div>

          <div className="grid gap-4">
            {loading ? (
              <div className="bg-white rounded-xl shadow-sm p-8 text-center text-gray-500">Loading...</div>
            ) : filteredTickets.length === 0 ? (
              <div className="bg-white rounded-xl shadow-sm p-8 text-center text-gray-500">
                <AlertTriangle className="w-6 h-6 mx-auto mb-2 text-gray-400" />
                No tickets found
              </div>
            ) : (
              filteredTickets.map((ticket) => (
                <button
                  key={ticket.id}
                  type="button"
                  onClick={() => navigate(`/admin/tickets/${ticket.id}`)}
                  className="text-left bg-white rounded-xl shadow-sm p-5 border border-gray-100 hover:border-indigo-300 hover:shadow-md transition"
                >
                  {(() => {
                    const evidence = parseEvidence(ticket);
                    if (!evidence || evidence.cancelRate === undefined) {
                      return null;
                    }
                    const current = (evidence.cancelRate || 0) * 100;
                    const prev = (evidence.previousCancelRate || 0) * 100;
                    const delta = current - prev;
                    return (
                      <div className="mb-3 text-xs text-gray-500 flex flex-wrap items-center gap-2">
                        <span className="px-2 py-1 rounded-full bg-slate-100">Current cancel rate {current.toFixed(1)}%</span>
                        <span className="px-2 py-1 rounded-full bg-slate-100">Previous window {prev.toFixed(1)}%</span>
                        <span className={`px-2 py-1 rounded-full ${delta >= 0 ? 'bg-red-100 text-red-700' : 'bg-green-100 text-green-700'}`}>
                          {delta >= 0 ? '+' : ''}{delta.toFixed(1)}%
                        </span>
                      </div>
                    );
                  })()}
                  <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-3">
                    <div>
                      <h3 className="text-lg font-semibold text-gray-900">{ticket.title}</h3>
                      <p className="text-sm text-gray-600 mt-1">{ticket.description}</p>
                      <div className="flex flex-wrap items-center gap-2 mt-3 text-xs">
                        <span className={`px-2 py-1 rounded-full ${statusStyles[ticket.status] || 'bg-gray-100 text-gray-600'}`}>
                          {ticket.status}
                        </span>
                        <span className={`px-2 py-1 rounded-full ${priorityStyles[ticket.priority] || 'bg-gray-100 text-gray-600'}`}>
                          {ticket.priority}
                        </span>
                        {(() => {
                          if (!ticket.slaDeadline || ['RESOLVED', 'CLOSED'].includes(ticket.status)) {
                            return null;
                          }
                          const deadline = new Date(ticket.slaDeadline);
                          const diffMs = deadline - Date.now();
                          if (ticket.slaOverdue || diffMs < 0) {
                            const hours = ticket.slaOverdueMinutes
                              ? (ticket.slaOverdueMinutes / 60).toFixed(1)
                              : (Math.abs(diffMs) / 3600000).toFixed(1);
                            return (
                              <span className="px-2 py-1 rounded-full bg-red-100 text-red-700">
                                SLA overdue {hours}h
                              </span>
                            );
                          }
                          const hoursLeft = diffMs / 3600000;
                          return (
                            <span className="px-2 py-1 rounded-full bg-emerald-100 text-emerald-700">
                              SLA remaining {hoursLeft.toFixed(1)}h
                            </span>
                          );
                        })()}
                        <span className="px-2 py-1 rounded-full bg-purple-100 text-purple-700">
                          {ticket.category}
                        </span>
                        {ticket.restaurantId && (
                          <span className="px-2 py-1 rounded-full bg-slate-100 text-slate-600">
                            Restaurant #{ticket.restaurantId}
                          </span>
                        )}
                      </div>
                    </div>
                    <div className="text-sm text-gray-500">
                      <div>Created at: {ticket.createdAt ? new Date(ticket.createdAt).toLocaleString() : '--'}</div>
                      <div>Assigned to: {ticket.assignedTo || 'Unassigned'} {ticket.assignedRole ? `(${ticket.assignedRole})` : ''}</div>
                      <div>Source: {ticket.source || '--'} · Created by: {ticket.createdBy || '--'}</div>
                    </div>
                  </div>
                </button>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default AdminTickets;
