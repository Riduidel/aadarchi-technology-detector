<h1 align="center">Welcome to aadarchi-technology-detector 👋</h1>
<p>
  <img alt="Version" src="https://img.shields.io/badge/version-1.00-blue.svg?cacheSeconds=2592000" />
  <a href="https://github.com/kefranabg/readme-md-generator#readme" target="_blank">
    <img alt="Documentation" src="https://img.shields.io/badge/documentation-yes-brightgreen.svg" />
  </a>
  <a href="https://github.com/kefranabg/readme-md-generator/graphs/commit-activity" target="_blank">
    <img alt="Maintenance" src="https://img.shields.io/badge/Maintained%3F-yes-green.svg" />
  </a>
  <a href="https://github.com/kefranabg/readme-md-generator/blob/master/LICENSE" target="_blank">
    <img alt="License: MIT" src="https://img.shields.io/github/license/kefranabg/aadarchi-technology-detector" />
  </a>
</p>

> A Java project linked to aadarchi which aims to detect notable technologies in a variety of languages

### 🏠 [Homepage](https://github.com/Riduidel/aadarchi-technology-detector)

## Prerequisites

. Install [jbang](https://www.jbang.dev/)
. Clone [TechEmpower frameworks](https://github.com/TechEmpower/FrameworkBenchmarks/) repository beside this repository

## Usage

### Fetching data from reports branches

This repository provides for each observed repository one branch containing in a given folder the file
aggregating all informations.

#### Available sources

| Source website    | Branch name           | Path of artifacts file(s)    |
| ----------------- | --------------------- | ---------------------------- |
| mvnrepository.com | reports_mvnrepository | mvnrepository/artifacts.json |

If you take a look at these branches, 
in each of them, you'll find one commit per month.

### Produced file format

Each scrapper should produce a file with this structure

```
[
  // A list of artifacts
  {
    // Artifact coordinates (depending upon the artifact type)
    "coordinates": "androidx.annotation.annotation",
    // Artifact visible name
    "name": "Annotation",
    // Artifact description (optionnal, but highly recommended)
    "description": "Provides source annotations for tooling and readability.",
    // Artifact licenses (optionnal)
    "license": [
      "Apache 2.0"
    ],
    // Artifact categories (optionnal)
    "categories": [
      "Annotation Libraries"
    ],
    // Artifact tags (optionnal)
    "tags": [
      "android",
      "annotations",
      "metadata"
    ],
    // Artifact ranking in registry (1 is the most important artifact - optionnal)
    "ranking": "114",
    // Artifact users number (optionnal)
    "users": 4307,
    // Number of downloads of this artifact
    "downloads": "-1",
    // Artifact repositories (optionnal)
    "repositories": [
      "Google",
      "SciJava Public"
    ],
    // Artifact versions (optionnal, but highly recommended)
    "versions": {
      // Each version has a number
      "1.8.0-alpha01": {
        // usage count for this version
        "usages": "3",
        // version release date
        "date": "Feb 21, 2024",
        // users count for this version
        "users": 3
      },
...
```


### Command line

Each process can be run on your machine to validate everything is good

#### MvnRepository

```
cd mvnrepository
jbang ExtractPopularMvnRepositorytArtifacts.java
```

It will produce an `artifacts.json` file with the artifacts usage count at the time the command is run

##### Extract MvnRepository
Thanks to the Internet Wayback Machine, it is possible to get the whole history for mvn repository by running the command

```
jbang ExtractPopularMvnRepositorytArtifacts.java --generate-history
```

This will fetch the whole history for MvnRepository and generate in a branch called history one commit for each month since the first capture available at https://archive.org.
This code is kept for tracability reason, to make sure the history fetching process is reproducable, but should not be run.

## Author

👤 **Riduidel & Helielzel**

* Github: [@Riduidel](https://github.com/riduidel)
* Github: [@Helielzel](https://github.com/helielzel)

## 🤝 Contributing

Contributions, issues and feature requests are welcome!<br />Feel free to check [issues page](https://github.com/kefranabg/readme-md-generator/issues). You can also take a look at the [contributing guide](https://github.com/kefranabg/readme-md-generator/blob/master/CONTRIBUTING.md).

## Show your support

Give a ⭐️ if this project helped you!

## 📝 License

Copyright © 2023 [Riduidel & Helielzel](https://github.com/Helielzel).<br />
This project is [MIT](https://github.com/kefranabg/readme-md-generator/blob/master/LICENSE) licensed.

***
_This README was generated with ❤️ by [readme-md-generator](https://github.com/kefranabg/readme-md-generator)_