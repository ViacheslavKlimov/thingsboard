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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.export.EntityRelationExportData;
import org.thingsboard.server.common.data.export.ExportEntityType;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.dao.relation.RelationService;

// fixme: what relations to export ? not all, but maybe of entities that are being exported along
@Service
@RequiredArgsConstructor
public class EntityRelationExportImportService extends AbstractEntityExportImportService<UUIDBased, EntityRelation, EntityRelationExportData>  {
    private final RelationService relationService;

    @Override
    protected EntityRelationExportData toExportData(TenantId tenantId, EntityRelation entityRelation) {
        EntityRelationExportData exportData = new EntityRelationExportData();
        exportData.setEntityRelation(entityRelation);
        return exportData;
    }

    @Override
    protected EntityRelation prepareAndSave(TenantId tenantId, EntityRelation entityRelation, EntityRelationExportData exportData) {
        entityRelation.setId(null);
        entityRelation.setFrom(getInternalId(tenantId, entityRelation.getFrom()));
        entityRelation.setTo(getInternalId(tenantId, entityRelation.getTo()));

        relationService.saveRelation(tenantId, entityRelation);

        return entityRelation;
    }

    @Override
    public ExportEntityType getEntityType() {
        return ExportEntityType.RELATION;
    }

}
