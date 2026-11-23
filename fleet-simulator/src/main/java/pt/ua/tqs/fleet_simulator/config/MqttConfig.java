package pt.ua.tqs.fleet_simulator.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.StringUtils;

import pt.ua.tqs.fleet_simulator.adapters.mqtt.MqttCommandHandler;

/**
 * Wires the MQTT adapters: readings go out through one client, commands and control
 * messages come in through another.
 */
@Configuration
public class MqttConfig {

    public static final String TELEMETRY_CHANNEL = "telemetryChannel";
    public static final String TOPIC_LEVEL_SEPARATOR = "/";

    /** QoS 1 (at least once), as the telemetry contract requires. */
    private static final int QOS_AT_LEAST_ONCE = 1;
    private static final String ANY_VEHICLE = TOPIC_LEVEL_SEPARATOR + "+";
    private static final String OUTBOUND_CLIENT_SUFFIX = "-out";
    private static final String INBOUND_CLIENT_SUFFIX = "-in";

    private final MqttProperties properties;

    public MqttConfig(MqttProperties properties) {
        this.properties = properties;
    }

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] {properties.brokerUrl()});
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        // Paho reconnects after a broker restart; the inbound adapter then subscribes again.
        options.setAutomaticReconnect(true);
        if (StringUtils.hasText(properties.username())) {
            options.setUserName(properties.username());
            options.setPassword(properties.password().toCharArray());
        }
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        // Nothing is written to disk: the container runs as a user without write access.
        factory.setPersistence(new MemoryPersistence());
        return factory;
    }

    @Bean(name = TELEMETRY_CHANNEL)
    public MessageChannel telemetryChannel() {
        return new DirectChannel();
    }

    @Bean
    public IntegrationFlow telemetryOutboundFlow(MqttPahoClientFactory clientFactory) {
        MqttPahoMessageHandler handler = new MqttPahoMessageHandler(
                properties.clientId() + OUTBOUND_CLIENT_SUFFIX, clientFactory);
        handler.setDefaultQos(QOS_AT_LEAST_ONCE);
        handler.setDefaultRetained(false);
        return IntegrationFlow.from(TELEMETRY_CHANNEL).handle(handler).get();
    }

    @Bean
    public IntegrationFlow commandInboundFlow(MqttPahoClientFactory clientFactory,
            MqttCommandHandler commandHandler) {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(
                properties.clientId() + INBOUND_CLIENT_SUFFIX, clientFactory,
                properties.commandTopicPrefix() + ANY_VEHICLE,
                properties.controlTopicPrefix() + ANY_VEHICLE);
        adapter.setQos(QOS_AT_LEAST_ONCE);
        // An unexpected error is logged instead of dropping the connection to the broker.
        adapter.setErrorChannelName(IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME);
        return IntegrationFlow.from(adapter)
                .handle(message -> commandHandler.handle(
                        message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class),
                        (String) message.getPayload()))
                .get();
    }
}