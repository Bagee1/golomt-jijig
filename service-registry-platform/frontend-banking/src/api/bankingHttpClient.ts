import { bankErrorMessage } from '../utils/bankingErrors'
import { ApiError, UNAUTHORIZED_EVENT } from './httpClient'

export class BankApiError extends ApiError {
  readonly code: string | null

  constructor(message: string, status: number, code: string | null) {
    super(message, status)
    this.name = 'BankApiError'
    this.code = code
  }
}

interface BankingRequestOptions extends RequestInit {
  token?: string | null
}

const BANKING_API_BASE_URL = import.meta.env.VITE_BANKING_API_URL ?? 'http://localhost:8084'

export async function bankingRequest<T>(path: string, options: BankingRequestOptions = {}): Promise<T> {
  const { token, headers, ...requestOptions } = options

  let response: Response
  try {
    response = await fetch(`${BANKING_API_BASE_URL}${path}`, {
      ...requestOptions,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...headers,
      },
    })
  } catch {
    throw new BankApiError('Banking service-тэй холбогдож чадсангүй (8084 порт унтарсан байж магадгүй)', 0, null)
  }

  if (!response.ok) {
    if (response.status === 401 && token) {
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT))
    }
    const { code, message } = await readErrorBody(response)
    throw new BankApiError(bankErrorMessage(code, message), response.status, code)
  }

  return response.json() as Promise<T>
}

async function readErrorBody(response: Response): Promise<{ code: string | null; message: string }> {
  try {
    const body = (await response.json()) as { code?: string; message?: string }
    return { code: body.code ?? null, message: body.message ?? response.statusText }
  } catch {
    return { code: null, message: response.statusText }
  }
}
