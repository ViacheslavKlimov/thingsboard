package org.thingsboard.server.common.data.export;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DeviceExportData.class, name = "DEVICE")
})
@Data
public abstract class EntityExportData<E extends ExportableEntity<? extends EntityId, E>> {
    private E entity;

    @JsonIgnore
    public abstract ExportEntityType getType();

}
