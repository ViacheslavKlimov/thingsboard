package org.thingsboard.server.common.data.export;

import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

public interface ExportableEntity<I extends EntityId, E> {

    I getExternalId();

    void setExternalId(I externalId);

    I getId();

    void setId(I id);

    void setTenantId(TenantId tenantId);

    E updateEntityData(E entity);

}
