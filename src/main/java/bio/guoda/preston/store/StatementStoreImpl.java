package bio.guoda.preston.store;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFTerm;
import bio.guoda.preston.Hasher;
import bio.guoda.preston.RDFUtil;
import bio.guoda.preston.model.RefNodeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

public class StatementStoreImpl implements StatementStore {

    private final Persistence persistence;

    public StatementStoreImpl(Persistence persistence) {
        this.persistence = persistence;
    }

    @Override
    public void put(Pair<RDFTerm, RDFTerm> queryKey, RDFTerm value) throws IOException {
        // write-once, read-many
        IRI key = calculateKeyFor(queryKey);
        persistence.put(key.getIRIString(), value instanceof IRI ? ((IRI) value).getIRIString() : value.toString());
    }

    protected static IRI calculateKeyFor(Pair<RDFTerm, RDFTerm> unhashedKeyPair) {
        IRI left = calculateHashFor(unhashedKeyPair.getLeft());
        IRI right = calculateHashFor(unhashedKeyPair.getRight());
        return Hasher.calcSHA256(left.getIRIString() + right.getIRIString());
    }

    protected static IRI calculateHashFor(RDFTerm left1) {
        return Hasher.calcSHA256(RDFUtil.getValueFor(left1));
    }

    @Override
    public IRI get(Pair<RDFTerm, RDFTerm> queryKey) throws IOException {
        InputStream inputStream = persistence.get(calculateKeyFor(queryKey).getIRIString());
        return inputStream == null
                ? null
                : RefNodeFactory.toIRI(URI.create(IOUtils.toString(inputStream, StandardCharsets.UTF_8)));
    }

    ;
}
