package bio.guoda.preston.cmd;

import bio.guoda.preston.process.BlobStoreReadOnly;
import bio.guoda.preston.process.StatementListener;
import bio.guoda.preston.store.VersionUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Triple;

import java.io.IOException;
import java.io.InputStream;

public class VersionRetriever implements StatementListener {
    private static final Log LOG = LogFactory.getLog(VersionRetriever.class);

    private final BlobStoreReadOnly blobStore;

    public VersionRetriever(BlobStoreReadOnly blobStore) {
        this.blobStore = blobStore;
    }

    @Override
    public void on(Triple statement) {
        IRI mostRecentVersion = VersionUtil.mostRecentVersionForStatement(statement);
        if (mostRecentVersion != null) {
            touchMostRecentVersion(mostRecentVersion);
        }
    }

    private void touchMostRecentVersion(IRI mostRecentVersion) {
        try {
            InputStream inputStream = blobStore.get(mostRecentVersion);
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            LOG.warn("failed to access [" + mostRecentVersion.getIRIString() + "]");
        }
    }

}
