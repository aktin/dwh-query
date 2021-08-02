package org.aktin.request.manager;

import org.aktin.Preferences;
import org.aktin.broker.client.BrokerClient;
import org.aktin.broker.client.auth.HttpApiKeyAuth;
import org.aktin.dwh.BrokerResourceManager;
import org.aktin.dwh.PreferenceKey;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interface implementation to allow injecting classes use the resource handling functionality of broker-client
 */
@Singleton
public class BrokerResourceManagerImpl implements BrokerResourceManager {

    private static final Logger LOGGER = Logger.getLogger(BrokerResourceManagerImpl.class.getName());

    private BrokerClient client;

    @Inject
    private Preferences preferences;

    /**
     * Grab broker-uri and broker-api from aktin.properties and use them
     * to create a new broker-client
     */
    @PostConstruct
    public void initBrokerClient() {
        String broker = preferences.get(PreferenceKey.brokerEndpointURI);
        if (broker == null || broker.trim().length() == 0) {
            client = null;
            LOGGER.log(Level.WARNING, "Could not retrieve broker uri");
            return;
        }
        client = new BrokerClient(URI.create(broker));
        String apiKey = preferences.get(PreferenceKey.brokerEndpointKeys);
        client.setClientAuthenticator(HttpApiKeyAuth.newBearer(apiKey));
    }

    @Override
    public void putMyResource(String name, String contentType, final InputStream content) {
        try {
            client.putMyResource(name, contentType, content);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Broker communication failed", e);
        }
    }

    @Override
    public void putMyResource(String name, String contentType, final String content) {
        try {
            client.putMyResource(name, contentType, content);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Broker communication failed", e);
        }
    }

    @Override
    public void putMyResourceProperties(final String name, final Properties properties) {
        try {
            client.putMyResourceProperties(name, properties);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Broker communication failed", e);
        }
    }

    @Override
    public void putMyResourceXml(String name, final Object jaxbObject) {
        try {
            client.putMyResourceXml(name, jaxbObject);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Broker communication failed", e);
        }
    }

    @Override
    public Properties getMyResourceProperties(String name) {
        Properties properties = new Properties();
        try {
            properties = client.getMyResourceProperties(name);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Broker communication failed", e);
        }
        return properties;
    }

    @Override
    public void deleteMyResource(String name) {
        try {
            client.deleteMyResource(name);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Broker communication failed", e);
        }
    }
}
