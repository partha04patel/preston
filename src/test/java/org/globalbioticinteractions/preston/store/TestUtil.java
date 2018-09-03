package org.globalbioticinteractions.preston.store;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.globalbioticinteractions.preston.cmd.CrawlContext;
import org.globalbioticinteractions.preston.process.BlobStoreReadOnly;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.globalbioticinteractions.preston.model.RefNodeFactory.toIRI;

public class TestUtil {
    public static String toUTF8(InputStream content) throws IOException {
        return content == null ? null : IOUtils.toString(content, StandardCharsets.UTF_8);
    }

    public static Persistence getTestPersistence() {
        return AppendOnlyBlobStoreTest.getTestPersistence();
    }

    public static BlobStoreReadOnly getTestBlobStore() {
        return new AppendOnlyBlobStore(getTestPersistence());
    }

    public static CrawlContext getTestCrawlContext() {
        return new CrawlContext() {
            @Override
            public IRI getActivity() {
                return toIRI("https://example.com/testActivity");
            }

            @Override
            public IRI getArchive() {
                return toIRI("https://example.com/testArchive");
            }

            @Override
            public IRI getGraph() {
                return toIRI("https://example.com/testGraph");
            }
        };
    }
}
