package org.globalbioticinteractions.preston.store;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Triple;
import org.globalbioticinteractions.preston.Hasher;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.globalbioticinteractions.preston.RefNodeConstants.HAS_PREVIOUS_VERSION;
import static org.globalbioticinteractions.preston.RefNodeConstants.HAS_VERSION;
import static org.globalbioticinteractions.preston.RefNodeConstants.WAS_DERIVED_FROM;
import static org.globalbioticinteractions.preston.RefNodeConstants.WAS_REVISION_OF;
import static org.globalbioticinteractions.preston.model.RefNodeFactory.isBlankOrSkolemizedBlank;
import static org.globalbioticinteractions.preston.model.RefNodeFactory.toBlank;
import static org.globalbioticinteractions.preston.model.RefNodeFactory.toIRI;
import static org.globalbioticinteractions.preston.model.RefNodeFactory.toSkolemizedBlank;
import static org.globalbioticinteractions.preston.model.RefNodeFactory.toStatement;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

public class IRIInflaterTest {

    private static final IRI SOME_IRI = toIRI(URI.create("http://some"));

    @Test
    public void putContentThatFailsToDownload() throws IOException {
        BlankNode blank = toBlank();
        Triple statement
                = toStatement(blank,
                WAS_DERIVED_FROM,
                toIRI(URI.create("http://some")));

        Dereferencer dereferencer = uri -> {
            throw new IOException("fails to dereference");
        };

        Persistence testPersistence = TestUtil.getTestPersistence();

        IRIInflater relationStore = new IRIInflater(
                new AppendOnlyBlobStore(testPersistence),
                dereferencer, new StatementStoreImpl(testPersistence));

        relationStore.on(statement);

        // dereference subject

        IRI contentHash = relationStore.getStatementStore().get(
                Pair.of(WAS_DERIVED_FROM,
                        toIRI(URI.create("http://some"))));

        assertTrue(isBlankOrSkolemizedBlank(contentHash));
    }

    @Test
    public void doNotEmitSkolemizedBlanks() throws IOException {
        IRI skolemizedBlank = toSkolemizedBlank(toBlank());
        Triple statement
                = toStatement(skolemizedBlank,
                WAS_DERIVED_FROM,
                toIRI(URI.create("http://some")));

        Dereferencer dereferencer = uri -> {
            throw new IOException("fails to dereference");
        };

        Persistence testPersistence = TestUtil.getTestPersistence();

        List<Triple> nodes = new ArrayList<>();

        IRIInflater relationStore = new IRIInflater(
                new AppendOnlyBlobStore(testPersistence),
                dereferencer, new StatementStoreImpl(testPersistence),
                nodes::add);

        relationStore.on(statement);

        assertThat(nodes.size(), Is.is(1));

        // dereference subject
        IRI contentHash = relationStore.getStatementStore().get(
                Pair.of(WAS_DERIVED_FROM,
                        toIRI(URI.create("http://some"))));

        assertNull(contentHash);
    }

    @Test
    public void putContentThatNeedsDownload() throws IOException {
        BlankNode blank = toBlank();
        Triple statement
                = toStatement(blank,
                WAS_DERIVED_FROM,
                toIRI(URI.create("http://some")));

        Dereferencer dereferencer = new DereferenceTest("derefData@");
        Persistence testPersistence = TestUtil.getTestPersistence();

        IRIInflater relationStore = new IRIInflater(
                new AppendOnlyBlobStore(testPersistence),
                dereferencer, new StatementStoreImpl(testPersistence));

        BlobStore blobStore = new AppendOnlyBlobStore(testPersistence);

        relationStore.on(statement);

        // dereference subject

        IRI contentHash = relationStore.getStatementStore().get(
                Pair.of(WAS_DERIVED_FROM,
                        toIRI(URI.create("http://some"))));
        InputStream content = blobStore.get(contentHash);
        assertNotNull(contentHash);

        String expectedContent = "derefData@http://some";

        String actualContent = toUTF8(content);
        assertThat(actualContent, Is.is(expectedContent));
        assertThat(contentHash, Is.is(Hasher.calcSHA256(expectedContent)));
    }

    private IRIInflater getAppendOnlyRelationStore(Dereferencer dereferencer, BlobStore blobStore, Persistence testPersistencetence) {
        return new IRIInflater(blobStore, dereferencer, new StatementStoreImpl(testPersistencetence));
    }

