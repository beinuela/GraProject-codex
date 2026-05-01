import json
import time
import urllib.request
import urllib.parse
import urllib.error

base = 'http://127.0.0.1:8080'

def api(method, path, token=None, body=None, query=None):
    url = base + path
    if query:
        url += '?' + urllib.parse.urlencode(query)
    data = None
    headers = {}
    if token:
        headers['Authorization'] = f'Bearer {token}'
    if body is not None:
        data = json.dumps(body).encode('utf-8')
        headers['Content-Type'] = 'application/json'
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=20) as resp:
            raw = resp.read().decode('utf-8')
            return {'status': resp.getcode(), 'body': json.loads(raw) if raw else None, 'url': url}
    except urllib.error.HTTPError as e:
        raw = e.read().decode('utf-8')
        return {'status': e.code, 'body': json.loads(raw) if raw else None, 'url': url}

def login(username):
    resp = api('POST', '/api/auth/login', body={'username': username, 'password': 'Abc@123456'})
    if resp['status'] != 200:
        raise RuntimeError(f'login failed for {username}: {resp["status"]}')
    return resp['body']['data']['accessToken']

def first_or_none(seq):
    return seq[0] if seq else None

def body_data(resp):
    return (resp.get('body') or {}).get('data')

suffix = int(time.time() * 1000)
names = {
    'category': f'TEST_BROWSER_USE_FIX_CAT_{suffix}',
    'warehouse': f'TEST_BROWSER_USE_FIX_WH_{suffix}',
    'material_code': f'TESTFIX{suffix}',
    'material_name': f'TEST_BROWSER_USE_FIX_MAT_{suffix}',
    'batch': f'TESTFIXBATCH_{suffix}',
    'apply_reason': f'TEST_BROWSER_USE_FIX_APPLY_{suffix}',
    'transfer_reason': f'TEST_BROWSER_USE_FIX_TRANSFER_{suffix}',
    'event_title': f'TEST_BROWSER_USE_FIX_EVENT_{suffix}',
    'username': f'test_fix_{suffix}',
    'role_code': f'TEST_FIX_ROLE_{suffix}',
}
admin = login('admin')
warehouse = login('warehouse')
approver = login('approver')
dept = login('dept')
summary = {
    'timestamp': time.strftime('%Y-%m-%dT%H:%M:%S'),
    'testNames': names,
    'startup': {
        'backendProbe': api('GET', '/api/auth/me')['status'],
        'adminLogin': 200,
        'warehouseLogin': 200,
        'approverLogin': 200,
        'deptLogin': 200,
    },
    'menus': {
        'admin': len(body_data(api('GET', '/api/auth/menus', token=admin))),
        'warehouse': len(body_data(api('GET', '/api/auth/menus', token=warehouse))),
        'approver': len(body_data(api('GET', '/api/auth/menus', token=approver))),
        'dept': len(body_data(api('GET', '/api/auth/menus', token=dept))),
    },
    'permission': {
        'meWithoutToken': api('GET', '/api/auth/me')['status'],
        'warehouseTokenPolicy': api('GET', '/api/auth/token-policy', token=warehouse)['status'],
        'deptWarehouseList': api('GET', '/api/warehouse/list', token=dept)['status'],
        'adminTokenPolicy': api('GET', '/api/auth/token-policy', token=admin)['status'],
    },
}
summary['analyticsBefore'] = body_data(api('GET', '/api/analytics/overview', token=admin))
category_first = api('POST', '/api/material/category', token=admin, body={'categoryName': names['category'], 'remark': 'regression'})
category_second = api('POST', '/api/material/category', token=admin, body={'categoryName': names['category'], 'remark': 'regression'})
warehouse_first = api('POST', '/api/warehouse', token=admin, body={'warehouseName': names['warehouse'], 'campus': 'science', 'address': 'regression-store', 'manager': 'tester'})
warehouse_second = api('POST', '/api/warehouse', token=admin, body={'warehouseName': names['warehouse'], 'campus': 'science', 'address': 'regression-store', 'manager': 'tester'})
user_missing_pwd = api('POST', '/api/rbac/users', token=admin, body={'username': names['username'] + '_nopwd', 'realName': 'NoPwd', 'deptId': 4, 'roleId': 3, 'status': 1})
user_first = api('POST', '/api/rbac/users', token=admin, body={'username': names['username'], 'password': 'Abc@123456', 'realName': 'Regression User', 'deptId': 4, 'roleId': 3, 'status': 1})
user_second = api('POST', '/api/rbac/users', token=admin, body={'username': names['username'], 'password': 'Abc@123456', 'realName': 'Regression User', 'deptId': 4, 'roleId': 3, 'status': 1})
role_first = api('POST', '/api/rbac/roles', token=admin, body={'roleCode': names['role_code'], 'roleName': 'Regression Role', 'description': 'regression'})
role_second = api('POST', '/api/rbac/roles', token=admin, body={'roleCode': names['role_code'], 'roleName': 'Regression Role', 'description': 'regression'})
category_id = body_data(category_first)['id']
warehouse_id = body_data(warehouse_first)['id']
material_create = api('POST', '/api/material/info', token=admin, body={
    'materialCode': names['material_code'],
    'materialName': names['material_name'],
    'categoryId': category_id,
    'spec': 'Regression Spec',
    'unit': 'pcs',
    'safetyStock': 5,
    'shelfLifeDays': 60,
    'supplier': 'Regression Supplier',
    'unitPrice': 12.5,
    'remark': 'regression'
})
material_id = body_data(material_create)['id']
stock_in = api('POST', '/api/inventory/stock-in', token=warehouse, body={
    'warehouseId': warehouse_id,
    'sourceType': 'PURCHASE',
    'remark': 'Regression stock in',
    'items': [{'materialId': material_id, 'batchNo': names['batch'], 'quantity': 20, 'productionDate': '2026-04-30', 'expireDate': '2026-06-30'}]
})
invalid_apply = api('POST', '/api/apply', token=dept, body={'deptId': 5, 'urgencyLevel': 1, 'reason': 'invalid-qty', 'scenario': 'regression', 'items': [{'materialId': material_id, 'applyQty': 0}]})
over_stock_out = api('POST', '/api/inventory/stock-out', token=warehouse, body={'warehouseId': warehouse_id, 'remark': 'over-stock-out', 'items': [{'materialId': material_id, 'quantity': 999}]})
apply_create = api('POST', '/api/apply', token=dept, body={'deptId': 5, 'urgencyLevel': 1, 'reason': names['apply_reason'], 'scenario': 'regression-main-flow', 'items': [{'materialId': material_id, 'applyQty': 5}]})
apply_id = body_data(apply_create)['order']['id']
apply_submit = api('POST', f'/api/apply/{apply_id}/submit', token=dept)
apply_approve = api('POST', f'/api/apply/{apply_id}/approve', token=approver, body={'remark': 'approved'})
apply_stock_out = api('POST', '/api/inventory/stock-out', token=warehouse, body={'applyOrderId': apply_id, 'warehouseId': warehouse_id, 'remark': 'Regression outbound', 'items': [{'materialId': material_id, 'quantity': 5}]})
apply_receive = api('POST', f'/api/apply/{apply_id}/receive', token=dept)
apply_detail = api('GET', f'/api/apply/{apply_id}', token=dept)
apply_timeline = api('GET', f'/api/apply/{apply_id}/timeline', token=dept)
inventory_source_after_apply = api('GET', '/api/inventory/list', token=warehouse, query={'page': 1, 'size': 20, 'materialId': material_id, 'warehouseId': warehouse_id})
batch_source_after_apply = api('GET', '/api/inventory/batches', token=warehouse, query={'materialId': material_id, 'warehouseId': warehouse_id})
recommend_transfer = api('GET', '/api/transfer/recommend', token=warehouse, query={'targetCampus': '东风校区', 'materialId': material_id, 'qty': 3})
transfer_create = api('POST', '/api/transfer', token=warehouse, body={'fromWarehouseId': warehouse_id, 'toWarehouseId': 2, 'reason': names['transfer_reason'], 'items': [{'materialId': material_id, 'quantity': 3}]})
transfer_id = body_data(transfer_create)['order']['id']
transfer_submit = api('POST', f'/api/transfer/{transfer_id}/submit', token=warehouse)
transfer_approve = api('POST', f'/api/transfer/{transfer_id}/approve', token=approver, body={'remark': 'approved'})
transfer_execute = api('POST', f'/api/transfer/{transfer_id}/execute', token=warehouse)
transfer_receive = api('POST', f'/api/transfer/{transfer_id}/receive', token=warehouse)
transfer_detail = api('GET', f'/api/transfer/{transfer_id}', token=warehouse)
inventory_source_after_transfer = api('GET', '/api/inventory/list', token=warehouse, query={'page': 1, 'size': 20, 'materialId': material_id, 'warehouseId': warehouse_id})
inventory_target_after_transfer = api('GET', '/api/inventory/list', token=warehouse, query={'page': 1, 'size': 20, 'materialId': material_id, 'warehouseId': 2})
batch_source_after_transfer = api('GET', '/api/inventory/batches', token=warehouse, query={'materialId': material_id, 'warehouseId': warehouse_id})
batch_target_after_transfer = api('GET', '/api/inventory/batches', token=warehouse, query={'materialId': material_id, 'warehouseId': 2})
unread_before = api('GET', '/api/notification/unread-count', token=admin)
event_create = api('POST', '/api/event', token=admin, body={'eventTitle': names['event_title'], 'eventType': 'TEST', 'eventLevel': 'NORMAL', 'campusId': 1, 'location': 'regression-location', 'description': 'notification regression', 'eventTime': '2026-04-30 15:00:00'})
notification_list = api('GET', '/api/notification', token=admin, query={'page': 1, 'size': 10})
unread_after_create = api('GET', '/api/notification/unread-count', token=admin)
notif_records = body_data(notification_list)['records']
notification_id = first_or_none(notif_records)['id'] if first_or_none(notif_records) else None
mark_read = api('POST', f'/api/notification/{notification_id}/read', token=admin) if notification_id else {'status': None, 'body': None}
unread_after_read = api('GET', '/api/notification/unread-count', token=admin)
summary['duplicateChecks'] = {
    'category': {'firstStatus': category_first['status'], 'secondStatus': category_second['status'], 'secondCode': category_second['body']['code']},
    'warehouse': {'firstStatus': warehouse_first['status'], 'secondStatus': warehouse_second['status'], 'secondCode': warehouse_second['body']['code']},
    'userMissingPassword': {'status': user_missing_pwd['status'], 'code': user_missing_pwd['body']['code']},
    'user': {'firstStatus': user_first['status'], 'secondStatus': user_second['status'], 'secondCode': user_second['body']['code']},
    'role': {'firstStatus': role_first['status'], 'secondStatus': role_second['status'], 'secondCode': role_second['body']['code']},
}
summary['exceptionChecks'] = {
    'invalidApply': {'status': invalid_apply['status'], 'code': invalid_apply['body']['code']},
    'overStockOut': {'status': over_stock_out['status'], 'code': over_stock_out['body']['code']},
}
summary['createdData'] = {'categoryId': category_id, 'warehouseId': warehouse_id, 'materialId': material_id, 'applyId': apply_id, 'transferId': transfer_id}
source_apply_rec = first_or_none(body_data(inventory_source_after_apply)['records'])
source_transfer_rec = first_or_none(body_data(inventory_source_after_transfer)['records'])
target_transfer_rec = first_or_none(body_data(inventory_target_after_transfer)['records'])
source_apply_batch = first_or_none(body_data(batch_source_after_apply))
source_transfer_batch = first_or_none(body_data(batch_source_after_transfer))
target_transfer_batch = first_or_none(body_data(batch_target_after_transfer))
summary['applyFlow'] = {
    'createStatus': apply_create['status'],
    'submitStatus': apply_submit['status'],
    'approveStatus': apply_approve['status'],
    'stockOutStatus': apply_stock_out['status'],
    'receiveStatus': apply_receive['status'],
    'finalStatus': body_data(apply_detail)['order']['status'],
    'reservedWarehouseId': body_data(apply_detail)['order'].get('reservedWarehouseId'),
    'actualQty': body_data(apply_detail)['items'][0]['actualQty'],
    'sourceQtyAfterApply': source_apply_rec['currentQty'] if source_apply_rec else None,
    'sourceBatchRemainAfterApply': source_apply_batch['remainQty'] if source_apply_batch else None,
    'timelineCount': len(body_data(apply_timeline))
}
summary['transferFlow'] = {
    'recommendStatus': recommend_transfer['status'],
    'recommendCount': len(body_data(recommend_transfer)),
    'createStatus': transfer_create['status'],
    'submitStatus': transfer_submit['status'],
    'approveStatus': transfer_approve['status'],
    'executeStatus': transfer_execute['status'],
    'receiveStatus': transfer_receive['status'],
    'finalStatus': body_data(transfer_detail)['order']['status'],
    'sourceQtyAfterTransfer': source_transfer_rec['currentQty'] if source_transfer_rec else None,
    'targetQtyAfterTransfer': target_transfer_rec['currentQty'] if target_transfer_rec else None,
    'sourceBatchRemain': source_transfer_batch['remainQty'] if source_transfer_batch else None,
    'targetBatchRemain': target_transfer_batch['remainQty'] if target_transfer_batch else None
}
summary['notificationFlow'] = {
    'eventCreateStatus': event_create['status'],
    'unreadBefore': body_data(unread_before),
    'unreadAfterCreate': body_data(unread_after_create),
    'notificationListStatus': notification_list['status'],
    'notificationCount': len(notif_records),
    'firstNotificationId': notification_id,
    'markReadStatus': mark_read['status'],
    'unreadAfterRead': body_data(unread_after_read)
}
overview_after = body_data(api('GET', '/api/analytics/overview', token=admin))
ratio_after = body_data(api('GET', '/api/analytics/inventory-ratio', token=admin))
ratio_item = next((item for item in ratio_after if item['name'] == names['material_name']), None)
summary['analyticsAfter'] = {
    'overview': overview_after,
    'ratioContainsTestMaterial': ratio_item is not None,
    'ratioValue': ratio_item['value'] if ratio_item else None,
}
print(json.dumps(summary, ensure_ascii=False, indent=2))
