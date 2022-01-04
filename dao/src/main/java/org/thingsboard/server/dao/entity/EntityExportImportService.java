package org.thingsboard.server.dao.entity;

import org.thingsboard.server.common.data.export.EntityExportData;
import org.thingsboard.server.common.data.export.ExportEntityType;
import org.thingsboard.server.common.data.export.ExportableEntity;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface EntityExportImportService<I extends EntityId, E extends ExportableEntity<I, E>, D extends EntityExportData<E>> {

    E findById(TenantId tenantId, I id);

    E findByExternalId(TenantId tenantId, I externalId);

    PageData<E> findByTenantId(TenantId tenantId, PageLink pageLink);


    default PageData<D> exportEntities(TenantId tenantId, PageLink pageLink) {
        return findByTenantId(tenantId, pageLink).mapData(this::toExportData);
    }

    default D exportEntity(TenantId tenantId, I id) {
        return toExportData(findById(tenantId, id));
    }

    D toExportData(E entity);

    default void importEntity(TenantId tenantId, D exportData) {
        E entity = exportData.getEntity();
        E existingEntity = findByExternalId(tenantId, entity.getId());

        if (existingEntity == null) {
            entity.setTenantId(tenantId);
            entity.setExternalId(entity.getId());
            entity.setId(null);
        } else {
            entity = existingEntity.updateEntityData(entity);
        }

        saveEntityWithLinkedEntities(tenantId, entity, exportData);
    }

    void saveEntityWithLinkedEntities(TenantId tenantId, E entity, D exportData);

    Map<EntityId, EntityId> internalIdsCache = new HashMap<>();

    @SuppressWarnings("unchecked")
    default <ID extends EntityId> ID getInternalId(TenantId tenantId, ID externalId) { // fixme !!!!
        if (externalId == null || externalId.isNullUid()) {
            return null;
        } else {
            ID internalId = (ID) internalIdsCache.get(externalId);
            if (internalId == null) {
//                internalId = Optional.ofNullable(findByExternalId(tenantId, externalId)).map(ExportableEntity::getId).orElse(null);
            }
            return Optional.ofNullable(internalId)
                    .orElseThrow(() -> new IllegalStateException("Firstly import " + externalId.getEntityType()));
        }
    }

    ExportEntityType getExportEntityType();

}
