import requests
import json
import urllib.parse
import re
import logging
import sys
import requests_cache
import pydoc

MAX_CONCURRENT_REQUESTS = 30
ALL_TAG_NAMES = []

"""
    Function that will parse the description of each tag to find occurences of a technology in ALL_TAG_NAMES.
    If it does, it means that the tag in question depends upon another technology, therefore it has a parent.
"""
def get_parents_from_desc(tech_tags):
    for tech in tech_tags:
        """
            We only take the first sentence of the description because that's where the information we're looking for is, hence the split.
        """
        desc = tech["description"].lower().split(". ")[0] if tech["description"] else ""
        for name in ALL_TAG_NAMES:
            """
                The regex should match the potential occurence of a technology in the tag description, here "name" is only technology names.
            """
            pattern = r'\b(?!(?:[a-zA-Z])\b)' + re.escape(name) + r'\b'
            if re.search(pattern, desc) and tech["name"].lower() != name:
                tech["parent"] = name
    return tech_tags

"""
    get_tag_infos returns a list containing two informations : the description of a tag and the link to its homepage.
"""
def get_tag_infos(tech, api_key):
    api_url = f"https://api.stackexchange.com/2.3/tags/{urllib.parse.quote(tech)}/wikis?site=stackoverflow&filter=!nNPvSNMavg&key={api_key}"
    
    """
        This block just calls the stackexchange api to fetch the description of each tag, the filter element gets us the body of the page, which should contain the link.
    """
    try:
        response = requests.get(api_url)
        response.raise_for_status()
        data = response.json()
    except requests.exceptions.RequestException as e:
        logging.error("Request error for description of %s: %s", tech, e)
        return None

    to_return = []
    for elem in data.get("items", []):

        """ Excerpt is the description """
        
        if elem.get("excerpt"):
            to_return.append(elem["excerpt"])
        else:
            to_return.append(f"No description found for {tech}")

        """ This regex returns the part of the body containing the link"""
        link = re.search(r'<a\s+href=[\'"]?(https?://[^\'" >]+)', elem["body"])
        if link is None:
            to_return.append(f"No link found for {tech}")
            logging.warning("No link found for %s", tech)
        else:
            """ Here we select the part of the match we want, i.e the first one and we slice it to remove the html part """
            to_return.append(link.group()[9:])
    return to_return

"""
    process_tag is the middle-man between get_tchnology_tags and get_tag_infos. It fills up ALL_TAG_NAMES with the technologies names
    and returns a dictionary with the content of each tags that'll become the json result file.
"""
def process_tag(api_key, tag):
    logging.info("Processing tag: %s", tag["name"])
    ALL_TAG_NAMES.append(tag["name"].lower())
    infos = get_tag_infos(tag['name'].strip(" "), api_key)
    if len(infos) == 0:
        return None

    tag_info = {
        'name': tag['name'],
        'popularity': tag['count'],
        'description': infos[0],
        'link': infos[1]
    }
    return tag_info

"""
    That's the core function, it calls the stackexchange api to get all the basic data : each technology tag we need; 
    then it'll call process tag to get the final structure of the all_technology_tag dictionary.
"""
def get_technology_tags(api_key):
    api_url = "https://api.stackexchange.com/2.3/tags?order=desc&sort=popular&site=stackoverflow&pagesize=100"
    page = 1
    all_technology_tags = []

    """
        Each api call contains a boolean "has_more" field, indicating if there's gonna be another call or not, hence the while True.
    """
    while True:
        try:
            """ So we call all the pages with 100 technologies each """
            response = requests.get(f"{api_url}&page={page}&key={api_key}")
            response.raise_for_status()
            data = response.json()
        except requests.exceptions.RequestException as e:
            logging.error("Request error in get_technology_tags: %s", e)
            return None

        """ 
            Here we just collect the data in a list, call process_tag so that we can get in return each tag with their informations, 
            and we add the result to all_technology_tags, 100 by 100.
        """
        tag_list = data['items']
        tag_info_list = [process_tag(api_key, tag) for tag in tag_list]
        all_technology_tags.extend(tag_info_list)
        
        logging.info("Quota remaining : " + str(data["quota_remaining"]))
        if not data.get("has_more"):
            break
        logging.info("PAGE : " + str(page))
        page += 1

    all_technology_tags = get_parents_from_desc(all_technology_tags)
    return all_technology_tags

def main(api_key):
    logging.basicConfig(format='%(asctime)s - %(levelname)s - %(message)s', level=logging.INFO)
    """ Installation of the cache, which allows to drastically shorten the running time of the program. It's configured to last 8 days."""
    requests_cache.install_cache('cache_stackoverflow', expire_after=691200)

    technology_tags = get_technology_tags(api_key)
    if technology_tags is not None:
        with open('technologies.json', 'w') as fp:
            json.dump(technology_tags, fp, indent=4)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python script.py <API_KEY>")
        sys.exit(1)
    main(sys.argv[1])
