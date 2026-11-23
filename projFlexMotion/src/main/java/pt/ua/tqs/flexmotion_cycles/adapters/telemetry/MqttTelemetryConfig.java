package pt.ua.tqs.flexmotion_cycles.adapters.telemetry;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.messaging.MessageChannel;

@Configuration
@ConditionalOnProperty(name = "flexmotion.telemetry.adapter", havingValue = "mqtt")
@EnableConfigurationProperties(TelemetryProperties.class)
class MqttTelemetryConfig {

    private static final int QOS_AT_LEAST_ONCE = 1;
    private static final String ANY_VEHICLE = "+";
    private static final String COMMAND_CLIENT_SUFFIX = "-commands";

    @Bean
    MessageChannel telemetryCommandChannel() {
        return new DirectChannel();
    }

    @Bean
    MqttTelemetryAdapter mqttTelemetryAdapter(TelemetryMessageParser parser,
            ApplicationEventPublisher eventPublisher, MessageChannel telemetryCommandChannel,
            TelemetryProperties properties) {
        return new MqttTelemetryAdapter(parser, eventPublisher, telemetryCommandChannel,
                properties.commandTopicPrefix());
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

    @Bean
    IntegrationFlow telemetryCommandFlow(MessageChannel telemetryCommandChannel,
            MqttPahoClientFactory telemetryMqttClientFactory, TelemetryProperties properties) {
        MqttPahoMessageHandler outbound = new MqttPahoMessageHandler(
                properties.clientId() + COMMAND_CLIENT_SUFFIX, telemetryMqttClientFactory);
        outbound.setDefaultQos(QOS_AT_LEAST_ONCE);
        return IntegrationFlow.from(telemetryCommandChannel).handle(outbound).get();
    }
}
