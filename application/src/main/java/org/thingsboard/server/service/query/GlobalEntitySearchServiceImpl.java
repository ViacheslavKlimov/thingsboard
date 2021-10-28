/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2021 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.service.query;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ContactBased;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityDataSortOrder;
import org.thingsboard.server.common.data.query.EntityIdOrSearchTextFilter;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.data.search.EntityResponse;
import org.thingsboard.server.data.search.GlobalSearchRequest;
import org.thingsboard.server.data.search.GlobalSearchResponse;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.state.DefaultDeviceStateService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.thingsboard.server.common.data.EntityType.ASSET;
import static org.thingsboard.server.common.data.EntityType.BLOB_ENTITY;
import static org.thingsboard.server.common.data.EntityType.CONVERTER;
import static org.thingsboard.server.common.data.EntityType.CUSTOMER;
import static org.thingsboard.server.common.data.EntityType.DASHBOARD;
import static org.thingsboard.server.common.data.EntityType.DEVICE;
import static org.thingsboard.server.common.data.EntityType.DEVICE_PROFILE;
import static org.thingsboard.server.common.data.EntityType.EDGE;
import static org.thingsboard.server.common.data.EntityType.ENTITY_GROUP;
import static org.thingsboard.server.common.data.EntityType.ENTITY_VIEW;
import static org.thingsboard.server.common.data.EntityType.INTEGRATION;
import static org.thingsboard.server.common.data.EntityType.OTA_PACKAGE;
import static org.thingsboard.server.common.data.EntityType.RULE_CHAIN;
import static org.thingsboard.server.common.data.EntityType.TB_RESOURCE;
import static org.thingsboard.server.common.data.EntityType.TENANT;
import static org.thingsboard.server.common.data.EntityType.TENANT_PROFILE;
import static org.thingsboard.server.common.data.EntityType.USER;
import static org.thingsboard.server.common.data.EntityType.WIDGETS_BUNDLE;
import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.CREATED_TIME;
import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.CUSTOMER_ID;
import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.LAST_ACTIVITY_TIME;
import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.NAME;
import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.OWNER_ID;
import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.TENANT_ID;
import static org.thingsboard.server.dao.sql.query.EntityKeyMapping.TYPE;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalEntitySearchServiceImpl implements GlobalEntitySearchService {
    private final EntityQueryService entityQueryService;

    private final TenantService tenantService;
    private final CustomerService customerService;
    private final DefaultDeviceStateService deviceStateService;

    private static final List<EntityKey> entityResponseFields = Stream.of(CREATED_TIME, NAME, TYPE, TENANT_ID, CUSTOMER_ID, OWNER_ID)
            .map(field -> new EntityKey(EntityKeyType.ENTITY_FIELD, field))
            .collect(Collectors.toList());

    public static final Set<EntityType> searchableEntityTypes = EnumSet.of(
            TENANT, CUSTOMER, USER, DASHBOARD, ASSET, DEVICE, ENTITY_GROUP, CONVERTER, INTEGRATION,
            RULE_CHAIN, BLOB_ENTITY, ENTITY_VIEW, WIDGETS_BUNDLE, TENANT_PROFILE, DEVICE_PROFILE,
            TB_RESOURCE, OTA_PACKAGE, EDGE
    );

    @Override
    public GlobalSearchResponse searchEntitiesGlobally(GlobalSearchRequest request, PageLink pageLink, SecurityUser user) {
        PageData<EntityResponse> result = searchEntities(request.getEntityType(), request.getSearchQuery(), pageLink, user);

        GlobalSearchResponse response = new GlobalSearchResponse();
        response.setResult(result);
        return response;
    }

    private PageData<EntityResponse> searchEntities(EntityType entityType, String searchQuery, PageLink pageLink, SecurityUser user) {
        if (!searchableEntityTypes.contains(entityType)) {
            return PageData.emptyPageData();
        }

        EntityDataQuery query = createSearchQuery(searchQuery, entityType, pageLink);

        PageData<EntityData> resultPage = entityQueryService.findEntityDataByQuery(user, query);

        Map<EntityId, ContactBased<? extends EntityId>> localOwnersCache = new HashMap<>();
        return resultPage.mapData(entityData -> {
            Map<String, String> fields = new HashMap<>();
            entityData.getLatest().values().stream()
                    .flatMap(values -> values.entrySet().stream())
                    .forEach(entry -> fields.put(entry.getKey(), Strings.emptyToNull(entry.getValue().getValue())));

            EntityResponse entityResponse = new EntityResponse();

            entityResponse.setEntityId(entityData.getEntityId());
            entityResponse.setFields(fields);
            setOwnerInfo(entityResponse, localOwnersCache);

            return entityResponse;
        });
    }

    /*
     * the method does not support valid sorting and retrieves all entities from the database regardless of the page request
     * */
    private PageData<EntityResponse> searchAllEntities(String searchQuery, PageLink pageLink, SecurityUser user) {
        if (StringUtils.isEmpty(searchQuery)) {
            return PageData.emptyPageData();
        }

        List<EntityResponse> entities = searchableEntityTypes.stream()
                .flatMap(entityType -> searchEntities(entityType, searchQuery, new PageLink(1_000_000, 0,
                        pageLink.getTextSearch(), pageLink.getSortOrder()), user).getData().stream())
                .collect(Collectors.toList());

        return DaoUtil.cropDataToPage(entities, pageLink);
    }

    private EntityDataQuery createSearchQuery(String searchQuery, EntityType entityType, PageLink pageLink) {
        EntityDataPageLink entityDataPageLink = new EntityDataPageLink();
        entityDataPageLink.setPageSize(pageLink.getPageSize());
        entityDataPageLink.setPage(pageLink.getPage());
        if (pageLink.getSortOrder() != null && StringUtils.isNotEmpty(pageLink.getSortOrder().getProperty())) {
            entityDataPageLink.setSortOrder(new EntityDataSortOrder(new EntityKey(EntityKeyType.ENTITY_FIELD, pageLink.getSortOrder().getProperty()),
                    EntityDataSortOrder.Direction.valueOf(Optional.ofNullable(pageLink.getSortOrder().getDirection()).orElse(SortOrder.Direction.ASC).name())));
        }

        EntityIdOrSearchTextFilter filter = new EntityIdOrSearchTextFilter();
        filter.setEntityType(entityType);
        filter.setIdOrSearchText(searchQuery);

        List<EntityKey> entityFields = GlobalEntitySearchServiceImpl.entityResponseFields;
        List<EntityKey> latestValues = Collections.emptyList();

        if (entityType == USER) {
            entityFields = new ArrayList<>(entityFields);
            entityFields.add(new EntityKey(EntityKeyType.ENTITY_FIELD, LAST_ACTIVITY_TIME));
        } else if (entityType == DEVICE) {
            EntityKey lastActivityTimeKey;
            if (deviceStateService.isPersistToTelemetry()) {
                lastActivityTimeKey = new EntityKey(EntityKeyType.TIME_SERIES, LAST_ACTIVITY_TIME);
            } else {
                lastActivityTimeKey = new EntityKey(EntityKeyType.SERVER_ATTRIBUTE, LAST_ACTIVITY_TIME);
            }
            latestValues = List.of(lastActivityTimeKey);
            if (entityDataPageLink.getSortOrder() != null && entityDataPageLink.getSortOrder().getKey().getKey().equals(LAST_ACTIVITY_TIME)) {
                entityDataPageLink.getSortOrder().setKey(lastActivityTimeKey);
            }
        }

        return new EntityDataQuery(filter, entityDataPageLink, entityFields, latestValues, Collections.emptyList());
    }

    private void setOwnerInfo(EntityResponse entityResponse, Map<EntityId, ContactBased<? extends EntityId>> localOwnersCache) {
        Map<String, String> fields = entityResponse.getFields();

        UUID tenantUuid = toUuid(fields.remove(TENANT_ID));
        UUID customerUuid = toUuid(fields.remove(CUSTOMER_ID));
        UUID ownerUuid = toUuid(fields.remove(OWNER_ID));

        ownerUuid = ownerUuid == null ? (customerUuid != null ? customerUuid : tenantUuid) : ownerUuid;

        Tenant tenant = null;
        if (tenantUuid != null) {
            tenant = getTenant(new TenantId(tenantUuid), localOwnersCache);
        }

        ContactBased<? extends EntityId> owner = null;
        if (ownerUuid != null) {
            Customer customerOwner = getCustomer(new CustomerId(ownerUuid), localOwnersCache);
            Tenant tenantOwner;
            if (customerOwner == null) {
                tenantOwner = getTenant(new TenantId(ownerUuid), localOwnersCache);
                owner = tenantOwner;
                if (tenant == null) {
                    tenant = tenantOwner;
                }
            } else {
                owner = customerOwner;
                if (tenant == null) {
                    tenant = getTenant(customerOwner.getTenantId(), localOwnersCache);
                }
            }
        }

        if (tenant != null) {
            entityResponse.setTenantInfo(new EntityResponse.TenantInfo(tenant.getId(), tenant.getName()));
        }

        if (owner != null) {
            entityResponse.setOwnerInfo(new EntityResponse.OwnerInfo(owner.getId(), owner.getName()));
        }
    }

    private Tenant getTenant(TenantId tenantId, Map<EntityId, ContactBased<? extends EntityId>> localOwnersCache) {
        return (Tenant) localOwnersCache.computeIfAbsent(tenantId, id -> tenantService.findTenantById(tenantId));
    }

    private Customer getCustomer(CustomerId customerId, Map<EntityId, ContactBased<? extends EntityId>> localOwnersCache) {
        return (Customer) localOwnersCache.computeIfAbsent(customerId, id -> customerService.findCustomerById(TenantId.SYS_TENANT_ID, customerId));
    }

    private UUID toUuid(String uuid) {
        try {
            UUID id = UUID.fromString(uuid);
            if (!id.equals(EntityId.NULL_UUID)) {
                return id;
            }
        } catch (Exception ignored) {}

        return null;
    }

}
