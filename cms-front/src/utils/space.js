import { SPACE_SLUG_ID_MAP } from '@/consts/space'

export function resolveSpaceSlug (explicitSlug) {
  if (explicitSlug) {
    return explicitSlug
  }
  const segments = window.location.pathname.split('/').filter(Boolean)
  return segments[segments.length - 1] || ''
}

export function resolveSpaceId (explicitSlug, spacesStore) {
  const slug = resolveSpaceSlug(explicitSlug)
  if (spacesStore?.getSpaceId) {
    const id = spacesStore.getSpaceId(slug)
    if (id != null) {
      return id
    }
  }
  return SPACE_SLUG_ID_MAP[slug] ?? null
}
