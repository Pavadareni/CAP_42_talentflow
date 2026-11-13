
import { useEffect, useMemo, useState } from 'react';
import {
  Plus, Search, X, Eye, RefreshCw, ArrowUpDown,
  Users, Building2, BriefcaseBusiness
} from 'lucide-react';
import { api } from '../api/client';

const departments = [
  'Engineering',
  'IT',
  'Data Science',
  'Artificial Intelligence',
  'Human Resources',
  'Finance',
  'Operations',
  'Marketing',
  'Sales',
  'Quality Assurance'
];

const emptyForm = {
  employeeCode: '',
  name: '',
  email: '',
  department: '',
  weeklyCapacityHours: '40',
  role: 'EMPLOYEE',
  password: '',
  confirmPassword: ''
};

export default function Employees() {
  const [rows, setRows] = useState([]);
  const [form, setForm] = useState(emptyForm);

  const [search, setSearch] = useState('');
  const [departmentFilter, setDepartmentFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [sortField, setSortField] = useState('name');
  const [sortDirection, setSortDirection] = useState('asc');

  const [open, setOpen] = useState(false);
  const [selected, setSelected] = useState(null);

  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  async function loadEmployees() {
    setLoading(true);
    setError('');

    try {
      const data = await api.employees();
      setRows(Array.isArray(data) ? data : data?.content || []);
    } catch (err) {
      setError(
        err.response?.data?.message ||
        'Unable to load employees.'
      );
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadEmployees();
  }, []);

  function updateForm(field, value) {
    setForm(previous => ({
      ...previous,
      [field]: value
    }));
  }

  function openCreateModal() {
    setForm(emptyForm);
    setError('');
    setSuccess('');
    setOpen(true);
  }

  function closeCreateModal() {
    setOpen(false);
    setForm(emptyForm);
  }

  async function saveEmployee(e) {
    e.preventDefault();
    setError('');
    setSuccess('');

    if (form.password.length < 8) {
      setError('Password must contain at least 8 characters.');
      return;
    }

    if (form.password !== form.confirmPassword) {
      setError('Passwords do not match.');
      return;
    }

    const capacity = Number(form.weeklyCapacityHours);

    if (!Number.isFinite(capacity) || capacity <= 0 || capacity > 168) {
      setError('Weekly capacity must be between 0.01 and 168 hours.');
      return;
    }

    const payload = {
      employeeCode: form.employeeCode.trim(),
      name: form.name.trim(),
      email: form.email.trim().toLowerCase(),
      department: form.department,
      weeklyCapacityHours: capacity,
      role: form.role,
      password: form.password
    };

    setSaving(true);

    try {
      // Backend must create the employee profile AND linked login account.
      await api.createEmployee(payload);

      closeCreateModal();
      setSuccess(
        `${form.role === 'MANAGER' ? 'Manager' : 'Employee'} account created successfully.`
      );

      await loadEmployees();
    } catch (err) {
      setError(
        err.response?.data?.message ||
        err.response?.data?.detail ||
        'Unable to create employee account.'
      );
    } finally {
      setSaving(false);
    }
  }

  async function deactivateEmployee(employee) {
    if (!window.confirm(`Deactivate ${employee.name}?`)) return;

    setError('');
    setSuccess('');

    try {
      await api.deactivateEmployee(employee.id);
      setSuccess(`${employee.name} has been deactivated.`);
      await loadEmployees();
    } catch (err) {
      setError(
        err.response?.data?.message ||
        'Unable to deactivate employee.'
      );
    }
  }

  function changeSort(field) {
    if (sortField === field) {
      setSortDirection(previous =>
        previous === 'asc' ? 'desc' : 'asc'
      );
    } else {
      setSortField(field);
      setSortDirection('asc');
    }
  }

  const filteredEmployees = useMemo(() => {
    let result = rows.filter(employee => {
      const searchText = [
        employee.name,
        employee.employeeCode,
        employee.email,
        employee.department
      ].join(' ').toLowerCase();

      const matchesSearch = searchText.includes(search.toLowerCase());

      const matchesDepartment =
        departmentFilter === 'ALL' ||
        employee.department === departmentFilter;

      const matchesStatus =
        statusFilter === 'ALL' ||
        (statusFilter === 'ACTIVE' && employee.active) ||
        (statusFilter === 'INACTIVE' && !employee.active);

      return matchesSearch && matchesDepartment && matchesStatus;
    });

    result.sort((a, b) => {
      const first = a[sortField] ?? '';
      const second = b[sortField] ?? '';

      const comparison =
        typeof first === 'number' && typeof second === 'number'
          ? first - second
          : String(first).localeCompare(String(second));

      return sortDirection === 'asc' ? comparison : -comparison;
    });

    return result;
  }, [rows, search, departmentFilter, statusFilter, sortField, sortDirection]);

  return (
    <div className="page">

      <div className="page-heading">
        <div>
          <small className="eyebrow">WORKFORCE DIRECTORY</small>
          <h1>Employees</h1>
          <p>Manage employee records, accounts, skills and weekly capacity.</p>
        </div>

        <button className="primary" onClick={openCreateModal}>
          <Plus size={16} />
          Add employee
        </button>
      </div>

      {success && <div className="success">{success}</div>}
      {error && <div className="error">{error}</div>}

      <section className="panel">

        <div className="panel-head">
          <h3>
            <Users size={17} style={{ verticalAlign: 'middle', marginRight: 7 }} />
            Employee directory
          </h3>
          <span>{filteredEmployees.length} records</span>
        </div>

        <div className="employee-filters">

          <div className="search">
            <Search size={16} />
            <input
              placeholder="Search name, code or email..."
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>

          <select
            value={departmentFilter}
            onChange={e => setDepartmentFilter(e.target.value)}
          >
            <option value="ALL">All departments</option>
            {departments.map(department => (
              <option key={department} value={department}>
                {department}
              </option>
            ))}
          </select>

          <select
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value)}
          >
            <option value="ALL">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="INACTIVE">Inactive</option>
          </select>

          <button
            className="outline"
            type="button"
            onClick={loadEmployees}
            title="Refresh employees"
          >
            <RefreshCw size={16} />
          </button>

        </div>

        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>
                  <button className="sort-button" onClick={() => changeSort('name')}>
                    Employee <ArrowUpDown size={13} />
                  </button>
                </th>

                <th>
                  <button className="sort-button" onClick={() => changeSort('department')}>
                    Department <ArrowUpDown size={13} />
                  </button>
                </th>

                <th>
                  <button className="sort-button" onClick={() => changeSort('weeklyCapacityHours')}>
                    Weekly capacity <ArrowUpDown size={13} />
                  </button>
                </th>

                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>

            <tbody>
              {filteredEmployees.map(employee => (
                <tr key={employee.id}>
                  <td>
                    <b>{employee.name}</b>
                    <small>
                      {employee.employeeCode} · {employee.email}
                    </small>
                  </td>

                  <td>{employee.department || '—'}</td>

                  <td>{employee.weeklyCapacityHours} hrs</td>

                  <td>
                    <span className={`status ${employee.active ? 'active' : 'inactive'}`}>
                      {employee.active ? 'Active' : 'Inactive'}
                    </span>
                  </td>

                  <td>
                    <button
                      className="outline"
                      onClick={() => setSelected(employee)}
                    >
                      <Eye size={15} />
                      View
                    </button>

                    {employee.active && (
                      <button
                        className="text-danger"
                        onClick={() => deactivateEmployee(employee)}
                      >
                        Deactivate
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {loading && <div className="loading">Loading employees...</div>}

        {!loading && filteredEmployees.length === 0 && (
          <div className="empty">
            No employees found. Change your filters or add an employee.
          </div>
        )}

      </section>

      {/* CREATE EMPLOYEE ACCOUNT MODAL */}

      {open && (
        <div className="modal-bg">
          <form className="modal" onSubmit={saveEmployee}>

            <div className="modal-head">
              <div>
                <h3>Add employee</h3>
                <small>Create an employee profile and login account.</small>
              </div>

              <button
                type="button"
                className="icon-btn"
                onClick={closeCreateModal}
              >
                <X size={18} />
              </button>
            </div>

            <div className="form-grid">

              <label>
                Employee code
                <input
                  required
                  value={form.employeeCode}
                  placeholder="EMP005"
                  onChange={e => updateForm('employeeCode', e.target.value)}
                />
              </label>

              <label>
                Full name
                <input
                  required
                  value={form.name}
                  placeholder="Employee name"
                  onChange={e => updateForm('name', e.target.value)}
                />
              </label>

              <label>
                Work email
                <input
                  required
                  type="email"
                  value={form.email}
                  placeholder="employee@company.com"
                  onChange={e => updateForm('email', e.target.value)}
                />
              </label>

              <label>
                Department
                <select
                  required
                  value={form.department}
                  onChange={e => updateForm('department', e.target.value)}
                >
                  <option value="">Select department</option>
                  {departments.map(department => (
                    <option key={department} value={department}>
                      {department}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Weekly capacity (hours)
                <input
                  required
                  type="number"
                  min="0.01"
                  max="168"
                  step="0.01"
                  value={form.weeklyCapacityHours}
                  onChange={e => updateForm('weeklyCapacityHours', e.target.value)}
                />
              </label>

              <label>
                Account role
                <select
                  required
                  value={form.role}
                  onChange={e => updateForm('role', e.target.value)}
                >
                  <option value="EMPLOYEE">Employee</option>
                  <option value="MANAGER">Manager</option>
                </select>
              </label>

              <label>
                Initial password
                <input
                  required
                  type="password"
                  minLength="8"
                  autoComplete="new-password"
                  placeholder="Minimum 8 characters"
                  value={form.password}
                  onChange={e => updateForm('password', e.target.value)}
                />
              </label>

              <label>
                Confirm password
                <input
                  required
                  type="password"
                  minLength="8"
                  autoComplete="new-password"
                  placeholder="Re-enter password"
                  value={form.confirmPassword}
                  onChange={e => updateForm('confirmPassword', e.target.value)}
                />
              </label>

            </div>

            <p className="form-hint">
              The initial password is securely hashed by the backend.
              Share it with the employee through a secure channel.
            </p>

            <div className="modal-actions">
              <button
                type="button"
                className="outline"
                onClick={closeCreateModal}
              >
                Cancel
              </button>

              <button className="primary" disabled={saving}>
                {saving ? 'Creating account...' : 'Create account'}
              </button>
            </div>

          </form>
        </div>
      )}

      {/* EMPLOYEE DETAILS MODAL */}

      {selected && (
        <div className="modal-bg">
          <div className="modal">

            <div className="modal-head">
              <div>
                <h3>Employee details</h3>
                <small>Workforce profile information</small>
              </div>

              <button
                className="icon-btn"
                onClick={() => setSelected(null)}
              >
                <X size={18} />
              </button>
            </div>

            <div className="detail-grid">

              <div>
                <small>Full name</small>
                <b>{selected.name}</b>
              </div>

              <div>
                <small>Employee code</small>
                <b>{selected.employeeCode}</b>
              </div>

              <div>
                <small>Work email</small>
                <b>{selected.email}</b>
              </div>

              <div>
                <small>Department</small>
                <b>{selected.department || 'Not assigned'}</b>
              </div>

              <div>
                <small>Weekly capacity</small>
                <b>{selected.weeklyCapacityHours} hours</b>
              </div>

              <div>
                <small>Status</small>
                <b>{selected.active ? 'Active' : 'Inactive'}</b>
              </div>

            </div>

            <div className="modal-actions">
              <button
                className="primary"
                onClick={() => setSelected(null)}
              >
                Close
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}
