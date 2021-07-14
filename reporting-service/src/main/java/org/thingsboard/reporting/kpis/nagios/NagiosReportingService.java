package org.thingsboard.reporting.kpis.nagios;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.reporting.kpis.KpiStats;
import org.thingsboard.reporting.util.SnmpGatewayComponent;
import org.thingsboard.reporting.util.snmp.SnmpAgent;
import org.thingsboard.server.common.data.stats.KpiKey;
import org.thingsboard.server.queue.util.AfterStartUp;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
@SnmpGatewayComponent
public class NagiosReportingService {
    @Value("${snmp.binding_port}")
    private int snmpPort;
    @Value("${snmp.community}")
    private String snmpCommunity;
    @Value("${nagios.kpi_statistics.oid}")
    private String baseOid;

    @Value("${monitoring_service_url}")
    private String monitoringServiceUrl;

    private final List<KpiKey> kpiKeysToReport = Arrays.stream(KpiKey.values())
            .filter(kpiKey -> kpiKey.getId() != null)
            .collect(Collectors.toList());

    private final RestTemplate restTemplate = new RestTemplate();

    @AfterStartUp
    public void run() throws Exception {
        SnmpAgent snmpAgent = new SnmpAgent(snmpPort, snmpCommunity);
        snmpAgent.start();

        List<Integer> ids = Arrays.stream(KpiKey.values()).map(KpiKey::getId).filter(Objects::nonNull).collect(Collectors.toList());
        snmpAgent.registerVariables(ids, () -> {
            KpiStats kpiStats;
            try {
                kpiStats = requestKpiStats();
                if (kpiStats == null || kpiStats.getEntries() == null) {
                    throw new IllegalStateException("empty response");
                }
            } catch (Exception e) {
                log.error("Failed to get KPI stats", e);
                kpiStats = new KpiStats();
            }
            return toValues(kpiStats);
        }, baseOid);
    }

    private KpiStats requestKpiStats() {
        return restTemplate.getForObject(monitoringServiceUrl + "/kpi_stats", KpiStats.class);
    }

    private Map<Integer, Object> toValues(KpiStats kpiStats) {
        return kpiKeysToReport.stream()
                .collect(Collectors.toMap(
                        KpiKey::getId, kpiKey -> kpiStats.getOrDefault(kpiKey, kpiKey.getDefaultValue())
                ));
    }

}
