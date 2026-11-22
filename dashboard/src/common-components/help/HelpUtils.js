export const HELP_MAX_TITLE_LENGTH = 100
export const HELP_MAX_BODY_LENGTH = 3000

export const scopeLabel = (thread) => {
  if (!thread) return ''
  if (thread.scopeType === 'PROJECT') return 'Project'
  const type = thread.scopeType === 'SUBJECT' ? 'Subject' : 'Skill'
  if (thread.skillDeleted) return `${type}: (deleted ${type.toLowerCase()})`
  return `${type}: ${thread.skillName}`
}

export const roleLabel = (role) => {
  if (role === 'ADMIN') return 'Admin'
  if (role === 'APPROVER') return 'Approver'
  return 'Student'
}

export const awaitingLabel = (thread) => {
  if (!thread || thread.status === 'RESOLVED') return ''
  return thread.awaiting === 'STAFF' ? 'Awaiting staff' : 'Awaiting student'
}
