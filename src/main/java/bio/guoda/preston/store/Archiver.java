package bio.guoda.preston.store;

import bio.guoda.preston.cmd.ActivityContext;
import bio.guoda.preston.model.RefNodeFactory;
import bio.guoda.preston.process.StatementListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Triple;

import java.io.IOException;
import java.util.UUID;

import static bio.guoda.preston.RefNodeConstants.GENERATED_AT_TIME;
import static bio.guoda.preston.RefNodeConstants.HAS_VERSION;
import static bio.guoda.preston.RefNodeConstants.IS_A;
import static bio.guoda.preston.RefNodeConstants.WAS_GENERATED_BY;
import static bio.guoda.preston.model.RefNodeFactory.getVersionSource;
import static bio.guoda.preston.model.RefNodeFactory.toIRI;
import static bio.guoda.preston.model.RefNodeFactory.toSkolemizedBlank;
import static bio.guoda.preston.model.RefNodeFactory.toStatement;


public class Archiver extends VersionProcessor {
    private static Log LOG = LogFactory.getLog(Archiver.class);

    private final ActivityContext activityCtx;

    private final Dereferencer<IRI> dereferencer;

    public Archiver(Dereferencer<IRI> dereferencer, ActivityContext activityCtx, StatementListener... listener) {
        super(listener);
        this.activityCtx = activityCtx;
        this.dereferencer = dereferencer;
    }

    @Override
    void handleBlankVersion(Triple statement, BlankNode blankVersion) throws IOException {
        IRI versionSource = getVersionSource(statement);
        if (getDereferencer() != null) {
            IRI newVersion = null;
            try {
                newVersion = dereferencer.dereference(versionSource);
            } catch (IOException e) {
                LOG.warn("failed to dereference [" + versionSource.toString() + "]", e);
            } finally {
                if (newVersion == null) {
                    newVersion = toSkolemizedBlank(blankVersion);
                }
                putVersion(versionSource, newVersion);
            }
        }
    }

    private void putVersion(IRI versionSource, BlankNodeOrIRI newVersion) throws IOException {
        Literal nowLiteral = RefNodeFactory.nowDateTimeLiteral();
        emit(toStatement(newVersion,
                GENERATED_AT_TIME,
                nowLiteral));

        if (activityCtx != null) {
            emit(toStatement(newVersion,
                    WAS_GENERATED_BY,
                    activityCtx.getActivity()));
            IRI downloadActivity = toIRI(UUID.randomUUID());
            emit(toStatement(newVersion,
                    toIRI("http://www.w3.org/ns/prov#qualifiedGeneration"),
                    downloadActivity));
            emit(toStatement(downloadActivity,
                    IS_A,
                    toIRI("http://www.w3.org/ns/prov#Generation")));
            emit(toStatement(downloadActivity,
                    toIRI("http://www.w3.org/ns/prov#activity"),
                    (activityCtx.getActivity())));
            emit(toStatement(downloadActivity,
                    toIRI("http://www.w3.org/ns/prov#used"),
                    versionSource));
        }
        emit(toStatement(versionSource, HAS_VERSION, newVersion));
    }

    private Dereferencer<IRI> getDereferencer() {
        return dereferencer;
    }

}
