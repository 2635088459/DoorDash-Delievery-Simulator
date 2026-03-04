import { useEffect, useMemo, useState } from 'react';
import { Download, FileSearch } from 'lucide-react';
import toast from 'react-hot-toast';
import useAuthStore from '../store/authStore';
import { ticketService } from '../services/apiService';

const statusOptions = ['ALL', 'PENDING', 'SUCCESS', 'FAILED'];

const AdminAuditLogs = () => {
  const user = useAuthStore((state) => state.user);
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [search, setSearch] = useState('');
  const [dateRange, setDateRange] = useState({ start: '', end: '' });
  const [page, setPage] = useState(1);
  const [pageSize, setPageSize] = useState(10);

  const loadLogs = async () => {
    try {
      setLoading(true);
      const data = await ticketService.getAllActionLogs();
      setLogs(data || []);
    } catch (error) {
      console.error('Failed to load logs:', error);
      toast.error(error.response?.data?.message || 'Failed to load audit logs');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (user?.role !== 'ADMIN') return;
    loadLogs();
  }, [user?.role]);

  const filteredLogs = useMemo(() => {
    const normalized = search.trim().toLowerCase();
    const base = statusFilter === 'ALL'
      ? logs
      : logs.filter((log) => log.status === statusFilter);

    const dateFiltered = base.filter((log) => {
      if (!log.createdAt) return false;
      const createdAt = new Date(log.createdAt);
      if (dateRange.start) {
        const startDate = new Date(`${dateRange.start}T00:00:00`);
        if (createdAt < startDate) return false;
      }
      if (dateRange.end) {
        const endDate = new Date(`${dateRange.end}T23:59:59`);
        if (createdAt > endDate) return false;
      }
      return true;
    });

    return normalized
      ? dateFiltered.filter((log) => [
        log.actionType,
        log.operator,
        log.note,
        log.resultMessage,
        log.ticketId?.toString(),
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase()
        .includes(normalized))
      : dateFiltered;
  }, [logs, statusFilter, search, dateRange]);

  const totalPages = Math.max(1, Math.ceil(filteredLogs.length / pageSize));
  const currentPage = Math.min(page, totalPages);
  const paginatedLogs = useMemo(() => {
    const startIndex = (currentPage - 1) * pageSize;
    return filteredLogs.slice(startIndex, startIndex + pageSize);
  }, [filteredLogs, currentPage, pageSize]);

  const handleExport = () => {
    const headers = [
      'id',
      'ticketId',
      'actionType',
      'status',
      'operator',
      'note',
      'resultMessage',
      'createdAt',
      'updatedAt',
    ];
    const rows = filteredLogs.map((log) => headers.map((key) => {
      const value = log[key];
      return value === null || value === undefined ? '' : String(value).replace(/"/g, '""');
    }));
    const csv = [headers.join(','), ...rows.map((row) => row.map((cell) => `"${cell}"`).join(','))].join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `ticket-action-logs-${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(link.href);
  };

  useEffect(() => {
    setPage(1);
  }, [statusFilter, search, dateRange, pageSize]);

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3">
              <FileSearch className="w-7 h-7 text-indigo-600" />
              <h1 className="text-3xl font-bold text-gray-900">Action audit logs</h1>
            </div>
            <p className="text-gray-600 mt-2">Review agent action results and execution logs.</p>
          </div>
          <button
            type="button"
            onClick={loadLogs}
            className="flex items-center gap-2 px-4 py-2 bg-white border border-gray-200 rounded-lg shadow-sm hover:bg-gray-50"
          >
            Refresh
          </button>
        </div>

        <div className="mt-6 bg-white rounded-xl shadow-sm p-4 border border-gray-100 flex flex-wrap gap-3">
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            className="border border-gray-200 rounded-lg px-3 py-2 text-sm w-full sm:w-64"
            placeholder="Search action/note/ticket ID"
          />
          <input
            type="date"
            value={dateRange.start}
            onChange={(event) => setDateRange((prev) => ({ ...prev, start: event.target.value }))}
            className="border border-gray-200 rounded-lg px-3 py-2 text-sm"
          />
          <input
            type="date"
            value={dateRange.end}
            onChange={(event) => setDateRange((prev) => ({ ...prev, end: event.target.value }))}
            className="border border-gray-200 rounded-lg px-3 py-2 text-sm"
          />
          <select
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
            className="border border-gray-200 rounded-lg px-3 py-2 text-sm"
          >
            {statusOptions.map((option) => (
              <option key={option} value={option}>{option}</option>
            ))}
          </select>
          <select
            value={pageSize}
            onChange={(event) => setPageSize(Number(event.target.value))}
            className="border border-gray-200 rounded-lg px-3 py-2 text-sm"
          >
            {[10, 20, 50].map((size) => (
              <option key={size} value={size}>{size} / page</option>
            ))}
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

        <div className="mt-6 bg-white rounded-xl shadow-sm border border-gray-100">
          {loading ? (
            <div className="p-6 text-center text-gray-500">Loading...</div>
          ) : filteredLogs.length === 0 ? (
            <div className="p-6 text-center text-gray-500">No logs yet</div>
          ) : (
            <div className="divide-y divide-gray-100">
              {paginatedLogs.map((log) => (
                <div key={log.id} className="p-4">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div>
                      <p className="font-semibold text-gray-900">{log.actionType}</p>
                      <p className="text-xs text-gray-500">Ticket #{log.ticketId} · {log.operator || 'SYSTEM'}</p>
                    </div>
                    <span className={`text-xs px-2 py-1 rounded-full ${log.status === 'SUCCESS' ? 'bg-green-100 text-green-700' : log.status === 'FAILED' ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-700'}`}>
                      {log.status}
                    </span>
                  </div>
                  <p className="text-xs text-gray-500 mt-2">{log.note || '—'}</p>
                  {log.resultMessage && (
                    <p className="text-xs text-gray-600 mt-1">Result: {log.resultMessage}</p>
                  )}
                  <p className="text-xs text-gray-400 mt-2">
                    {log.createdAt ? new Date(log.createdAt).toLocaleString() : '--'}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>

        {filteredLogs.length > 0 && (
          <div className="mt-4 flex flex-wrap items-center justify-between gap-3 text-sm text-gray-600">
            <span>{filteredLogs.length} total · Page {currentPage} / {totalPages}</span>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => setPage((prev) => Math.max(1, prev - 1))}
                className="px-3 py-1 border border-gray-200 rounded-lg hover:bg-gray-50"
                disabled={currentPage === 1}
              >
                Previous
              </button>
              <button
                type="button"
                onClick={() => setPage((prev) => Math.min(totalPages, prev + 1))}
                className="px-3 py-1 border border-gray-200 rounded-lg hover:bg-gray-50"
                disabled={currentPage === totalPages}
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminAuditLogs;
