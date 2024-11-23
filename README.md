[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0) [![Work In Progress](https://img.shields.io/badge/Status-Work%20In%20Progress-yellow)](https://guide.unitvectorylabs.com/bestpractices/status/#work-in-progress)

# firepubauditsource

Publishes Firestore data changes to Pub/Sub as JSON audit records for downstream processing.

## References

- [firepubauditsource](https://github.com/UnitVectorY-Labs/firepubauditsource) - Publishes Firestore data changes to Pub/Sub as JSON audit records for downstream processing.
- [firepubauditsource-tofu](https://github.com/UnitVectorY-Labs/firepubauditsource-tofu) - A module for OpenTofu that deploys firepubauditsource to GCP Cloud Run, along with configuring essential services including Eventarc for Firestore and Pub/Sub.
- [bqpubauditsink](https://github.com/UnitVectorY-Labs/bqpubauditsink) - Ingests Pub/Sub audit JSON events and inserts the records into BigQuery.
- [bqpubauditsink-tofu](https://github.com/UnitVectorY-Labs/bqpubauditsink-tofu) - A module for OpenTofu that deploys bqpubauditsink to GCP Cloud Run, along with configuring essential services including the Pub/Sub subscription and BigQuery dataset and table.

## Overview

The purpose of this application is to take all record changes from a Firestore table and publish them to a Pub/Sub topic. This allows for downstream applications to process the complete database record changes including the old and new values.

This application is designed to run in Cloud Run and is triggered by changes to Firestore records using Eventarc.

### Use Case: BigQuery

- A companion application [bqpubauditsink](https://github.com/UnitVectorY-Labs/bqpubauditsink) takes the Pub/Sub messages and writes them to BigQuery in a way that allows BigQuery to be a direct replica of the data stored in Firestore.

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

## Limitations

- The translation from the Firestore document to JSON is not perfect, it uses [firestoreproto2json](https://github.com/UnitVectorY-Labs/firestoreproto2json) with the default settings to convert the Firestore document to JSON. This means that some data types may not be converted correctly.
