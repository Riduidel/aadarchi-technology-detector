import requests
import json
import urllib.parse
import re
import logging
import sys
import concurrent.futures

MAX_CONCURRENT_REQUESTS = 10

def get_tag_infos(tech, api_key):
    api_url = f"https://api.stackexchange.com/2.3/tags/{urllib.parse.quote(tech)}/wikis?site=stackoverflow&filter=!nNPvSNMavg&key={api_key}"
    
    try:
        response = requests.get(api_url)
        response.raise_for_status()
        data = response.json()
    except requests.exceptions.RequestException as e:
        logging.error("Request error for description of %s: %s", tech, e)
        return None

    to_return = []
    for elem in data.get("items", []):
        if elem.get("excerpt"):
            to_return.append(elem["excerpt"])
        else:
            to_return.append(f"No description found for {tech}")

        link = re.search(r'<a\s+href=[\'"]?(https?://[^\'" >]+)', elem["body"])
        if link is None:
            to_return.append(f"No link found for {tech}")
            logging.warning("No link found for %s", tech)
        else:
            to_return.append(link.group(0).strip('<a href='))
    return to_return

def process_tag(api_key, tag):
    logging.info("Processing tag: %s", tag["name"])
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

def get_technology_tags(api_key):
    api_url = "https://api.stackexchange.com/2.3/tags?order=desc&sort=popular&site=stackoverflow&pagesize=100"
    page = 1
    all_technology_tags = []

    while True:
        try:
            response = requests.get(f"{api_url}&page={page}&key={api_key}")
            response.raise_for_status()
            data = response.json()
        except requests.exceptions.RequestException as e:
            logging.error("Request error in get_technology_tags: %s", e)
            return None

        tag_list = data['items']
        with concurrent.futures.ThreadPoolExecutor(max_workers=MAX_CONCURRENT_REQUESTS) as executor:
            tag_info_list = list(executor.map(lambda tag: process_tag(api_key, tag), tag_list))
            all_technology_tags.extend(tag_info_list)
        
        logging.info("Quota remaining : " + str(data["quota_remaining"]))
        if not data.get("has_more"):
            break
        logging.info("PAGE : " + str(page))
        page += 1

    return all_technology_tags

def main(api_key):
    logging.basicConfig(format='%(asctime)s - %(levelname)s - %(message)s', level=logging.INFO)
    technology_tags = get_technology_tags(api_key)
    if technology_tags is not None:
        with open('technologies.json', 'w') as fp:
            json.dump(technology_tags, fp, indent=4)

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python script.py <API_KEY>")
        sys.exit(1)
    main(sys.argv[1])