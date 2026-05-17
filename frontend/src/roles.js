export const roleGroups = {
  all: ['ADMIN', 'WAREHOUSE_ADMIN', 'APPROVER', 'DEPT_USER', 'USER', 'PURCHASER', 'DISPATCHER'],
  admin: ['ADMIN'],
  warehouse: ['ADMIN', 'WAREHOUSE_ADMIN'],
  purchaser: ['ADMIN', 'WAREHOUSE_ADMIN', 'PURCHASER'],
  approver: ['ADMIN', 'APPROVER'],
  stockViewer: ['ADMIN', 'WAREHOUSE_ADMIN', 'APPROVER', 'PURCHASER', 'DISPATCHER'],
  applicant: ['ADMIN', 'WAREHOUSE_ADMIN', 'APPROVER', 'DEPT_USER', 'USER', 'DISPATCHER'],
  delivery: ['ADMIN', 'WAREHOUSE_ADMIN', 'DISPATCHER', 'APPROVER', 'DEPT_USER', 'USER'],
  analytics: ['ADMIN', 'WAREHOUSE_ADMIN', 'APPROVER', 'DEPT_USER', 'USER', 'PURCHASER', 'DISPATCHER'],
  warningViewer: ['ADMIN', 'WAREHOUSE_ADMIN', 'APPROVER'],
  logViewer: ['ADMIN', 'WAREHOUSE_ADMIN', 'APPROVER'],
  eventViewer: ['ADMIN', 'WAREHOUSE_ADMIN', 'APPROVER', 'DEPT_USER'],
  materialManager: ['ADMIN', 'WAREHOUSE_ADMIN', 'PURCHASER']
}

export const hasAnyRole = (roleCode, allowedRoles) => Array.isArray(allowedRoles) && allowedRoles.includes(roleCode)
