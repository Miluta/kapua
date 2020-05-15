/*******************************************************************************
 * Copyright (c) 2020 Eurotech and/or its affiliates and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Eurotech - initial API and implementation
 *******************************************************************************/
package org.eclipse.kapua.integration.misc;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kapua.service.device.call.message.kura.KuraPayload;
import org.eclipse.kapua.transport.message.mqtt.MqttPayload;
import org.eclipse.kapua.transport.message.mqtt.MqttTopic;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientTest {

    private static final Logger logger = LoggerFactory.getLogger(ClientTest.class);

    //it's not thread safe but for this test it doesn't care
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("DD/MM/YYYY HH:mm:SS");
    private static final int MAX_MESSAGES = 100;
    private static final long WAIT = 500;

    private String userName = "kapua-broker";
    private String password = "kapua-password";
    private String clientId = "test-datastore-client";

    @Test
    public void testDatastore() throws Exception {
        MqttClient client = new MqttClient("tcp://localhost:1883", clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(userName);
        options.setPassword(password.toCharArray());
        options.setCleanSession(true);
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        client.setCallback(new MqttCallback() {

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                logger.info("Received message: {} - {} - {}", topic, message.getPayload().length, new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                try {
                    logger.info("Sent message: {} - {} - len: {}", token.getTopics()[0], token.getMessageId(), token.getMessage());
                } catch (Exception e) {
                    logger.error("ERROR!", e);
                }
            }

            @Override
            public void connectionLost(Throwable cause) {
                logger.warn("Connection lost: {}", cause.getMessage(), cause);
            }
        });
        client.connect(options);
        String subTopic = "kapua-sys/" + clientId + "/#";
        String pubTopic = "kapua-sys/" + clientId + "/topic1";
        client.subscribe(subTopic);
        for (int i=0; i<MAX_MESSAGES; i++) {
            client.publish(pubTopic, getMessage(pubTopic), 1, false);
            Thread.sleep(WAIT);
        }
        client.disconnect();
    }

    private byte[] getMessage(String topicStr) {
        MqttTopic topic = new MqttTopic(topicStr);
        KuraPayload payloadKura = new KuraPayload();
        payloadKura.setBody(("Birth message - " + DATE_FORMAT.format(new Date())).getBytes());
        Map<String, Object> metrics = new HashMap<String, Object>();
        metrics.put("metric1", "value1");
        payloadKura.setMetrics(metrics);
        MqttPayload payload = new MqttPayload(payloadKura.toByteArray());
        org.eclipse.kapua.transport.message.mqtt.MqttMessage mqttMessage = new org.eclipse.kapua.transport.message.mqtt.MqttMessage(topic, new Date(), payload);
        return mqttMessage.getPayload().getBody();
    }
}
