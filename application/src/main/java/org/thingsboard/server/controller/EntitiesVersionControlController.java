package org.thingsboard.server.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.export.ExportEntityType;
import org.thingsboard.server.service.EntitiesVersionControlService;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/entities/vc")
@PreAuthorize("hasAuthority('TENANT_ADMIN')")
public class EntitiesVersionControlController extends BaseController {
    private final EntitiesVersionControlService entitiesVersionControlService;

    @PostMapping("/save_new_version") // fixme: rename
    public void saveNewVersion(@RequestBody Map<ExportEntityType, List<String>> entities,
                               @RequestParam(required = false) String version,
                               @RequestParam(required = false) String branch) throws ThingsboardException {
        try {
            entitiesVersionControlService.saveNewVersion(getTenantId(), branch, version, entities);
        } catch (Exception e) {
            throw handleException(e);
        }
    }

    @PostMapping("/load_version")
    public void loadVersion(@RequestBody Map<ExportEntityType, List<String>> entities,
                            @RequestParam String version,
                            @RequestParam(required = false) String branch) {
//        try {
//            entitiesVersionControlService.loadVersion(branch, version, entities);
//        }
    }

    @GetMapping("/list_versions")
    public List<String> listVersions(@RequestParam(required = false) String branch,
                                     @RequestParam ExportEntityType entityType,
                                     @RequestParam(required = false) String entityId) throws ThingsboardException {
        return entitiesVersionControlService.listVersions(getTenantId(), branch, entityType, entityId);
    }

}
