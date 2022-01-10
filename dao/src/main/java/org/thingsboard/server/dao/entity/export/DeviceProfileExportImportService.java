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
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.export.DeviceProfileExportData;
import org.thingsboard.server.common.data.export.ExportEntityType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.DeviceProfileService;

@Service
@RequiredArgsConstructor
public class DeviceProfileExportImportService extends AbstractEntityExportImportService<DeviceProfileId, DeviceProfile, DeviceProfileExportData> {
    private final DeviceProfileService deviceProfileService;

    @Override
    protected DeviceProfileExportData toExportData(TenantId tenantId, DeviceProfile deviceProfile) {
        DeviceProfileExportData exportData = new DeviceProfileExportData();
        exportData.setDeviceProfile(deviceProfile);
        return exportData;
    }

    @Override
    protected DeviceProfile prepareAndSave(TenantId tenantId, DeviceProfile deviceProfile, DeviceProfileExportData exportData) {
        if (deviceProfile.getId() == null) {
            deviceProfile.setDefaultRuleChainId(getInternalId(tenantId, deviceProfile.getDefaultRuleChainId()));
            deviceProfile.setDefaultDashboardId(getInternalId(tenantId, deviceProfile.getDefaultDashboardId()));
            deviceProfile.setFirmwareId(getInternalId(tenantId, deviceProfile.getFirmwareId()));
            deviceProfile.setSoftwareId(getInternalId(tenantId, deviceProfile.getSoftwareId()));
        }

        // fixme: push entity action

        return deviceProfileService.saveDeviceProfile(deviceProfile);
    }

    @Override
    public ExportEntityType getEntityType() {
        return ExportEntityType.DEVICE_PROFILE;
    }

}
