# Анализатор страниц

[![Actions Status](https://github.com/4l3xT4lk3r/java-project-72/workflows/hexlet-check/badge.svg)](https://github.com/4l3xT4lk3r/java-project-72/actions)
[![project72-build](https://github.com/4l3xT4lk3r/java-project-72/actions/workflows/project72-build.yml/badge.svg)](https://github.com/4l3xT4lk3r/java-project-72/actions)
[![Maintainability](https://api.codeclimate.com/v1/badges/170b31e5c042efef6da1/maintainability)](https://codeclimate.com/github/4l3xT4lk3r/java-project-72/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/170b31e5c042efef6da1/test_coverage)](https://codeclimate.com/github/4l3xT4lk3r/java-project-72/test_coverage)  

[Demo on Render](https://page-analyzer-6gcp.onrender.com/)

## Requirements

* JDK 20
* Gradle 8.3
* GNU Make
* H2/PostgreSQL

## Preparation

```bash
git clone repo
cd app
```

## Run test server
1. Set environment variable PORT. For example - `export PORT=8080`
1. ```bash
   make run-dev
   ```
1. Open in browser http://127.0.0.1:PORT

## Run prod server

1. Import schema from `app/src/main/resources/schema.sql` to your database.
1. Set environment variable JDBC_DATABASE_URL. For example - `export JDBC_DATABASE_URL=jdbc:postgresql://db:5432/postgres?password=password&user=postgres`
1. Set environment variable PORT. For example - `export PORT=8080`
1. ```bash
   make run-prod
   ```
1. Open in browser http://127.0.0.1:PORT
