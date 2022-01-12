/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.controller;

import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.export.EntityExportData;
import org.thingsboard.server.common.data.export.ExportEntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.export.AbstractEntityExportImportService;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/entities")
public class EntitiesExportImportController extends BaseController {
    private final Map<ExportEntityType, AbstractEntityExportImportService<?, ?, ?>> entityExportImportServices = new EnumMap<>(ExportEntityType.class);

    public EntitiesExportImportController(List<AbstractEntityExportImportService<?, ?, ?>> entityExportImportServices) {
        entityExportImportServices.forEach(entityExportImportService -> {
            this.entityExportImportServices.put(entityExportImportService.getEntityType(), entityExportImportService);
        });
    }


    @PostMapping("/export")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public Map<ExportEntityType, List<? extends EntityExportData<?>>> exportEntities(@RequestBody ExportRequest request) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        Map<ExportEntityType, List<? extends EntityExportData<?>>> result = new HashMap<>();

        request.getEntities().forEach((entityType, entitiesIds) -> {
            AbstractEntityExportImportService entityExportImportService = getEntityExportImportService(entityType);
            List<EntityExportData<?>> exportDataList = new LinkedList<>();

            if (CollectionUtils.isNotEmpty(entitiesIds)) {
                entitiesIds.forEach(entityId -> {
                    exportDataList.add(entityExportImportService.exportEntity(tenantId, (UUIDBased) EntityIdFactory.getByTypeAndId(entityType.getEntityType().name(), entityId)));
                });
            } else {
                exportDataList.addAll(entityExportImportService.exportEntities(tenantId, new PageLink(Integer.MAX_VALUE)));
            }

            result.put(entityType, exportDataList);
        });

        return result;
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    public void importEntities(@RequestBody ImportRequest request) throws ThingsboardException {
        TenantId tenantId = getTenantId();
        request.getExportDataList().forEach(entityExportData -> {
            ExportEntityType entityType = entityExportData.getType();
            getEntityExportImportService(entityType).importEntity(tenantId, entityExportData);
        });
    }

    private AbstractEntityExportImportService getEntityExportImportService(ExportEntityType type) {
        return Optional.ofNullable(entityExportImportServices.get(type))
                .orElseThrow(() -> new IllegalArgumentException("Entity type " + type + " is not exportable"));
    }


    @Data
    public static class ExportRequest {
        private Map<ExportEntityType, List<String>> entities;
    }

    @Data
    public static class ImportRequest {
        private List<EntityExportData<?>> exportDataList;
    }

}
