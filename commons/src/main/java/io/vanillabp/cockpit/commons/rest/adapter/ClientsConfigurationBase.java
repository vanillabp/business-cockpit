package io.vanillabp.cockpit.commons.rest.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Feign.Builder;
import feign.Request;
import feign.Retryer;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import io.vanillabp.cockpit.commons.rest.adapter.oauth.OauthBearerTokenHandler;
import io.vanillabp.cockpit.commons.rest.adapter.tls.TlsTruststoreUtil;
import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InjectionPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public abstract class ClientsConfigurationBase {

    @Autowired
    protected Optional<ObjectMapper> objectMapper;
    
    protected void configureOkHttpClient(
            final Class<?> clientClass,
            final Client properties,
            final OkHttpClient.Builder httpClientBuilder,
            final Consumer<OkHttpClient.Builder> adoptHttpClientBuilder) {

        configureTls(properties, httpClientBuilder);

        if (adoptHttpClientBuilder != null) {
            adoptHttpClientBuilder.accept(httpClientBuilder);
        }
        configureProxyAndLogger(httpClientBuilder, clientClass, properties);

    }

    protected void configureFeignBuilder(
            final Class<?> clientClass,
            final Builder builder,
            final Client properties) {
        
        configureFeignBuilder(clientClass, builder, properties, null);
        
    }
    
    protected void configureRetry(
            final Builder builder,
            final Client properties) {
        
        final var retryProperties = properties.getRetry();
        if ((retryProperties == null)
                || !retryProperties.isEnabled()) {
            
            builder.retryer(Retryer.NEVER_RETRY);
            return;
            
        }
        
        builder.retryer(new Retryer.Default(
                retryProperties.getPeriod().toMillis(),
                retryProperties.getMaxPeriod().toMillis(),
                retryProperties.getMaxAttempts()));
        
    }

    protected void configureFeignBuilder(
            final Class<?> clientClass,
            final Builder builder,
            final Client properties,
            final Consumer<OkHttpClient.Builder> adoptHttpClientBuilder) {

        builder.options(new Request.Options(
                properties.getConnectTimeout(), TimeUnit.MILLISECONDS,
                properties.getReadTimeout(), TimeUnit.MILLISECONDS, true));

        OkHttpClient.Builder httpClientBuilder = new OkHttpClient.Builder();
        configureOkHttpClient(clientClass, properties, httpClientBuilder, adoptHttpClientBuilder);
        
        builder.client(new feign.okhttp.OkHttpClient(httpClientBuilder.build()));

        configureRetry(builder, properties);
        
        configureJsonEncoding(builder);

        if (configureBasicAuthentication(builder, properties)) {
            return;
        }
        if (configureOauthAuthentication(builder, properties)) {
            return;
        }

    }
    
    protected void configureJsonEncoding(
            final Builder builder) {
        
        if (objectMapper.isPresent()) {
            builder.decoder(
                    new JacksonDecoder(objectMapper.get()));
            builder.encoder(
                    new FormEncoder(new JacksonEncoder(objectMapper.get())));
        }

    }
    
    protected void configureTls(
            final Client properties,
            OkHttpClient.Builder httpClientBuilder) {
        
        if (StringUtils.hasText(properties.getSslTruststoreFilename())) {
            
            if (!properties.isVerifySsl()) {
                httpClientBuilder.hostnameVerifier(TlsTruststoreUtil.noHostnameCheckVerifier());
            }
            
            final var trustManagers = TlsTruststoreUtil
                    .clientCertificateCheckTrustManagers(
                            properties.getSslTruststoreFilename(),
                            properties.getSslTruststorePassword());
            final var x509TrustManager = Arrays.stream(trustManagers)
                    .filter(manager -> manager instanceof X509TrustManager)
                    .map(manager -> (X509TrustManager) manager)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("truststore file '"
                            + properties.getSslTruststoreFilename()
                            + "' does not contain at least one X509 certificate required for HTTPS clients"));
            final var keyManagers = TlsTruststoreUtil
                    .clientCertificateCheckKeyManagers(
                            properties.getSslTruststoreFilename(),
                            properties.getSslTruststorePassword());
            httpClientBuilder.sslSocketFactory(getSSLSocketFactory(trustManagers, keyManagers), x509TrustManager);
                
            
        } else if (!properties.isVerifySsl()) {

            httpClientBuilder.hostnameVerifier(TlsTruststoreUtil.noHostnameCheckVerifier());
            final var trustManager = TlsTruststoreUtil.noCertificateCheckTrustManager();
            httpClientBuilder
                    .sslSocketFactory(getSSLSocketFactory(new TrustManager[] { trustManager }, null), trustManager);

        }
        
    }

    protected OkHttpClient.Builder configureProxyAndLogger(
            final OkHttpClient.Builder builder,
            final Class<?> clientClass,
            final Client properties) {

        final var clientLogger = LoggerFactory.getLogger(clientClass);

        if (properties.useProxy()) {
            final var proxy = properties.getProxy();

            builder
                    .proxy(new java.net.Proxy(java.net.Proxy.Type.HTTP,
                            new InetSocketAddress(proxy.getHost(), proxy.getPort())));

            if (proxy.getUsername() != null) {
                builder.proxyAuthenticator(new Authenticator() {
                    @Override
                    public okhttp3.Request authenticate(Route route, Response response) throws IOException {
                        final var credential = Credentials.basic(proxy.getUsername(), proxy.getPassword());
                        return response.request().newBuilder().header("Proxy-Authorization", credential).build();
                    }
                });
            }
        }

        if (properties.isLog()) {
            final HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor((msg) -> {
                clientLogger.info(msg);
            });
            interceptor.setLevel(okhttp3.logging.HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(interceptor);
        }

        return builder;

    }

    protected boolean configureOauthAuthentication(
            final Builder builder,
            final Client properties) {

        if (properties.getAuthentication() == null) {
            return false;
        }
        if (properties.getAuthentication().getOauth() == null) {
            return false;
        }
        if (!properties.getAuthentication().getOauth().isInitialized()) {
            return false;
        }

        final var tokenHandler = new OauthBearerTokenHandler(properties.getAuthentication());
        builder.requestInterceptor(tokenHandler);
        builder.errorDecoder(tokenHandler);

        return true;

    }

    protected boolean configureBasicAuthentication(
            final Builder builder,
            final Client properties) {

        if (properties.getAuthentication() == null) {
            return false;
        }
        if (!properties.getAuthentication().isBasic()) {
            return false;
        }

        final var credential = Credentials
                .basic(properties.getAuthentication().getUsername(), properties.getAuthentication().getPassword());
        builder.requestInterceptor((template) -> {
            template.header("Authorization", credential);
        });

        return true;

    }

    private SSLSocketFactory getSSLSocketFactory(
            final TrustManager[] trustManagers,
            final KeyManager[] keyManagers) {

        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(keyManagers, trustManagers, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Filter the given list of properties beans to find the one implementing the
     * given interfaces within the same workflow module as the class of the given
     * injection point.
     * 
     * @param <T>                 The type of the desired properties bean
     * @param injectionPoint      The injection point to identify the workflow
     *                            module
     * @param propertiesInterface The interface the properties bean has to implement
     * @param properties          All properties found at runtime
     * @return The desired properties bean
     */
    protected <T> T getProperties(
            final InjectionPoint injectionPoint,
            final Class<T> propertiesInterface,
            final List<T> properties) {
        
        final var classPackageName = injectionPoint
                .getMember()
                .getDeclaringClass()
                .getPackageName();
        
        // find properties having the closest package-name
        return properties
                .stream()
                .map(props -> {
                    final var propsPackageName = props.getClass().getPackageName();
                    final var minLength = Math.min(classPackageName.length(), propsPackageName.length());
                    for (int i = 0; i < minLength; i++) {
                        if (classPackageName.charAt(i) != propsPackageName.charAt(i)) {
                            return Map.entry(props, i);
                        }
                    }
                    return Map.entry(props, minLength);
                })
                .sorted((p1, p2) -> 0 - p1.getValue().compareTo(p2.getValue()))
                .findFirst()
                .map(entry -> entry.getKey())
                .orElseThrow(() -> new NoSuchElementException(
                        "There is no @ConfigurationProperties implementing '"
                        + propertiesInterface.getName()
                        + "' in the workflow-module of class '"
                        + injectionPoint.getMember().getDeclaringClass().getName()
                        + "'!"));
        
    }
    
}
