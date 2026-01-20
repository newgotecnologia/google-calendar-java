# Google Calendar Java Module

A Java library for integrating with Google Calendar API. This module provides a simple interface to create, update, and cancel events in Google Calendar using a service account.

## Overview

This module is designed to be used as a Qualitrix project that needed Google Calendar integration. It handles authentication, event management, and attendee configuration through a clean domain-driven design.

## Features

- ✅ Create events in Google Calendar
- ✅ Update existing events
- ✅ Cancel/delete events
- ✅ Manage event attendees
- ✅ Service account authentication
- ✅ Domain-wide delegation support

## Requirements

- Java 8 or higher
- Maven 3.x
- Google Cloud Platform project with Calendar API enabled
- Service account with domain-wide delegation (for Google Workspace)

## Installation

Add this module as a dependency in your parent project's `pom.xml`:

```xml
<dependency>
    <groupId>org.newgo</groupId>
    <artifactId>google-calendar</artifactId>
    <version>1.1.1</version>
</dependency>
```

## Configuration

### Google Cloud Setup

1. Create a project in [Google Cloud Console](https://console.cloud.google.com/)
2. Enable the **Google Calendar API**
3. Create a **Service Account** and download the JSON credentials file
4. If using Google Workspace, configure **Domain-wide Delegation** for the service account

### Application Properties

Create an `application.properties` file in your parent project's `src/main/resources` directory:

```properties
# Path to the service account credentials JSON file
google.calendar.serviceAccount=${GOOGLE_CALENDAR_CREDENTIALS_PATH}

# Email of the user to impersonate (for domain-wide delegation)
google.calendar.emailSender=${GOOGLE_CALENDAR_EMAIL_SENDER}
```

### Environment Variables

Set the following environment variables in your parent project:

| Variable | Description | Example |
|----------|-------------|---------|
| `GOOGLE_CALENDAR_CREDENTIALS_PATH` | Absolute path to the service account JSON credentials file | `/app/credentials/service-account.json` |
| `GOOGLE_CALENDAR_EMAIL_SENDER` | Email address of the user to impersonate | `calendar@yourdomain.com` |

## API Reference

### EventDomain

| Field | Type | Description |
|-------|------|-------------|
| `id` | `String` | Google Calendar event ID (null for new events) |
| `calendarId` | `String` | Google Calendar ID (null for new events) |
| `title` | `String` | Event title/summary |
| `description` | `String` | Event description |
| `startDate` | `ZonedDateTime` | Event start date and time |
| `endDate` | `ZonedDateTime` | Event end date and time |
| `attendees` | `List<AttendeeDomain>` | List of event attendees |
| `isDeleted` | `boolean` | Flag to indicate if event should be cancelled |

### AttendeeDomain

| Field | Type | Description |
|-------|------|-------------|
| `name` | `String` | Attendee display name |
| `email` | `String` | Attendee email address |
| `optional` | `boolean` | Whether the attendee is optional |
| `responseStatus` | `String` | Response status (default: "accepted") |

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| Google Calendar API | v3-rev411-1.25.0 | Calendar operations |
| Google Auth Library | 1.24.0 | OAuth2 authentication |
| Google API Client | 1.35.1 | API client utilities |
| Google HTTP Client | 1.44.2 | HTTP transport |
| SLF4J | 2.0.13 | Logging facade |
| Log4j2 | 2.23.1 | Logging implementation |
| Lombok | 1.18.34 | Boilerplate reduction |
