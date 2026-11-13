
import { useEffect, useMemo, useState } from 'react';
import {
  BriefcaseBusiness, Search, Plus, X, Pencil,
  Trash2, CalendarDays, Flag, Hash, RefreshCw,
  Eye
} from 'lucide-react';

import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';

const EMPTY_FORM = {
  projectCode: '',
  projectName: '',
  description: '',
  priority: 3,
  startDate: '',
  endDate: '',
  status: 'PLANNED'
};

const STATUSES = ['PLANNED', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'CANCELLED'];

export default function Projects() {
  const { user } = useAuth();
  const isAdmin = user?.role === 'ADMIN';

  const [rows, setRows] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [sortBy, setSortBy] = useState('name');

  const [showForm, setShowForm] = useState(false);
  const [editingProject, setEditingProject] = useState(null);
  const [selectedProject, setSelectedProject] = useState(null);

  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadProjects();
  }, []);

  const loadProjects = async () => {
    try {
      setLoading(true);
      setError('');

      const data = await api.projects();
      const projects = Array.isArray(data) ? data : data?.content || [];

      setRows(projects.filter(p => p.active !== false));
    } catch (e) {
      setError(e.response?.data?.message || 'Unable to load projects.');
    } finally {
      setLoading(false);
    }
  };

  const filteredProjects = useMemo(() => {
    let result = [...rows];

    const term = search.trim().toLowerCase();

    if (term) {
      result = result.filter(p =>
        (p.projectName || '').toLowerCase().includes(term) ||
        (p.projectCode || '').toLowerCase().includes(term) ||
        (p.description || '').toLowerCase().includes(term)
      );
    }

    if (statusFilter !== 'ALL') {
      result = result.filter(p => p.status === statusFilter);
    }

    result.sort((a, b) => {
      switch (sortBy) {
        case 'priority':
          return (a.priority ?? 999) - (b.priority ?? 999);
        case 'startDate':
          return (a.startDate || '').localeCompare(b.startDate || '');
        case 'endDate':
          return (a.endDate || '').localeCompare(b.endDate || '');
        default:
          return (a.projectName || '').localeCompare(b.projectName || '');
      }
    });

    return result;
  }, [rows, search, statusFilter, sortBy]);

  const formatDate = date => {
    if (!date) return 'Not specified';

    return new Date(`${date}T00:00:00`).toLocaleDateString('en-IN', {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  };

  const openCreateForm = () => {
    setEditingProject(null);
    setForm(EMPTY_FORM);
    setError('');
    setSuccess('');
    setShowForm(true);
  };

  const openEditForm = project => {
    setEditingProject(project);
    setForm({
      projectCode: project.projectCode || '',
      projectName: project.projectName || '',
      description: project.description || '',
      priority: project.priority ?? 3,
      startDate: project.startDate || '',
      endDate: project.endDate || '',
      status: project.status || 'PLANNED'
    });
    setError('');
    setShowForm(true);
  };

  const handleFormChange = e => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async e => {
    e.preventDefault();

    if (form.endDate < form.startDate) {
      setError('End date cannot be before start date.');
      return;
    }

    const payload = {
      ...form,
      projectCode: form.projectCode.trim(),
      projectName: form.projectName.trim(),
      description: form.description.trim(),
      priority: Number(form.priority)
    };

    try {
      setSaving(true);
      setError('');
      setSuccess('');

      if (editingProject) {
        await api.updateProject(editingProject.id, payload);
        setSuccess('Project updated successfully.');
      } else {
        await api.createProject(payload);
        setSuccess('Project created successfully.');
      }

      setShowForm(false);
      setEditingProject(null);
      setForm(EMPTY_FORM);
      await loadProjects();
    } catch (e) {
      setError(
        e.response?.data?.message ||
        e.response?.data?.error ||
        'Unable to save project. Please check the details.'
      );
    } finally {
      setSaving(false);
    }
  };

  const openDetails = async project => {
    try {
      setError('');
      const details = await api.projectById(project.id);
      setSelectedProject(details);
    } catch (e) {
      setError(e.response?.data?.message || 'Unable to load project details.');
    }
  };

  const deactivateProject = async project => {
    const confirmed = window.confirm(
      `Are you sure you want to deactivate "${project.projectName}"?`
    );

    if (!confirmed) return;

    try {
      setError('');
      setSuccess('');

      await api.deleteProject(project.id);

      setRows(prev => prev.filter(p => p.id !== project.id));
      setSelectedProject(null);
      setSuccess('Project deactivated successfully.');
    } catch (e) {
      setError(
        e.response?.data?.message ||
        'Unable to deactivate project. Check whether it has allocations.'
      );
    }
  };

  return (
    <div className="page">

      <div className="page-heading">
        <div>
          <small className="eyebrow">PROJECT PORTFOLIO</small>
          <h1>Projects</h1>
          <p>Manage project timelines, priorities and status.</p>
        </div>

        <div className="project-heading-actions">
          <button className="btn btn-secondary" onClick={loadProjects}>
            <RefreshCw size={16} /> Refresh
          </button>

          {isAdmin && (
            <button className="btn btn-primary" onClick={openCreateForm}>
              <Plus size={17} /> Create project
            </button>
          )}
        </div>
      </div>

      {error && <div className="error">{error}</div>}
      {success && <div className="success">{success}</div>}

      <section className="panel">

        <div className="panel-head">
          <h3>Project directory</h3>
          <span>{filteredProjects.length} of {rows.length} projects</span>
        </div>

        <div className="project-filters">

          <div className="search project-search">
            <Search size={17} />
            <input
              placeholder="Search project name, code or description..."
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
          </div>

          <select
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value)}
          >
            <option value="ALL">All statuses</option>
            {STATUSES.map(status => (
              <option key={status} value={status}>{status.replace('_', ' ')}</option>
            ))}
          </select>

          <select
            value={sortBy}
            onChange={e => setSortBy(e.target.value)}
          >
            <option value="name">Sort: Name</option>
            <option value="priority">Sort: Priority</option>
            <option value="startDate">Sort: Start date</option>
            <option value="endDate">Sort: End date</option>
          </select>

        </div>

        {loading ? (
          <div className="empty">Loading projects...</div>
        ) : (
          <div className="project-grid">

            {filteredProjects.map(project => (
              <article className="project-card" key={project.id}>

                <div className="project-top">
                  <i><BriefcaseBusiness size={18} /></i>
                  <span className={`status ${String(project.status || '').toLowerCase()}`}>
                    {project.status?.replace('_', ' ') || 'Unknown'}
                  </span>
                </div>

                <h3>{project.projectName}</h3>

                <p>{project.description || 'No description available.'}</p>

                <div className="project-dates">
                  <span>
                    Start
                    <b>{formatDate(project.startDate)}</b>
                  </span>
                  <span>
                    End
                    <b>{formatDate(project.endDate)}</b>
                  </span>
                </div>

                <div className="project-card-footer">
                  <span>Priority: {project.priority ?? '—'}</span>

                  <button
                    className="text-button"
                    onClick={() => openDetails(project)}
                  >
                    <Eye size={15} /> Details
                  </button>
                </div>

              </article>
            ))}

          </div>
        )}

        {!loading && filteredProjects.length === 0 && (
          <div className="empty">No matching projects found.</div>
        )}

      </section>

      {/* CREATE / EDIT PROJECT MODAL */}
      {showForm && (
        <div className="modal-bg" onClick={() => setShowForm(false)}>
          <div className="modal project-form-modal" onClick={e => e.stopPropagation()}>

            <div className="project-modal-header">
              <div>
                <small className="eyebrow">PROJECT MANAGEMENT</small>
                <h2>{editingProject ? 'Edit project' : 'Create project'}</h2>
              </div>

              <button className="icon-button" onClick={() => setShowForm(false)}>
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSubmit}>

              <div className="form-grid">

                <label>
                  Project code *
                  <input
                    name="projectCode"
                    value={form.projectCode}
                    onChange={handleFormChange}
                    placeholder="PRJ-001"
                    required
                  />
                </label>

                <label>
                  Project name *
                  <input
                    name="projectName"
                    value={form.projectName}
                    onChange={handleFormChange}
                    placeholder="Enter project name"
                    required
                  />
                </label>

                <label className="full">
                  Description
                  <textarea
                    name="description"
                    value={form.description}
                    onChange={handleFormChange}
                    placeholder="Describe the project..."
                    rows={3}
                  />
                </label>

                <label>
                  Priority *
                  <select
                    name="priority"
                    value={form.priority}
                    onChange={handleFormChange}
                    required
                  >
                    {[1, 2, 3, 4, 5].map(n => (
                      <option key={n} value={n}>{n}</option>
                    ))}
                  </select>
                </label>

                <label>
                  Status *
                  <select
                    name="status"
                    value={form.status}
                    onChange={handleFormChange}
                    required
                  >
                    {STATUSES.map(status => (
                      <option key={status} value={status}>
                        {status.replace('_', ' ')}
                      </option>
                    ))}
                  </select>
                </label>

                <label>
                  Start date *
                  <input
                    type="date"
                    name="startDate"
                    value={form.startDate}
                    onChange={handleFormChange}
                    required
                  />
                </label>

                <label>
                  End date *
                  <input
                    type="date"
                    name="endDate"
                    value={form.endDate}
                    onChange={handleFormChange}
                    min={form.startDate || undefined}
                    required
                  />
                </label>

              </div>

              {error && <div className="error">{error}</div>}

              <div className="project-modal-footer">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={() => setShowForm(false)}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="btn btn-primary"
                  disabled={saving}
                >
                  {saving ? 'Saving...' : editingProject ? 'Update project' : 'Create project'}
                </button>
              </div>

            </form>
          </div>
        </div>
      )}

      {/* PROJECT DETAILS MODAL */}
      {selectedProject && (
        <div className="modal-bg" onClick={() => setSelectedProject(null)}>
          <div className="modal project-detail-modal" onClick={e => e.stopPropagation()}>

            <div className="project-modal-header">
              <div>
                <small className="eyebrow">PROJECT DETAILS</small>
                <h2>{selectedProject.projectName}</h2>
              </div>

              <button className="icon-button" onClick={() => setSelectedProject(null)}>
                <X size={20} />
              </button>
            </div>

            <div className="project-detail-status">
              <span className={`status ${String(selectedProject.status || '').toLowerCase()}`}>
                {selectedProject.status?.replace('_', ' ')}
              </span>
            </div>

            <div className="project-detail-description">
              <label>Description</label>
              <p>{selectedProject.description || 'No description provided.'}</p>
            </div>

            <div className="detail-grid">

              <div>
                <Hash size={17} />
                <label>Project code</label>
                <strong>{selectedProject.projectCode}</strong>
              </div>

              <div>
                <Flag size={17} />
                <label>Priority</label>
                <strong>{selectedProject.priority}</strong>
              </div>

              <div>
                <CalendarDays size={17} />
                <label>Start date</label>
                <strong>{formatDate(selectedProject.startDate)}</strong>
              </div>

              <div>
                <CalendarDays size={17} />
                <label>End date</label>
                <strong>{formatDate(selectedProject.endDate)}</strong>
              </div>

              <div>
                <BriefcaseBusiness size={17} />
                <label>Project ID</label>
                <strong>#{selectedProject.id}</strong>
              </div>

            </div>

            {isAdmin && (
              <div className="project-detail-actions">
                <button
                  className="btn btn-secondary"
                  onClick={() => {
                    setSelectedProject(null);
                    openEditForm(selectedProject);
                  }}
                >
                  <Pencil size={15} /> Edit
                </button>

                <button
                  className="btn btn-danger"
                  onClick={() => deactivateProject(selectedProject)}
                >
                  <Trash2 size={15} /> Deactivate
                </button>
              </div>
            )}

            <div className="project-modal-footer">
              <button className="btn btn-primary" onClick={() => setSelectedProject(null)}>
                Close
              </button>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}
