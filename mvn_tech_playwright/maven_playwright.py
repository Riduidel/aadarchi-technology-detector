from playwright.sync_api import sync_playwright

def get_html_code():

    with sync_playwright() as plwr:
        browser = plwr.chromium.launch(headless=False)
        page = browser.new_page()
        page.goto("https://mvnrepository.com/popular")
        html = page.inner_html("div.content")
        print(html)
        browser.close()
    
    # div_element = page.query_selector("div#content")
    # div_html = page.inner_html(div_element)
    # browser.close()
    # print(div_html)

if __name__ == "__main__":
    get_html_code()