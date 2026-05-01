import { expect, test } from '@playwright/test'

const login = async (page, username, password) => {
  await page.goto('/login')
  await page.getByPlaceholder('用户名').fill(username)
  await page.getByPlaceholder('密码').fill(password)
  await page.getByRole('button', { name: '登录系统' }).click()
  await expect(page).toHaveURL(/dashboard/)
}

test.describe.configure({ mode: 'serial' })

test('admin can log in and view the inventory screen', async ({ page }) => {
  await login(page, 'admin', 'Abc@123456')

  await page.goto('/inventory/list')
  await expect(page.getByRole('heading', { name: '库存台账' })).toBeVisible()
  await expect(page.getByText('库存记录', { exact: true })).toBeVisible()
})

test('dept user can create an apply order', async ({ page }) => {
  await login(page, 'dept', 'Abc@123456')
  await page.goto('/apply/list')
  await page.getByRole('button', { name: '新建申领' }).click()

  await page.locator('.el-dialog input[role="spinbutton"]').first().fill('5')
  await page.locator('.el-dialog textarea').first().fill(`自动化申领-${Date.now()}`)
  await page.locator('.el-dialog .el-form-item').nth(1).locator('.el-select').click()
  await page.getByRole('option', { name: '紧急' }).click()
  await page.locator('.collection-editor .el-select').first().click()
  await page.getByRole('option', { name: '医用口罩' }).click()
  await page.locator('.collection-editor input[role="spinbutton"]').first().fill('2')
  await page.getByRole('button', { name: '保存' }).click()

  await expect(page.getByText('自动化申领')).toBeVisible()
})

test('warehouse admin can create a transfer order', async ({ page }) => {
  await login(page, 'warehouse', 'Abc@123456')
  await page.goto('/transfer/list')
  await page.getByRole('button', { name: '新建调拨' }).click()

  await page.locator('.el-dialog .el-form-item').nth(0).locator('.el-select').click()
  await page.getByRole('option', { name: '东风校区分仓 (东风校区)', exact: true }).click()
  await page.locator('.el-dialog .el-form-item').nth(1).locator('.el-select').click()
  await page.getByRole('option', { name: '科学校区总仓', exact: true }).click()
  await page.locator('.el-dialog textarea').fill(`自动化调拨-${Date.now()}`)
  await page.locator('.collection-editor .el-select').first().click()
  await page.getByRole('option', { name: '医用口罩' }).click()
  await page.locator('.collection-editor input[role="spinbutton"]').first().fill('1')
  await page.getByRole('button', { name: '提交申请' }).click()

  await expect(page.getByText('自动化调拨')).toBeVisible()
})

test('warehouse admin can handle an existing warning', async ({ page }) => {
  await login(page, 'warehouse', 'Abc@123456')
  await page.goto('/warning/list')

  const handleButton = page.getByRole('button', { name: '处理' }).first()
  await expect(handleButton).toBeVisible()
  await handleButton.click()

  await page.locator('.el-message-box__input input').fill('自动化已处理')
  await page.getByRole('button', { name: '确定' }).click()

  await expect(page.getByText('自动化已处理')).not.toBeVisible()
})
