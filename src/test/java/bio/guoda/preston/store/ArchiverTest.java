package bio.guoda.preston.store;

import bio.guoda.preston.model.RefNodeFactory;
import bio.guoda.preston.process.StatementListener;
import bio.guoda.preston.process.VersionLogger;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Triple;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static bio.guoda.preston.RefNodeConstants.HAS_VERSION;
import static bio.guoda.preston.model.RefNodeFactory.isBlankOrSkolemizedBlank;
import static bio.guoda.preston.model.RefNodeFactory.toBlank;
import static bio.guoda.preston.model.RefNodeFactory.toIRI;
import static bio.guoda.preston.model.RefNodeFactory.toSkolemizedBlank;
import static bio.guoda.preston.model.RefNodeFactory.toStatement;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class ArchiverTest {

    private static final IRI SOME_IRI = toIRI(URI.create("http://some"));

    @Test
    public void putContentThatFailsToDownload() throws IOException {
        BlankNode blank = toBlank();
        Triple statement
                = toStatement(toIRI(URI.create("http://some")),
                HAS_VERSION,
                blank);

        Dereferencer<IRI> dereferencer = uri -> {
            throw new IOException("fails to dereference");
        };

        final StatementStoreImpl versionStore = new StatementStoreImpl(TestUtil.getTestPersistence());
        StatementListener versionLogger = createVersionLogger(versionStore);

        Archiver relationStore = new Archiver(
                dereferencer,
                TestUtil.getTestCrawlContext(),
                versionLogger);

        relationStore.on(statement);

        // dereference subject
        IRI contentHash = versionStore
                .get(Pair.of(toIRI(URI.create("http://some")), HAS_VERSION));

        assertTrue(isBlankOrSkolemizedBlank(contentHash));
    }

    private StatementListener createVersionLogger(final StatementStore versionStore) {
        return new StatementListener() {
            @Override
            public void on(Triple statement) {

                if (HAS_VERSION.equals(statement.getPredicate())) {
                    try {
                        versionStore.put(Pair.of(statement.getSubject(), HAS_VERSION), statement.getObject());
                    } catch (IOException e) {
                        fail(e.getMessage());
                    }
                }
            }
        };
    }

    @Test
    public void doNotEmitSkolemizedBlanks() throws IOException {
        IRI skolemizedBlank = toSkolemizedBlank(toBlank());
        Triple statement = toStatement(toIRI(URI.create("http://some")),
                HAS_VERSION,
                skolemizedBlank);

        Dereferencer<IRI> dereferencer = uri -> {
            throw new IOException("fails to dereference");
        };

        KeyValueStore testKeyValueStore = TestUtil.getTestPersistence();

        List<Triple> nodes = new ArrayList<>();

        StatementStoreImpl versionStore = new StatementStoreImpl(testKeyValueStore);

        Archiver relationStore = new Archiver(
                dereferencer,
                TestUtil.getTestCrawlContext(),
                nodes::add, createVersionLogger(versionStore));

        relationStore.on(statement);

        assertThat(nodes.size(), Is.is(1));

        // dereference subject
        IRI contentHash = versionStore.get(
                Pair.of(toIRI(URI.create("http://some")), HAS_VERSION));

        assertThat(contentHash.getIRIString(), startsWith("https://deeplinker.bio/.well-known/genid/"));
    }

    @Test
    public void putContentThatNeedsDownload() throws IOException {
        BlankNode blank = toBlank();

        Triple statement
                = toStatement(toIRI(URI.create("http://some")),
                HAS_VERSION,
                blank);

        Dereferencer<IRI> dereferencer = new DereferenceTest("#derefData");
        KeyValueStore testKeyValueStore = TestUtil.getTestPersistence();

        StatementStoreImpl versionStore = new StatementStoreImpl(testKeyValueStore);
        Archiver relationStore = getAppendOnlyRelationStore(dereferencer, versionStore);

        relationStore.on(statement);

        // dereference subject

        IRI contentHash = versionStore.get(
                Pair.of(toIRI(URI.create("http://some")), HAS_VERSION));
        assertNotNull(contentHash);
        assertThat(contentHash, Is.is(RefNodeFactory.toIRI("http://some#derefData")));
    }

    private Archiver getAppendOnlyRelationStore(Dereferencer<IRI> dereferencer1, StatementStore statementStore) {
        return new Archiver(dereferencer1, TestUtil.getTestCrawlContext(), createVersionLogger(statementStore));
    }

    @Test
    public void putNewVersionOfContent() throws IOException {
        Triple statement = toStatement(SOME_IRI, HAS_VERSION, toBlank());

        KeyValueStore keyValueStore = TestUtil.getTestPersistence();
        StatementStore versionStore = new StatementStoreImpl(keyValueStore);

        final DereferenceTest dereferencer = new DereferenceTest("#derefData");
        Archiver relationstore = getAppendOnlyRelationStore(dereferencer, versionStore);

        relationstore.on(statement);

        IRI contentHash = versionStore.get(Pair.of(SOME_IRI, HAS_VERSION));
        assertThat(contentHash.getIRIString(), Is.is("http://some#derefData"));

        final DereferenceTest dereferencer1 = new DereferenceTest("#derefData2");
        relationstore = getAppendOnlyRelationStore(dereferencer1, versionStore);
        relationstore.on(statement);

        IRI contentHash2 = versionStore.get(Pair.of(SOME_IRI, HAS_VERSION));

        assertThat(contentHash, Is.is(contentHash2));
    }

    @Test
    public void archiveLastestTwo() throws IOException {
        Triple statement = toStatement(SOME_IRI, HAS_VERSION, toBlank());

        KeyValueStore keyValueStore = TestUtil.getTestPersistence();
        StatementStore versionStore = new StatementStoreImpl(keyValueStore);

        Archiver relationstore = getAppendOnlyRelationStore(
                new DereferenceTest("#derefData"), versionStore);

        relationstore.on(statement);

        IRI contentHash = versionStore.get(Pair.of(SOME_IRI, HAS_VERSION));
        assertNotNull(contentHash);

        final DereferenceTest dereferencer1 = new DereferenceTest("#derefData2");
        relationstore = getAppendOnlyRelationStore(
                dereferencer1, versionStore);
        relationstore.on(statement);

        IRI contentHash2 = versionStore.get(Pair.of(SOME_IRI, HAS_VERSION));
        assertThat(contentHash, Is.is(contentHash2));
    }

    private class DereferenceTest implements Dereferencer<IRI> {
        private final String prefix;

        public DereferenceTest(String s) {
            this.prefix = s;
        }

        @Override
        public IRI dereference(IRI uri) throws IOException {
            return RefNodeFactory.toIRI(uri.getIRIString() + prefix);
        }
    }
}
