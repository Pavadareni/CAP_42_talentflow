
import {
  Navigate,
  NavLink,
  Route,
  Routes
} from "react-router-dom";

import {
  BriefcaseBusiness,
  ChartNoAxesCombined,
  CheckCircle2,
  LayoutDashboard,
  LogOut,
  Network,
  Users,
  UserRound,
  Award
} from "lucide-react";

import { useAuth } from "./context/AuthContext";

import Login from "./pages/Login";
import Dashboard from "./pages/Dashboard";
import Employees from "./pages/Employees";
import EmployeeSkills from "./pages/EmployeeSkills";
import Projects from "./pages/Projects";
import Allocations from "./pages/Allocations";
import Approvals from "./pages/Approvals";
import MyProfile from "./pages/MyProfile";

function Layout() {
  const { user, logout } = useAuth();

  const isAdmin = user?.role === "ADMIN";
  const isManager = user?.role === "MANAGER";
  const canManageSkills = isAdmin || isManager;

  const nav = isAdmin
    ? [
        ["/", "Overview", LayoutDashboard],
        ["/employees", "Employees", Users],
        ["/employee-skills", "Employee Skills", Award],
        ["/projects", "Projects", BriefcaseBusiness],
        ["/allocations", "Allocations", ChartNoAxesCombined]
      ]
    : isManager
    ? [
        ["/", "Overview", LayoutDashboard],
        ["/employee-skills", "Employee Skills", Award],
        ["/projects", "Projects", BriefcaseBusiness],
        ["/allocations", "Allocations", ChartNoAxesCombined],
        ["/approvals", "Approvals", CheckCircle2]
      ]
    : [
        ["/", "My overview", LayoutDashboard],
        ["/my-profile", "My workspace", UserRound]
      ];

  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">
          <i>
            <Network size={19} />
          </i>

          <span>
            <b>TalentFlow</b>
            <small>RESOURCE OPTIMIZATION</small>
          </span>
        </div>

        <div className="nav-label">WORKSPACE</div>

        {nav.map(([to, label, Icon]) => (
          <NavLink
            end={to === "/"}
            key={to}
            to={to}
            className={({ isActive }) =>
              isActive ? "nav active" : "nav"
            }
          >
            <Icon size={18} />
            {label}
          </NavLink>
        ))}

        <div className="side-bottom">
          <div className="user-avatar">
            {(user?.name || "U")[0].toUpperCase()}
          </div>

          <div className="user-copy">
            <b>{user?.name}</b>
            <small>{user?.role}</small>
          </div>

          <button onClick={logout} title="Sign out">
            <LogOut size={17} />
          </button>
        </div>
      </aside>

      <main className="main">
        <header className="topbar">
          <div>
            <small>TALENTFLOW / WORKSPACE</small>
            <b>Resource management</b>
          </div>

          <span className="role-pill">{user?.role}</span>
        </header>

        <Routes>
          <Route path="/" element={<Dashboard />} />

          <Route
            path="/employees"
            element={
              isAdmin
                ? <Employees />
                : <Navigate to="/" />
            }
          />

          <Route
            path="/employee-skills"
            element={
              canManageSkills
                ? <EmployeeSkills />
                : <Navigate to="/" />
            }
          />

          <Route
            path="/projects"
            element={
              canManageSkills
                ? <Projects />
                : <Navigate to="/" />
            }
          />

          <Route
            path="/allocations"
            element={
              canManageSkills
                ? <Allocations />
                : <Navigate to="/" />
            }
          />

          <Route
            path="/approvals"
            element={
              isManager
                ? <Approvals />
                : <Navigate to="/" />
            }
          />

          <Route
            path="/my-profile"
            element={
              user?.role === "EMPLOYEE"
                ? <MyProfile />
                : <Navigate to="/" />
            }
          />

          <Route path="*" element={<Navigate to="/" />} />
        </Routes>
      </main>
    </div>
  );
}

export default function App() {
  const { isAuthenticated } = useAuth();

  return (
    <Routes>
      <Route
        path="/login"
        element={
          isAuthenticated
            ? <Navigate to="/" />
            : <Login />
        }
      />

      <Route
        path="/*"
        element={
          isAuthenticated
            ? <Layout />
            : <Navigate to="/login" />
        }
      />
    </Routes>
  );
}
