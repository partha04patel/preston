package bio.guoda.preston.process;

import org.apache.commons.rdf.api.Triple;

public interface StatementListener {
    void on(Triple statement);
}
