package no.nav.fo.config;


import no.nav.sbl.dialogarena.types.Pingable;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;

import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.naming.NamingException;

import static no.nav.sbl.dialogarena.types.Pingable.Ping.feilet;
import static no.nav.sbl.dialogarena.types.Pingable.Ping.lyktes;

@EnableJms
@Configuration
public class MQMockConfig {


    @Bean(name = "jmsListenerContainerFactory")
    public DefaultJmsListenerContainerFactory jmsListenerContainerFactory() throws NamingException {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory());
        factory.setDestinationResolver((session, s, b) -> {
            try {
                return endreVeilederKo();
            } catch (NamingException e) {
                e.printStackTrace();
            }
            return null;
        });
        factory.setConcurrency("3-10");
        return factory;
    }

    @Bean
    public ConnectionFactory connectionFactory() throws NamingException {
        return new ActiveMQConnectionFactory("tcp://localhost:61616");
    }

    @Bean(name = "endreveilederqueue")
    public JmsTemplate endreVeilederueue() throws NamingException {
        JmsTemplate jmsTemplate = new JmsTemplate();
        jmsTemplate.setDefaultDestination(endreVeilederKo());
        jmsTemplate.setDefaultDestinationName("endreveilederqueue");
        jmsTemplate.setConnectionFactory(connectionFactory());
        return jmsTemplate;
    }

    @Bean
    public Destination endreVeilederKo() throws NamingException {
        return new ActiveMQQueue("endreVeilederKo");
    }

    @Bean
    public Pingable endreVeilederPing() {

        return () -> {
            try {
                connectionFactory().createConnection().close();
                return lyktes("ENDRING_AV_VEILEDER");
            } catch (Exception e) {
                return feilet("ENDRING_AV_VEILEDER", e);
            }
        };
    }
}