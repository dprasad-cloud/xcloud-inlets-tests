package com.extremecloudiq.inlets.tests.startup;

import com.extremecloudiq.inlets.tests.vo.DeviceConnectBaseRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author dprasad
 */
@Slf4j
@Configuration
public class DeviceConnectHealthCheckTest {

    @Value("${inlets.tests.serial.prefix:IQEMU}")
    private String serialPrefix;

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


    RestTemplate restTemplate = new RestTemplate();

    class Runner extends Thread {
        int start = 0;
        int poolSize = 0;

        public Runner(int start, int max) {
            this.start = start;
            this.poolSize = max;
        }

        public void run() {

            int startIndex = (maxDevices / poolSize) * start;
            int lastIndex = (startIndex + maxDevices / poolSize);
            for (int i = startIndex; i < lastIndex; i++) {
                DeviceConnectBaseRequest request = new DeviceConnectBaseRequest();
                request.setDeviceFamily("VSP_SERIES");
                request.setDeviceModel("VOSS5520");
                request.setInletsVersion("0.1.0.0");
                request.setPlatform("VOSS5520");

                String serialNumber = String.format(serialPrefix, i);
                long st = 0L;
                String resp = null;
                try {
                    st = System.currentTimeMillis();
                    restTemplate.postForEntity(healthCheckBaseUrl + serialNumber, request, Object.class).toString();

                } catch (Exception e) {
                    resp = e.getMessage();
                }

                resp = resp!=null?resp.replaceAll(" ", ""):resp;
                long et = System.currentTimeMillis();
                log.debug("resp: " + resp);
                log.info(serialNumber +"="+ resp + " | " + (et - st));

            }
        }
    }

    @PostConstruct
    public void init() {

        if (!enabled) {
            log.info("Health Check tests not enabled.");
            return;
        }

        new Thread() {
            @SneakyThrows
            @Override
            public void run() {
                while (true) {
                    long t1 = System.currentTimeMillis();
                    ExecutorService service = Executors.newFixedThreadPool(poolSize);
                    for (int j = 0; j < poolSize; j = j + 1) {
                        service.submit(new Runner(j, poolSize));
                    }

                    System.out.println("Tasks submitted: time taken = " + (System.currentTimeMillis() - t1));
                    service.shutdown();
                    service.awaitTermination(waitTime, TimeUnit.MILLISECONDS);
                    System.out.println("completed current iteration, time taken = " + (System.currentTimeMillis() - t1));
                }

            }
        }.start();

    }
}