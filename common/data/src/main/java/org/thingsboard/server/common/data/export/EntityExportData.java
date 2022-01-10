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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UUIDBased;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = DeviceExportData.class, name = "DEVICE"),
        @JsonSubTypes.Type(value = DeviceProfileExportData.class, name = "DEVICE_PROFILE"),
        @JsonSubTypes.Type(value = AssetExportData.class, name = "ASSET"),
        @JsonSubTypes.Type(value = RuleChainExportData.class, name = "RULE_CHAIN")
})
public interface EntityExportData<E extends ExportableEntity<? extends UUIDBased, E>> {

    @JsonIgnore
    E getMainEntity();

    @JsonIgnore
    ExportEntityType getType();

}
