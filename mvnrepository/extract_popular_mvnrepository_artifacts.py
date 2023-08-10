#!/usr/bin/env python3
from playwright.sync_api import sync_playwright
from playwright_stealth import stealth_sync
from time import sleep
import re
import json
import logging
import pathlib
parent_path = pathlib.Path(__file__).parent.resolve()

logger = logging.getLogger('mvnrepository')
logger.setLevel(logging.INFO)

ch = logging.StreamHandler()
ch.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
ch.setFormatter(formatter)

logger.addHandler(ch)

def load_javascript(javascript_file):
    with open("%s/%s"%(parent_path, javascript_file), 'r') as file:
        return file.read()

SERVER = "mvnrepository.com"

def load_artifact(browser_tab, page_to_load):
    ''' Augment known details from artifact with all known versions, some artifact redirections, categories, and so on'''
    logger.info("processing artifact %s", page_to_load)
    browser_tab.goto(page_to_load)
    # Now we're on artifact page, let's read all details
    script = load_javascript("artifact_details_extractor.js")
    return browser_tab.evaluate(script)

def locate_popular_artifacts(browser_tab, page_to_load="https://%s/popular"%(SERVER)):
    '''
    To get popular artifacts, we start from an initial page and navigate the next button as long as it is not grayed out.
    This method returns a list of **absolute** links to artifacts pages.
    '''
    has_next_page = True
    script = load_javascript("artifacts_urls_extractor.js")
    popular_artifacts_urls = []
    while page_to_load:
        logger.info("Loading page %s"%page_to_load)
        browser_tab.goto(page_to_load)
        page_infos = browser_tab.evaluate(script)
        popular_artifacts_urls.extend(page_infos["data"])
        if "next" in page_infos["page"] and page_infos["page"]["next"]:
            page_to_load = page_infos["page"]["next"]
        else:
            page_to_load = None
    for url in popular_artifacts_urls:
        if not "mvnrepository" in url:
            logger.warning("The url %s is not complete during parsing lists. How is it even possible?", url)
    return popular_artifacts_urls


def get_popular_artifacts():
    popular_artifacts = dict()

    with sync_playwright() as plwr:
        browser = plwr.chromium.launch(headless=False)
        context = browser.new_context(
#            user_agent='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.2227.0 Safari/537.36'
        )
        context.add_init_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")
        browser_tab = context.new_page()
        stealth_sync(browser_tab)
        popular_artifacts_urls_reference = locate_popular_artifacts(browser_tab)
        popular_artifacts_urls = popular_artifacts_urls_reference.copy()
        # Now we have the artifact urls, it's time to transform that into artifacts infos
        while popular_artifacts_urls:
            url = popular_artifacts_urls.pop()
            if not "mvnrepository" in url:
                logger.warning("The url %s is not complete for detailled analysis. How is it even possible?", url)
            # For each artifact, extract more informations from artifact home page
            artifact_infos = load_artifact(browser_tab, url)
            if "Central" in artifact_infos["repositories"]:
                popular_artifacts[artifact_infos["coordinates"]] = artifact_infos
            else:
                logger.error("Maven central does not contains artifact %s. It will be ignored", artifact_infos["name"])
            if "relocation" in artifact_infos:
                if not artifact_infos["relocation"]["coordinates"] in popular_artifacts:
                    if artifact_infos["relocation"]["coordinates"].endswith("."):
                        logger.warning("artifact %s (see %s) has an invalid relocation definition, it will be ignored", 
                            artifact_infos["name"], artifact_infos["page"])
                    else:
                        popular_artifacts_urls.append(artifact_infos["relocation"]["page"])
        browser.close()
        # Notice we don't get any version here!
        return dict(sorted(popular_artifacts.items()))

if __name__ == "__main__":
    artifacts = get_popular_artifacts()
    print(json.dumps(artifacts))
