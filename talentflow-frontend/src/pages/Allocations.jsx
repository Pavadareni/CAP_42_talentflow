
import { useEffect, useState } from 'react';
import { Play } from 'lucide-react';
import { api } from '../api/client';

function Table({ rows }) {
    return rows.length ? (
        <div className="table-wrap">
            <table>
                <thead>
                    <tr>
                        <th>Employee</th>
                        <th>Project</th>
                        <th>Period</th>
                        <th>Hours / week</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    {rows.map(x => (
                        <tr key={x.id}>
                            <td>
                                <b>{x.employeeName || `Employee #${x.employeeId}`}</b>
                            </td>
                            <td>{x.projectName || `Project #${x.projectId}`}</td>
                            <td>
                                {x.startDate}
                                <small>to {x.endDate}</small>
                            </td>
                            <td>{x.allocatedHoursPerWeek}</td>
                            <td>
                                <span className={`status ${String(x.status || '').toLowerCase()}`}>
                                    {x.status}
                                </span>
                            </td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    ) : (
        <div className="empty">No allocation records yet.</div>
    );
}

function Detail({ label, value }) {
    return (
        <div>
            <small>{label}</small>
            <b>{value ?? '—'}</b>
        </div>
    );
}

// Extract a structured result from either a normal response
// or an HTTP error response.
function extractOptimizationResult(response) {
    let data = response?.data ?? response;

    if (data?.detail) {
        let detail = data.detail;

        if (typeof detail === 'string') {
            try {
                detail = JSON.parse(detail);
            } catch {
                return null;
            }
        }

        if (detail && typeof detail === 'object' && detail.status) {
            return detail;
        }
    }

    if (data && typeof data === 'object' && data.status) {
        return data;
    }

    return null;
}

export default function Allocations() {
    const [rows, setRows] = useState([]);
    const [projects, setProjects] = useState([]);
    const [error, setError] = useState('');
    const [open, setOpen] = useState(false);
    const [loading, setLoading] = useState(false);

    const [projectId, setProjectId] = useState('');
    const [hours, setHours] = useState(20);
    const [result, setResult] = useState(null);

    async function load() {
        try {
            const [a, p] = await Promise.all([
                api.allocations(),
                api.projects()
            ]);

            setRows(Array.isArray(a) ? a : a?.content || []);
            setProjects(Array.isArray(p) ? p : p?.content || []);
        } catch (e) {
            setError(
                e.response?.data?.message ||
                'Unable to load allocations or projects.'
            );
        }
    }

    useEffect(() => {
        load();
    }, []);

    async function optimize(e) {
        e.preventDefault();

        setError('');
        setResult(null);
        setLoading(true);

        try {
            const response = await api.optimize(
            projectId,
            Number(hours)
        );
        if (!response?.status) {
    throw new Error(
        response?.message ||
        response?.detail ||
        'Invalid response from optimization service.'
    );
}

            const data = extractOptimizationResult(response);

            if (!data) {
                throw new Error('Invalid optimization response received.');
            }

            setResult(data);
            setOpen(false);

            // Reload allocation records only when proposals were saved.
            if (
                data.persistenceStatus === 'SAVED_AS_PROPOSED' ||
                Number(data.savedAllocationCount || 0) > 0
            ) {
                await load();
            }

        } catch (e) {
            // Some APIs return business results inside HTTP error responses.
            const data = extractOptimizationResult(e.response);

            if (data) {
                setResult(data);
                setOpen(false);
            } else {
                console.error('Optimization error:', e);

                setError(
                    e.response?.data?.message ||
                    e.response?.data?.detail ||
                    e.message ||
                    'Unable to communicate with the optimization service.'
                );
            }
        } finally {
            setLoading(false);
        }
    }

    function getStatusMessage(status) {
        switch (status) {
            case 'OPTIMAL':
                return 'Optimal project team selected successfully.';

            case 'INSUFFICIENT_CAPACITY':
                return 'No employees have sufficient remaining capacity for this allocation.';

            case 'INSUFFICIENT_SKILLS':
                return 'Not enough qualified employees are available to satisfy the mandatory skill requirements.';

            case 'INSUFFICIENT_CANDIDATES':
                return 'No eligible employees are available for this project.';

            case 'NO_REQUIREMENTS':
                return 'No skill requirements have been configured for this project.';

            default:
                return result?.message || 'Optimization completed.';
        }
    }

    return (
        <div className="page">

            <div className="page-heading">
                <div>
                    <small className="eyebrow">RESOURCE PLANNING</small>
                    <h1>Allocations</h1>
                    <p>
                        Review assignments and run skills-based optimization.
                    </p>
                </div>

                <button
                    className="primary"
                    onClick={() => {
                        setError('');
                        setResult(null);
                        setOpen(true);
                    }}
                >
                    <Play size={15} />
                    Optimize project
                </button>
            </div>

            {error && (
                <div className="error">
                    {error}
                </div>
            )}

            {result && (
                <section className="panel">
                    <div className="panel-head">
                        <h3>
                            Optimization result · {
                                result.projectName || `Project ${projectId}`
                            }
                        </h3>

                        <span className={`status ${String(result.status || '').toLowerCase()}`}>
                            {result.status}
                        </span>
                    </div>

                    <div className="optimization-message">
                        <h3>{getStatusMessage(result.status)}</h3>
                        <p>{result.message}</p>
                    </div>

                    <div className="details">
                        <Detail
                            label="Selected employees"
                            value={result.selectedEmployees?.length ?? 0}
                        />

                        <Detail
                            label="Eligible candidates"
                            value={result.eligibleCandidateCount ?? 0}
                        />

                        <Detail
                            label="Persistence"
                            value={result.persistenceStatus || 'NOT_SAVED'}
                        />

                        <Detail
                            label="Saved allocations"
                            value={result.savedAllocationCount ?? 0}
                        />
                    </div>

                    {result.selectedEmployees?.length > 0 && (
                        <>
                            <h3>Selected employees</h3>
                            <div className="table-wrap">
                                <table>
                                    <thead>
                                        <tr>
                                            <th>Employee</th>
                                            <th>Employee ID</th>
                                            <th>Final score</th>
                                            <th>Hours remaining</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {result.selectedEmployees.map(emp => (
                                            <tr key={emp.employeeId}>
                                                <td>{emp.employeeName}</td>
                                                <td>{emp.employeeId}</td>
                                                <td>{emp.finalScore}</td>
                                                <td>{emp.remainingCapacityHours}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </>
                    )}

                    {result.unfilledRequirements?.length > 0 && (
                        <>
                            <h3>Unfilled skill requirements</h3>
                            <div className="table-wrap">
                                <table>
                                    <thead>
                                        <tr>
                                            <th>Skill</th>
                                            <th>Required level</th>
                                            <th>Required employees</th>
                                            <th>Available qualified</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {result.unfilledRequirements.map((item, index) => (
                                            <tr key={`${item.skillId}-${index}`}>
                                                <td>{item.skillName}</td>
                                                <td>{item.requiredLevel}</td>
                                                <td>{item.requiredHeadcount}</td>
                                                <td>{item.availableQualifiedEmployees}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </>
                    )}

                    {result.capacityRejections?.length > 0 && (
                        <>
                            <h3>Employee rejection details</h3>
                            <div className="table-wrap">
                                <table>
                                    <thead>
                                        <tr>
                                            <th>Employee</th>
                                            <th>Reason</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {result.capacityRejections.map((item, index) => (
                                            <tr key={`${item.employeeId}-${index}`}>
                                                <td>
                                                    {item.employeeName || `Employee #${item.employeeId}`}
                                                </td>
                                                <td>
                                                    {[
                                                        ...(item.rejectionReasons || []),
                                                        ...(item.reason ? [item.reason] : [])
                                                    ].join(', ') || 'Not eligible'}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </>
                    )}
                </section>
            )}

            <section className="panel">
                <div className="panel-head">
                    <h3>Allocation register</h3>
                    <span>{rows.length} records</span>
                </div>

                <Table rows={rows} />
            </section>

            {open && (
                <div className="modal-bg">
                    <form className="modal" onSubmit={optimize}>
                        <div className="modal-head">
                            <h3>Optimize project allocation</h3>

                            <button
                                type="button"
                                className="icon-btn"
                                onClick={() => setOpen(false)}
                            >
                                ×
                            </button>
                        </div>

                        <label>
                            Project

                            <select
                                required
                                value={projectId}
                                onChange={e => setProjectId(e.target.value)}
                            >
                                <option value="">Select a project</option>

                                {projects.map(p => (
                                    <option key={p.id} value={p.id}>
                                        {p.projectName || `Project #${p.id}`}
                                    </option>
                                ))}
                            </select>
                        </label>

                        <label>
                            Requested hours per week

                            <input
                                type="number"
                                min="1"
                                max="168"
                                required
                                value={hours}
                                onChange={e => setHours(e.target.value)}
                            />
                        </label>

                        <div className="modal-actions">
                            <button
                                type="button"
                                className="outline"
                                onClick={() => setOpen(false)}
                                disabled={loading}
                            >
                                Cancel
                            </button>

                            <button
                                className="primary"
                                disabled={loading}
                            >
                                {loading ? 'Optimizing...' : 'Run optimization'}
                            </button>
                        </div>
                    </form>
                </div>
            )}

        </div>
    );
}
