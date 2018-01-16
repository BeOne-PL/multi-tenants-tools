# MultiTenants Tools

The project contains tools useful for users who are in many tenants (e.g. admins).
There are two parts of the project:
- AMP addon for Alfresco
- ADF application

Angular project template was generated using [ADF application generator for Yeoman](https://github.com/Alfresco/generator-ng2-alfresco-app)

## Features

- searching nodes in several tenancy at the same time

## Settings

You can find Alfresco connection settings in the `proxy.conf.json` file.

## Quick start

### AMP

```sh
java -jar bin\alfresco-mmt.jar install amps/multitenant-1.0-SNAPSHOT.amp tomcat\webapps\alfresco.war
```

### ADF application

```sh
npm install
npm start
```
