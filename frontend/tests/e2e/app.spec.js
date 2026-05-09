import { test, expect } from '@playwright/test'

test('loads the AIOps console', async ({ page }) => {
  await page.goto('/')

  await expect(page).toHaveTitle(/AIOps Agent Console/)
  await expect(page.locator('.app-shell')).toBeVisible()
})
