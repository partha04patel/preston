package bio.guoda.preston.store;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFTerm;

import java.io.IOException;

public interface StatementStoreReadOnly {

    IRI get(Pair<RDFTerm, RDFTerm> queryKey) throws IOException;

}
