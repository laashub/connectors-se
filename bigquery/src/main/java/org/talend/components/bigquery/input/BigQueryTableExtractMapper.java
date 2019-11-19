/*
 * Copyright (C) 2006-2019 Talend Inc. - www.talend.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.talend.components.bigquery.input;

import com.google.api.gax.paging.Page;
import com.google.cloud.bigquery.*;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.talend.components.bigquery.datastore.BigQueryConnection;
import org.talend.components.bigquery.service.BigQueryService;
import org.talend.components.bigquery.service.I18nMessage;
import org.talend.sdk.component.api.component.Icon;
import org.talend.sdk.component.api.component.Version;
import org.talend.sdk.component.api.configuration.Option;
import org.talend.sdk.component.api.input.*;
import org.talend.sdk.component.api.meta.Documentation;
import org.talend.sdk.component.api.service.record.RecordBuilderFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Version(1)
@Icon(Icon.IconType.BIGQUERY)
@PartitionMapper(name = "BigQueryTableExtractInput")
@Documentation("This component reads a table from BigQuery (buffering with GS).")
@Slf4j
public class BigQueryTableExtractMapper implements Serializable {

    protected final BigQueryService service;

    protected final I18nMessage i18n;

    protected final RecordBuilderFactory builderFactory;

    protected final BigQueryTableExtractInputConfig configuration;

    protected final String gsBlob;

    protected transient BigQuery bigQuery;

    private transient Table table;

    public BigQueryTableExtractMapper(@Option("configuration") final BigQueryTableExtractInputConfig configuration,
            final BigQueryService service, final I18nMessage i18n, final RecordBuilderFactory builderFactory) {
        this.i18n = i18n;
        this.builderFactory = builderFactory;
        this.service = service;
        this.configuration = configuration;
        this.gsBlob = null;
    }

    protected BigQueryTableExtractMapper(BigQueryTableExtractInputConfig configuration, final BigQueryService service,
            final I18nMessage i18n, final RecordBuilderFactory builderFactory, String gsBlob) {
        this.i18n = i18n;
        this.builderFactory = builderFactory;
        this.service = service;
        this.configuration = configuration;
        this.gsBlob = gsBlob;
    }

    @PostConstruct
    public void init() {
        // Connect and get table metadata
        BigQueryConnection connection = configuration.getDataStore();
        bigQuery = BigQueryService.createClient(connection);
        TableId tableId = TableId.of(connection.getProjectName(), configuration.getTableDataset().getBqDataset(),
                configuration.getTableDataset().getTableName());
        table = bigQuery.getTable(tableId);
    }

    @Assessor
    public long estimateSize() {
        return table.getNumBytes();
    }

    @Split
    public List<BigQueryTableExtractMapper> split(@PartitionSize final long bundleSize) {

        try {
            // extract table to Google Storage
            String uuid = UUID.randomUUID().toString();
            String blobGenericName = "gs://" + configuration.getTableDataset().getGsBucket() + "/tmp/" + uuid + "/f_*.avro";

            ExtractJobConfiguration jobConfig = ExtractJobConfiguration.newBuilder(table.getTableId(), blobGenericName)
                    .setFormat("Avro")
                    // .setCompression("Snappy")
                    .build();

            JobInfo jobInfo = JobInfo.newBuilder(jobConfig).build();

            Job extractJob = bigQuery.create(jobInfo);
            extractJob.waitFor();

            StorageOptions storageOptions = StorageOptions.newBuilder().setCredentials(bigQuery.getOptions().getCredentials())
                    .build();
            Storage storage = new StorageOptions.DefaultStorageFactory().create(storageOptions);
            String prefix = "tmp/" + uuid + "/f_";
            log.info("Blobs prefix : {}", prefix);
            Page<Blob> blobs = storage.list(configuration.getTableDataset().getGsBucket(), Storage.BlobListOption.prefix(prefix));

            // Create and return mapper
            List<BigQueryTableExtractMapper> mappers = new ArrayList<>();
            blobs.iterateAll().forEach(
                    b -> mappers.add(new BigQueryTableExtractMapper(configuration, service, i18n, builderFactory, b.getName())));

            log.info("nb mappers : {}", mappers.size());
            return mappers;

        } catch (Exception e) {
            log.error("Error during split", e);
            throw new RuntimeException();
        }
    }

    @Emitter
    public BigQueryTableExtractInput createSource() {
        return new BigQueryTableExtractInput(configuration, service, i18n, builderFactory, gsBlob);
    }

    @PreDestroy
    public void release() {
        // Nothing to do yet
    }
}
