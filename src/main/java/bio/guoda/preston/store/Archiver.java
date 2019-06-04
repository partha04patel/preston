package bio.guoda.preston.store;

import bio.guoda.preston.model.RefNodeFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Triple;
import bio.guoda.preston.cmd.CmdList;
import bio.guoda.preston.cmd.CrawlContext;
import bio.guoda.preston.process.StatementListener;
import bio.guoda.preston.process.StatementProcessor;

import java.io.IOException;

import static bio.guoda.preston.RefNodeConstants.GENERATED_AT_TIME;
import static bio.guoda.preston.RefNodeConstants.HAS_PREVIOUS_VERSION;
import static bio.guoda.preston.RefNodeConstants.HAS_VERSION;
import static bio.guoda.preston.RefNodeConstants.WAS_GENERATED_BY;
import static bio.guoda.preston.model.RefNodeFactory.getVersion;
import static bio.guoda.preston.model.RefNodeFactory.getVersionSource;
import static bio.guoda.preston.model.RefNodeFactory.toSkolemizedBlank;
import static bio.guoda.preston.model.RefNodeFactory.toStatement;


public class Archiver extends StatementProcessor {
    private static Log LOG = LogFactory.getLog(CmdList.class);

    private final CrawlContext crawlContext;

    private Dereferencer<IRI> dereferencer;

    private boolean resolveOnMissingOnly = false;

    private final StatementStore statementStore;

    public Archiver(Dereferencer<IRI> dereferencer, StatementStore statementStore, CrawlContext crawlContext, StatementListener... listener) {
        super(listener);
        this.crawlContext = crawlContext;
        this.statementStore = statementStore;
        this.dereferencer = dereferencer;

    }

    StatementStore getStatementStore() {
        return this.statementStore;
    }

    @Override
    public void on(Triple statement) {
        try {
            BlankNodeOrIRI version = getVersion(statement);
            if (version instanceof BlankNode) {
                handleVersions(statement, (BlankNode) version);
            } else {
                emit(statement);

            }
        } catch (Throwable e) {
            LOG.warn("failed to handle [" + statement.toString() + "]", e);
        }

    }

    private void handleVersions(Triple statement, BlankNode blankVersion) throws IOException {
        IRI versionSource = getVersionSource(statement);
        IRI previousVersion = VersionUtil.findMostRecentVersion(versionSource, getStatementStore(), new VersionListener() {

            @Override
            public void onVersion(Triple statement) throws IOException {
                emit(statement);
            }
        });
        if (previousVersion == null || !shouldResolveOnMissingOnly()) {
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
                    putVersion(versionSource, previousVersion, newVersion);
                }
            }
        }
    }

    private void putVersion(IRI versionSource, IRI previousVersion, BlankNodeOrIRI newVersion) throws IOException {
        if (null != previousVersion && !previousVersion.equals(newVersion)) {
            emitGenerationTime(newVersion);
            getStatementStore().put(Pair.of(HAS_PREVIOUS_VERSION, previousVersion), newVersion);
            emit(toStatement(newVersion, HAS_PREVIOUS_VERSION, previousVersion));

        } else if (null == previousVersion) {
            emitGenerationTime(newVersion);
            getStatementStore().put(Pair.of(versionSource, HAS_VERSION), newVersion);
            emit(toStatement(versionSource, HAS_VERSION, newVersion));
        }
    }

    private void emitGenerationTime(BlankNodeOrIRI derivedSubject) throws IOException {
        Literal nowLiteral = RefNodeFactory.nowDateTimeLiteral();
        emit(toStatement(derivedSubject,
                GENERATED_AT_TIME,
                nowLiteral));

        if (crawlContext != null) {
            emit(toStatement(derivedSubject,
                    WAS_GENERATED_BY,
                    crawlContext.getActivity()));
        }
    }

    public boolean shouldResolveOnMissingOnly() {
        return resolveOnMissingOnly;
    }

    public void setResolveOnMissingOnly(boolean resolveOnMissingOnly) {
        this.resolveOnMissingOnly = resolveOnMissingOnly;
    }


    private Dereferencer<IRI> getDereferencer() {
        return dereferencer;
    }

}
