#!/usr/bin/env python3
from playwright.sync_api import sync_playwright
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

with open("%s/%s"%(parent_path, 'artifacts_list_extractor.js'), 'r') as file:
    ARTIFACTS_LIST_EXTRACTOR = file.read().rstrip()
with open("%s/%s"%(parent_path, 'artifact_details_extractor.js'), 'r') as file:
    ARTIFACT_DETAILS_EXTRACTOR = file.read().rstrip()

def validate_cloudflare(browser_tab):
    ''' If we see cloudflare page, validate it, otherwise let's just go through'''
    if "Maven Repository" in browser_tab.title():
        logger.info("Seems like we successfully loaded %s", browser_tab.title())
    else:
        logger.warning("Seems like Cloudflare wants our interest : page title is %s", browser_tab.title())
        browser_tab.locator("#challenge-stage").wait_for()
        iframeLocator = browser_tab.frame_locator("iframe")
        iframe = iframeLocator.first
        interesting_text = iframe.locator("label.ctp-checkbox-label input")
        interesting_text.check()
        # Run the same check in loop because sometimes Cloudflare wants to make sure I'm an okay person
        validate_cloudflare(browser_tab)

def augment_artifact(browser_tab, artifact):
    ''' Augment known details from artifact with all known versions, some artifact redirections, categories, and so on'''
    page_to_load = "https://mvnrepository.com/%s"%(artifact["page"])
    logger.info("Loading page %s"%page_to_load)
    browser_tab.goto(page_to_load)
    validate_cloudflare(browser_tab)
    # Now we're on artifact page, let's read all details
    return browser_tab.evaluate(ARTIFACT_DETAILS_EXTRACTOR, artifact)

def get_popular_libs(browser_tab, page_index):
    ''' Get popular libs from the given page.
    To get the libs, we run some JS script that will parse details from the page
    '''
    popular_artifacts = {}
    page_to_load = "https://mvnrepository.com/popular?p=%s"%(page_index)
    logger.info("Loading page %s"%page_to_load)
    browser_tab.goto(page_to_load)
    validate_cloudflare(browser_tab)
    # Mind you, goto automagically wait for page to load, so we're good!
    # Now get all techs in page. And mind you, the easiest way to do that is to run some JS that will generate
    # a JSON String containing the data we want
    infos = browser_tab.evaluate(ARTIFACTS_LIST_EXTRACTOR)
    for artifact in infos:
        # For each artifact, extract more informations from artifact home page
        popular_artifacts[artifact["coordinates"]] = artifact
#        popular_artifacts[artifact["coordinates"]] = augment_artifact(browser_tab, artifact)
    # Notice we don't get any version here!
    return popular_artifacts
    

def get_popular_artifacts():
    popular_artifacts = dict()

    with sync_playwright() as plwr:
        browser = plwr.chromium.launch(headless=False)
        browser_tab = browser.new_page()
        for page_index in range(1, 50):
            popular_artifacts_on_page = get_popular_libs(browser_tab, page_index)
            popular_artifacts = popular_artifacts | popular_artifacts_on_page
            logger.info("We know the %s most popular Maven Artifacts!", len(popular_artifacts))
        browser.close()
    print(json.dumps(popular_artifacts))

if __name__ == "__main__":
    get_popular_artifacts()