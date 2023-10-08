function extract_artifact_details() {
    function toUrl(url) {
        return url.startsWith("https://") ? url : 
            url.startsWith("/") ? "https://"+document.domain+url : window.location.href.split('?')[0]+url
    }
    artifact = {}
    const main = document.querySelector("div.content")
    const headerLinks = main.querySelectorAll("h2.im-title a")
    for(const link of headerLinks) {
        if(!artifact.hasOwnProperty("name")) {
            artifact["name"]=link.innerText
            artifact["page"] = document.URL
        } else if(!artifact.hasOwnProperty("usages")) {
            artifact["usages"]=link.querySelector("b").innerText
        }
    }
    // There may be empty elements due to adblockers
    if(main.querySelector("div.im-description")) {
        artifact["description"] = main.querySelector("div.im-description").innerText
    }
//    artifact["coordinates"] = document.querySelector("div.breadcrumb").innerText.replaceAll(" ", "").replace("Home»", "").replaceAll("»", ".")
    artifact["coordinates"] = document.location.pathname.substring("/artifact/".length).split("/").join(".")
    const logo = main.querySelector("img.im-logo")
    if(logo) {
        artifact["logo"] = toUrl(logo.getAttribute("src"))
    }
    // First table should contain some details (license, categories, and so on)
    const artifact_details = main.querySelector("table.grid")
    // Yeah, there is a bad TBODY in that table
    for(row of artifact_details.children[0].children) {
        header_cell = row.children[0].innerText.toLowerCase()
        value_cell = Array.from(row.children[1].children).map(node => node.innerText)+""
        if(header_cell=="categories") {
            artifact["categories"] = Array.from(row.children[1].children).map(node => node.innerText)
        } else if(header_cell=="tags") {
            artifact["tags"] = value_cell
        } else if(header_cell=="ranking") {
            re = /#(\d+) in/g
            if(result = re.exec(value_cell)) {
                artifact["ranking"] = result[1]
            } else {
                console.log("no match found in text "+value_cell)
            }
        } else if(header_cell=="used by") {
            artifact["users"] = value_cell.substring(0, value_cell.indexOf(" "))
        } else {
            console.log(header_cell+" cell not processed")
        }
    }

    // Now it become quite cumbersome
    // we may have a relocation table
    // we must have a verson table
    // we may have a book table
    // All of which are table.grid below some specifc elements ...
    if(main.querySelector("div table.grid")) {
        // There may be a relocation table
        relocation = main.querySelector("div[style*=\"align-content\"] table.grid")
        // Beware: this can match the versions table. To avoid that, we count the column number
        // if greater than 1, we give up
        // Unfortunatly
        if(relocation && relocation.querySelectorAll("td").length==1) {
            links = Array.from(relocation.querySelectorAll("a"))
            artifact["relocation"] = {
                "page": {
                    groupId: links[0].text,
                    artifactId: links[1].text
                },
                "coordinates": links.map(element => element.innerText).join(".")
            }
        }
    }

    // Process exposing repositories (which will allow us a nice hack)
    // beware! All repositories don't contain all versions, and we use as reference the first repository
    const filter = /(.*) \(\d+\)$/
    artifact["repositories"] = Array.from(main.querySelectorAll("#snippets ul.tabs li"))
        .map(element => element.innerText)
        .map(text =>  text.match(filter)[1])
    // Now process known versions
    versionsTable = main.querySelector("table.versions").querySelector("tbody") 
    artifact["versions"] = Array.from(versionsTable.children)
        .map(element => element.children[0].getAttribute("rowspan") ? 
            [element.children[1], element.children[5]] : 
            [element.children[0], element.children[4]])
        .map(element => [element[0].innerText, element[1].innerText])
        .reduce((map, array) => {
            map[array[0]] = array[1]
            return map
        }, {})

    console.log("extracted details of artifact", artifact)
    return artifact
}