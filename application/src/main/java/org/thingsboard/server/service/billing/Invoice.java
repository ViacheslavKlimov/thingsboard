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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

@XmlRootElement(name = "M2MRechnung")
@XmlAccessorType(XmlAccessType.FIELD)
public class Invoice {
    @XmlElement(name = "Kopfdaten")
    private InvoiceHeader header;

    @XmlElementWrapper(name = "Detaildaten")
    @XmlElement(name = "Zeile")
    private List<InvoiceRecord> records = new LinkedList<>();

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class InvoiceHeader {
        @XmlElement(name = "PLATTFORM_ID")
        private String platformId;

        @XmlElement(name = "PARTNER_ID")
        private String partnerId;

        @XmlElement(name = "RECHNUNGSIDENT")
        private String invoiceNumber;

        @XmlElement(name = "ERSTELLUNGSDATUM")
        @XmlJavaTypeAdapter(DateFormatter.class)
        private Date creationDate;

        @XmlElement(name = "ABRECHNUNGSZEITRAUM_VON")
        @XmlJavaTypeAdapter(DateFormatter.class)
        private Date billingPeriodStart;

        @XmlElement(name = "ABRECHNUNGSZEITRAUM_BIS")
        @XmlJavaTypeAdapter(DateFormatter.class)
        private Date billingPeriodEnd;

        @XmlElement(name = "DEBITORENNR")
        private String debtor;

        @XmlElement(name = "WAEHRUNG")
        private String currency;

        @XmlElement(name = "REFERENCE")
        private String customerReference;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class InvoiceRecord {
        @XmlElement(name = "SUBDOC_ID")
        private Integer id; // incremented

        @XmlElement(name = "SUBDOC_NAME")
        private String type;

        @XmlElement(name = "MATERIALNR")
        private Integer materialNumber;

        @XmlElement(name = "TEXT")
        private String description;

        @XmlElement(name = "LE_EINZEL")
        @XmlJavaTypeAdapter(NumberFormatter.class)
        private Integer chargedUnit;

        @XmlElement(name = "PREIS_JE_LE")
        @XmlJavaTypeAdapter(NumberFormatter.class)
        private Double pricePerUnit;

        @XmlElement(name = "LE_GESAMT")
        @XmlJavaTypeAdapter(NumberFormatter.class)
        private Double unitsAmount;

        @XmlElement(name = "AMOUNT_GESAMT")
        @XmlJavaTypeAdapter(NumberFormatter.class)
        private Double totalPrice;
    }


    private static class DateFormatter extends XmlAdapter<String, Date> {
        private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        @Override
        public Date unmarshal(String s) throws Exception {
            return format.parse(s);
        }

        @Override
        public String marshal(Date date) throws Exception {
            return format.format(date);
        }
    }

    private static class NumberFormatter extends XmlAdapter<String, Number> {
        private static final DecimalFormat format;

        static {
            DecimalFormatSymbols formatSymbols = DecimalFormatSymbols.getInstance();
            formatSymbols.setDecimalSeparator(',');
            format = new DecimalFormat("#0.00", formatSymbols);
        }

        @Override
        public Double unmarshal(String s) throws Exception {
            return format.parse(s).doubleValue();
        }

        @Override
        public String marshal(Number number) throws Exception {
            return format.format(number);
        }
    }

}
