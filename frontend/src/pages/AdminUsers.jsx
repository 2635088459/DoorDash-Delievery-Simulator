import { useEffect, useMemo, useState } from 'react';
import { Shield, UserCog } from 'lucide-react';
import toast from 'react-hot-toast';
import useAuthStore from '../store/authStore';
import { userAdminService } from '../services/apiService';

const roleOptions = ['CUSTOMER', 'RESTAURANT_OWNER', 'DRIVER', 'ADMIN'];

const AdminUsers = () => {
  const user = useAuthStore((state) => state.user);
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const loadUsers = async () => {
    try {
      setLoading(true);
      const data = await userAdminService.getAll();
      setUsers(data || []);
    } catch (error) {
      console.error('Failed to load users:', error);
  toast.error(error.response?.data?.message || 'Failed to load users');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (user?.role !== 'ADMIN') {
      return;
    }
    loadUsers();
  }, [user?.role]);

  const filteredUsers = useMemo(() => {
    const normalized = search.trim().toLowerCase();
    const base = roleFilter === 'ALL' ? users : users.filter((item) => item.role === roleFilter);
    const statusFiltered = statusFilter === 'ALL'
      ? base
      : base.filter((item) => (statusFilter === 'ACTIVE' ? item.isActive : !item.isActive));

    return normalized
      ? statusFiltered.filter((item) => {
        const haystack = [item.firstName, item.lastName, item.email, item.phoneNumber, item.role]
          .filter(Boolean)
          .join(' ')
          .toLowerCase();
        return haystack.includes(normalized);
      })
      : statusFiltered;
  }, [users, search, roleFilter, statusFilter]);

  const handleRoleChange = async (targetUser, nextRole) => {
    if (targetUser.role === nextRole) return;
    try {
      await userAdminService.updateRole(targetUser.id, { role: nextRole });
  toast.success(`Updated ${targetUser.email} role to ${nextRole}`);
      await loadUsers();
    } catch (error) {
      console.error('Failed to update role:', error);
  toast.error(error.response?.data?.message || 'Failed to update role');
    }
  };

  const handleToggleStatus = async (targetUser) => {
    try {
      await userAdminService.toggleStatus(targetUser.id, !targetUser.isActive);
  toast.success(`${targetUser.isActive ? 'Disabled' : 'Enabled'} ${targetUser.email}`);
      await loadUsers();
    } catch (error) {
      console.error('Failed to toggle user status:', error);
  toast.error(error.response?.data?.message || 'Failed to update status');
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        <div className="flex items-start justify-between">
          <div>
            <div className="flex items-center gap-3">
              <Shield className="w-7 h-7 text-indigo-600" />
              <h1 className="text-3xl font-bold text-gray-900">Access Management</h1>
            </div>
            <p className="text-gray-600 mt-2">Admins can adjust user roles and activation status here.</p>
          </div>
          <button
            type="button"
            onClick={loadUsers}
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
            placeholder="Search name/email/phone"
          />
          <select
            value={roleFilter}
            onChange={(event) => setRoleFilter(event.target.value)}
            className="border border-gray-200 rounded-lg px-3 py-2 text-sm"
          >
            <option value="ALL">All roles</option>
            {roleOptions.map((role) => (
              <option key={role} value={role}>{role}</option>
            ))}
          </select>
          <select
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
            className="border border-gray-200 rounded-lg px-3 py-2 text-sm"
          >
            <option value="ALL">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>
        </div>

        <div className="mt-6 bg-white rounded-xl shadow-sm border border-gray-100">
          {loading ? (
            <div className="p-6 text-center text-gray-500">Loading...</div>
          ) : filteredUsers.length === 0 ? (
            <div className="p-6 text-center text-gray-500">No users found</div>
          ) : (
            <div className="divide-y divide-gray-100">
              {filteredUsers.map((item) => (
                <div key={item.id} className="p-4 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                  <div>
                    <div className="flex items-center gap-2 text-gray-900 font-semibold">
                      <UserCog className="w-4 h-4 text-indigo-600" />
                      {item.firstName} {item.lastName}
                    </div>
                    <div className="text-sm text-gray-500 mt-1">
                      {item.email} · {item.phoneNumber}
                    </div>
                    <div className="text-xs text-gray-400 mt-1">
                      ID: {item.id} · Current role: {item.role}
                    </div>
                  </div>
                  <div className="flex flex-wrap items-center gap-3">
                    <select
                      value={item.role}
                      onChange={(event) => handleRoleChange(item, event.target.value)}
                      className="border border-gray-200 rounded-lg px-3 py-2 text-sm"
                    >
                      {roleOptions.map((role) => (
                        <option key={role} value={role}>{role}</option>
                      ))}
                    </select>
                    <button
                      type="button"
                      onClick={() => handleToggleStatus(item)}
                      className={`px-3 py-2 rounded-lg text-sm ${item.isActive ? 'bg-red-50 text-red-600' : 'bg-green-50 text-green-600'}`}
                    >
                      {item.isActive ? 'Disable' : 'Enable'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default AdminUsers;
