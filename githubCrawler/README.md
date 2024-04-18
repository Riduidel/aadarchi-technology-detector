## How to add a site to crawl?

In the `sites.json` file, you can add new sites by adding an object with the following properties:

```json
{
  "github/trendings": {
    "selector": "h2 a.Link |> prepend:https://github.com",
    "pages": [
      "https://github.com/trending/typescript?since=monthly",
      "https://github.com/trending/javascript?since=monthly"
    ]
  },
  "codebase.show": {
    "selector": "a[href^='https://github.com'] > div |> parent",
    "pages": [
      "https://codebase.show/projects/realworld?category=frontend",
      "https://codebase.show/projects/realworld?category=backend",
      "https://codebase.show/projects/realworld?category=fullstack",
      "https://codebase.show/projects/todomvc"
    ]
  }
}
```

`selector` is the CSS selector of the element you want to crawl, `pages` are the URLs where you want to crawl from.

`selector` can have a particular syntax to modify the curated href, for example:

- `a[href^='https://github.com'] > div |> parent` will crawl all links that starts with https://github.com which contain a div and then crawl their parents
- `h2 a.Link |> prepend:https://github.com` will crawl all h2 element, then crawl their children (a elements), then prepend "https://github.com" to each href attribute of those links

## How to run the crawler?

With `bun crawl`, you can start a new crawling session with the given configuration.