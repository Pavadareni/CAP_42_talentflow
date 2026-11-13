
import { useEffect, useMemo, useState } from "react";
import { api } from "../api/client";

const LEVELS = ["BEGINNER", "INTERMEDIATE", "ADVANCED"];

const emptyForm = {
  skillId: "",
  proficiencyLevel: "BEGINNER",
  lastVerifiedAt: "",
  evidenceSource: ""
};

export default function EmployeeSkills() {
  const [employees, setEmployees] = useState([]);
  const [skills, setSkills] = useState([]);
  const [selectedEmployee, setSelectedEmployee] = useState("");
  const [employeeSkills, setEmployeeSkills] = useState([]);

  const [form, setForm] = useState(emptyForm);
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const selectedSkillIds = useMemo(
    () => new Set(employeeSkills.map(item => Number(item.skillId))),
    [employeeSkills]
  );

  const availableSkills = useMemo(
    () => skills.filter(skill =>
      editing || !selectedSkillIds.has(Number(skill.id))
    ),
    [skills, selectedSkillIds, editing]
  );

  useEffect(() => {
    loadInitialData();
  }, []);

  useEffect(() => {
    if (selectedEmployee) {
      loadEmployeeSkills(selectedEmployee);
    } else {
      setEmployeeSkills([]);
    }
  }, [selectedEmployee]);

  async function loadInitialData() {
    setLoading(true);
    setError("");

    try {
      const [employeeData, skillData] = await Promise.all([
        api.employees(),
        api.skills()
      ]);

      const activeEmployees = (employeeData || []).filter(
        employee => employee.active !== false &&
                    employee.isActive !== false
      );

      setEmployees(activeEmployees);
      setSkills(skillData || []);

      if (activeEmployees.length > 0) {
        setSelectedEmployee(String(activeEmployees[0].id));
      }
    } catch (err) {
      setError(
        err.response?.data?.message ||
        err.response?.data?.error ||
        "Unable to load employees and skills."
      );
    } finally {
      setLoading(false);
    }
  }

  async function loadEmployeeSkills(employeeId) {
    try {
      setError("");
      const data = await api.employeeSkills(employeeId);
      setEmployeeSkills(data || []);
    } catch (err) {
      setError(
        err.response?.data?.message ||
        err.response?.data?.error ||
        "Unable to load employee skills."
      );
    }
  }

  function handleFormChange(event) {
    const { name, value } = event.target;
    setForm(previous => ({
      ...previous,
      [name]: value
    }));
  }

  function resetForm() {
    setForm(emptyForm);
    setEditing(false);
  }

  function startEdit(skill) {
    setForm({
      skillId: String(skill.skillId),
      proficiencyLevel: skill.proficiencyLevel,
      lastVerifiedAt: skill.lastVerifiedAt || "",
      evidenceSource: skill.evidenceSource || ""
    });

    setEditing(true);
    setError("");
    setSuccess("");
  }

  async function handleSubmit(event) {
    event.preventDefault();

    if (!selectedEmployee || !form.skillId) {
      setError("Please select an employee and a skill.");
      return;
    }

    setSaving(true);
    setError("");
    setSuccess("");

    const payload = {
      skillId: Number(form.skillId),
      proficiencyLevel: form.proficiencyLevel,
      lastVerifiedAt: form.lastVerifiedAt || null,
      evidenceSource: form.evidenceSource.trim() || null
    };

    try {
      if (editing) {
        await api.updateEmployeeSkill(
          selectedEmployee,
          form.skillId,
          payload
        );
        setSuccess("Employee skill updated successfully.");
      } else {
        await api.assignEmployeeSkill(
          selectedEmployee,
          payload
        );
        setSuccess("Skill assigned successfully.");
      }

      resetForm();
      await loadEmployeeSkills(selectedEmployee);
    } catch (err) {
      setError(
        err.response?.data?.message ||
        err.response?.data?.error ||
        err.response?.data?.detail ||
        "Unable to save employee skill."
      );
    } finally {
      setSaving(false);
    }
  }

  async function handleRemove(skill) {
    const confirmed = window.confirm(
      `Remove ${skill.skillName} from ${skill.employeeName}?`
    );

    if (!confirmed) return;

    setError("");
    setSuccess("");

    try {
      await api.removeEmployeeSkill(
        selectedEmployee,
        skill.skillId
      );

      if (editing && Number(form.skillId) === Number(skill.skillId)) {
        resetForm();
      }

      setSuccess(
        `${skill.skillName} removed from the employee's active skills.`
      );

      await loadEmployeeSkills(selectedEmployee);
    } catch (err) {
      setError(
        err.response?.data?.message ||
        err.response?.data?.error ||
        "Unable to remove employee skill."
      );
    }
  }

  const selectedEmployeeName = employees.find(
    employee => String(employee.id) === String(selectedEmployee)
  )?.name || "Employee";

  if (loading) {
    return <div className="skills-loading">Loading employee skills...</div>;
  }

  return (
    <div className="employee-skills-page">

      <div className="skills-page-header">
        <div>
          <span className="skills-eyebrow">TALENT MANAGEMENT</span>
          <h1>Employee Skills</h1>
          <p>Manage employee competencies and skill proficiency.</p>
        </div>

        <div className="skills-total">
          <span>Assigned skills</span>
          <strong>{employeeSkills.length}</strong>
        </div>
      </div>

      {error && <div className="skills-alert error">{error}</div>}
      {success && <div className="skills-alert success">{success}</div>}

      <section className="skills-panel employee-selector-panel">
        <div>
          <label htmlFor="employeeSelect">Select employee</label>
          <select
            id="employeeSelect"
            value={selectedEmployee}
            onChange={event => {
              resetForm();
              setSuccess("");
              setSelectedEmployee(event.target.value);
            }}
          >
            <option value="">Choose employee</option>
            {employees.map(employee => (
              <option key={employee.id} value={employee.id}>
                {employee.name} (ID: {employee.id})
              </option>
            ))}
          </select>
        </div>

        <div className="selected-employee-info">
          <span>Currently managing</span>
          <strong>{selectedEmployeeName}</strong>
        </div>
      </section>

      <div className="skills-content-grid">

        <section className="skills-panel skill-form-panel">
          <div className="panel-heading">
            <div>
              <h2>{editing ? "Edit employee skill" : "Assign a skill"}</h2>
              <p>
                {editing
                  ? "Update proficiency and verification details."
                  : "Add a competency to the selected employee."}
              </p>
            </div>
            {editing && (
              <button
                type="button"
                className="cancel-edit"
                onClick={resetForm}
              >
                Cancel
              </button>
            )}
          </div>

          <form onSubmit={handleSubmit}>
            <label htmlFor="skillId">Skill</label>
            <select
              id="skillId"
              name="skillId"
              value={form.skillId}
              onChange={handleFormChange}
              required
              disabled={editing}
            >
              <option value="">Select skill</option>
              {availableSkills.map(skill => (
                <option key={skill.id} value={skill.id}>
                  {skill.skillName}
                  {skill.category ? ` — ${skill.category}` : ""}
                </option>
              ))}
            </select>

            <label htmlFor="proficiencyLevel">Proficiency level</label>
            <select
              id="proficiencyLevel"
              name="proficiencyLevel"
              value={form.proficiencyLevel}
              onChange={handleFormChange}
              required
            >
              {LEVELS.map(level => (
                <option key={level} value={level}>
                  {level}
                </option>
              ))}
            </select>

            <label htmlFor="lastVerifiedAt">Last verified date</label>
            <input
              id="lastVerifiedAt"
              name="lastVerifiedAt"
              type="date"
              max={new Date().toISOString().split("T")[0]}
              value={form.lastVerifiedAt}
              onChange={handleFormChange}
            />

            <label htmlFor="evidenceSource">Evidence source</label>
            <input
              id="evidenceSource"
              name="evidenceSource"
              type="text"
              placeholder="e.g. Project experience, certification"
              value={form.evidenceSource}
              onChange={handleFormChange}
              maxLength={255}
            />

            <button
              type="submit"
              className="save-skill-button"
              disabled={saving || !selectedEmployee}
            >
              {saving
                ? "Saving..."
                : editing
                  ? "Update Skill"
                  : "Assign Skill"}
            </button>
          </form>
        </section>

        <section className="skills-panel assigned-skills-panel">
          <div className="panel-heading">
            <div>
              <h2>Assigned competencies</h2>
              <p>Active skills for {selectedEmployeeName}.</p>
            </div>
            <span className="skills-count">{employeeSkills.length}</span>
          </div>

          {employeeSkills.length === 0 ? (
            <div className="empty-skills">
              <div className="empty-icon">◇</div>
              <h3>No skills assigned yet</h3>
              <p>Select a skill from the form to get started.</p>
            </div>
          ) : (
            <div className="assigned-skills-list">
              {employeeSkills.map(skill => (
                <article className="assigned-skill-card" key={skill.id}>
                  <div className="assigned-skill-top">
                    <div>
                      <h3>{skill.skillName}</h3>
                      <span className="skill-category">
                        {skill.category || "Uncategorized"}
                      </span>
                    </div>

                    <span
                      className={`proficiency-badge ${skill.proficiencyLevel?.toLowerCase()}`}
                    >
                      {skill.proficiencyLevel}
                    </span>
                  </div>

                  <div className="skill-details">
                    <div>
                      <span>Last verified</span>
                      <strong>{skill.lastVerifiedAt || "Not verified"}</strong>
                    </div>
                    <div>
                      <span>Evidence</span>
                      <strong>{skill.evidenceSource || "Not provided"}</strong>
                    </div>
                  </div>

                  <div className="skill-actions">
                    <button
                      type="button"
                      className="edit-skill-button"
                      onClick={() => startEdit(skill)}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      className="remove-skill-button"
                      onClick={() => handleRemove(skill)}
                    >
                      Remove
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}
