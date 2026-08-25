import { useCallback, useState } from 'react'
import { useOutletContext } from 'react-router-dom'
import { UserCheck } from 'lucide-react'
import Topbar from '../components/Topbar.jsx'
import ReviewPanel from '../components/ReviewPanel.jsx'
import LoadingSpinner from '../components/LoadingSpinner.jsx'
import ErrorState from '../components/ErrorState.jsx'
import EmptyState from '../components/EmptyState.jsx'
import { useFetch } from '../hooks/useFetch.js'
import { useToast } from '../hooks/useToast.jsx'
import { getEmails, getEmailClassification, reviewEmail } from '../services/api.js'

export default function HumanReview() {
  const { onMenuClick } = useOutletContext()
  const toast = useToast()
  const [submittingId, setSubmittingId] = useState(null)
  const [resolvedIds, setResolvedIds] = useState(new Set())

  const fetcher = useCallback(async () => {
    const emails = await getEmails({ status: 'NEEDS_REVIEW' })
    return Promise.all(
      emails.map(async (email) => {
        try {
          const classification = await getEmailClassification(email.id)
          return { email, classification }
        } catch {
          return { email, classification: null }
        }
      })
    )
  }, [])

  const { data: items, loading, error, refetch } = useFetch(fetcher)

  async function handleSubmit(emailId, intent) {
    setSubmittingId(emailId)
    try {
      await reviewEmail(emailId, intent)
      setResolvedIds((prev) => new Set(prev).add(emailId))
      toast.success('Human decision recorded — action executed.')
      setTimeout(refetch, 1200)
    } catch (err) {
      toast.error(err.message)
    } finally {
      setSubmittingId(null)
    }
  }

  return (
    <>
      <Topbar
        title="Human Review"
        subtitle="Emails below the confidence threshold, awaiting a decision"
        onMenuClick={onMenuClick}
      />

      <main className="flex-1 p-4 md:p-6 max-w-6xl w-full mx-auto">
        {loading && <LoadingSpinner label="Loading review queue…" />}
        {!loading && error && <ErrorState message={error.message} onRetry={refetch} />}

        {!loading && !error && items?.length === 0 && (
          <div className="card">
            <EmptyState
              icon={UserCheck}
              title="No emails require human review"
              description="Every email is either confidently classified or already resolved."
            />
          </div>
        )}

        {!loading && !error && items?.length > 0 && (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {items.map(({ email, classification }) => (
              <ReviewPanel
                key={email.id}
                email={email}
                classification={classification}
                submitting={submittingId === email.id}
                justResolved={resolvedIds.has(email.id)}
                onSubmit={(intent) => handleSubmit(email.id, intent)}
              />
            ))}
          </div>
        )}
      </main>
    </>
  )
}
