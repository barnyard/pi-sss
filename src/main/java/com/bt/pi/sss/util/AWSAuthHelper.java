/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;

import com.bt.pi.sss.exception.CryptographicSystemException;
import com.sun.jersey.spi.container.ContainerRequest;

@Component
public class AWSAuthHelper {
    private static final String SLASH = "/";
    private static final String QUESTION_MARK = "?";
    private static final Log LOG = LogFactory.getLog(AWSAuthHelper.class);
    private static final String HMAC_SHA1_ALGORITHM = "HmacSHA1";
    private static final String NEW_LINE = "\n";
    private static final String UTF8 = "UTF-8";
    private static final List<String> EXTENSIONS = new ArrayList<String>();

    public AWSAuthHelper() {
        EXTENSIONS.add("acl");
        EXTENSIONS.add("torrent");
        EXTENSIONS.add("logging");
        EXTENSIONS.add("location");
    }

    public String getCanonicalizedResource(ContainerRequest request) {
        String path = request.getRequestUri().getRawPath();

        if (path.length() > 1 && path.indexOf(SLASH, 1) > 0) {
            String bucket = path.substring(0, path.indexOf(SLASH, 1));
            if (path.length() > bucket.length()) {
                String object = path.substring(path.indexOf(SLASH, 1) + 1);
                path = bucket + SLASH + object.replace(SLASH, "%2F");
            } else {
                path = bucket;
            }
        }

        String queryString = request.getRequestUri().getQuery();

        if (null != queryString && queryString.length() > 0) {
            int ampPos = queryString.indexOf("&");
            if (ampPos > -1)
                queryString = queryString.substring(0, ampPos);
            int equalsPos = queryString.indexOf("=");
            if (equalsPos > -1)
                queryString = queryString.substring(0, equalsPos);
        }

        if (EXTENSIONS.contains(queryString)) {
            path = path + QUESTION_MARK + queryString;
        }

        return path;
    }

    public String getCanonicalizedAmzHeaders(ContainerRequest request) {
        SortedMap<String, List<String>> sortedHeaders = new TreeMap<String, List<String>>();

        MultivaluedMap<String, String> requestHeaders = request.getRequestHeaders();
        for (Entry<String, List<String>> entry : requestHeaders.entrySet()) {
            String key = entry.getKey();
            String fixedKey = key.toLowerCase(Locale.getDefault()).trim();
            if (fixedKey.startsWith("x-amz")) {
                sortedHeaders.put(key, entry.getValue());
            }
        }

        LOG.debug(sortedHeaders);

        StringBuilder sb = new StringBuilder();

        for (Entry<String, List<String>> entry : sortedHeaders.entrySet()) {
            String key = entry.getKey();
            String fixedKey = key.toLowerCase(Locale.getDefault()).trim();
            sb.append(fixedKey);
            sb.append(":");

            StringBuilder val = new StringBuilder();
            for (String value : entry.getValue()) {
                val.append(value.trim().replace(NEW_LINE, " "));
                val.append(",");
            }
            String value = val.toString();
            value = value.substring(0, value.length() - 1);
            sb.append(value);
            sb.append(NEW_LINE);
        }
        return sb.toString();
    }

    public String getSignature(String secretKey, String stringToSign) throws SignatureException {
        // most of this method was borrowed from Amazon's docs at
        // http://docs.amazonwebservices.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/index.html?AuthJavaSampleHMACSignature.html

        String result;
        try {
            // get an hmac_sha1 key from the raw key bytes
            SecretKeySpec signingKey = new SecretKeySpec(secretKey.getBytes(), HMAC_SHA1_ALGORITHM);

            // get an hmac_sha1 Mac instance and initialize with the signing key
            Mac mac = Mac.getInstance(HMAC_SHA1_ALGORITHM);
            mac.init(signingKey);

            // compute the hmac on input data bytes
            byte[] rawHmac = mac.doFinal(stringToSign.getBytes(UTF8));

            // base64-encode the hmac
            result = new String(Base64.encodeBase64(rawHmac));

        } catch (UnsupportedEncodingException ex) {
            LOG.error("UTF8 is not supported...craziness...", ex);
            throw new CryptographicSystemException(ex);
        } catch (NoSuchAlgorithmException ex) {
            LOG.error("HMAC_SHA1 is not supported...craziness...", ex);
            throw new CryptographicSystemException(ex);
        } catch (InvalidKeyException ex) {
            throw new SignatureException("The key was invalid!", ex);
        }

        return result;
    }
}
