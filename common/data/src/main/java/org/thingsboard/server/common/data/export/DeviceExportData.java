package org.thingsboard.server.common.data.export;

import lombok.Data;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.security.DeviceCredentials;

@Data
public class DeviceExportData extends EntityExportData<Device> {
    private DeviceCredentials credentials;

    @Override
    public ExportEntityType getType() {
        return ExportEntityType.DEVICE;
    }
}
