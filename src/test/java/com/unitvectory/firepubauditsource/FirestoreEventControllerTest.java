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
package com.unitvectory.firepubauditsource;

import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import com.google.cloud.pubsub.v1.Publisher;
import com.google.pubsub.v1.PubsubMessage;
import com.unitvectory.consistgen.epoch.StaticEpochTimeProvider;
import com.unitvectory.fileparamunit.ListFileSource;
import com.unitvectory.firepubauditsource.controller.FirestoreEventController;
import com.unitvectory.firepubauditsource.service.PubSubService;
import com.unitvectory.jsonassertify.JSONAssert;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.SettableApiFuture;

/**
 * Test case for FirestoreEventController
 * 
 * @author Jared Hatfield (UnitVectorY Labs)
 */
public class FirestoreEventControllerTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @ParameterizedTest
    @ListFileSource(resources = "/tests/", fileExtension = ".json", recurse = true)
    public void testPost(String fileName) throws Exception {

        // Arrange
        Path eventFilePath = Path.of(fileName);
        String eventContent = Files.readString(eventFilePath);
        JsonNode jsonNode = OBJECT_MAPPER.readTree(eventContent);
        String inputData = jsonNode.get("input").asText();

        Publisher mockPublisher = Mockito.mock(Publisher.class);
        SettableApiFuture<String> mockFuture = SettableApiFuture.create();
        mockFuture.set("mocked-message-id");
        Mockito.when(mockPublisher.publish(Mockito.any(PubsubMessage.class))).thenReturn(mockFuture);

        PubSubService pubSubService = new PubSubService(mockPublisher);
        StaticEpochTimeProvider epochTimeProvider = StaticEpochTimeProvider.builder()
                .epochTimeMilliseconds(1732406400000L).build();
        FirestoreEventController controller = new FirestoreEventController(pubSubService, epochTimeProvider);

        // Act
        byte[] decodedData = Base64.getDecoder().decode(inputData);
        controller.handleFirestoreEvent(decodedData);

        // Assert
        ArgumentCaptor<PubsubMessage> messageCaptor = ArgumentCaptor.forClass(PubsubMessage.class);
        Mockito.verify(mockPublisher).publish(messageCaptor.capture());
        PubsubMessage capturedMessage = messageCaptor.getValue();
        assertNotNull(capturedMessage);

        JsonNode output = jsonNode.get("output");

        String actualData = capturedMessage.getData().toStringUtf8();
        String expectedData = output.get("message").toString();
        JSONAssert.assertEquals(expectedData, actualData, true);

        String actualAttributes = OBJECT_MAPPER.writeValueAsString(capturedMessage.getAttributesMap());
        String expectedAttributes = output.get("attributes").toString();
        JSONAssert.assertEquals(expectedAttributes, actualAttributes, true);

        String actualOrderingKey = capturedMessage.getOrderingKey();
        String expectedOrderingKey = output.get("orderingKey").asText();
        assertEquals(expectedOrderingKey, actualOrderingKey);
    }
}
