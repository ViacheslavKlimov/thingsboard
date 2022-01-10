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
package org.thingsboard.server.dao.entity.export;

import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.export.EntityExportData;
import org.thingsboard.server.common.data.export.ExportEntityType;
import org.thingsboard.server.common.data.export.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.device.DeviceProfileDao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractEntityExportImportService<I extends UUIDBased, E extends ExportableEntity<I, E>, D extends EntityExportData<E>> {
    private Map<ExportEntityType, ExportableEntityDao<?>> daos;
    private ExportableEntityDao<E> dao;

    @Autowired
    private void setDaos(DeviceDao deviceDao, DeviceProfileDao deviceProfileDao) {
        this.daos = Map.of(
                ExportEntityType.DEVICE, deviceDao,
                ExportEntityType.DEVICE_PROFILE, deviceProfileDao
        );
        this.dao = (ExportableEntityDao<E>) this.daos.get(getEntityType());
    }

    public D exportEntity(TenantId tenantId, I entityId) {
        E entity = dao.findById(tenantId, entityId.getId());
        return toExportData(tenantId, entity);
    }

    public List<D> exportEntities(TenantId tenantId, PageLink pageLink) {
            return dao.findByTenantId(tenantId, pageLink).getData().stream()
                .map(e -> toExportData(tenantId, e))
                .collect(Collectors.toList());
    }

    protected abstract D toExportData(TenantId tenantId, E entity);


    public void importEntity(TenantId tenantId, D exportData) {
        E entity = exportData.getMainEntity();
        E existingEntity = (E) daos.get(exportData.getType()).findByExternalId(tenantId, entity.getId().getId());

        if (existingEntity == null) {
            entity.setTenantId(tenantId);
            entity.setExternalId(entity.getId());
            entity.setId(null);
        } else {
            entity = existingEntity.updateEntityData(entity);
        }

        E savedEntity = prepareAndSave(tenantId, entity, exportData);

        internalIdsCache.put(exportData.getMainEntity().getId(), savedEntity.getId());
    }

    protected abstract E prepareAndSave(TenantId tenantId, E entity, D exportData);


    public abstract ExportEntityType getEntityType();


    private static final Map<UUIDBased, UUIDBased> internalIdsCache = new HashMap<>();

    @SuppressWarnings("unchecked")
    protected <I extends EntityId> I getInternalId(TenantId tenantId, I externalId) {
        if (externalId == null || externalId.isNullUid()) {
            return null;
        } else {
            I internalId = (I) internalIdsCache.get(externalId);
            if (internalId == null) {
                internalId = (I) Optional.ofNullable(daos.get(ExportEntityType.forEntityType(externalId.getEntityType()))
                        .findByExternalId(tenantId, externalId.getId()))
                        .map(ExportableEntity::getId).orElse(null);
            }
            return Optional.ofNullable(internalId)
                    .orElseThrow(() -> new IllegalStateException("Firstly import " + externalId.getEntityType()));
        }
    }

}
