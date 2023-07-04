#!/usr/bin/env python3
from playwright.sync_api import sync_playwright
from time import sleep
import re

#simulates a sliht pause then a scroll to the bottom of the page
def scroll_down(page):
    sleep(1)
    page.mouse.wheel(0, 15000)
    sleep(1)

# Matches "1. ", "2. " with a regex to get technologies name, and returns
# a nested dictionary.
def from_text_to_tech_dict(popular, dico_tech):
    for elem in popular:
        if re.match('\d+\.\s', elem):
            dico_tech[re.sub('\d+\.\s', "", elem)] = {}
    return dico_tech

def check_cloudflare_captcha(page):
    while page.title() == "Just a moment...":
        sleep(2)
        page.locator("#challenge-stage").wait_for()
        iframeLocator = page.frame_locator("iframe")
        iframe = iframeLocator.first
        interesting_text = iframe.get_by_text("Verify you are human")
        interesting_text.check()
    

def get_html_code():
    dico_tech = dict()

    with sync_playwright() as plwr:
        browser = plwr.chromium.launch(headless=False)
        page = browser.new_page()
        #page.set_default_navigation_timeout(10000000)
        #page.set_default_timeout(100000000)
        page.goto("https://mvnrepository.com/popular?p=1")
        sleep(2)
        
        while True:
            popular_techs = page.inner_text("div.content").split("\n")
            dico_tech = from_text_to_tech_dict(popular_techs, dico_tech)
            scroll_down(page)
            try: 
                page.get_by_text("Next").click()
            except:
                print("There's been a problem when clicking next button")
                break
            #click on checkbox loop until the page changes name
            check_cloudflare_captcha(page)

            #last popular categories page    
            if page.url == "https://mvnrepository.com/popular?p=20":
                break
        
        for elem in dico_tech:
            print (elem)
        browser.close()

if __name__ == "__main__":
    get_html_code()