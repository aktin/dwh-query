package org.aktin.request.manager;

import org.aktin.Preferences;
import org.aktin.broker.client.BrokerClient;
import org.aktin.broker.client.auth.HttpApiKeyAuth;
import org.aktin.dwh.BrokerResourceManager;
import org.aktin.dwh.PreferenceKey;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.net.URI;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.TimerService;

@Singleton
public class BrokerResourceManagerImpl implements BrokerResourceManager {

    private static final Logger LOGGER = Logger.getLogger(BrokerResourceManagerImpl.class.getName());

    private BrokerClient client;

    private String linuxDistribution;

    @Inject
    private Preferences preferences;

    /**
     * Initialize connection to broker and set running linux environment
     */
    @PostConstruct
    public void initBrokerResourceManager() {
        initBrokerClient();
        setLinuxDistribution();
    }

    /**
     * Grab broker-uri and broker-api from aktin.properties and use them
     * to create a new broker-client
     */
    private void initBrokerClient() {
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

    /**
     * Run "cat /etc/issue" in bash shell and set output as linuxDistribution
     */
    private void setLinuxDistribution() {
        String command = String.join(" ", "cat", "/etc/issue");
        this.linuxDistribution = runBashCommand(command);
    }

    /**
     * Run a given bash command in a java Process. Grab the output via InputStream and return it as converted string
     * @param command bash command to run
     * @return output of bash command
     */
    private String runBashCommand(String command) {
        String result = "";
        ProcessBuilder processBuilder = new ProcessBuilder("/usr/bin/bash", "-c", command);
        Process process = null;
        try {
            process = processBuilder.start();
            try (InputStream output = process.getInputStream(); InputStream error = process.getErrorStream()) {
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    output.close();
                    error.close();
                    process.destroy();
                    process.waitFor();
                    LOGGER.log(Level.WARNING, String.format("Timeout while running command %s", command));
                }
                if (process.exitValue() == 0) {
                    result = convertStreamToString(output);
                } else
                    throw new IOException(convertStreamToString(error));
            }
        } catch (IOException | InterruptedException e) {
            if (process != null && process.isAlive())
                process.destroy();
            LOGGER.log(Level.WARNING, String.format("Unable to run command %s", command), e);
        }
        return result;
    }

    private String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null)
            builder.append(line).append("\n");
        return builder.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putResourceGroup(String name, Map<String, String> resources) {
        Properties properties = new Properties();
        properties.putAll(resources);
        try {
            client.putMyResourceProperties(name, properties);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Broker communication failed", e);
        }
    }

    /**
     * Returns the installed version of a given linux package. Case differentiation is done by
     * linux distribution.
     * TODO what if the packages are named differently in package managers?
     * @param package_linux name of linxu packege
     * @return corresponding package version or "[not installed]" if package was not found or "[error]" if exception was thrown
     */
    @Override
    public String getLinuxPackageVersion(String package_linux) {
        String version = "[error]";
        try {
            Objects.requireNonNull(this.linuxDistribution);
            if (this.linuxDistribution.contains("Ubuntu") || this.linuxDistribution.contains("Debian"))
                version = getAptPackageVersion(package_linux);
            else
                throw new IllegalArgumentException("Unknow linux distribution");
        } catch (NullPointerException | IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, String.format("Error while retrieving version of linux package %s", package_linux), e);
        }
        return version;
    }

    /**
     * Returns the  installed version of a linux package from apt (Advanced Package Tool) package manager.
     * Runs "apt list {package}" in a bash shell and extracts the version number from output.
     *
     * @param package_apt apt package to get installed version from
     * @return corresponding package version or "[not installed]" if package was not found or "[error]" if exception was thrown
     */
    private String getAptPackageVersion(String package_apt) {
        String command = String.join(" ", "apt", "list", package_apt);
        String output_command = runBashCommand(command);
        String version = "[error]";
        if (output_command != null) {
            version = extractAptVersionFromString(output_command, package_apt);
            if (version.isEmpty())
                version = "[not installed]";
        }
        return version;
    }

    private String extractAptVersionFromString(String string, String package_apt) {
        Pattern pattern = Pattern.compile(String.format("%s.*\\s(\\d\\S*)\\s.*\\[installed\\]", package_apt));
        Matcher matcher = pattern.matcher(string);
        String result = "";
        while (matcher.find())
            result = matcher.group(1);
        return result;
    }


    @Override
    public String getDatabaseVersion() {
        return getLinuxPackageVersion("postgresql-12");
    }

    @Override
    public String getApacheVersion() {
        return getLinuxPackageVersion("apache2");
    }

    @Override
    public String getDwhVersion() {
        String version = "";
        try {
            version = (String) (new InitialContext().lookup("java:app/AppName"));
        } catch (NamingException e) {
            LOGGER.log(Level.WARNING, "Unable to get ear version via java:app/AppName");
        }
        if (version.isEmpty())
            version = "[undefined]";
        return version;
    }

    @Override
    public String getDwhApiVersion() {
        return Objects.toString(PreferenceKey.class.getPackage().getImplementationVersion());
    }

    @Override
    public String getJavaVersion() {
        return String.join("/", System.getProperty("java.vendor"), System.getProperty("java.version"));
    }

    @Override
    public String getApplicationServerVersion() {
        return Objects.toString(TimerService.class.getPackage().getImplementationVersion());
    }
}
