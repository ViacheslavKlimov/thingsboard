package org.thingsboard.server.service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.export.EntityExportData;
import org.thingsboard.server.common.data.export.ExportEntityType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.controller.EntitiesExportImportController;
import org.thingsboard.server.utils.GitUtil;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EntitiesVersionControlService {
    @Value("${r:https://github.com/ViacheslavKlimov/thingsboard-vcs.git}")
    private String repositoryUri;
    @Value("${u:ViacheslavKlimov}")
    private String username;
    @Value("${p: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}")
    private String password;

    @Value("${db:main}")
    private String defaultBranch;
    @Value("${d:/tmp/thingsboard-vcs}")
    private String workingDirectory;

    private GitUtil gitUtil;
    private final EntitiesExportImportController exportImportController;

    @PostConstruct
    private void init() throws GitAPIException, IOException {
        this.gitUtil = new GitUtil(repositoryUri, defaultBranch, workingDirectory, username, password);
    }

    @SneakyThrows
    public void saveNewVersion(TenantId tenantId, String branch, String version, Map<ExportEntityType, List<String>> entities) {
        EntitiesExportImportController.ExportRequest exportRequest = new EntitiesExportImportController.ExportRequest();
        exportRequest.setEntities(entities);

        List<? extends EntityExportData<?>> entitiesExportDataList = exportImportController.exportEntities(exportRequest).values()
                .stream().flatMap(Collection::stream).collect(Collectors.toList());

        gitUtil.checkout(StringUtils.defaultIfEmpty(branch, defaultBranch));
        gitUtil.pull();

        entitiesExportDataList.forEach(exportData -> createFileForExportData(tenantId, exportData));

        version = StringUtils.isBlank(version) ? new Date().toString() : version;
        gitUtil.commit(version);
        gitUtil.push();
    }

    @SneakyThrows
    public void loadVersion(TenantId tenantId, String branch, String version, ExportEntityType entityType, String entityId) {
        EntityExportData<?> exportData = JacksonUtil.fromString(IOUtils.toString(gitUtil.getFileAtRevision(version, getPath(tenantId, entityType, entityId)), StandardCharsets.UTF_8), EntityExportData.class);
        if (exportData == null) {
            throw new IllegalArgumentException("Entity version does not exist");
        }

        EntitiesExportImportController.ImportRequest importRequest = new EntitiesExportImportController.ImportRequest();
        importRequest.setExportDataList(List.of(exportData));
        exportImportController.importEntities(importRequest);
    }

    @SneakyThrows
    public List<String> listVersions(TenantId tenantId, String branch, ExportEntityType entityType, String entityId) {
        // fixme: branch not found via repo.resolve or repo.findRef, even after fetch; checkout does not find the branch as well, only after create
        gitUtil.fetch(StringUtils.defaultIfEmpty(branch, defaultBranch);
        return gitUtil.listCommits(branch, getPath(tenantId, entityType, entityId));
    }

    @SneakyThrows
    private EntityExportData<?> retrieveExportData(TenantId tenantId, ExportEntityType entityType, String entityId) {
        File exportDataFile = new File(getPath(tenantId, entityType, entityId));
        if (!exportDataFile.exists()) {
            return null;
        } else {
            return JacksonUtil.fromString(FileUtils.readFileToString(exportDataFile, StandardCharsets.UTF_8), EntityExportData.class);
        }
    }

    private String getPath(TenantId tenantId, ExportEntityType entityType, String entityId) {
        String path = tenantId.toString() + "/" + workingDirectory + "/" + entityType.name().toLowerCase();
        if (StringUtils.isNotEmpty(entityId)) {
            path += "/" + entityId;
        }
        return path;
    }

    @SneakyThrows
    private void createFileForExportData(TenantId tenantId, EntityExportData<?> exportData) {
        String entityDirectory = tenantId.toString() + "/" + workingDirectory + "/" + exportData.getType().name().toLowerCase();
        new File(entityDirectory).mkdirs();

        String entityFile = entityDirectory + "/" + exportData.getMainEntity().getId().toString();
        FileUtils.write(new File(entityFile), JacksonUtil.toString(exportData), StandardCharsets.UTF_8);
    }

}
