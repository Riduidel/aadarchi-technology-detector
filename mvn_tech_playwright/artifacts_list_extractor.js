function extract_libraries() {
    const elements = document.querySelectorAll("div.im")
    result = []
    elements.forEach(function(element) {
        node = {}
        const headerLinks = element.querySelectorAll("h2.im-title a")
        for(const link of headerLinks) {
            if(!node.hasOwnProperty("name")) {
                node["name"]=link.innerText
                node["page"] = link.getAttribute("href")
            } else if(!node.hasOwnProperty("usages")) {
                node["usages"]=link.querySelector("b").innerText
            }
        }
        // There may be empty elements due to adblockers
        if(element.querySelector("div.im-description")) {
            node["description"] = element.querySelector("div.im-description").innerText
        }
        if(element.querySelector("p.im-subtitle")) {
            coordinatesAndLicense = element.querySelector("p.im-subtitle")
            const coordinates = Array.from(coordinatesAndLicense.querySelectorAll("a"))
                .map((node) => node.innerText)
                .join(".")
            node["coordinates"] = coordinates
            if(coordinatesAndLicense.querySelector(".im-lic")) {
                node["license"] = coordinatesAndLicense.querySelector(".im-lic").innerText
            }
        }
        if(Object.keys(node).length)
            result.push(node)
    })
    console.log("extracted artifacts", result)
    return result
}
