import { Routes, Route, Navigate } from 'react-router-dom'
import { ToastProvider } from './hooks/useToast.jsx'
import Layout from './components/Layout.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Inbox from './pages/Inbox.jsx'
import EmailDetails from './pages/EmailDetails.jsx'
import HumanReview from './pages/HumanReview.jsx'
import Invoices from './pages/Invoices.jsx'
import Tasks from './pages/Tasks.jsx'
import ReplyDrafts from './pages/ReplyDrafts.jsx'
import AuditLog from './pages/AuditLog.jsx'

function App() {
  return (
    <ToastProvider>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/inbox" element={<Inbox />} />
          <Route path="/inbox/:id" element={<EmailDetails />} />
          <Route path="/review" element={<HumanReview />} />
          <Route path="/invoices" element={<Invoices />} />
          <Route path="/tasks" element={<Tasks />} />
          <Route path="/reply-drafts" element={<ReplyDrafts />} />
          <Route path="/audit" element={<AuditLog />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Route>
      </Routes>
    </ToastProvider>
  )
}

export default App
