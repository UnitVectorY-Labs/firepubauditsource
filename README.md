# firepubauditsource

Publishes Firestore data changes to Pub/Sub as JSON audit records for downstream processing.

## Overview

The purpose of this application is to take all record changes from a Firestore table and publish them to a Pub/Sub topic. This allows for downstream applications to process the complete database record changes including the old and new values. The companion application [bqpubauditsink](https://github.com/UnitVectorY-Labs/bqpubauditsink) takes the Pub/Sub messages and writes them to BigQuery in a way that allows BigQuery to be a direct replica of the data stored in Firestore.

This application is designed to run in Cloud Run and is triggered by changes to Firestore records using Eventarc.

## Configuration

This application is run as a docker container and requires the following environment variables to be set:

- `PROJECT_ID`: The GCP project ID where the Firestore and Pub/Sub resources are located.
- `PUBSUB_TOPIC`: The Pub/Sub topic to publish the audit records to.

## Example Pub/Sub Message

The following show what the JSON message will look like when published to Pub/Sub. The `oldValue` field will be `null` for inserts, and `value` will be `null` for deletes.

Inserting a Record:

```json
{
  "timestamp": "2024-10-27 12:00:00.000000",
  "database": "(default)",
  "documentPath": "mycollection/mydoc",
  "value": {
    "foo": "new"
  },
  "oldValue": null
}
```

Updating a Record:

```json
{
  "timestamp": "2024-10-27 12:00:10.000000",
  "database": "(default)",
  "documentPath": "mycollection/mydoc",
  "value": {
    "foo": "updated"
  },
  "oldValue": {
    "foo": "bar"
  }
}
```

Deleting a Record:

```json
{
  "timestamp": "2024-10-27 12:00:20.000000",
  "database": "(default)",
  "documentPath": "mycollection/mydoc",
  "value": null,
  "oldValue": {
    "foo": "bar"
  }
}
```
