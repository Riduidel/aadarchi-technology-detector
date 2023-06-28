#!/usr/bin/env python3
from playwright.sync_api import sync_playwright
import re

def from_text_to_tech_dict(popular):
    dico_tech = dict()
    #print(popular)
    for elem in popular:
        if re.match('\d+\.\s', elem):
            dico_tech[re.sub('\d+\.\s', "", elem)] = {}
    print(dico_tech)


def get_html_code():
    with sync_playwright() as plwr:
        browser = plwr.chromium.launch(headless=False)
        page = browser.new_page()
        page.goto("https://mvnrepository.com/popular")
        #page.wait_for_selector("div.content")
        popular_techs = page.inner_text("div.content").split("\n")
        from_text_to_tech_dict(popular_techs)
        browser.close()

if __name__ == "__main__":
    get_html_code()