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

> A Java project loosely spawned aadarchi which aims to detect notable technologies in a variety of languages

### 🏠 [Homepage](https://github.com/Riduidel/aadarchi-technology-detector)

## Prerequisites

* Install Java 21
* Create a [libraries.io API token](https://libraries.io/api#:~:text=API%20Docs-,authentication,-All%20API%20requests)
* Create a [GitHub API token](https://docs.github.com/en/rest/authentication/authenticating-to-the-rest-api?apiVersion=2022-11-28)

## Usage

### Accessing to generated metrics

As of today, latest version of metrics are available in [src/main/resources/csv](https://github.com/Riduidel/aadarchi-technology-detector/tree/main/src/main/resources/csv).
Once [#93](https://github.com/Riduidel/aadarchi-technology-detector/issues/93) will have been implemented, the CSV files will also be available as GitHub releases.

### Building the JAR

Since we mainly use Camel Quarkus, the whole application can be built the usual maven way: `mvn install`

### Developping new features

This project is a "simple" Camel Quarkus project.
But it also uses various API credentials (at least GitHub API and Libraries.io API).

So you first need to create in your maven settings a tech-trends profile grouping these settings:

```
		<profile>
			<id>settings-tech-trends</id>
			<properties>
				<tech-trends.libraries.io.token><!-- Replace with your own Libraries.io token --></tech-trends.libraries.io.token>
				<tech-trends.github.token><!-- Replace with your own GitHub API token --></tech-trends.github.token>
			</properties>
		</profile>
```

Once this profile is created, developing is as easy as

1. Load project in your preferred IDE
2. Run `mvn quarkus:dev -Psettings-tech-trends`
3. Profit (you can even remote debug the application on port 5005)

### Do not develop features without having discussed first with the team

We do want to have new features.
But we want to have these features discussed **first**.
So if you want to develop a new feature, check first if there is an associated issue (and believe us, it's easy to add new issues on this project).
Once the issue exists, you can create your PR and we will try to review it as fast as possible.

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
