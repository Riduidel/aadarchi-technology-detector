import asyncio
from playwright.async_api import async_playwright

async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch(headless=False)
        page = await browser.new_page()
        await page.goto("https://mvnrepository.com/popular")
        #print(await page.title())
        await page.wait_for_selector("div.content")
        html = await page.inner_html("div.content")
        print(html)
        await browser.close()

asyncio.run(main())