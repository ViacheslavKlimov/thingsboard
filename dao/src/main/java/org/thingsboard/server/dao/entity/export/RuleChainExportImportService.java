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
import org.thingsboard.server.common.data.export.ExportEntityType;
import org.thingsboard.server.common.data.export.RuleChainExportData;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.dao.rule.RuleChainService;

@Service
@RequiredArgsConstructor
public class RuleChainExportImportService extends AbstractEntityExportImportService<RuleChainId, RuleChain, RuleChainExportData> {
    private final RuleChainService ruleChainService;

    @Override
    protected RuleChainExportData toExportData(TenantId tenantId, RuleChain ruleChain) {
        RuleChainExportData exportData = new RuleChainExportData();
        exportData.setRuleChain(ruleChain);
        exportData.setMetaData(ruleChainService.loadRuleChainMetaData(tenantId, ruleChain.getId()));
        return exportData;
    }

    @Override
    protected RuleChain prepareAndSave(TenantId tenantId, RuleChain ruleChain, RuleChainExportData exportData) {
        RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);
        RuleChainMetaData metaData = exportData.getMetaData();

//        ruleChainService.deleteRuleChainById();
        // need to delete all old rule nodes and set new ?
        if (ruleChain.getId() == null) {
//            metaData.set
        }

        // todo: find rule node "To other Rule Chain", and replace rule chain id with internal

        return null;
    }

    @Override
    public ExportEntityType getEntityType() {
        return ExportEntityType.RULE_CHAIN;
    }

}
