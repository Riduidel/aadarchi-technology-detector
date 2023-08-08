function extract_details_of(artifact) {
    const main = document.querySelector("div.content")
    const logo = main.querySelector("im.im-logo")
    if(logo) {
        artifact["logo"] = logo.getAttribute("src")
    }
    const tables = main.querySelectorAll("table.grid")

    // First table should contain some details (license, categories, and so on)
    const artifact_details = tables[0]
    // Yeah, there is a bad TBODY in that table
    for(row of artifact_details.children[0].children) {
        header_cell = row.children[0].innerText.toLowerCase()
        if(header_cell=="categories") {
            artifact["categories"] = Array.from(row.children[1].children).map(node => node.innerText)
        } else if(header_cell=="tags") {
            artifact["tags"] = Array.from(row.children[1].children).map(node => node.innerText)
        }
    }

    if(tables.length>2) {
        // There may be a relocation table
    }

    // Now process known versions
    versionsTable = main.querySelector("table.versions").querySelector("tbody") 
    artifact["versions"] = Array.from(versionsTable.children)
        .map(element => element.children[0].getAttribute("rowspan") ? element.children[1] : element.children[0])
        .map(element => element.innerText) 

    return artifact
}

extract_details_of({
    "name": "JUnit", 
    "page": "/artifact/junit/junit", 
    "usages": "123,479", 
    "description": "JUnit is a unit testing framework to write and run repeatable automated tests on Java.\nLast Release on Feb 13, 2021", 
    "coordinates": "junit.junit", 
    "license": "EPL"
})