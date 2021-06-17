package org.thingsboard.server.service.apiusage;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RestApiCallsCounter extends GenericFilterBean { // FIXME
    private final TbApiUsageReportClient apiUsageReportClient;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        apiUsageReportClient.report(TenantId.SYS_TENANT_ID, null, ApiUsageRecordKey.REST_API_CALLS_COUNT);
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
