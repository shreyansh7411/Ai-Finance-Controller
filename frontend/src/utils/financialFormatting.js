const CATEGORY_LABELS = {
  AMOUNT_MISMATCH: 'Amount mismatch',
  MISSING_SETTLEMENT: 'Missing settlement',
  DUPLICATE_PAYMENT: 'Duplicate payment',
  REFUND_MISMATCH: 'Refund mismatch',
  ADJUSTMENT_MISMATCH: 'Adjustment mismatch',
  TIMING_MISMATCH: 'Timing mismatch',
  FEE_MISMATCH: 'Fee mismatch',
  TAX_MISMATCH: 'Tax mismatch',
}

const MATCH_TYPE_LABELS = {
  EXACT_MATCH: 'Exact match',
  AMOUNT_MISMATCH: 'Amount mismatch',
  MISSING_SETTLEMENT: 'Missing settlement',
  DUPLICATE_PAYMENT: 'Duplicate payment',
  REFUND_MISMATCH: 'Refund mismatch',
  ADJUSTMENT_MISMATCH: 'Adjustment mismatch',
  TIMING_MISMATCH: 'Timing mismatch',
  FEE_MISMATCH: 'Fee mismatch',
  TAX_MISMATCH: 'Tax mismatch',
}

const STATUS_LABELS = {
  RESOLVED: 'Resolved',
  UNRESOLVED: 'Unresolved',
  INVESTIGATING: 'Under investigation',
  IGNORED: 'Ignored',
  INSUFFICIENT_EVIDENCE: 'Insufficient evidence',
  AUTO_RESOLVE: 'Automatically resolved',
  HUMAN_REVIEW: 'Human review',
}

const SEVERITY_LABELS = {
  LOW: 'Low',
  MEDIUM: 'Medium',
  HIGH: 'High',
  CRITICAL: 'Critical',
}

const AUDIT_ACTION_LABELS = {
  CREATED: 'Exception created',
  INVESTIGATED: 'AI investigation completed',
  DECIDED: 'Decision recorded',
  STATUS_UPDATED: 'Status updated',
  AUTO_RESOLVED: 'Automatically resolved',
  HUMAN_REVIEW: 'Human review performed',
}

const ACTOR_LABELS = {
  MERCHANT_UI: 'Merchant',
  AI: 'AI controller',
  SYSTEM: 'System',
  RECONCILIATION_ENGINE: 'Reconciliation engine',
}

const EVIDENCE_LABELS = {
  EXCEPTION_EXPECTED_AMOUNT: 'Expected amount',
  EXCEPTION_ACTUAL_AMOUNT: 'Actual amount',
  EXCEPTION_DIFFERENCE: 'Reconciliation difference',
  PAYMENT_ID: 'Payment',
  PAYMENT_AMOUNT: 'Payment amount',
  ORDER_ID: 'Order',
  ORDER_AMOUNT: 'Order amount',
  SETTLEMENT_ID: 'Settlement',
  SETTLEMENT_AMOUNT: 'Settlement amount',
  SETTLEMENT_FEES: 'Settlement fees',
  SETTLEMENT_TAX: 'Settlement tax',
  REFUND_ID: 'Refund',
  REFUND_AMOUNT: 'Refund amount',
  ADJUSTMENT_ID: 'Adjustment',
  ADJUSTMENT_AMOUNT: 'Adjustment amount',
  ADJUSTMENT_TYPE: 'Adjustment type',
}

function humanize(value) {
  if (value === null || value === undefined || value === '') {
    return '—'
  }

  return String(value)
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ')
}

export function formatCurrency(value, currency = 'INR') {
  if (value === null || value === undefined || value === '') {
    return '—'
  }

  const amount = Number(value)

  if (!Number.isFinite(amount)) {
    return '—'
  }

  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(amount)
}

export function formatPercentage(value, fractionDigits = 1) {
  if (value === null || value === undefined || value === '') {
    return '—'
  }

  const number = Number(value)

  if (!Number.isFinite(number)) {
    return '—'
  }

  return `${(number * 100).toFixed(fractionDigits)}%`
}

export function formatExceptionCategory(value) {
  return CATEGORY_LABELS[value] || humanize(value)
}

export function formatMatchType(value) {
  return MATCH_TYPE_LABELS[value] || humanize(value)
}

export function formatStatus(value) {
  return STATUS_LABELS[value] || humanize(value)
}

export function formatSeverity(value) {
  return SEVERITY_LABELS[value] || humanize(value)
}

export function formatAuditAction(value) {
  return AUDIT_ACTION_LABELS[value] || humanize(value)
}

export function formatActor(value) {
  return ACTOR_LABELS[value] || humanize(value)
}

export function formatEvidenceReference(value) {
  if (value === null || value === undefined || value === '') {
    return '—'
  }

  return EVIDENCE_LABELS[value] || humanize(value)
}

export function formatDateTime(value) {
  if (!value) {
    return '—'
  }

  const date = new Date(value)

  if (Number.isNaN(date.getTime())) {
    return '—'
  }

  return date.toLocaleString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
