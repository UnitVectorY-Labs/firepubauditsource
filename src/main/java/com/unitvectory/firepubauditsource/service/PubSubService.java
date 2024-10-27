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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.TopicName;

/**
 * The Pub/Sub Service
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
@Service
public class PubSubService {

    @Value("${pubsub.topic}")
    private String pubsubTopic;

    @Value("${project.id}")
    private String projectId;

    @Bean
    public Publisher publisher() {
        TopicName topicName = TopicName.of(projectId, pubsubTopic);

        try {
            return Publisher.newBuilder(topicName).build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to create publisher", e);
        }
    }
}
