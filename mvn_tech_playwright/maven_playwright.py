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

def ARTIFACTS_LIST_EXTRACTOR():
    with open("%s/%s"%(parent_path, 'artifacts_list_extractor.js'), 'r') as file:
        return file.read()

def ARTIFACT_DETAILS_EXTRACTOR():
    with open("%s/%s"%(parent_path, 'artifact_details_extractor.js'), 'r') as file:
        return file.read()

SERVER = "mvnrepository.com"

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
    page_to_load = "https://%s%s"%(SERVER, artifact["page"])
    logger.info("Loading page %s"%page_to_load)
    browser_tab.goto(page_to_load)
    validate_cloudflare(browser_tab)
    # Now we're on artifact page, let's read all details
    script = ARTIFACT_DETAILS_EXTRACTOR()
    return browser_tab.evaluate(script, artifact)

def get_popular_libs(browser_tab, page_index):
    ''' Get popular libs from the given page.
    To get the libs, we run some JS script that will parse details from the page
    '''
    popular_artifacts = {}
    page_to_load = "https://%s/popular?p=%s"%(SERVER, page_index)
    logger.info("Loading page %s"%page_to_load)
    browser_tab.goto(page_to_load)
    validate_cloudflare(browser_tab)
    # Mind you, goto automagically wait for page to load, so we're good!
    # Now get all techs in page. And mind you, the easiest way to do that is to run some JS that will generate
    # a JSON String containing the data we want
    script = ARTIFACTS_LIST_EXTRACTOR()
    infos = browser_tab.evaluate(script)
    for artifact in infos:
        # For each artifact, extract more informations from artifact home page
#        popular_artifacts[artifact["coordinates"]] = artifact
        while artifact:
            augmented = augment_artifact(browser_tab, artifact)
            if "Central" in augmented["repositories"]:
                popular_artifacts[artifact["coordinates"]] = augmented
            else:
                logger.error("Maven central does not contains artifact %s. It will be ignored", augmented["name"])
            if "relocation" in augmented:
                logger.info("artifact %s has been relocated as %s"%(artifact["coordinates"], augmented["relocation"]["coordinates"]))
                if augmented["relocation"]["coordinates"] in popular_artifacts:
                    logger.info("But we already know relocated artifact %s! So nothing to do"%(augmented["relocation"]["coordinates"]))
                    artifact = False
                else:
                    relocated = artifact.copy()
                    relocated["page"] = augmented["relocation"]["page"]
                    relocated["coordinates"] = augmented["relocation"]["coordinates"]
                    artifact = relocated
            else:
                artifact = False
    # Notice we don't get any version here!
    return popular_artifacts
    

def get_popular_artifacts():
    popular_artifacts = dict()

    with sync_playwright() as plwr:
        try:
            browser = plwr.chromium.launch(headless=False)
            context = browser.new_context(
    #            user_agent='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.2227.0 Safari/537.36'
            )
            context.add_init_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")
            browser_tab = context.new_page()
            stealth_sync(browser_tab)
            for page_index in range(1, 20):
                popular_artifacts_on_page = get_popular_libs(browser_tab, page_index)
                popular_artifacts = popular_artifacts | popular_artifacts_on_page
                logger.info("We know the %s most popular Maven Artifacts!", len(popular_artifacts))
            browser.close()
        except KeyboardInterrupt:
            logger.fatal("Interrupted by keyboard, finalizing writing")
    return popular_artifacts

if __name__ == "__main__":
    artifacts = get_popular_artifacts()
    print(json.dumps(artifacts))
