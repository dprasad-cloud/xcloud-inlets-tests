package com.extremecloudiq.inlets.tests.startup;

import com.extremecloudiq.inlets.tests.vo.DeviceConnectBaseRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;

/**
 * @author dprasad
 */
@Slf4j
@Configuration
public class DeviceConnectHealthCheckTest extends AbstractTest {

    @Value("${inlets.tests.hc.enable:true}")
    private boolean enabled;

    @Value("${inlets.tests.hc.url:http://device-connector:9921/device-connect/rest/v1/health-check/}")
    private String healthCheckBaseUrl;

    @Value("${inlets.tests.hc.pool.size:25}")
    private int poolSize;

    @Value("${inlets.tests.hc.max.devices:50000}")
    private int maxDevices;

    @Value("${inlets.tests.hc.wait.time:180000}")
    private long waitTime;

    @Value("${inlets.tests.hc.host-header:device-connector}")
    private String hostHeader;

    RestTemplate restTemplate = new RestTemplate();

    public void execute(String serialNumber) {
        DeviceConnectBaseRequest request = new DeviceConnectBaseRequest();
        request.setDeviceFamily("VSP_SERIES");
        request.setDeviceModel("VOSS5520");
        request.setInletsVersion("0.1.0.0");
        request.setPlatform("VOSS5520");

        HttpHeaders headers = new HttpHeaders();
        headers.set("Host", hostHeader);
        HttpEntity requestEntity = new HttpEntity(request, headers);

        long st = 0L;
        String resp;
        try {
            st = System.currentTimeMillis();
            ResponseEntity<String> response = restTemplate.postForEntity(healthCheckBaseUrl + serialNumber, requestEntity, String.class);
            resp = response.getStatusCode() + "";

        } catch (Exception e) {
            resp = e.getMessage();
        }

        logResponse(log, resp, st, serialNumber);

    }

    @Override
    public String getName() {
        return "DHC";
    }

    @PostConstruct
    public void init() {
        setValuesAndStartTests(log, enabled, poolSize, maxDevices, waitTime);
    }
}