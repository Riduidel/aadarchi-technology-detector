function extract_details_of(artifact) {
    const main = document.querySelector("div.content")
    const logo = main.querySelector("im.im-logo")
    if(logo) {
        artifact["logo"] = logo.getAttribute("src")
    }
    // First table should contain some details (license, categories, and so on)
    const artifact_details = main.querySelector("table.grid")
    // Yeah, there is a bad TBODY in that table
    for(row of artifact_details.children[0].children) {
        header_cell = row.children[0].innerText.toLowerCase()
        if(header_cell=="categories") {
            artifact["categories"] = Array.from(row.children[1].children).map(node => node.innerText)
        } else if(header_cell=="tags") {
            artifact["tags"] = Array.from(row.children[1].children).map(node => node.innerText)
        }
    }

    // Now it become quite cumbersome
    // we may have a relocation table
    // we must have a verson table
    // we may have a book table
    // All of which are table.grid below some specifc elements ...
    if(main.querySelector("div table.grid")) {
        // There may be a relocation table
        relocation = main.querySelector("div table.grid")
        // Beware: this can match the versions table. To avoid that, we count the column number
        // if greater than 1, we give up
        // Unfortunatly
        if(relocation.querySelectorAll("td").length==1) {
            links = Array.from(relocation.querySelectorAll("a"))
            artifact["relocation"] = {
                "page": links[links.length-1].getAttribute("href"),
                "coordinates": links.map(element => element.innerText).join(".")
            }
        }
    }

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
/*
extract_details_of({
    "name": "Guava", 
    "page": "/artifact/com.google.guava/guava", 
    "usages": "35,020", 
    "description": "...", 
    "coordinates": "com.google.guava.guava", 
    "license": "EPL"
})
*/