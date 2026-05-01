[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$base = 'http://127.0.0.1:8080'
function Invoke-Api {
  param(
    [string]$Method,
    [string]$Path,
    [string]$Token,
    $Body,
    [hashtable]$Query
  )
  $uri = $base + $Path
  if ($Query -and $Query.Count -gt 0) {
    $pairs = foreach ($entry in $Query.GetEnumerator()) {
      '{0}={1}' -f [uri]::EscapeDataString([string]$entry.Key), [uri]::EscapeDataString([string]$entry.Value)
    }
    $uri = $uri + '?' + ($pairs -join '&')
  }
  $headers = @{}
  if ($Token) { $headers['Authorization'] = "Bearer $Token" }
  if ($null -ne $Body) {
    $jsonBody = $Body | ConvertTo-Json -Depth 10 -Compress
    $resp = Invoke-WebRequest -Uri $uri -Method $Method -Headers $headers -ContentType 'application/json' -Body $jsonBody -SkipHttpErrorCheck
  } else {
    $resp = Invoke-WebRequest -Uri $uri -Method $Method -Headers $headers -SkipHttpErrorCheck
  }
  $parsed = $null
  if ($resp.Content) {
    $parsed = $resp.Content | ConvertFrom-Json -Depth 20
  }
  [pscustomobject]@{
    Status = [int]$resp.StatusCode
    Body = $parsed
    Uri = $uri
  }
}
function Login([string]$username) {
  $resp = Invoke-Api -Method 'POST' -Path '/api/auth/login' -Body @{ username = $username; password = 'Abc@123456' }
  if ($resp.Status -ne 200) { throw "login failed for ${username}: $($resp.Status)" }
  return $resp.Body.data.accessToken
}
$summary = [ordered]@{}
$summary.timestamp = (Get-Date).ToString('s')
$suffix = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
$names = [ordered]@{
  Category = "TEST_BROWSER_USE_FIX_CAT_$suffix"
  Warehouse = "TEST_BROWSER_USE_FIX_WH_$suffix"
  MaterialCode = "TESTFIX$suffix"
  MaterialName = "TEST_BROWSER_USE_FIX_MAT_$suffix"
  Batch = "TESTFIXBATCH_$suffix"
  ApplyReason = "TEST_BROWSER_USE_FIX_APPLY_$suffix"
  TransferReason = "TEST_BROWSER_USE_FIX_TRANSFER_$suffix"
  EventTitle = "TEST_BROWSER_USE_FIX_EVENT_$suffix"
  UserName = "test_fix_$suffix"
  RoleCode = "TEST_FIX_ROLE_$suffix"
}
$summary.testNames = $names
$adminToken = Login 'admin'
$warehouseToken = Login 'warehouse'
$approverToken = Login 'approver'
$deptToken = Login 'dept'
$summary.startup = [ordered]@{
  backendProbe = (Invoke-Api -Method 'GET' -Path '/api/auth/me').Status
  adminLogin = 200
  warehouseLogin = 200
  approverLogin = 200
  deptLogin = 200
}
$summary.menus = [ordered]@{
  admin = (Invoke-Api -Method 'GET' -Path '/api/auth/menus' -Token $adminToken).Body.data.Count
  warehouse = (Invoke-Api -Method 'GET' -Path '/api/auth/menus' -Token $warehouseToken).Body.data.Count
  approver = (Invoke-Api -Method 'GET' -Path '/api/auth/menus' -Token $approverToken).Body.data.Count
  dept = (Invoke-Api -Method 'GET' -Path '/api/auth/menus' -Token $deptToken).Body.data.Count
}
$summary.permission = [ordered]@{
  meWithoutToken = (Invoke-Api -Method 'GET' -Path '/api/auth/me').Status
  warehouseTokenPolicy = (Invoke-Api -Method 'GET' -Path '/api/auth/token-policy' -Token $warehouseToken).Status
  deptWarehouseList = (Invoke-Api -Method 'GET' -Path '/api/warehouse/list' -Token $deptToken).Status
  adminTokenPolicy = (Invoke-Api -Method 'GET' -Path '/api/auth/token-policy' -Token $adminToken).Status
}
$overviewBefore = (Invoke-Api -Method 'GET' -Path '/api/analytics/overview' -Token $adminToken).Body.data
$summary.analyticsBefore = $overviewBefore
$categoryFirst = Invoke-Api -Method 'POST' -Path '/api/material/category' -Token $adminToken -Body @{ categoryName = $names.Category; remark = 'regression' }
$categorySecond = Invoke-Api -Method 'POST' -Path '/api/material/category' -Token $adminToken -Body @{ categoryName = $names.Category; remark = 'regression' }
$warehouseFirst = Invoke-Api -Method 'POST' -Path '/api/warehouse' -Token $adminToken -Body @{ warehouseName = $names.Warehouse; campus = 'science'; address = 'regression-store'; manager = 'tester' }
$warehouseSecond = Invoke-Api -Method 'POST' -Path '/api/warehouse' -Token $adminToken -Body @{ warehouseName = $names.Warehouse; campus = 'science'; address = 'regression-store'; manager = 'tester' }
$userMissingPwd = Invoke-Api -Method 'POST' -Path '/api/rbac/users' -Token $adminToken -Body @{ username = "${($names.UserName)}_nopwd"; realName = 'NoPwd'; deptId = 4; roleId = 3; status = 1 }
$userFirst = Invoke-Api -Method 'POST' -Path '/api/rbac/users' -Token $adminToken -Body @{ username = $names.UserName; password = 'Abc@123456'; realName = 'Regression User'; deptId = 4; roleId = 3; status = 1 }
$userSecond = Invoke-Api -Method 'POST' -Path '/api/rbac/users' -Token $adminToken -Body @{ username = $names.UserName; password = 'Abc@123456'; realName = 'Regression User'; deptId = 4; roleId = 3; status = 1 }
$roleFirst = Invoke-Api -Method 'POST' -Path '/api/rbac/roles' -Token $adminToken -Body @{ roleCode = $names.RoleCode; roleName = 'Regression Role'; description = 'regression' }
$roleSecond = Invoke-Api -Method 'POST' -Path '/api/rbac/roles' -Token $adminToken -Body @{ roleCode = $names.RoleCode; roleName = 'Regression Role'; description = 'regression' }
$categoryId = [int64]$categoryFirst.Body.data.id
$warehouseId = [int64]$warehouseFirst.Body.data.id
$materialCreate = Invoke-Api -Method 'POST' -Path '/api/material/info' -Token $adminToken -Body @{
  materialCode = $names.MaterialCode
  materialName = $names.MaterialName
  categoryId = $categoryId
  spec = 'Regression Spec'
  unit = 'pcs'
  safetyStock = 5
  shelfLifeDays = 60
  supplier = 'Regression Supplier'
  unitPrice = 12.5
  remark = 'regression'
}
$materialId = [int64]$materialCreate.Body.data.id
$stockIn = Invoke-Api -Method 'POST' -Path '/api/inventory/stock-in' -Token $warehouseToken -Body @{
  warehouseId = $warehouseId
  sourceType = 'PURCHASE'
  remark = 'Regression stock in'
  items = @(@{
    materialId = $materialId
    batchNo = $names.Batch
    quantity = 20
    productionDate = '2026-04-30'
    expireDate = '2026-06-30'
  })
}
$invalidApply = Invoke-Api -Method 'POST' -Path '/api/apply' -Token $deptToken -Body @{
  deptId = 5
  urgencyLevel = 1
  reason = 'invalid-qty'
  scenario = 'regression'
  items = @(@{ materialId = $materialId; applyQty = 0 })
}
$overStockOut = Invoke-Api -Method 'POST' -Path '/api/inventory/stock-out' -Token $warehouseToken -Body @{
  warehouseId = $warehouseId
  remark = 'over-stock-out'
  items = @(@{ materialId = $materialId; quantity = 999 })
}
$applyCreate = Invoke-Api -Method 'POST' -Path '/api/apply' -Token $deptToken -Body @{
  deptId = 5
  urgencyLevel = 1
  reason = $names.ApplyReason
  scenario = 'regression-main-flow'
  items = @(@{ materialId = $materialId; applyQty = 5 })
}
$applyId = [int64]$applyCreate.Body.data.order.id
$applySubmit = Invoke-Api -Method 'POST' -Path "/api/apply/$applyId/submit" -Token $deptToken
$applyApprove = Invoke-Api -Method 'POST' -Path "/api/apply/$applyId/approve" -Token $approverToken -Body @{ remark = 'approved' }
$applyStockOut = Invoke-Api -Method 'POST' -Path '/api/inventory/stock-out' -Token $warehouseToken -Body @{
  applyOrderId = $applyId
  warehouseId = $warehouseId
  remark = 'Regression outbound'
  items = @(@{ materialId = $materialId; quantity = 5 })
}
$applyReceive = Invoke-Api -Method 'POST' -Path "/api/apply/$applyId/receive" -Token $deptToken
$applyDetail = Invoke-Api -Method 'GET' -Path "/api/apply/$applyId" -Token $deptToken
$applyTimeline = Invoke-Api -Method 'GET' -Path "/api/apply/$applyId/timeline" -Token $deptToken
$inventorySourceAfterApply = Invoke-Api -Method 'GET' -Path '/api/inventory/list' -Token $warehouseToken -Query @{ page = 1; size = 20; materialId = $materialId; warehouseId = $warehouseId }
$batchSourceAfterApply = Invoke-Api -Method 'GET' -Path '/api/inventory/batches' -Token $warehouseToken -Query @{ materialId = $materialId; warehouseId = $warehouseId }
$recommendTransfer = Invoke-Api -Method 'GET' -Path '/api/transfer/recommend' -Token $warehouseToken -Query @{ targetCampus = 'east'; materialId = $materialId; qty = 3 }
$transferCreate = Invoke-Api -Method 'POST' -Path '/api/transfer' -Token $warehouseToken -Body @{
  fromWarehouseId = $warehouseId
  toWarehouseId = 2
  reason = $names.TransferReason
  items = @(@{ materialId = $materialId; quantity = 3 })
}
$transferId = [int64]$transferCreate.Body.data.order.id
$transferSubmit = Invoke-Api -Method 'POST' -Path "/api/transfer/$transferId/submit" -Token $warehouseToken
$transferApprove = Invoke-Api -Method 'POST' -Path "/api/transfer/$transferId/approve" -Token $approverToken -Body @{ remark = 'approved' }
$transferExecute = Invoke-Api -Method 'POST' -Path "/api/transfer/$transferId/execute" -Token $warehouseToken
$transferReceive = Invoke-Api -Method 'POST' -Path "/api/transfer/$transferId/receive" -Token $warehouseToken
$transferDetail = Invoke-Api -Method 'GET' -Path "/api/transfer/$transferId" -Token $warehouseToken
$inventorySourceAfterTransfer = Invoke-Api -Method 'GET' -Path '/api/inventory/list' -Token $warehouseToken -Query @{ page = 1; size = 20; materialId = $materialId; warehouseId = $warehouseId }
$inventoryTargetAfterTransfer = Invoke-Api -Method 'GET' -Path '/api/inventory/list' -Token $warehouseToken -Query @{ page = 1; size = 20; materialId = $materialId; warehouseId = 2 }
$batchSourceAfterTransfer = Invoke-Api -Method 'GET' -Path '/api/inventory/batches' -Token $warehouseToken -Query @{ materialId = $materialId; warehouseId = $warehouseId }
$batchTargetAfterTransfer = Invoke-Api -Method 'GET' -Path '/api/inventory/batches' -Token $warehouseToken -Query @{ materialId = $materialId; warehouseId = 2 }
$unreadBeforeEvent = Invoke-Api -Method 'GET' -Path '/api/notification/unread-count' -Token $adminToken
$eventCreate = Invoke-Api -Method 'POST' -Path '/api/event' -Token $adminToken -Body @{
  eventTitle = $names.EventTitle
  eventType = 'TEST'
  eventLevel = 'NORMAL'
  campusId = 1
  location = 'regression-location'
  description = 'notification regression'
  eventTime = '2026-04-30 15:00:00'
}
$notificationList = Invoke-Api -Method 'GET' -Path '/api/notification' -Token $adminToken -Query @{ page = 1; size = 10 }
$unreadAfterEvent = Invoke-Api -Method 'GET' -Path '/api/notification/unread-count' -Token $adminToken
$notificationId = [int64]$notificationList.Body.data.records[0].id
$markRead = Invoke-Api -Method 'POST' -Path "/api/notification/$notificationId/read" -Token $adminToken
$unreadAfterRead = Invoke-Api -Method 'GET' -Path '/api/notification/unread-count' -Token $adminToken
$overviewAfter = (Invoke-Api -Method 'GET' -Path '/api/analytics/overview' -Token $adminToken).Body.data
$inventoryRatioAfter = (Invoke-Api -Method 'GET' -Path '/api/analytics/inventory-ratio' -Token $adminToken).Body.data
$summary.duplicateChecks = [ordered]@{
  category = @{ firstStatus = $categoryFirst.Status; secondStatus = $categorySecond.Status; secondCode = $categorySecond.Body.code }
  warehouse = @{ firstStatus = $warehouseFirst.Status; secondStatus = $warehouseSecond.Status; secondCode = $warehouseSecond.Body.code }
  userMissingPassword = @{ status = $userMissingPwd.Status; code = $userMissingPwd.Body.code }
  user = @{ firstStatus = $userFirst.Status; secondStatus = $userSecond.Status; secondCode = $userSecond.Body.code }
  role = @{ firstStatus = $roleFirst.Status; secondStatus = $roleSecond.Status; secondCode = $roleSecond.Body.code }
}
$summary.exceptionChecks = [ordered]@{
  invalidApply = @{ status = $invalidApply.Status; code = $invalidApply.Body.code }
  overStockOut = @{ status = $overStockOut.Status; code = $overStockOut.Body.code }
}
$summary.createdData = [ordered]@{
  categoryId = $categoryId
  warehouseId = $warehouseId
  materialId = $materialId
  applyId = $applyId
  transferId = $transferId
}
$summary.applyFlow = [ordered]@{
  createStatus = $applyCreate.Status
  submitStatus = $applySubmit.Status
  approveStatus = $applyApprove.Status
  stockOutStatus = $applyStockOut.Status
  receiveStatus = $applyReceive.Status
  finalStatus = $applyDetail.Body.data.order.status
  reservedWarehouseId = $applyDetail.Body.data.order.reservedWarehouseId
  actualQty = $applyDetail.Body.data.items[0].actualQty
  sourceQtyAfterApply = [int]$inventorySourceAfterApply.Body.data.records[0].currentQty
  sourceBatchRemainAfterApply = [int]$batchSourceAfterApply.Body.data[0].remainQty
  timelineCount = $applyTimeline.Body.data.Count
}
$summary.transferFlow = [ordered]@{
  recommendStatus = $recommendTransfer.Status
  recommendCount = $recommendTransfer.Body.data.Count
  createStatus = $transferCreate.Status
  submitStatus = $transferSubmit.Status
  approveStatus = $transferApprove.Status
  executeStatus = $transferExecute.Status
  receiveStatus = $transferReceive.Status
  finalStatus = $transferDetail.Body.data.order.status
  sourceQtyAfterTransfer = [int]$inventorySourceAfterTransfer.Body.data.records[0].currentQty
  targetQtyAfterTransfer = [int]$inventoryTargetAfterTransfer.Body.data.records[0].currentQty
  sourceBatchRemain = [int]$batchSourceAfterTransfer.Body.data[0].remainQty
  targetBatchRemain = [int]$batchTargetAfterTransfer.Body.data[0].remainQty
}
$summary.notificationFlow = [ordered]@{
  eventCreateStatus = $eventCreate.Status
  unreadBefore = [int]$unreadBeforeEvent.Body.data
  unreadAfterCreate = [int]$unreadAfterEvent.Body.data
  notificationListStatus = $notificationList.Status
  firstNotificationId = $notificationId
  markReadStatus = $markRead.Status
  unreadAfterRead = [int]$unreadAfterRead.Body.data
}
$ratioItem = $inventoryRatioAfter | Where-Object { $_.name -eq $names.MaterialName } | Select-Object -First 1
$summary.analyticsAfter = [ordered]@{
  overview = $overviewAfter
  ratioContainsTestMaterial = [bool]($null -ne $ratioItem)
  ratioValue = if ($ratioItem) { [int]$ratioItem.value } else { $null }
}
$summary | ConvertTo-Json -Depth 10
