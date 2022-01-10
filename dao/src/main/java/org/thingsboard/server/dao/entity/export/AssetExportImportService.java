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
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.export.AssetExportData;
import org.thingsboard.server.common.data.export.ExportEntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.asset.AssetService;

@Service
@RequiredArgsConstructor
public class AssetExportImportService extends AbstractEntityExportImportService<AssetId, Asset, AssetExportData> {
    private final AssetService assetService;

    @Override
    protected AssetExportData toExportData(TenantId tenantId, Asset asset) {
        AssetExportData exportData = new AssetExportData();
        exportData.setAsset(asset);
        return exportData;
    }

    @Override
    protected Asset prepareAndSave(TenantId tenantId, Asset asset, AssetExportData exportData) {
        if (asset.getId() == null) {
            asset.setCustomerId(getInternalId(tenantId, asset.getCustomerId())); // fixme: should we update the customer of existing entity?..
        }

        return assetService.saveAsset(asset);
    }

    @Override
    public ExportEntityType getEntityType() {
        return ExportEntityType.ASSET;
    }

}
