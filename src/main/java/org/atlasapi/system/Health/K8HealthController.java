package org.atlasapi.system.Health;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.metabroadcast.common.health.Health;
import com.metabroadcast.common.health.Result;
import com.metabroadcast.common.health.Status;
import com.metabroadcast.common.health.probes.ProbeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;


@Controller
public class K8HealthController {

    private static final Logger log = LoggerFactory.getLogger(K8HealthController.class);
    private static final String JSON_TYPE = "application/json";

    private final ObjectMapper mapper;

    private Map<String, Health> healthMap;

    private K8HealthController(ObjectMapper mapper) {
        healthMap = Maps.newConcurrentMap();
        this.mapper = mapper;
    }

    public static K8HealthController create(ObjectMapper mapper) {
        return new K8HealthController(mapper);
    }

    public void registerHealth(String name, Health health) {
        healthMap.put(name, health);
    }

    @RequestMapping("/system/healthcheck/alive")
    public void isAlive(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_OK);
    }

    @RequestMapping("/system/healthcheck/list")
    public void listProbes(HttpServletResponse response) throws IOException {
        response.setStatus(SC_OK);
        response.setContentType(JSON_TYPE);
        OutputStream out = response.getOutputStream();
        mapper.writeValue(out, healthMap.keySet());
        out.close();
    }

    @RequestMapping("/system/healthcheck/probes")
    public void showHealthForProbes(HttpServletResponse response) throws IOException {

        ServletOutputStream out = response.getOutputStream();
        response.setContentType(JSON_TYPE);

        healthMap.entrySet().forEach(entry -> {
                    Result result = entry.getValue().status(Health.FailurePolicy.ANY);
                    response.setStatus(result.getStatus() == Status.HEALTHY ?
                                       SC_OK :
                                       SC_INTERNAL_SERVER_ERROR);
                    writeResult(out, result.getProbeResults());
                });

        out.close();
    }

    @RequestMapping("/system/healthcheck/probes/{slug}")
    public void showHealthForProbe(
            HttpServletResponse response,
            @PathVariable("slug") String slug
    ) throws IOException {

        Optional<Health> health = Optional.ofNullable(healthMap.get(slug));
        response.setContentType(JSON_TYPE);

        if (health.isPresent()) {
            Result result = health.get().status(Health.FailurePolicy.ANY);

            response.setStatus(result.getStatus() == Status.HEALTHY ? SC_OK : SC_INTERNAL_SERVER_ERROR);

            ServletOutputStream out = response.getOutputStream();
            writeResult(out, result.getProbeResults());

            out.close();
        } else {
            response.setStatus(SC_NOT_FOUND);
        }

    }

    @RequestMapping("/system/info/threads")
    public void showThreads(HttpServletResponse response) throws IOException {
        response.setContentType(JSON_TYPE);
        response.setStatus(200);
        ServletOutputStream out = response.getOutputStream();
        Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();

        try {
            mapper.writeValue(out, traces.entrySet());
        } catch (IOException e) {
            log.error("Could not write trace for threads");
        }

    }

    private void writeResult(OutputStream out, List<ProbeResult> probeResults) {
        try {
            mapper.writeValue(out, probeResults);
        } catch (IOException e) {
            log.error("Could not write probe results", e);
        }
    }
}
