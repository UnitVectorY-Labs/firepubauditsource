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
package com.unitvectory.firepubauditsource.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.api.core.ApiFuture;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.google.pubsub.v1.TopicName;
import com.unitvectory.firepubauditsource.model.RecordAction;

import lombok.extern.slf4j.Slf4j;

/**
 * The Pub/Sub Service
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Service
@Slf4j
public class PubSubService {

    private final Publisher publisher;

    public PubSubService(@Value("${pubsub.topic}") String pubsubTopic,
            @Value("${project.id}") String projectId) {
        TopicName topicName = TopicName.of(projectId, pubsubTopic);

        try {
            this.publisher = Publisher.newBuilder(topicName).setEnableMessageOrdering(true).build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create publisher", e);
        }
    }

    public void publish(String jsonString, String documentPath, String database, RecordAction action) {
        // Preparing attributes for Pub/Sub message
        Map<String, String> attributes = new HashMap<>();
        attributes.put("database", database);

        // Split the documentPath on / and loop through each segment
        String[] segments = documentPath.split("/");
        for (int i = 0; i < segments.length; i++) {
            attributes.put("path" + i, segments[i]);
        }

        // Add the action to the attributes
        attributes.put("action", action.getAction());

        // Convert JSON string to bytes
        ByteString jsonData = ByteString.copyFromUtf8(jsonString);

        // Prepare the message to be published
        PubsubMessage message = PubsubMessage.newBuilder().setOrderingKey(documentPath)
                .setData(jsonData).putAllAttributes(attributes).build();

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
