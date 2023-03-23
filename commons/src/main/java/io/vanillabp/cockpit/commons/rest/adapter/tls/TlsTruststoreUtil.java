package io.vanillabp.cockpit.commons.rest.adapter.tls;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

public class TlsTruststoreUtil {

    public static X509TrustManager noCertificateCheckTrustManager() {

        return new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[] {};
            }
        };

    }

    @SuppressWarnings("resource")
    public static TrustManager[] clientCertificateCheckTrustManagers(
            final String filename,
            final String keystorePassword) {

    	try {
	        final var keyStore = KeyStore.getInstance("PKCS12");
	        keyStore.load(new FileInputStream(filename), keystorePassword.toCharArray());
	        final var trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	        trustFactory.init(keyStore);
	        return trustFactory.getTrustManagers();
    	} catch(Exception e) {
    		throw new RuntimeException(e);
    	}
    }

    @SuppressWarnings("resource")
    public static KeyManager[] clientCertificateCheckKeyManagers(
            final String filename,
            final String keystorePassword) {

    	try {
	        final var keyStore = KeyStore.getInstance("PKCS12");
	        keyStore.load(new FileInputStream(filename), keystorePassword.toCharArray());
	        final var keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
	        keyFactory.init(keyStore, keystorePassword.toCharArray());
	        return keyFactory.getKeyManagers();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}

    }

    public static HostnameVerifier noHostnameCheckVerifier() {

        return new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

    }

}
