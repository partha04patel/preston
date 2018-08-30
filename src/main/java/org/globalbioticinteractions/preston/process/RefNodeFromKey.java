package org.globalbioticinteractions.preston.process;

import org.globalbioticinteractions.preston.model.RefNode;
import org.globalbioticinteractions.preston.store.BlobStore;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class RefNodeFromKey implements RefNode {

    private BlobStore store;
    private final URI key;

    public RefNodeFromKey(BlobStore blobStore, URI key) {
        this.store = blobStore;
        this.key = key;
    }

    @Override
    public InputStream getContent() throws IOException {
        return store.get(getContentHash());
    }

    @Override
    public String getLabel() {
        return getContentHash().toString();
    }

    @Override
    public URI getContentHash() {
        return key;
    }

    @Override
    public boolean equivalentTo(RefNode node) {
        URI id = getContentHash();
        URI otherId = node == null ? null : node.getContentHash();
        return id != null && id.equals(otherId);
    }
}
