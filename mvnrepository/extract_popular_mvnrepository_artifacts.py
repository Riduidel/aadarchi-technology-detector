#!/usr/bin/env python3
from playwright.sync_api import sync_playwright
from playwright_stealth import stealth_sync
from time import sleep
import os
import sys
import re
import json
import logging
import pathlib
import subprocess
parent_path = pathlib.Path(__file__).parent.resolve()

logger = logging.getLogger('mvnrepository')
logger.setLevel(logging.INFO)

ch = logging.StreamHandler(stream=sys.stderr)
ch.setLevel(logging.INFO)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
ch.setFormatter(formatter)

logger.addHandler(ch)

SERVER = "mvnrepository.com"
        
def load_javascript(javascript_file):
    with open("%s/%s"%(parent_path, javascript_file), 'r') as file:
        return file.read()

class ArtifactInformations(object):
    # The artifact group id. Should never be null
    groupId=None
    # The artifact id, which should never be null
    artifactId=None

    def __init__(self, **kwargs):
        if 'groupId' in kwargs:
            self.groupId = kwargs['groupId']
        if 'artifactId' in kwargs:
            self.artifactId = kwargs['artifactId']

    def __eq__(self, other):
        """Overrides the default implementation"""
        if isinstance(other, ArtifactInformations):
            return self.groupId == other.groupId and self.artifactId == other.artifactId
        return NotImplemented

    def __ne__(self, other):
        """Overrides the default implementation (unnecessary in Python 3)"""
        x = self.__eq__(other)
        if x is not NotImplemented:
            return not x
        return NotImplemented

    def __hash__(self):
        """Overrides the default implementation"""
        return hash(tuple(sorted(self.__dict__.items())))
    
    def __str__(self):
        return f"{self.groupId}:{self.artifactId}"
    
    def __repr__(self):
        return f"ArtifactInformations<{self.groupId}:{self.artifactId}>"
    def __lt__(a, b):
        return a.__repr__()<b.__repr__()

    def to_json(self, browser_tab):
        ''' Augment known details from artifact with all known versions, some artifact redirections, categories, and so on'''
        page_to_load = f"https://{SERVER}/artifact/{self.groupId}/{self.artifactId}"
        logger.debug("processing artifact %s", page_to_load)
        tries = 0
        while tries<5:
            try:
                response = browser_tab.goto(page_to_load)
                if response.status<300:
                    # Now we're on artifact page, let's read all details
                    script = load_javascript("artifact_details_extractor.js")
                    return browser_tab.evaluate(script)
                else:
                    logger.error("Unable to process artifact %s due to %s", page_to_load, response.status_text)
                    return None
            except Exception:
                tries = tries+1

class Loader:
    def load(self):
        logger.info("Unable to run loader %s", type(self).__name__)
    def load_page_list(self, page_to_load):
        has_next_page = True
        script = load_javascript("artifacts_urls_extractor.js")
        popular_artifacts_urls = []
        while page_to_load:
            logger.info("Loading page %s"%page_to_load)
            self.browser_tab.goto(page_to_load)
            page_infos = self.browser_tab.evaluate(script)
            if not page_infos["data"]:
                logger.warning(f"Page {page_to_load} has empty data {page_infos}")
            for artifact in page_infos["data"]:
                popular_artifacts_urls.append(ArtifactInformations(**artifact))
            if "next" in page_infos["page"] and page_infos["page"]["next"]:
                page_to_load = page_infos["page"]["next"]
            else:
                page_to_load = None
        return popular_artifacts_urls


class LocalFile(Loader):
    def __init__(self, browser_tab):
        self.browser_tab = browser_tab
    def load(self):
        '''
        Load a list of artifacts from a local file.
        '''
        interesting_local_artifacts = os.path.join(os.path.dirname(__file__), "interesting_artifacts.json")
        returned = []
        if os.path.isfile(interesting_local_artifacts):
            with open(interesting_local_artifacts, 'r') as file:
                file_contents = file.read()
                artifacts = json.loads(file_contents)
                for a in artifacts:
                    if "artifactId" in a:
                        returned.append(ArtifactInformations(**a))
                    else:
                        groupId = a["groupId"]
                        artifacts_of_group = self.load_page_list(f"https://{SERVER}/artifact/{groupId}")
                        returned.extend(artifacts_of_group)
        return returned

