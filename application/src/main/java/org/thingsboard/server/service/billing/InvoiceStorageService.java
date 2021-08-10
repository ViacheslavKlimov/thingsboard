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
package org.thingsboard.server.service.billing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.blob.BlobEntity;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.tools.SchedulerUtils;
import org.thingsboard.server.dao.blob.BlobEntityService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.utils.SftpClient;
import org.thingsboard.server.utils.XmlUtils;

import javax.annotation.PostConstruct;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
@Slf4j
@TbCoreComponent
public class InvoiceStorageService {
    private final InvoiceGenerationService invoiceGenerationService;
    private final TenantService tenantService;
    private final BlobEntityService blobEntityService;
    private final PartitionService partitionService;

    @Value("${billing.sftp.host}")
    private String host;
    @Value("${billing.sftp.username}")
    private String username;
    @Value("${billing.sftp.password}")
    private String password;
    @Value("${billing.sftp.connect_timeout}")
    private int connectTimeout;

    @Value("${billing.sftp.xml_invoices_directory}")
    private String xmlInvoicesDirectory;
    @Value("${billing.sftp.xml_invoices_archive_directory}")
    private String xmlInvoicesArchiveDirectory;
    @Value("${billing.sftp.pdf_invoices_directory}")
    private String pdfInvoicesDirectory;
    @Value("${billing.sftp.pdf_invoices_archive_directory}")
    private String pdfInvoicesArchiveDirectory;

    private SftpClient sftpClient;
    private final Lock sftpLock = new ReentrantLock();

    private static final String INVOICE_FILENAME_FORMAT = "THB_%s-%s.xml";

    private static final LocalTime INVOICE_GENERATION_TIME = LocalTime.of(23, 30, 0);

    @PostConstruct
    private void init() {
        this.sftpClient = new SftpClient(host, username, password, connectTimeout, connectTimeout);
    }

    @PostConstruct
    private void generateAndUploadXmlInvoicesBySchedule() {
        SchedulerUtils.scheduleForEachDayAtSpecificTime(() -> {
            if (LocalDate.now().getDayOfMonth() == LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth() &&
                    partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition()) {
                generateAndUploadXmlInvoices();
            }
        }, INVOICE_GENERATION_TIME, Executors.newSingleThreadScheduledExecutor(), true);
    }

    public void generateAndUploadXmlInvoices() {
        try {
            sftpLock.lock();
            sftpClient.establishConnection();

            Set<String> existingInvoices = sftpClient.listFiles(xmlInvoicesDirectory);
            existingInvoices.addAll(sftpClient.listFiles(xmlInvoicesArchiveDirectory));

            PageLink pageLink = new PageLink(1000);
            boolean hasNext = true;
            while (hasNext) {
                PageData<Tenant> tenants = tenantService.findTenants(pageLink);
                tenants.getData().stream()
                        .filter(tenant -> !existingInvoices.contains(getInvoiceFileName(tenant.getId())))
                        .forEach(tenant -> {
                            try {
                                Invoice invoice = invoiceGenerationService.generateInvoiceForTenant(tenant);
                                saveTenantInvoice(invoice, tenant);
                                log.info("Uploaded xml invoice for tenant {} ({})", tenant.getId(), tenant.getName());
                            } catch (Exception e) {
                                log.error("Failed to upload xml invoice for tenant {} ({})", tenant.getId(), tenant.getName(), e);
                            }
                        });

                hasNext = tenants.hasNext();
                if (hasNext) {
                    pageLink = pageLink.nextPageLink();
                }
            }
        } catch (Exception e) {
            log.error("Failed to generate and upload XML invoices", e);
        } finally {
            sftpClient.destroyConnection();
            sftpLock.unlock();
        }
    }

    private void saveTenantInvoice(Invoice invoice, Tenant tenant) throws Exception {
        sftpClient.createFile(
                XmlUtils.objectToXmlInputStream(invoice),
                xmlInvoicesDirectory + "/" + getInvoiceFileName(tenant.getId())
        );
    }

    private static String getInvoiceFileName(TenantId tenantId) {
        return String.format(INVOICE_FILENAME_FORMAT, LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE), InvoiceGenerationService.getInvoiceNumber(tenantId).substring(0, 6));
    }

    @Scheduled(initialDelay = 60 * 60 * 1000, fixedDelay = 12 * 60 * 60 * 1000)
    public void loadPdfInvoices() throws Exception {
        if (!partitionService.resolve(ServiceType.TB_CORE, TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID).isMyPartition()) {
            return;
        }

        try {
            sftpLock.lock();
            sftpClient.establishConnection();

            sftpClient.cd(pdfInvoicesDirectory);
            sftpClient.listFiles(".").forEach(invoiceFilename -> {
                try {
                    InputStream pdfInvoice = sftpClient.getFile(invoiceFilename);

                    BlobEntity blobEntity = new BlobEntity();
                    blobEntity.setName(invoiceFilename);
                    blobEntity.setData(ByteBuffer.wrap(IOUtils.toByteArray(pdfInvoice)));
                    blobEntity.setContentType("pdf");
                    blobEntity.setType("invoice");

                    String magentaCustomerId = getMagentaCustomerIdFromPdfInvoiceName(invoiceFilename);
                    Tenant tenant = tenantService.findTenantByMagentaCustomerId(magentaCustomerId);
                    if (tenant == null) {
                        throw new IllegalArgumentException("Tenant not found for pdf invoice " + invoiceFilename);
                    }
                    blobEntity.setTenantId(tenant.getId());

                    blobEntityService.saveBlobEntity(blobEntity);

                    sftpClient.moveFile(invoiceFilename, pdfInvoicesArchiveDirectory + "/" + invoiceFilename);

                    log.info("Loaded pdf invoice for tenant {} ({})", tenant.getId(), tenant.getName());
                } catch (Exception e) {
                    log.error("Failed to load pdf invoice {}", invoiceFilename, e);
                }
            });

        } catch (Exception e) {
            log.error("Failed to load PDF invoices", e);
        } finally {
            sftpClient.destroyConnection();
            sftpLock.unlock();
        }
    }

    private String getMagentaCustomerIdFromPdfInvoiceName(String invoiceFilename) {
        return StringUtils.substringBeforeLast(StringUtils.substringAfter(invoiceFilename, "THB_"), "_");
    }

}
