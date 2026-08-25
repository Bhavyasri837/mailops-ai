import { useCallback, useEffect, useState } from 'react'

/**
 * Generic data-fetching hook: runs `fetcher` on mount (and whenever `deps`
 * change), tracking loading/error/data so pages don't each hand-roll the
 * same three useState calls. `fetcher` must be stable-ish (wrap in
 * useCallback at the call site if it captures changing values).
 */
export function useFetch(fetcher, deps = []) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback(() => {
    setLoading(true)
    setError(null)
    return fetcher()
      .then((result) => {
        setData(result)
        return result
      })
      .catch((err) => {
        setError(err)
        throw err
      })
      .finally(() => setLoading(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  useEffect(() => {
    load().catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [load])

  return { data, setData, loading, error, refetch: load }
}