class TechEmpower(Loader):
    def __init__(self, browser_tab):
        self.browser_tab = browser_tab
        
    def identify_interesting_artifact_dependencies_in_gradle_project(self, framework_path):
        logger.error("TODO implement gradle project analysis for %s", framework_path)
        return []

    def identify_interesting_artifact_dependencies_in_maven_project(self, framework_path):
        '''
        Load interesting dependencies of given pom.
        You know how to do it the easiest way? By gently asking the dependencies maven plugin!
        Unfortunatly, maven is unable to produce a machine-ready output
        So we have to get the output in a file, then read back that file
        '''
        output_file = os.path.join(os.path.dirname(__file__),
            ".cache", 
            os.path.basename(framework_path)+".log")
        from pathlib import Path
        Path(output_file).parent.mkdir(parents=True, exist_ok=True)
        command = "mvn dependency:list -DexcludeTransitive -DoutputFile={}".format(output_file)
        logger.info("Will run command {} in folder {}".format(
            command, framework_path))
        if not os.path.isfile(output_file):
            def run_in_maven(command):
                return subprocess.run(command,
                    check=False,
                    shell=True,
                    cwd=framework_path)
            execution_result = run_in_maven(command)
            if execution_result.returncode!=0:
                # We will try to install the project, then rerun the command
                run_in_maven("mvn install")
                # Then rerun command (and hope for the result to be ok)
                execution_result = run_in_maven(command)
                if execution_result.returncode!=0:
                    logger.error("Unable to compile %s to have valid dependencies", framework_path)
        returned = []
        # Now the command has run, it is time to load the results
        if os.path.isfile(output_file):
            with open(output_file, 'r') as file:
                command_result = file.read()
                if command_result:
                    for dependency in re.findall("\s+([^:^\s]+):([^:^\s]+):", command_result):
                        returned.append(ArtifactInformations(groupId=dependency[0], artifactId=dependency[1]))
                        logger.info("found interesting dependency {}".format(dependency))
        return returned

    def find_frameworks_folder_in(self, folder):
        '''
        If the given folder contains the FrameworkBenchmarks/frameworks folder, return it.
        Otherwise test with parent.
        If folder is null, throw a ValueError
        '''
        returned = os.path.join(folder,"FrameworkBenchmarks/frameworks")
        if os.path.isdir(returned):
            return returned
        elif not folder:
            raise ValueError("couldn't find framewok folder")
        else:
            return self.find_frameworks_folder_in(os.path.dirname(folder))


    def load(self):
        '''
        Identify interesting artifacts in **local** clone of techempower frameworks (git@github.com:TechEmpower/FrameworkBenchmarks.git)
        For this to work optimally, the associated git repo must be in main branch
        '''
        interesting_artifacts_urls = []
        frameworks_folder = self.find_frameworks_folder_in(os.path.dirname(__file__))
        files_and_dirs = os.listdir(frameworks_folder)
        if not files_and_dirs:
            logger.error("There must be a problem : frameworks folder {} seems to be empty".format(os.path.abspath(frameworks_folder)))
        for language in files_and_dirs:
            if language in ["Java"]:
                language_full_path = frameworks_folder+"/"+language
                logger.info("Now exploring {}".format(os.path.abspath(language)))
                frameworks = os.listdir(language_full_path)
                for framework_folder in frameworks:
                    framework_full_path = os.path.realpath(language_full_path + "/" + framework_folder)
                    if os.path.isdir(framework_full_path):
                        logger.info("Now exploring {}".format(os.path.abspath(framework_full_path)))
                        # Now we have a framework path, if it is has a maven pom, everything is cool
                        if os.path.exists(framework_full_path+"/pom.xml"):
                            interesting_artifacts_urls.extend(self.identify_interesting_artifact_dependencies_in_maven_project(framework_full_path))
                        elif os.path.exists(framework_full_path+"/build.gradle"):
                            interesting_artifacts_urls.extend(self.identify_interesting_artifact_dependencies_in_gradle_project(framework_full_path))
                        else:
                            logger.error("Pretty sure %s uses Gradle. How to parse infos in?", framework_full_path)
        return interesting_artifacts_urls

class Popular(Loader):
    def __init__(self, browser_tab):
        self.browser_tab = browser_tab
    def load(self, page_to_load="https://%s/popular"%(SERVER)):
        '''
        To get popular artifacts, we start from an initial page and navigate the next button as long as it is not grayed out.
        This method returns a list of **absolute** links to artifacts pages.
        '''
        return self.load_page_list(page_to_load)

def locate_interesting_artifacts(browser_tab):
    '''
    Combines both the mvn repository popular artifacts and some interesting projects 
    (techempower benchmarks, medium clone, ...)

    returns a list of ArtifactInformations
    '''
    interesting_artifacts_urls = []
    for loader in [LocalFile(browser_tab),
        TechEmpower(browser_tab),
        Popular(browser_tab)]:
        interesting_artifacts_urls.extend(loader.load())
# Now we have a big list full of duplicates, dedup!
    return sorted(set(interesting_artifacts_urls))

def get_popular_artifacts():
    '''main method that will produce a dict of intersting rtifacts'''
    popular_artifacts = dict()

    with sync_playwright() as plwr:
        browser = plwr.chromium.launch(headless=False)
        context = browser.new_context(
#            user_agent='Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.2227.0 Safari/537.36'
        )
        context.add_init_script("Object.defineProperty(navigator, 'webdriver', {get: () => undefined})")
        browser_tab = context.new_page()
        stealth_sync(browser_tab)
        popular_artifacts_urls_reference = locate_interesting_artifacts(browser_tab)
        popular_artifacts_urls = popular_artifacts_urls_reference.copy()
        # Now we have the artifact urls, it's time to transform that into artifacts infos
        while popular_artifacts_urls:
            artifact = popular_artifacts_urls.pop()
            if len(popular_artifacts_urls)%10==0:
                logger.info("%d artifacts remaining.", len(popular_artifacts_urls))
            # For each artifact, extract more informations from artifact home page
            artifact_infos = artifact.to_json(browser_tab)
            if artifact_infos:
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
                            popular_artifacts_urls.append(ArtifactInformations(**artifact_infos["relocation"]["page"]))
        browser.close()
        # Notice we don't get any version here!
        return dict(sorted(popular_artifacts.items()))

if __name__ == "__main__":
    artifacts = get_popular_artifacts()
    # The subdirectory is used for pushing to another branch with ease
    with open("%s/artifacts.json"%(parent_path), 'w') as file:
        json.dump(artifacts, file, indent=4)
