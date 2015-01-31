function locate_artifacts_urls() {
    function toUrl(url) {
        return url.startsWith("https://") ? url : 
            url.startsWith("/") ? "https://"+document.domain+url : window.location.href.split('?')[0]+url
    }
    const main = document.querySelector(".content")
    const artifacts = Array.from(main.querySelectorAll(".im-subtitle"))
        .map(paragraph => paragraph.innerText)
        .map(text => text.substring(0, text.indexOf("\n")))
        .map(text => text.split(" "))
        .filter(array => array.length>2)
        .map(function(array) { return {"groupId":array[0], "artifactId":array[array.length-1]}})

    const nav = main.querySelector("ul.search-nav")
    const pages = nav.querySelectorAll("li")
    pagination = {}
    pagination["page"] = Array.from(pages)
        .filter(element => element.getAttribute("class") && element.getAttribute("class").includes("current"))
        .filter(element => element.innerText.match(/^\d+$/))
        [0].innerText

    function createPaginationFor(element) {
        if(element && !(element.getAttribute("class") && element.getAttribute("class").includes("current"))) {
            return toUrl(element.querySelector("a").getAttribute("href"))
        } else {
            return null
        }
    }

    const prev = pages[0]
    pagination["prev"] = createPaginationFor(pages[0])
    pagination["next"] = createPaginationFor(pages[pages.length-1])

    return {
        "data": artifacts,
        "page": pagination
    }
}