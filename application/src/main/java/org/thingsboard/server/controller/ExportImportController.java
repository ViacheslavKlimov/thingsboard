package org.thingsboard.server.controller;

import lombok.Data;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.export.DeviceExportData;
import org.thingsboard.server.common.data.export.EntityExportData;
import org.thingsboard.server.common.data.export.ExportEntityType;
import org.thingsboard.server.common.data.export.ExportableEntity;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.device.DeviceServiceImpl;
import org.thingsboard.server.dao.entity.EntityExportImportService;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class ExportImportController extends BaseController {

    private final Map<ExportEntityType, EntityExportImportService<?, ?, ? extends EntityExportData<? extends ExportableEntity<? extends UUIDBased>>>> entityExportImportServices = new EnumMap<>(ExportEntityType.class);

    public ExportImportController(List<EntityExportImportService<?, ?, ? extends EntityExportData<? extends ExportableEntity<? extends UUIDBased>>>> entityExportImportServices) {
        entityExportImportServices.forEach(entityExportImportService -> {
            this.entityExportImportServices.put(entityExportImportService.getExportEntityType(), entityExportImportService);
        });
    }


    @PostMapping("/export")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public Map<ExportEntityType, List<? extends EntityExportData<?>>> exportEntities(@RequestBody ExportRequest request) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        Map<ExportEntityType, List<? extends EntityExportData<?>>> result = new HashMap<>();

        request.getEntityTypes().forEach(exportEntityType -> {
            List<? extends EntityExportData<?>> exportData = getEntityExportImportService(exportEntityType)
                    .exportEntities(tenantId, new PageLink(Integer.MAX_VALUE)).getData();
            result.put(exportEntityType, exportData);
        });

        return result;
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public void importEntities(@RequestBody ImportRequest request) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        request.getExportDataList().forEach(entityExportData -> {
            ExportEntityType type = entityExportData.getType();
            switch (type) { // fixme: refactor !!!
                case DEVICE:
                    ((DeviceServiceImpl) getEntityExportImportService(type)).importEntity(tenantId, (DeviceExportData) entityExportData);
                    break;
            }
        });
    }


    private EntityExportImportService<?, ?, ? extends EntityExportData<? extends ExportableEntity<? extends UUIDBased>>> getEntityExportImportService(ExportEntityType type) {
        return Optional.ofNullable(entityExportImportServices.get(type))
                .orElseThrow(() -> new IllegalArgumentException("Entity type " + type + " is not supported for exporting"));
    }

    @Data
    public static class ExportRequest {
        private List<ExportEntityType> entityTypes;
        // entity id
    }

    @Data
    public static class ImportRequest {
        private List<? extends EntityExportData<? extends ExportableEntity<? extends UUIDBased>>> exportDataList;
    }

}
