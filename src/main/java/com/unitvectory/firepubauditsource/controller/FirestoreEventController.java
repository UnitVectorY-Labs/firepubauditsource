/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.unitvectory.firepubauditsource.controller;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.events.cloud.firestore.v1.DocumentEventData;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import com.unitvectory.firepubauditsource.util.DocumentResourceNameUtil;
import com.unitvectory.firestoreproto2json.FirestoreProto2Json;

import lombok.extern.slf4j.Slf4j;

/**
 * The Firestore event controller
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@RestController
@Slf4j
public class FirestoreEventController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS")
            .withZone(ZoneOffset.UTC);

    private Publisher publisher;

    public FirestoreEventController() {
        // TODO: Change this to spring boot loading, this is a mess right now
        String topicId = System.getenv("PUBSUB_TOPIC");
        String projectId = System.getenv("PROJECT_ID");

        TopicName topicName = TopicName.of(projectId, topicId);

        try {
            this.publisher = Publisher.newBuilder(topicName).build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create publisher", e);
        }
    }

    @PostMapping(value = "/firestore", consumes = "application/protobuf")
    public void handleFirestoreEvent(@RequestBody byte[] data) throws InvalidProtocolBufferException {

        // Timestamp for the current time
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

        // Parse the Firestore data
        DocumentEventData firestoreEventData = DocumentEventData.parseFrom(data);

        // Process the request

        // Create the JSON object using gson
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("timestamp", FORMATTER.format(now));

        // Get the resource name for the document for insert/update/delete
        String resourceName = null;
        if (firestoreEventData.hasValue()) {
            resourceName = firestoreEventData.getValue().getName();
        } else if (firestoreEventData.hasOldValue()) {
            resourceName = firestoreEventData.getOldValue().getName();
        }

        // Invalid input, no resource name means cannot process
        if (resourceName == null) {
            log.warn("resourceName is null");
            return;
        }

        // Extract the database name
        String database = DocumentResourceNameUtil.getDatabaseId(resourceName);
        if (database == null) {
            log.warn("extracted database is null");
            return;
        }

        jsonObject.addProperty("database", database);

        // Extract the document path
        String documentPath = DocumentResourceNameUtil.getDocumentPath(resourceName);
        if (documentPath == null) {
            log.warn("extracted documentPath is null");
            return;
        }

        jsonObject.addProperty("documentPath", documentPath);

        if (firestoreEventData.hasValue()) {
            // Convert the value to JSON
            JsonObject valueJson = FirestoreProto2Json.DEFAULT.valueToJsonObject(firestoreEventData);
            jsonObject.add("value", valueJson);
        } else {
            jsonObject.add("value", JsonNull.INSTANCE);
        }

        if (firestoreEventData.hasOldValue()) {
            // Convert the old value to JSON
            JsonObject oldVauleJson = FirestoreProto2Json.DEFAULT.valueToJsonObject(firestoreEventData);
            jsonObject.add("oldValue", oldVauleJson);
        } else {
            jsonObject.add("oldValue", JsonNull.INSTANCE);
        }

        // Publish the JSON message to the Pub/Sub topic

        // Preparing attributes for Pub/Sub message
        Map<String, String> attributes = new HashMap<>();
        attributes.put("database", database);

        // Prepare the message to be published
        PubsubMessage message = PubsubMessage.newBuilder().setOrderingKey(documentPath)
                .setData(ByteString.copyFrom(data)).putAllAttributes(attributes).build();

        // Publish the message
        try {
            ApiFuture<String> future = publisher.publish(message);
            publisher.publishAllOutstanding();
            String messageId = future.get();

            // Wait on the future to ensure message is sent
            log.info("Published " + message.getOrderingKey() + " with message ID: " + messageId);
        } catch (Exception e) {
            log.error("Failed to publish message: " + message.getOrderingKey());
            throw new RuntimeException("Failed to publish message.", e);
        }
    }
}