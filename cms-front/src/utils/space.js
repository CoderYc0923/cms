import { SPACE_SLUG_ID_MAP } from '@/consts/space'

export function resolveSpaceSlug (explicitSlug) {
  if (explicitSlug) {
    return explicitSlug
  }
  const segments = window.location.pathname.split('/').filter(Boolean)
  return segments[segments.length - 1] || ''
}

export function resolveSpaceId (explicitSlug) {
  const slug = resolveSpaceSlug(explicitSlug)
  return SPACE_SLUG_ID_MAP[slug] ?? null
}
