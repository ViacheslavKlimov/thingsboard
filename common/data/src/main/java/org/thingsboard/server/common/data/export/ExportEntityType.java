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
package org.thingsboard.server.common.data.export;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;

import java.util.Arrays;

@Getter
public enum ExportEntityType { // fixme: maybe get rid of this and use EntityType ?
    DEVICE_PROFILE(EntityType.DEVICE_PROFILE),
    DEVICE(EntityType.DEVICE),
    RULE_CHAIN(EntityType.RULE_CHAIN),
    ASSET,
    RELATION;

    private EntityType entityType;

    ExportEntityType(EntityType entityType) {
        this.entityType = entityType;
    }

    ExportEntityType() {
    }

    public static ExportEntityType forEntityType(EntityType entityType) {
        return Arrays.stream(values())
                .filter(exportEntityType -> exportEntityType.getEntityType() == entityType)
                .findFirst().orElse(null);
    }

}
