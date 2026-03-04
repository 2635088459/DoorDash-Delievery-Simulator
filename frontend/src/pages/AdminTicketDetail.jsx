import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { ArrowLeft, CheckCircle2, FileText, MessageSquarePlus, Save } from 'lucide-react';
import toast from 'react-hot-toast';
import useAuthStore from '../store/authStore';
import { ticketService } from '../services/apiService';

const statusOptions = ['NEW', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
const roleOptions = ['OPERATIONS', 'SUPPORT', 'ENGINEERING'];

const ticketTranslationMap = [
  ['【Agent 建议】', '[Agent Recommendations]'],
  ['根因假设:', 'Root cause hypotheses:'],
  ['排查清单:', 'Investigation checklist:'],
  ['建议动作:', 'Recommended actions:'],
  ['评分摘要: 平均评分 ', 'Rating summary: average rating '],
  ['，评价数 ', ', total reviews '],
  ['近期评价摘录:', 'Recent review highlights:'],
  ['工具调用:', 'Tools used:'],
  ['【工单总结】', '[Ticket Summary]'],
  ['标题:', 'Title:'],
  ['状态:', 'Status:'],
  ['优先级:', 'Priority:'],
  ['创建人:', 'Created by:'],
  ['指派角色:', 'Assigned role:'],
  ['指派到:', 'Assigned to:'],
  ['餐厅ID:', 'Restaurant ID:'],
  ['订单ID:', 'Order ID:'],
  ['骑手ID:', 'Driver ID:'],
  ['24h 取消率:', '24h cancel rate:'],
  ['取消/总量:', 'Cancelled/total:'],
  ['最新状态记录:', 'Latest status update:'],
  ['查看样本订单与时间线，确认需要升级或通知相关团队。', 'Review sample orders and the timeline to confirm whether escalation or notifications are needed.'],
  [' (状态:', ' (status:'],
  ['更新工单状态为', 'updated ticket status to'],
  ['，指派给 ', ', assigned to '],
  ['（角色 ', ' (role '],
  ['）', ')'],
  ['Agent 执行动作:', 'Agent executed action:'],
  ['动作结果回写:', 'Action result write-back:'],
  ['工单已自动标记为已解决', 'Ticket auto-marked as resolved'],
  ['证据链摘要已生成', 'Evidence summary generated'],
  ['证据链摘要:', 'Evidence summary:'],
  ['取消率 ', 'cancel rate '],
  ['延迟率 ', 'delay rate '],
  ['超时率 ', 'timeout rate '],
  ['退款率 ', 'refund rate '],
  ['评分 ', 'rating '],
  ['取消率异常', 'cancel rate anomaly'],
  ['配送延迟率异常', 'delivery delay anomaly'],
  ['超时订单激增', 'timeout orders spike'],
  ['退款率异常', 'refund rate anomaly'],
  ['最近24小时取消率达到 ', 'Cancel rate reached '],
  ['最近24小时配送延迟率达到 ', 'Delivery delay rate reached '],
  ['最近24小时超时率达到 ', 'Timeout rate reached '],
  ['最近24小时退款率达到 ', 'Refund rate reached '],
  ['(取消 ', '(cancelled '],
  [' / 总订单 ', ' / total '],
  ['(延迟 ', '(delayed '],
  [' / 已送达 ', ' / delivered '],
  ['，平均延迟 ', ', average delay '],
  [' 分钟。', ' minutes.'],
  ['(超时 ', '(timeout '],
  ['(退款 ', '(refunded '],
];

const translateTicketContent = (content) => {
  if (!content) return content;
  return ticketTranslationMap.reduce((text, [from, to]) => text.split(from).join(to), content);
};

const shouldTranslateContent = (item) => {
  if (!item) return false;
  return item.authorRole === 'SYSTEM'
    || item.authorRole === 'ASSISTANT'
    || item.type === 'SYSTEM_NOTE'
    || item.type === 'AGENT_NOTE';
};

const AdminTicketDetail = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const [ticket, setTicket] = useState(null);
  const [samples, setSamples] = useState([]);
  const [loading, setLoading] = useState(true);
  const [comment, setComment] = useState('');
  const [status, setStatus] = useState('NEW');
  const [assignedRole, setAssignedRole] = useState('OPERATIONS');
  const [assignedTo, setAssignedTo] = useState('');
  const [sampleStatus, setSampleStatus] = useState('ALL');
  const [minAmount, setMinAmount] = useState('');
  const [maxAmount, setMaxAmount] = useState('');
  const [sampleSort, setSampleSort] = useState('TIME_DESC');
  const [trendView, setTrendView] = useState('24H');
  const [summary, setSummary] = useState(null);
  const [summaryLoading, setSummaryLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [actionLogs, setActionLogs] = useState([]);

  const loadTicket = async () => {
    try {
      setLoading(true);
      const [data, sampleData] = await Promise.all([
        ticketService.getById(id),
        ticketService.getSamples(id),
      ]);
      setTicket(data);
      setSamples(sampleData || []);
      const logs = await ticketService.getActionLogs(id);
      setActionLogs(logs || []);
      setStatus(data.status || 'NEW');
      setAssignedRole(data.assignedRole || 'OPERATIONS');
      setAssignedTo(data.assignedTo || '');
      setSummary(null);
    } catch (error) {
      console.error('Failed to load ticket:', error);
  toast.error(error.response?.data?.message || 'Failed to load ticket');
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
    loadTicket();
  }, [user?.role, id]);

  useEffect(() => {
    const storedId = sessionStorage.getItem('adminTicketReturnId');
    if (storedId !== String(id)) return;
    const storedScroll = sessionStorage.getItem('adminTicketReturnScroll');
    if (!storedScroll) return;
    const scrollY = Number(storedScroll);
    if (Number.isNaN(scrollY)) return;
    setTimeout(() => {
      window.scrollTo({ top: scrollY, behavior: 'smooth' });
      sessionStorage.removeItem('adminTicketReturnId');
      sessionStorage.removeItem('adminTicketReturnScroll');
    }, 100);
  }, [id, ticket]);

  const evidenceJson = useMemo(() => {
    if (!ticket?.evidenceJson) return null;
    try {
      return JSON.stringify(JSON.parse(ticket.evidenceJson), null, 2);
    } catch (error) {
      return ticket.evidenceJson;
    }
  }, [ticket]);

  const evidenceData = useMemo(() => {
    if (!ticket?.evidenceJson) return null;
    try {
      return JSON.parse(ticket.evidenceJson);
    } catch (error) {
      return null;
    }
  }, [ticket]);

  const trendPoints = useMemo(() => {
    if (!evidenceData) return null;
    const selectedSeries = trendView === '7D' ? evidenceData.trendSeriesDaily : evidenceData.trendSeries;
    if (!selectedSeries?.length) return null;
    const series = selectedSeries.map((item) => Number(item.cancelRate || 0));
    const max = Math.max(...series, 0.01);
    const startLabel = trendView === '7D'
  ? (selectedSeries[0]?.start || '7d ago')
  : '24h ago';
    const endLabel = trendView === '7D'
  ? (selectedSeries[selectedSeries.length - 1]?.start || 'Today')
  : 'Now';
    return {
      series,
      max,
      startLabel,
      endLabel,
    };
  }, [evidenceData, trendView]);

  const filteredSamples = useMemo(() => {
    const min = minAmount === '' ? null : Number(minAmount);
    const max = maxAmount === '' ? null : Number(maxAmount);

    const filtered = samples.filter((order) => {
      if (sampleStatus !== 'ALL' && order.status !== sampleStatus) {
        return false;
      }
      const amount = Number(order.totalAmount || 0);
      if (min !== null && amount < min) return false;
      if (max !== null && amount > max) return false;
      return true;
    });

    const sorted = [...filtered];
    sorted.sort((a, b) => {
      if (sampleSort === 'AMOUNT_DESC') {
        return Number(b.totalAmount || 0) - Number(a.totalAmount || 0);
      }
      if (sampleSort === 'AMOUNT_ASC') {
        return Number(a.totalAmount || 0) - Number(b.totalAmount || 0);
      }
      if (sampleSort === 'TIME_ASC') {
        return new Date(a.createdAt || 0) - new Date(b.createdAt || 0);
      }
      return new Date(b.createdAt || 0) - new Date(a.createdAt || 0);
    });

    return sorted;
  }, [samples, sampleStatus, minAmount, maxAmount, sampleSort]);

  const timelineItems = useMemo(() => {
    if (!ticket) return [];
    const items = [{
      id: 'created',
      status: 'NEW',
  message: 'Ticket created',
      createdAt: ticket.createdAt,
      operator: ticket.createdBy,
    }];

    (ticket.comments || [])
      .filter((item) => item.type === 'SYSTEM_NOTE')
      .forEach((item) => {
        let evidence = null;
        if (item.evidenceJson) {
          try {
            evidence = JSON.parse(item.evidenceJson);
          } catch (error) {
            evidence = null;
          }
        }
        const match = item.content?.match(/更新工单状态为\s(\w+)/);
        items.push({
          id: item.id,
          status: evidence?.status || match?.[1] || ticket.status,
          message: translateTicketContent(item.content),
          createdAt: item.createdAt,
          operator: evidence?.operator,
          assignedTo: evidence?.assignedTo,
          assignedRole: evidence?.assignedRole,
          summaryType: evidence?.summaryType,
          summaryText: evidence?.summaryText,
          ratingSummary: evidence?.ratingSummary,
          reasonTags: evidence?.reasonTags,
          actionType: evidence?.actionType,
          resultStatus: evidence?.resultStatus,
          resultMessage: evidence?.resultMessage,
          slaHours: evidence?.slaHours,
          overdueMinutes: evidence?.overdueMinutes,
        });
      });

    return items.sort((a, b) => new Date(a.createdAt || 0) - new Date(b.createdAt || 0));
  }, [ticket]);

  const handleSummary = async () => {
    try {
      setSummaryLoading(true);
      const data = await ticketService.getSummary(id);
      setSummary(data);
  toast.success('Summary generated');
    } catch (error) {
      console.error('Failed to generate summary:', error);
  toast.error(error.response?.data?.message || 'Failed to generate summary');
    } finally {
      setSummaryLoading(false);
    }
  };

  const handleUpdate = async () => {
    try {
      await ticketService.updateStatus(id, {
        status,
        assignedRole,
        assignedTo: assignedTo || null,
      });
  toast.success('Status updated');
      await loadTicket();
    } catch (error) {
      console.error('Failed to update ticket:', error);
  toast.error(error.response?.data?.message || 'Update failed');
    }
  };

  const handleAddComment = async () => {
    if (!comment.trim()) return;
    try {
      await ticketService.addComment(id, { content: comment });
      setComment('');
  toast.success('Comment added');
      await loadTicket();
    } catch (error) {
      console.error('Failed to add comment:', error);
  toast.error(error.response?.data?.message || 'Failed to add comment');
    }
  };

  const handleQuickResolve = async () => {
    try {
      await ticketService.updateStatus(id, {
        status: 'RESOLVED',
        assignedRole,
        assignedTo: assignedTo || null,
      });
  toast.success('Marked as resolved');
      await loadTicket();
    } catch (error) {
      console.error('Failed to resolve ticket:', error);
  toast.error(error.response?.data?.message || 'Update failed');
    }
  };

  const handleExecuteAction = async (actionType, note, markResolved = false) => {
    try {
      setActionLoading(true);
      await ticketService.executeAction(id, {
        actionType,
        note,
        markResolved,
      });
  toast.success('Action executed');
      await loadTicket();
    } catch (error) {
      console.error('Failed to execute action:', error);
  toast.error(error.response?.data?.message || 'Execution failed');
    } finally {
      setActionLoading(false);
    }
  };

  const handleUpdateActionResult = async (actionId, status, resultMessage) => {
    try {
      setActionLoading(true);
      await ticketService.updateActionResult(id, actionId, {
        status,
        resultMessage,
      });
  toast.success('Result written back');
      await loadTicket();
    } catch (error) {
      console.error('Failed to update action result:', error);
  toast.error(error.response?.data?.message || 'Failed to write back');
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        <button
          type="button"
          onClick={() => navigate('/admin/tickets')}
          className="flex items-center gap-2 text-gray-600 hover:text-gray-900"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to list
        </button>

        {loading ? (
          <div className="bg-white rounded-xl shadow-sm p-8 text-center text-gray-500 mt-6">Loading...</div>
        ) : !ticket ? (
          <div className="bg-white rounded-xl shadow-sm p-8 text-center text-gray-500 mt-6">Ticket not found</div>
        ) : (
          <div className="grid lg:grid-cols-3 gap-6 mt-6">
            <div className="lg:col-span-2 space-y-6">
              <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
                <div className="flex items-center gap-3">
                  <FileText className="w-6 h-6 text-indigo-600" />
                  <h1 className="text-2xl font-bold text-gray-900">
                    {ticket.source === 'SYSTEM' ? translateTicketContent(ticket.title) : ticket.title}
                  </h1>
                </div>
                <p className="text-gray-600 mt-3">
                  {ticket.source === 'SYSTEM' ? translateTicketContent(ticket.description) : ticket.description}
                </p>
                <div className="mt-4 text-sm text-gray-500 space-y-1">
                  <div>Category: {ticket.category}</div>
                  <div>Source: {ticket.source}</div>
                  <div>Priority: {ticket.priority}</div>
                  <div>Created: {ticket.createdAt ? new Date(ticket.createdAt).toLocaleString() : '--'}</div>
                  {ticket.slaDeadline && (
                    <div>
                      SLA deadline: {new Date(ticket.slaDeadline).toLocaleString()} ·
                      {ticket.slaOverdue ? (
                        <span className="ml-2 text-red-600">
                          Overdue {((ticket.slaOverdueMinutes || 0) / 60).toFixed(1)}h
                        </span>
                      ) : (
                        <span className="ml-2 text-emerald-600">
                          Remaining {((new Date(ticket.slaDeadline) - Date.now()) / 3600000).toFixed(1)}h
                        </span>
                      )}
                    </div>
                  )}
                </div>
              </div>

              <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
                <h2 className="text-lg font-semibold text-gray-900">Evidence</h2>
                {evidenceData ? (
                  <div className="mt-4 space-y-4">
                    <div className="grid md:grid-cols-2 gap-4">
                      <div className="bg-slate-50 rounded-lg p-4">
                        <p className="text-xs text-gray-500">Cancel rate (last 24h)</p>
                        <p className="text-2xl font-semibold text-gray-900">
                          {((evidenceData.cancelRate || 0) * 100).toFixed(1)}%
                        </p>
                        <p className="text-xs text-gray-500 mt-1">
                          Cancelled {evidenceData.cancelledOrders || 0} / Total {evidenceData.totalOrders || 0}
                        </p>
                      </div>
                      <div className="bg-slate-50 rounded-lg p-4">
                        <p className="text-xs text-gray-500">Previous window cancel rate</p>
                        <p className="text-2xl font-semibold text-gray-900">
                          {((evidenceData.previousCancelRate || 0) * 100).toFixed(1)}%
                        </p>
                        <p className="text-xs text-gray-500 mt-1">
                          Cancelled {evidenceData.previousCancelledOrders || 0} / Total {evidenceData.previousTotalOrders || 0}
                        </p>
                      </div>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-gray-700 mb-2">Trend comparison</p>
                      <div className="flex gap-2 mb-3">
                        <button
                          type="button"
                          onClick={() => setTrendView('24H')}
                          className={`px-3 py-1 text-xs rounded-full border ${trendView === '24H' ? 'bg-indigo-600 text-white border-indigo-600' : 'border-gray-200 text-gray-500'}`}
                        >
                          24h view
                        </button>
                        <button
                          type="button"
                          onClick={() => setTrendView('7D')}
                          className={`px-3 py-1 text-xs rounded-full border ${trendView === '7D' ? 'bg-indigo-600 text-white border-indigo-600' : 'border-gray-200 text-gray-500'}`}
                        >
                          7d view
                        </button>
                      </div>
                      {trendPoints ? (
                        <div className="bg-gray-50 rounded-lg p-4">
                          <svg viewBox="0 0 200 80" className="w-full h-20">
                            <polyline
                              fill="none"
                              stroke="#94a3b8"
                              strokeWidth="2"
                              points={trendPoints.series
                                .map((value, index) => {
                                  const x = 20 + (index / Math.max(trendPoints.series.length - 1, 1)) * 160;
                                  const y = 70 - (value / trendPoints.max) * 50;
                                  return `${x},${y}`;
                                })
                                .join(' ')}
                            />
                            {trendPoints.series.map((value, index) => {
                              const x = 20 + (index / Math.max(trendPoints.series.length - 1, 1)) * 160;
                              const y = 70 - (value / trendPoints.max) * 50;
                              return <circle key={index} cx={x} cy={y} r="3" fill={index === trendPoints.series.length - 1 ? '#f87171' : '#94a3b8'} />;
                            })}
                          </svg>
                          <div className="flex justify-between text-xs text-gray-500 mt-2">
                            <span>{trendPoints.startLabel}</span>
                            <span>{trendPoints.endLabel}</span>
                          </div>
                        </div>
                      ) : (
                        <p className="text-xs text-gray-500">No trend data</p>
                      )}
                    </div>
                  </div>
                ) : (
                  <p className="text-sm text-gray-500 mt-3">No metrics available</p>
                )}
                <pre className="bg-gray-900 text-green-200 rounded-lg p-4 mt-3 text-xs overflow-auto max-h-64">
                  {evidenceJson || 'No evidence'}
                </pre>
              </div>

              <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
                <h2 className="text-lg font-semibold text-gray-900">Anomalous order samples</h2>
                <div className="flex flex-wrap gap-3 mt-3">
                  <select
                    value={sampleStatus}
                    onChange={(event) => setSampleStatus(event.target.value)}
                    className="border border-gray-200 rounded-lg px-3 py-2 text-sm"
                  >
                    <option value="ALL">All statuses</option>
                    <option value="PENDING">PENDING</option>
                    <option value="CONFIRMED">CONFIRMED</option>
                    <option value="PREPARING">PREPARING</option>
                    <option value="READY_FOR_PICKUP">READY_FOR_PICKUP</option>
                    <option value="PICKED_UP">PICKED_UP</option>
                    <option value="IN_TRANSIT">IN_TRANSIT</option>
                    <option value="DELIVERED">DELIVERED</option>
                    <option value="CANCELLED">CANCELLED</option>
                  </select>
                  <input
                    value={minAmount}
                    onChange={(event) => setMinAmount(event.target.value)}
                    className="border border-gray-200 rounded-lg px-3 py-2 text-sm w-32"
                    placeholder="Min amount"
                    type="number"
                    min="0"
                  />
                  <input
                    value={maxAmount}
                    onChange={(event) => setMaxAmount(event.target.value)}
                    className="border border-gray-200 rounded-lg px-3 py-2 text-sm w-32"
                    placeholder="Max amount"
                    type="number"
                    min="0"
                  />
                  <select
                    value={sampleSort}
                    onChange={(event) => setSampleSort(event.target.value)}
                    className="border border-gray-200 rounded-lg px-3 py-2 text-sm"
                  >
                    <option value="TIME_DESC">Time: newest first</option>
                    <option value="TIME_ASC">Time: oldest first</option>
                    <option value="AMOUNT_DESC">Amount: high to low</option>
                    <option value="AMOUNT_ASC">Amount: low to high</option>
                  </select>
                </div>
                {filteredSamples.length === 0 ? (
                  <p className="text-sm text-gray-500 mt-3">No sample orders</p>
                ) : (
                  <div className="mt-4 space-y-3">
                    {filteredSamples.map((order) => (
                      <button
                        key={order.orderId}
                        type="button"
                        onClick={() => {
                          sessionStorage.setItem('adminTicketReturnId', String(id));
                          sessionStorage.setItem('adminTicketReturnScroll', String(window.scrollY));
                          navigate(`/admin/orders/${order.orderId}`);
                        }}
                        className="text-left w-full border border-gray-100 rounded-lg p-4 hover:border-indigo-300 hover:shadow-sm transition"
                      >
                        <div className="flex flex-wrap items-center justify-between gap-2">
                          <div>
                            <p className="text-sm font-semibold text-gray-900">
                              Order {order.orderNumber}
                            </p>
                            <p className="text-xs text-gray-500">Customer: {order.customerName}</p>
                          </div>
                          <span className="text-xs px-2 py-1 rounded-full bg-slate-100 text-slate-700">
                            {order.status}
                          </span>
                        </div>
                        <div className="mt-2 text-xs text-gray-500 space-y-1">
                          <div>Created at: {order.createdAt ? new Date(order.createdAt).toLocaleString() : '--'}</div>
                          <div>Delivery fee: ¥{Number(order.deliveryFee || 0).toFixed(2)} · Tip: ¥{Number(order.tipAmount || 0).toFixed(2)}</div>
                          <div>Total: ¥{Number(order.totalAmount || 0).toFixed(2)}</div>
                        </div>
                      </button>
                    ))}
                  </div>
                )}
              </div>

              <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
                <div className="flex items-center gap-2">
                  <MessageSquarePlus className="w-5 h-5 text-indigo-600" />
                  <h2 className="text-lg font-semibold text-gray-900">Comments / Agent suggestions</h2>
                </div>
                <div className="space-y-4 mt-4">
                  {(ticket.comments || []).map((item) => {
                    const displayContent = shouldTranslateContent(item)
                      ? translateTicketContent(item.content)
                      : item.content;
                    return (
                      <div key={item.id} className="border border-gray-100 rounded-lg p-4">
                        <div className="flex items-center justify-between text-sm text-gray-500">
                          <span>{item.author} · {item.authorRole || 'SYSTEM'} · {item.type}</span>
                          <span>{item.createdAt ? new Date(item.createdAt).toLocaleString() : '--'}</span>
                        </div>
                        <p className="text-gray-700 mt-2 whitespace-pre-line">{displayContent}</p>
                      </div>
                    );
                  })}
                </div>

                <div className="mt-4">
                  <textarea
                    value={comment}
                    onChange={(event) => setComment(event.target.value)}
                    rows={4}
                    className="w-full border border-gray-200 rounded-lg p-3 focus:ring-2 focus:ring-indigo-500"
                    placeholder="Add handling notes or remarks"
                  />
                  <button
                    type="button"
                    onClick={handleAddComment}
                    className="mt-3 inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
                  >
                    <MessageSquarePlus className="w-4 h-4" />
                    Add comment
                  </button>
                </div>
              </div>
            </div>

            <div className="space-y-6">
              <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
                <h2 className="text-lg font-semibold text-gray-900">Handling status</h2>
                <div className="mt-4 space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-600">Status</label>
                    <select
                      value={status}
                      onChange={(event) => setStatus(event.target.value)}
                      className="mt-2 w-full border border-gray-200 rounded-lg px-3 py-2"
                    >
                      {statusOptions.map((item) => (
                        <option key={item} value={item}>{item}</option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-600">Assigned role</label>
                    <select
                      value={assignedRole}
                      onChange={(event) => setAssignedRole(event.target.value)}
                      className="mt-2 w-full border border-gray-200 rounded-lg px-3 py-2"
                    >
                      {roleOptions.map((item) => (
                        <option key={item} value={item}>{item}</option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-600">Assign to</label>
                    <input
                      value={assignedTo}
                      onChange={(event) => setAssignedTo(event.target.value)}
                      className="mt-2 w-full border border-gray-200 rounded-lg px-3 py-2"
                      placeholder="Email or name"
                    />
                  </div>

                  <button
                    type="button"
                    onClick={handleUpdate}
                    className="inline-flex items-center gap-2 px-4 py-2 bg-gray-900 text-white rounded-lg hover:bg-gray-800"
                  >
                    <Save className="w-4 h-4" />
                    Save changes
                  </button>
                  <button
                    type="button"
                    onClick={handleQuickResolve}
                    className="inline-flex items-center gap-2 px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
                  >
                    <CheckCircle2 className="w-4 h-4" />
                    Mark resolved
                  </button>
                </div>
              </div>

              <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
                <h2 className="text-lg font-semibold text-gray-900">Status timeline</h2>
                <div className="mt-4 space-y-4">
                  {timelineItems.length === 0 ? (
                    <p className="text-sm text-gray-500">No records</p>
                  ) : (
                    timelineItems.map((item) => (
                      <div key={item.id} className="flex gap-3">
                        <div className="mt-1 w-2 h-2 rounded-full bg-indigo-500" />
                        <div>
                          <p className="text-sm font-medium text-gray-900">{item.status}</p>
                          <p className="text-xs text-gray-500">{item.message}</p>
                          {item.summaryType === 'EVIDENCE_SNAPSHOT' && (
                            <div className="mt-2 text-xs text-gray-600 space-y-1">
                              {item.summaryText && (
                                <p className="text-gray-700">{translateTicketContent(item.summaryText)}</p>
                              )}
                              {item.reasonTags?.length > 0 && (
                                <p>Tags: {translateTicketContent(item.reasonTags.join(' / '))}</p>
                              )}
                              {item.ratingSummary?.averageRating !== undefined && (
                                <p>
                                  Rating: {Number(item.ratingSummary.averageRating || 0).toFixed(2)}
                                  · Reviews: {item.ratingSummary.totalReviews || 0}
                                </p>
                              )}
                            </div>
                          )}
                          {item.summaryType === 'ACTION_EXECUTION' && item.actionType && (
                            <p className="text-xs text-indigo-600 mt-2">Action executed: {item.actionType}</p>
                          )}
                          {item.summaryType === 'ACTION_RESULT' && (
                            <p className="text-xs text-emerald-600 mt-2">
                              Result write-back: {item.actionType || 'Action'} · {item.resultStatus || ''}
                              {item.resultMessage ? ` · ${item.resultMessage}` : ''}
                            </p>
                          )}
                          {item.summaryType === 'SLA_ALERT' && (
                            <p className="text-xs text-red-600 mt-2">
                              SLA overdue alert: {item.overdueMinutes ? `${(item.overdueMinutes / 60).toFixed(1)}h` : 'Overdue'}
                              {item.slaHours ? ` · SLA ${item.slaHours}h` : ''}
                            </p>
                          )}
                          {item.summaryType === 'AUTO_CLOSE' && (
                            <p className="text-xs text-gray-600 mt-2">
                              System auto-closed ticket · {item.reason || 'Resolved for over 24 hours'}
                            </p>
                          )}
                          {(item.operator || item.assignedTo || item.assignedRole) && (
                            <p className="text-xs text-gray-400 mt-1">
                              {item.operator ? `Operator: ${item.operator}` : 'Operator: --'}
                              {(item.assignedTo || item.assignedRole)
                                ? ` · Assigned: ${item.assignedTo || '--'}${item.assignedRole ? ` (${item.assignedRole})` : ''}`
                                : ''}
                            </p>
                          )}
                          <p className="text-xs text-gray-400 mt-1">
                            {item.createdAt ? new Date(item.createdAt).toLocaleString() : '--'}
                          </p>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </div>

              <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
                <h2 className="text-lg font-semibold text-gray-900">Agent actions</h2>
                <div className="mt-4 grid gap-3">
                  <button
                    type="button"
                    onClick={() => handleExecuteAction('NOTIFY_RESTAURANT', 'Notify restaurant to check prep/inventory issues')}
                    className="px-4 py-2 rounded-lg border border-gray-200 text-sm text-gray-700 hover:bg-gray-50"
                    disabled={actionLoading}
                  >
                    Notify restaurant
                  </button>
                  <button
                    type="button"
                    onClick={() => handleExecuteAction('NOTIFY_DRIVER', 'Notify driver/dispatch about delayed orders')}
                    className="px-4 py-2 rounded-lg border border-gray-200 text-sm text-gray-700 hover:bg-gray-50"
                    disabled={actionLoading}
                  >
                    Notify driver/dispatch
                  </button>
                  <button
                    type="button"
                    onClick={() => handleExecuteAction('ISSUE_COMPENSATION', 'Recommend support issue compensation')}
                    className="px-4 py-2 rounded-lg border border-gray-200 text-sm text-gray-700 hover:bg-gray-50"
                    disabled={actionLoading}
                  >
                    Recommend compensation
                  </button>
                  <button
                    type="button"
                    onClick={() => handleExecuteAction('PAUSE_ORDERS', 'Pause orders temporarily and monitor', true)}
                    className="px-4 py-2 rounded-lg border border-red-200 text-sm text-red-600 hover:bg-red-50"
                    disabled={actionLoading}
                  >
                    Pause orders (mark resolved)
                  </button>
                  <button
                    type="button"
                    onClick={() => handleExecuteAction('ESCALATE_ENGINEERING', 'Escalate to engineering for payment/system issues')}
                    className="px-4 py-2 rounded-lg border border-gray-200 text-sm text-gray-700 hover:bg-gray-50"
                    disabled={actionLoading}
                  >
                    Escalate to engineering
                  </button>
                </div>
              </div>

              <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
                <h2 className="text-lg font-semibold text-gray-900">Action audit</h2>
                {actionLogs.length === 0 ? (
                  <p className="text-sm text-gray-500 mt-3">No action logs</p>
                ) : (
                  <div className="mt-4 space-y-3">
                    {actionLogs.map((log) => (
                      <div key={log.id} className="border border-gray-100 rounded-lg p-3">
                        <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
                          <span className="font-medium text-gray-900">{log.actionType}</span>
                          <span className={`text-xs px-2 py-1 rounded-full ${log.status === 'SUCCESS' ? 'bg-green-100 text-green-700' : log.status === 'FAILED' ? 'bg-red-100 text-red-700' : 'bg-amber-100 text-amber-700'}`}>
                            {log.status}
                          </span>
                        </div>
                        <p className="text-xs text-gray-500 mt-1">{log.note || '—'}</p>
                        {log.resultMessage && (
                          <p className="text-xs text-gray-600 mt-1">Result: {log.resultMessage}</p>
                        )}
                        <p className="text-xs text-gray-400 mt-1">
                          {log.operator || 'SYSTEM'} · {log.createdAt ? new Date(log.createdAt).toLocaleString() : '--'}
                        </p>
                        {log.status === 'PENDING' && (
                          <div className="mt-2 flex flex-wrap gap-2">
                            <button
                              type="button"
                              onClick={() => handleUpdateActionResult(log.id, 'SUCCESS', 'Notification sent')}
                              className="px-2 py-1 text-xs rounded bg-green-50 text-green-700"
                              disabled={actionLoading}
                            >
                              Mark success
                            </button>
                            <button
                              type="button"
                              onClick={() => handleUpdateActionResult(log.id, 'FAILED', 'Execution failed, needs follow-up')}
                              className="px-2 py-1 text-xs rounded bg-red-50 text-red-700"
                              disabled={actionLoading}
                            >
                              Mark failed
                            </button>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>

              <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
                <div className="flex items-center justify-between">
                  <h2 className="text-lg font-semibold text-gray-900">One-click summary report</h2>
                  <button
                    type="button"
                    onClick={handleSummary}
                    className="inline-flex items-center gap-2 px-3 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
                    disabled={summaryLoading}
                  >
                    <FileText className="w-4 h-4" />
                    {summaryLoading ? 'Generating...' : 'Generate summary'}
                  </button>
                </div>
                {summary ? (
                  <div className="mt-4">
                    <p className="text-xs text-gray-400">
                      Generated at: {summary.generatedAt ? new Date(summary.generatedAt).toLocaleString() : '--'}
                    </p>
                    <pre className="mt-3 whitespace-pre-wrap text-sm text-gray-700 bg-slate-50 border border-slate-100 rounded-lg p-3">
                      {translateTicketContent(summary.summary)}
                    </pre>
                  </div>
                ) : (
                  <p className="text-sm text-gray-500 mt-3">Click the button to generate the latest summary.</p>
                )}
              </div>

              <div className="bg-white rounded-xl shadow-sm p-6 border border-gray-100">
                <h2 className="text-lg font-semibold text-gray-900">Related info</h2>
                <div className="mt-3 text-sm text-gray-600 space-y-2">
                  <div>Restaurant ID: {ticket.restaurantId || '--'}</div>
                  <div>Order ID: {ticket.orderId || '--'}</div>
                  <div>Driver ID: {ticket.driverId || '--'}</div>
                  <div>Created by: {ticket.createdBy || '--'}</div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default AdminTicketDetail;
