package org.globalbioticinteractions.preston.process;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Triple;
import org.globalbioticinteractions.preston.MimeTypes;
import org.globalbioticinteractions.preston.RefNodeConstants;
import org.globalbioticinteractions.preston.Seeds;
import org.globalbioticinteractions.preston.cmd.CrawlContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.globalbioticinteractions.preston.RefNodeConstants.CREATED_BY;
import static org.globalbioticinteractions.preston.RefNodeConstants.DESCRIPTION;
import static org.globalbioticinteractions.preston.RefNodeConstants.HAD_MEMBER;
import static org.globalbioticinteractions.preston.RefNodeConstants.IS_A;
import static org.globalbioticinteractions.preston.RefNodeConstants.ORGANIZATION;
import static org.globalbioticinteractions.preston.RefNodeConstants.USED_BY;
import static org.globalbioticinteractions.preston.RefNodeConstants.WAS_ASSOCIATED_WITH;
import static org.globalbioticinteractions.preston.model.RefNodeFactory.getVersion;
import static org.globalbioticinteractions.preston.model.RefNodeFactory.getVersionSource;
import static org.globalbioticinteractions.preston.model.RefNodeFactory.hasVersionAvailable;
import static org.globalbioticinteractions.preston.model.RefNodeFactory.toBlank;
import static org.globalbioticinteractions.preston.model.RefNodeFactory.toContentType;
import static org.globalbioticinteractions.preston.model.RefNodeFactory.toEnglishLiteral;
import static org.globalbioticinteractions.preston.model.RefNodeFactory.toIRI;
import static org.globalbioticinteractions.preston.model.RefNodeFactory.toStatement;
import static org.globalbioticinteractions.preston.model.RefNodeFactory.toUUID;

public class RegistryReaderGBIF extends ProcessorReadOnly {
    private static final Map<String, String> SUPPORTED_ENDPOINT_TYPES = new HashMap<String, String>() {{
        put("DWC_ARCHIVE", MimeTypes.MIME_TYPE_DWCA);
        put("EML", MimeTypes.MIME_TYPE_EML);
    }};

    public static final String GBIF_DATASET_REGISTRY_STRING = "https://api.gbif.org/v1/dataset";
    private final Log LOG = LogFactory.getLog(RegistryReaderGBIF.class);
    public static final IRI GBIF_REGISTRY = toIRI(GBIF_DATASET_REGISTRY_STRING);

    public RegistryReaderGBIF(BlobStoreReadOnly blobStoreReadOnly, StatementListener listener) {
        super(blobStoreReadOnly, listener);
    }

    @Override
    public void on(Triple statement) {
        if (Seeds.GBIF.equals(statement.getSubject())
                && WAS_ASSOCIATED_WITH.equals(statement.getPredicate())) {
            Stream.of(
                    toStatement(Seeds.GBIF, IS_A, ORGANIZATION),
                    toStatement(RegistryReaderGBIF.GBIF_REGISTRY, DESCRIPTION, toEnglishLiteral("Provides a registry of Darwin Core archives, and EML descriptors.")),
                    toStatement(RegistryReaderGBIF.GBIF_REGISTRY, CREATED_BY, Seeds.GBIF))
                    .forEach(this::emit);
            emitPageRequest(this, GBIF_REGISTRY);
        } else if (hasVersionAvailable(statement)
                && getVersionSource(statement).toString().startsWith("<" + GBIF_DATASET_REGISTRY_STRING)) {
            try {
                IRI currentPage = (IRI) getVersion(statement);
                parse(currentPage, this, get(currentPage));
            } catch (IOException e) {
                LOG.warn("failed to handle [" + statement.toString() + "]", e);
            }
        }
    }

    private static void emitNextPage(int offset, int limit, StatementEmitter emitter) {
        String uri = GBIF_DATASET_REGISTRY_STRING + "?offset=" + offset + "&limit=" + limit;
        IRI nextPage = toIRI(uri);
        emitPageRequest(emitter, nextPage);
    }

    private static void emitPageRequest(StatementEmitter emitter, IRI nextPage) {
        Stream.of(
                toStatement(nextPage, RefNodeConstants.HAS_FORMAT, toContentType(MimeTypes.MIME_TYPE_JSON)),
                toStatement(nextPage, RefNodeConstants.HAS_VERSION, toBlank()))
                .forEach(emitter::emit);
    }

    public static void parse(IRI currentPage, StatementEmitter emitter, InputStream in) throws IOException {
        JsonNode jsonNode = new ObjectMapper().readTree(in);
        if (jsonNode != null && jsonNode.has("results")) {
            for (JsonNode result : jsonNode.get("results")) {
                if (result.has("key") && result.has("endpoints")) {
                    String uuid = result.get("key").asText();
                    IRI datasetUUID = toUUID(uuid);
                    emitter.emit(toStatement(currentPage, RefNodeConstants.HAD_MEMBER, datasetUUID));

                    for (JsonNode endpoint : result.get("endpoints")) {
                        if (endpoint.has("url") && endpoint.has("type")) {
                            String urlString = endpoint.get("url").asText();
                            String type = endpoint.get("type").asText();

                            if (SUPPORTED_ENDPOINT_TYPES.containsKey(type)) {
                                IRI dataArchive = toIRI(urlString);
                                emitter.emit(toStatement(datasetUUID, RefNodeConstants.HAD_MEMBER, dataArchive));
                                emitter.emit(toStatement(dataArchive, RefNodeConstants.HAS_FORMAT, toContentType(SUPPORTED_ENDPOINT_TYPES.get(type))));
                                emitter.emit(toStatement(dataArchive, RefNodeConstants.HAS_VERSION, toBlank()));
                            }
                        }
                    }
                }
            }
        }

        boolean endOfRecords = jsonNode == null || (!jsonNode.has("endOfRecords") || jsonNode.get("endOfRecords").asBoolean(true));
        if (!endOfRecords && jsonNode.has("offset") && jsonNode.has("limit")) {
            int offset = jsonNode.get("offset").asInt();
            int limit = jsonNode.get("limit").asInt();
            emitNextPage(offset + limit, limit, emitter);
        }

    }

}
