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
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.export.DeviceExportData;
import org.thingsboard.server.common.data.export.ExportEntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceService;

@Service
@RequiredArgsConstructor
public class DeviceExportImportService extends AbstractEntityExportImportService<DeviceId, Device, DeviceExportData> {
    private final DeviceService deviceService;
    private final DeviceCredentialsService deviceCredentialsService;

    @Override
    protected DeviceExportData toExportData(TenantId tenantId, Device device) {
        DeviceExportData exportData = new DeviceExportData();
        exportData.setDevice(device);
        exportData.setCredentials(deviceCredentialsService.findDeviceCredentialsByDeviceId(device.getTenantId(), device.getId()));
        return exportData;
    }

    @Override
    protected Device prepareAndSave(TenantId tenantId, Device device, DeviceExportData exportData) {
        if (device.getId() == null) {
            device.setCustomerId(getInternalId(tenantId, device.getCustomerId()));
            device.setDeviceProfileId(getInternalId(tenantId, device.getDeviceProfileId()));
            device.setFirmwareId(getInternalId(tenantId, device.getFirmwareId()));
            device.setSoftwareId(getInternalId(tenantId, device.getSoftwareId()));
        }

        // fixme: push entity action

        return deviceService.saveDeviceWithCredentials(device, exportData.getCredentials());
    }

    @Override
    public ExportEntityType getEntityType() {
        return ExportEntityType.DEVICE;
    }

}
