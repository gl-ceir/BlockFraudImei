package com.gl.ceir.config.configuration;

import com.gl.ceir.config.service.impl.SystemParamServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

@Service
public class ApiHttpConnection {

    //@Value("${ceir.alert.url}")
    private String url;

    static final Logger logger = LogManager.getLogger(ApiHttpConnection.class);

    public void httpConnectionForApp(String alertId, String alertMessage, String alertProcess) {
        try {
            HttpHeaders headers = null;
            MultiValueMap<String, String> map = null;
            HttpEntity<MultiValueMap<String, String>> request = null;
            ResponseEntity<String> httpResponse = null;
            String respons = null;
            URI uri = new URI(url);
            final RestTemplate restTemplate = new RestTemplateBuilder()
                    .setConnectTimeout(Duration.ofMillis(10000))
                    .setReadTimeout(Duration.ofMillis(10000))
                    .build();
            headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            logger.info("Http Connection Header Created ");

            map = new LinkedMultiValueMap<>();
            map.add("alertId", alertId);
            map.add("alertMessage", alertMessage);
            map.add("alertProcess", alertProcess);
            logger.info("Http Connection Body Created ");
            request = new HttpEntity<>(map, headers);
            httpResponse = restTemplate.postForEntity(uri, request, String.class);
            respons = httpResponse.getBody();
            logger.info("Request:" + url + " Body:" + map.toString() + "alertId:" + alertId + " Response :" + respons);
        } catch (Exception e) {
            logger.error("Not able to http Api  " + e + " :: " + e.getCause());
        }
    }


}
