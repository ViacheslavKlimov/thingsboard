package org.thingsboard.server.common.data.export;

import lombok.Data;
import org.thingsboard.server.common.data.DeviceProfile;

@Data
public class DeviceProfileExportData extends EntityExportData<DeviceProfile> {
    @Override
    public ExportEntityType getType() {
        return ExportEntityType.DEVICE_PROFILE;
    }
}
