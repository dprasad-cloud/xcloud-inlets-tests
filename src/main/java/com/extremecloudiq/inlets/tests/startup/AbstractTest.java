package com.extremecloudiq.inlets.tests.startup;

import lombok.Data;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Data
@Configuration
public abstract class AbstractTest {

    private static final String INLETS_TESTS_PREFIX = "inlets-tests:";
    private boolean enabled;
    private int poolSize;
    private int maxDevices;
    private long waitTime;

    @Value("${inlets.tests.sleep.time:1000}")
    private long sleepTime;

    @Value("${inlets.tests.serial.realdevices:2021Q-30238}")
    private String realDevices;

    @Value("${inlets.tests.serial.prefix:IQEMU}")
    private String serialPrefix;

    @Value("${inlets.tests.stats.interval:600000}")
    private long statsInterval;

    @Autowired
    RedisTemplate<String, String> redisTemplate;

    public void setValuesAndStartTests(org.slf4j.Logger logger, boolean enabled, int poolSize, int maxDevices, long waitTime) {
        this.enabled = enabled;
        this.poolSize = poolSize;
        this.maxDevices = maxDevices;
        this.waitTime = waitTime;

        if (!enabled) {
            logger.info("Tests not enabled.");
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

                    Thread.sleep(sleepTime);

                }

            }
        }.start();

    }

    public void runOverDevices(int start) {
        int startIndex = (maxDevices / poolSize) * start;
        int lastIndex = (startIndex + maxDevices / poolSize);

        //Handle realDevices
        //Only execute in the first thread
        if (start == 0) {
            List<String> realDevicesList = Arrays.stream(realDevices.split(",")).
                filter(s -> s.length() > 0).
                collect(Collectors.toList());
            for (String serialNumber : realDevicesList)
                execute(serialNumber);

        }

        for (int i = startIndex; i < lastIndex; i++) {

            String serialNumber = String.format(serialPrefix, i);
            execute(serialNumber);

        }
    }

    class Runner extends Thread {
        int start;
        int poolSize;

        public Runner(int start, int max) {
            this.start = start;
            this.poolSize = max;
        }

        public void run() {
            runOverDevices(start);
        }

    }

    static long lastStatsUpdate = System.currentTimeMillis();
    static Map<String, Map<String, Long>> respMap = new HashMap<>();
    static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyMMddHHmmss");

    @SneakyThrows
    public void logResponse(Logger logger, String resp, long st, String serialNumber) {
        resp = resp != null ? resp.replaceAll(" ", "") : resp;
        resp = ( resp != null && resp.length()  > 30 ) ? resp.substring(resp.length() - 29, resp.length() - 1) : resp;

        long respTime = System.currentTimeMillis() - st;
        logger.debug("resp: " + resp);
        logger.info(serialNumber + "=" + resp + " | " + respTime);
        String className = getName();

        synchronized (respMap) {

            Map<String, Long> map = respMap.getOrDefault(className, new HashMap<>());
            map.put(resp, map.getOrDefault(resp, 0L) + 1);
            map.put("TOT", map.getOrDefault("TOT", 0L) + 1);
            map.put("TRT", map.getOrDefault("TRT", 0L) + respTime);
            map.put("ART", map.get("TRT") / map.get("TOT"));
            respMap.put(className, map);

            if ((System.currentTimeMillis() - lastStatsUpdate) >= statsInterval) {
                try {
                    String mapAsString = respMap.toString();
                    redisTemplate.opsForValue().set(INLETS_TESTS_PREFIX + simpleDateFormat.format(new Date()), mapAsString);
                    logger.info("Updated Redis ====>" + respMap);
                    lastStatsUpdate = System.currentTimeMillis();
                    respMap.clear();

                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("Error updating stats...", e);
                }
            }
        }
    }

    public abstract void execute(String serialNumber);

    /**
     * Every test has a three letter short code for easy formatting of the stats
     */
    public abstract String getName();

}
