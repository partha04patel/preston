package org.globalbioticinteractions.preston.process;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.globalbioticinteractions.preston.MimeTypes;
import org.globalbioticinteractions.preston.RefNodeConstants;
import org.globalbioticinteractions.preston.Seeds;
import org.globalbioticinteractions.preston.model.RefNode;
import org.globalbioticinteractions.preston.model.RefNodeString;
import org.globalbioticinteractions.preston.model.RefStatement;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import static org.globalbioticinteractions.preston.RefNodeConstants.CONTINUATION_OF;
import static org.globalbioticinteractions.preston.RefNodeConstants.DATASET_REGISTRY_OF;
import static org.globalbioticinteractions.preston.RefNodeConstants.HAD_MEMBER;
import static org.globalbioticinteractions.preston.RefNodeConstants.SEED_OF;
import static org.globalbioticinteractions.preston.RefNodeConstants.WAS_DERIVED_FROM;
import static org.globalbioticinteractions.preston.RefNodeConstants.WAS_REVISION_OF;

public class RegistryReaderGBIF extends RefStatementProcessor {
    private static final Map<String, String> SUPPORTED_ENDPOINT_TYPES = new HashMap<String, String>() {{
        put("DWC_ARCHIVE", MimeTypes.MIME_TYPE_DWCA);
        put("EML", MimeTypes.MIME_TYPE_EML);
    }};

    private static final String GBIF_DATASET_API_ENDPOINT = "https://api.gbif.org/v1/dataset";
    private final Log LOG = LogFactory.getLog(RegistryReaderGBIF.class);

    public RegistryReaderGBIF(RefStatementListener listener) {
        super(listener);
    }

    @Override
    public void on(RefStatement statement) {
        if (Seeds.SEED_NODE_GBIF.equivalentTo(statement.getSubject())
                && SEED_OF.equivalentTo(statement.getPredicate())) {
            RefNode refNodeRegistry = new RefNodeString(GBIF_DATASET_API_ENDPOINT);
            emitPageRequest(this, refNodeRegistry);
        } else if (statement.getSubject() != null
                && statement.getObject() != null
                && statement.getObject().getLabel().startsWith(GBIF_DATASET_API_ENDPOINT)
                && (WAS_DERIVED_FROM == statement.getPredicate()
                || WAS_REVISION_OF == statement.getPredicate())) {
            try {
                parse(statement.getSubject(), this, statement.getObject());
            } catch (IOException e) {
                LOG.warn("failed to handle [" + statement.getLabel() + "]", e);
            }
        }
    }

    private static void emitNextPage(RefNode previousPage, int offset, int limit, RefStatementEmitter emitter) {
        String uri = GBIF_DATASET_API_ENDPOINT + "?offset=" + offset + "&limit=" + limit;
        RefNode nextPage = new RefNodeString(uri);
        emitter.emit(new RefStatement(nextPage, CONTINUATION_OF, previousPage));
        emitPageRequest(emitter, nextPage);
    }

    private static void emitPageRequest(RefStatementEmitter emitter, RefNode nextPage) {
        emitter.emit(new RefStatement(nextPage, RefNodeConstants.HAS_FORMAT, new RefNodeString(MimeTypes.MIME_TYPE_JSON)));
        emitter.emit(new RefStatement(null, RefNodeConstants.WAS_DERIVED_FROM, nextPage));
    }

    public static void parse(RefNode currentPageContent, RefStatementEmitter emitter, RefNode currentPage) throws IOException {
        emitter.emit(new RefStatement(Seeds.SEED_NODE_GBIF, HAD_MEMBER, currentPageContent));
        JsonNode jsonNode = new ObjectMapper().readTree(currentPageContent.getContent());
        if (jsonNode != null && jsonNode.has("results")) {
            for (JsonNode result : jsonNode.get("results")) {
                if (result.has("key") && result.has("endpoints")) {
                    String uuid = result.get("key").asText();
                    RefNodeString datasetUUID = new RefNodeString(uuid);
                    emitter.emit(new RefStatement(currentPageContent, RefNodeConstants.HAD_MEMBER, datasetUUID));

                    for (JsonNode endpoint : result.get("endpoints")) {
                        if (endpoint.has("url") && endpoint.has("type")) {
                            String urlString = endpoint.get("url").asText();
                            String type = endpoint.get("type").asText();

                            if (SUPPORTED_ENDPOINT_TYPES.containsKey(type)) {
                                RefNodeString dataArchive = new RefNodeString(urlString);
                                emitter.emit(new RefStatement(datasetUUID, RefNodeConstants.HAD_MEMBER, dataArchive));
                                emitter.emit(new RefStatement(dataArchive, RefNodeConstants.HAS_FORMAT, new RefNodeString(SUPPORTED_ENDPOINT_TYPES.get(type))));
                                emitter.emit(new RefStatement(null, RefNodeConstants.WAS_DERIVED_FROM, dataArchive));
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
            emitNextPage(currentPage, offset + limit, limit, emitter);
        }

    }

}
