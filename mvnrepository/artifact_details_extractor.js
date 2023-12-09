function extract_artifact_details() {
    function parseUsageString(usages) {
        if(usages.includes(" ")) {
            usages = usages.substring(0, usages.indexOf(" "))
        }
        if(usages) {
            usages = usages.replace(",", "")
        } else {
            usages = "0"
        }
        return Number(usages)
    }
    /**
     * Given an element (which *must* be a table) and an artifact, read all the versions from the table and put them
     * into the artifact["version"]. This will contain a map linking version number to another map containing all 
     * the other fields (date, usages, vulns, ..)
     * @param {*} versionsTable 
     * @param {*} artifact 
     */
    function readWhateverVersionTable(versionsTable, artifact) {
        console.debug("Using ", arguments.callee.name, " for ", versionsTable, "to fill", artifact)
        if(versionsTable.tagName.toLowerCase()!="table") {
            throw "Can't read versions from element "+versionsTable+" in page "+document.location
        }
        // First, convert header to a map linking text to col index
        var headersToCols = {
            "_readAllColumns": function(tr) {
                const firstColum = tr.children[0]
                var returned = {}
                for(var property in this) {
                    if(!property.startsWith("_") && property!="repository" && property!="vulnerabilities" && property!="") {
                        // If the empty column exists, we have version grouping
                        if(this.hasOwnProperty("")) {
                            if(firstColum.getAttribute("rowspan")) {
                                returned[property] = tr.children[this[property]].innerText
                            } else {
                                returned[property] = tr.children[this[property]-1].innerText
                            }
                        } else {
                            returned[property] = tr.children[this[property]].innerText
                        }
                    }
                }
                // Reconvert some well-known columns
                if(returned.hasOwnProperty("usages")) {
                    returned["users"]=parseUsageString(returned["usages"])
//                    delete returned["usages"]
                }
                return returned
            },
            "_rowToKeyValue": function(tr) {
                var version = null
                var attributes = this._readAllColumns(tr)
                version = attributes[this._VERSION_COLUMN_NAME]
                delete attributes[this._VERSION_COLUMN_NAME]
                return [version, attributes]
            }
        }
        Array.from(versionsTable.querySelectorAll("th"))
            .forEach((th, index) => {
                const text = th.innerText.toLowerCase()
                headersToCols[text] = index
                if(text.startsWith("version")) {
                    headersToCols._VERSION_COLUMN_NAME=text
                }
            })
        artifact["versions"] = Array.from(versionsTable.querySelectorAll("tr"))
            .filter(element => element.querySelector("td"))
            .map(tr => headersToCols._rowToKeyValue(tr))
            .reduce((map, array) => {
                map[array[0]] = array[1]
                return map
            }, {})
    }
    function get2009StartCallback() {
        const read2009Versions =  function(element, artifact) {
            console.debug("Using ", arguments.callee.name, " for ", element, "to fill", artifact)
            if(element.nodeType==1) {
                if(element.tagName.toLowerCase()=="table") {
                    readWhateverVersionTable(element, artifact)
                    return null
                }
            }
            return read2009Versions
        }
        const read2009Tags = function(element, artifact) {
            if(!storage.hasOwnProperty(arguments.callee.name)) {
                storage[arguments.callee.name]={"inTags": false}
            }
            console.debug("Using ", arguments.callee.name, " for ", element, "to fill", artifact)
            if(element.nodeType==1) {
                if(element.tagName.toLowerCase()=="h3") {
                    return read2009Versions
                } else if(element.tagName.toLowerCase()=="a") {
                    if(storage[arguments.callee.name].inTags) {
                        if(!artifact["tags"]) {
                            artifact["tags"]=[]
                        }
                        artifact["tags"].push(element.innerText)
                    }
                }
            } else if(element.nodeType==3) {
                if(element.nodeValue.includes("tags")) {
                    storage[arguments.callee.name].inTags = true
                }
            }
            return read2009Tags
        }
        const read2009Name = function(element, artifact) {
            console.debug("Using ", arguments.callee.name, " for ", element, "to fill", artifact)
            if(element.nodeType==1) {
                if(element.tagName.toLowerCase()=="h3") {
                    artifact["name"] = element.innerText
                } else if(element.tagName.toLowerCase()=="p") {
                    artifact["description"]=element.innerText
                    return read2009Tags;
                }
            }
            return read2009Name
        }
        return read2009Name
    }
    function get2023StartCallback() {
        const read2023Versions =  function(element, artifact) {
            console.debug("Using ", arguments.callee.name, " for ", element, "to fill", artifact)
            if(element.nodeType==1) {
                if(element.tagName.toLowerCase()=="div") {
                    if(element.querySelector("#snippets ul.tabs li")) {
                        // Process exposing repositories (which will allow us a nice hack)
                        // beware! All repositories don't contain all versions, and we use as reference the first repository
                        const filter = /(.*) \(\d+\)$/
                        artifact["repositories"] = Array.from(element.querySelectorAll("#snippets ul.tabs li"))
                            .map(element => element.innerText)
                            .map(text =>  text.match(filter)[1])
                    }
                    // Now process known versions
                    const versionsTable = element.querySelector("table") 
                    if(versionsTable) {
                        readWhateverVersionTable(versionsTable, artifact)
                        return null
                    }
                }
            }
            return read2023Versions
        }
        // Can be tested on https://mvnrepository.com/artifact/junit/junit
        const read2023Relocation = function(element, artifact) {
            console.debug("Using ", arguments.callee.name, " for ", element, "to fill", artifact)
            if(element.nodeType==1) {
                if(element.tagName.toLowerCase()=="div") {
                    const relocation = element.querySelector("table.grid td")
                    if(relocation) {
                        links = Array.from(relocation.querySelectorAll("a"))
                        artifact["relocation"] = {
                            "page": {
                                groupId: links[0].text,
                                artifactId: links[1].text
                            },
                            "coordinates": links.map(element => element.innerText).join(".")
                        }
                    }
                } else if(element.tagName.toLowerCase()=="style") {
                    return read2023Versions
                }
            }
            return read2023Relocation
        }
        const read2023LicenseCategoryTable = function(element, artifact) {
            console.debug("Using ", arguments.callee.name, " for ", element, "to fill", artifact)
            if(element.nodeType==1) {
                if(element.tagName.toLowerCase()=="table") {
                    Array.from(element.children[0].children).forEach(tr => {
                        const name = tr.children[0].innerText
                        switch (name) {
                            case "License":
                            case "Categories":
                            case "Tags":
                                artifact[name.toLowerCase()]=Array.from(tr.children[1].children).map(node => node.innerText)
                                break;
                            case "Ranking":
                                const re = /#(\d+) in/g
                                const value_cell = Array.from(tr.children[1].children).map(node => node.innerText)+""
                                if(result = re.exec(value_cell)) {
                                    artifact["ranking"] = result[1]
                                } else {
                                    console.log("no match found in text "+value_cell)
                                }
                    
                            case "Used By":
                                const usages = tr.children[1].innerText
                                if(!isNaN(usages)) {
                                    artifact["users"] = parseUsageString(usages)
                                }
                                break;
                            default:
                                break;
                        }
                    })
                    return read2023Relocation
                }
            }
            return read2023LicenseCategoryTable
        }
        const read2015Tags = function(element, artifact) {
            console.debug("Using ", arguments.callee.name, " for ", element, "to fill", artifact)
            if(element.nodeType==1) {
                if(element.tagName.toLowerCase()=="div") {
                    artifact["tags"] = Array.from(element.querySelectorAll("a"))
                    .map(element => element.innerText)
                    return read2023Versions
                }
            }
            return read2015Tags
        }
        const read2023Name = function(element, artifact) {
            console.debug("Using ", arguments.callee.name, " for ", element, "to fill", artifact)
            if(element.nodeType==1) {
                if(element.tagName.toLowerCase()=="div") {
                    if(element.className.includes("breadcrumb")) {
    // Contains groupId/artifactId, ignored
                    } else if(element.className=="im") {
                        if(element.querySelector("img.im-logo")) {
                            artifact["logo"] = element.querySelector("img.im-logo").getAttribute("currentSrc")
                        }
                        if(element.querySelector(".im-usage")) {
                            const usages = element.querySelector(".im-usage").innerText
                            if(!isNaN(usages)) {
                                artifact["users"] = parseUsageString(usages)
                            }
                        }
                        if(element.querySelector("h2 a")) {
                            artifact["name"] = element.querySelector("h2 a").innerText
                        }
                        if(element.querySelector("div.im-description")) {
                            artifact["description"] = element.querySelector("div.im-description").innerText
                        }
                        // This table is in fact not always present
                        // (see https://web.archive.org/web/20150207202413/http://mvnrepository.com/artifact/ch.qos.logback/logback-classic)
                        // So perform some look-ahead to see what is coming next
                        if(!artifact.hasOwnProperty("users")) {
                            return read2023LicenseCategoryTable
                        } else {
                            return read2015Tags
                        }
                    }
                }
            }
            return read2023Name
        }
        return read2023Name
    }
    // A magic object allowing functions to have data associated with
    var storage = {}
    var analyzedElement = null
    var usedCallback = null
    const path = document.location.pathname
    artifact = {}
    artifact["coordinates"] = path.substring(path.indexOf("/artifact/")+"/artifact/".length).split("/").join(".")
    if(document.querySelector("div.content")) {
        analyzedElement = document.querySelector("div.content")
        console.log("Selected MvnRepository 2023")
        usedCallback = get2023StartCallback()
    } else if(document.querySelector("div#maincontent")) {
        analyzedElement = document.querySelector("div#maincontent")
        // Wait, we need more fine tuning
        if(analyzedElement.querySelector("div.breadcrumb")) {
            console.log("Selected MvnRepository 2021")
            usedCallback = get2023StartCallback()
        } else {
            // Can be tested on https://web.archive.org/web/20140209041351/http://mvnrepository.com/artifact/ch.qos.logback/logback-classic
            console.log("Selected MvnRepository 2014")
            usedCallback = get2009StartCallback()
        }
    } else if(document.querySelector("div#body")) {
        // Can be tested on https://web.archive.org/web/20090218110135/http://mvnrepository.com/artifact/ch.qos.logback/logback-classic
        analyzedElement = document.querySelector("div#body")
        console.log("Selected MvnRepository 2009")
        usedCallback = get2009StartCallback()
    } else {
        throw "Can't read artifact informations from "+document.location
    }
    Array.from(analyzedElement.childNodes).forEach(element => {
        if(usedCallback)
            usedCallback = usedCallback(element, artifact)
    })
    return artifact
}