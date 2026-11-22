package pt.ua.tqs.flexmotion_cycles.adapters;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;

@Configuration
@ConditionalOnBooleanProperty("flexmotion.telemetry.enabled")
@EnableConfigurationProperties(TelemetryProperties.class)
class MqttTelemetryConfig {

    private static final int QOS_AT_LEAST_ONCE = 1;
    private static final String ANY_VEHICLE = "+";

    @Bean
    MqttTelemetryAdapter mqttTelemetryAdapter(TelemetryMessageParser parser) {
        return new MqttTelemetryAdapter(parser);
    }

    @Bean
    MqttPahoClientFactory telemetryMqttClientFactory(TelemetryProperties properties) {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[] {properties.brokerUrl()});
        options.setUserName(properties.username());
        options.setPassword(properties.password().toCharArray());
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    IntegrationFlow telemetryInboundFlow(MqttPahoClientFactory telemetryMqttClientFactory,
            TelemetryProperties properties, MqttTelemetryAdapter adapter) {
        MqttPahoMessageDrivenChannelAdapter inbound = new MqttPahoMessageDrivenChannelAdapter(
                properties.clientId(), telemetryMqttClientFactory,
                properties.topicPrefix() + "/" + ANY_VEHICLE);
        inbound.setQos(QOS_AT_LEAST_ONCE);
        return IntegrationFlow.from(inbound)
                .handle(message -> adapter.handle((String) message.getPayload()))
                .get();
    }
}