    private String toUTF8(InputStream content) throws IOException {
        return TestUtil.toUTF8(content);
    }

    @Test
    public void putNewVersionOfContent() throws IOException {
        Triple statement
                = toStatement(toBlank(), WAS_DERIVED_FROM, SOME_IRI);


        String prefix = "derefData@";
        Dereferencer dereferencer1 = new DereferenceTest(prefix);

        BlobStore blogStore = new AppendOnlyBlobStore(TestUtil.getTestPersistence());

        IRIInflater relationstore = getAppendOnlyRelationStore(dereferencer1,
                blogStore,
                TestUtil.getTestPersistence());

        relationstore.on(statement);

        IRI contentHash = relationstore.getStatementStore().get(Pair.of(WAS_DERIVED_FROM, SOME_IRI));
        assertNotNull(contentHash);

        Dereferencer dereferencer = new DereferenceTest("derefData2@");
        relationstore.setDereferencer(dereferencer);
        relationstore.on(statement);

        IRI contentHash2 = relationstore.getStatementStore().get(Pair.of(WAS_DERIVED_FROM, SOME_IRI));


        assertThat(contentHash, Is.is(contentHash2));

        IRI newContentHash = relationstore.getStatementStore().get(Pair.of(WAS_REVISION_OF, contentHash));
        InputStream newContent = blogStore.get(newContentHash);

        assertThat(contentHash, not(Is.is(newContentHash)));
        assertThat(newContentHash.getIRIString(), Is.is("hash://sha256/960d96611c4048e05303f6f532590968fd5eb23d0035141c4b02653b436f568c"));

        assertThat(toUTF8(newContent), Is.is("derefData2@http://some"));

        relationstore.setDereferencer(new DereferenceTest("derefData3@"));
        relationstore.on(statement);

        IRI newerContentHash = relationstore.getStatementStore().get(Pair.of(WAS_REVISION_OF, newContentHash));
        InputStream newerContent = blogStore.get(newerContentHash);

        assertThat(newerContentHash.getIRIString(), Is.is("hash://sha256/7e66eac09d137afe06dd73614e966a417260a111208dabe7225b05f02ce380fd"));
        assertThat(toUTF8(newerContent), Is.is("derefData3@http://some"));
    }

    @Test
    public void rewriteFromExistingDerivedFrom() throws IOException {

        Dereferencer explodingDereferencer = uri -> {
            fail("should not attempt to dereference");
            throw new IOException("boom!");
        };
        assertRewrite(explodingDereferencer, true);
    }

    @Test
    public void rewriteFromExistingDerivedWithRefresh() throws IOException {

        Dereferencer explodingDereferencer = uri -> {
            fail("should not attempt to dereference");
            throw new IOException("boom!");
        };
        assertRewrite(explodingDereferencer, false);
    }

    public void assertRewrite(Dereferencer explodingDereferencer, boolean resolveOnMissingOnly) throws IOException {
        BlobStore blogStore = new AppendOnlyBlobStore(TestUtil.getTestPersistence());

        IRIInflater relationstore = getAppendOnlyRelationStore(explodingDereferencer,
                blogStore,
                TestUtil.getTestPersistence());
        relationstore.setResolveOnMissingOnly(resolveOnMissingOnly);

        String original = "https://example.org/source";

        IRI version1 = toIRI("https://example.org/version1");
        IRI version2 = toIRI("https://example.org/version2");

        relationstore.getStatementStore().put((
                Pair.of(WAS_DERIVED_FROM, toIRI(original))),
                version1);

        relationstore.getStatementStore().put(
                Pair.of(WAS_REVISION_OF, version1),
                version2);

        Triple request
                = toStatement(toIRI(original), HAS_VERSION, toBlank());

        relationstore.on(request);

        IRI contentHash = relationstore.getStatementStore().get(Pair.of(toIRI(original), HAS_VERSION));
        assertNotNull(contentHash);

        IRI contentHash2 = relationstore.getStatementStore().get(Pair.of(HAS_PREVIOUS_VERSION, version1));
        assertNotNull(contentHash2);
    }

    private class DereferenceTest implements Dereferencer {

        private final String prefix;

        DereferenceTest(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public InputStream dereference(IRI uri) {
            return IOUtils.toInputStream(prefix + uri.getIRIString(), StandardCharsets.UTF_8);
        }
    }

}
