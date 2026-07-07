import { useEffect, useState } from 'react'
import { getSecurityScores } from '../api/securityApi'
import { getSystems, type SystemSearchParams } from '../api/systemsApi'
import { useAuth } from '../auth/useAuth'
import type { SystemResponse } from '../types/api'

export interface SystemWithScore extends SystemResponse {
  securityScore: number
}

interface SystemsWithScoresState {
  systems: SystemWithScore[]
  totalElements: number
  totalPages: number
  isLoading: boolean
  error: string | null
}

interface UseSystemsWithScoresParams extends SystemSearchParams {
  reloadKey?: number
}

export function useSystemsWithScores(params: UseSystemsWithScoresParams = {}) {
  const auth = useAuth()
  const { keyword, page, size, status, type, reloadKey } = params
  const [state, setState] = useState<SystemsWithScoresState>({
    systems: [],
    totalElements: 0,
    totalPages: 0,
    isLoading: true,
    error: null,
  })

  useEffect(() => {
    let cancelled = false

    async function loadSystems() {
      const token = auth.token
      if (!token) {
        setState((current) => ({ ...current, isLoading: false }))
        return
      }

      setState((current) => ({ ...current, isLoading: true, error: null }))

      try {
        const [systemsPage, scores] = await Promise.all([
          getSystems(token, { keyword, page, size, status, type }),
          getSecurityScores(token),
        ])
        const scoreBySystemId = new Map(scores.map((score) => [score.systemId, score.score]))

        if (!cancelled) {
          setState({
            systems: systemsPage.content.map((system) => ({
              ...system,
              securityScore: scoreBySystemId.get(system.id) ?? 0,
            })),
            totalElements: systemsPage.totalElements,
            totalPages: systemsPage.totalPages,
            isLoading: false,
            error: null,
          })
        }
      } catch (exception) {
        if (!cancelled) {
          setState({
            systems: [],
            totalElements: 0,
            totalPages: 0,
            isLoading: false,
            error: exception instanceof Error ? exception.message : 'Системүүд уншихад алдаа гарлаа',
          })
        }
      }
    }

    void loadSystems()

    return () => {
      cancelled = true
    }
  }, [auth.token, keyword, page, size, status, type, reloadKey])

  return state
}
