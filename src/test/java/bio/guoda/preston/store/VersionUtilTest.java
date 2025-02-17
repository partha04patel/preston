package bio.guoda.preston.store;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.rdf.api.IRI;
import bio.guoda.preston.RefNodeConstants;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Triple;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static bio.guoda.preston.model.RefNodeFactory.toBlank;
import static bio.guoda.preston.model.RefNodeFactory.toDateTime;
import static bio.guoda.preston.model.RefNodeFactory.toIRI;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class VersionUtilTest {

    @Test
    public void getVersion() throws IOException {
        KeyValueStore testKeyValueStore = TestUtil.getTestPersistence();


        StatementStore statementStore = new StatementStoreImpl(testKeyValueStore);
        statementStore.put(Pair.of(toIRI("http://some"), RefNodeConstants.HAS_VERSION), toIRI("http://some/version"));


        IRI mostRecentVersion = VersionUtil.findMostRecentVersion(toIRI("http://some"), statementStore);

        assertThat(mostRecentVersion.toString(), is("<http://some/version>"));
    }

    @Test
    public void getTwoVersions() throws IOException {
        KeyValueStore testKeyValueStore = TestUtil.getTestPersistence();

        StatementStore statementStore = new StatementStoreImpl(testKeyValueStore);
        statementStore.put(Pair.of(toIRI("http://some"), RefNodeConstants.HAS_VERSION), toIRI("http://some/version"));
        statementStore.put(Pair.of(RefNodeConstants.HAS_PREVIOUS_VERSION, toIRI("http://some/version")), toIRI("http://some/later/version"));


        IRI mostRecentVersion = VersionUtil.findMostRecentVersion(toIRI("http://some"), statementStore);

        assertThat(mostRecentVersion.toString(), is("<http://some/later/version>"));
    }

    @Test
    public void versionPointingToItself() throws IOException {
        KeyValueStore testKeyValueStore = TestUtil.getTestPersistence();


        StatementStore statementStore = new StatementStoreImpl(testKeyValueStore);
        statementStore.put(Pair.of(toIRI("http://some"), RefNodeConstants.HAS_VERSION), toIRI("http://some/version"));
        statementStore.put(Pair.of(RefNodeConstants.HAS_PREVIOUS_VERSION, toIRI("http://some/version")), toIRI("http://some/version"));


        IRI mostRecentVersion = VersionUtil.findMostRecentVersion(toIRI("http://some"), statementStore);

        assertThat(mostRecentVersion.toString(), is("<http://some/version>"));
    }

    @Test
    public void versionPointingToItself2() throws IOException {
        KeyValueStore testKeyValueStore = TestUtil.getTestPersistence();


        StatementStore statementStore = new StatementStoreImpl(testKeyValueStore);
        statementStore.put(Pair.of(toIRI("http://some"), RefNodeConstants.HAS_VERSION), toIRI("http://some/version"));
        statementStore.put(Pair.of(RefNodeConstants.HAS_PREVIOUS_VERSION, toIRI("http://some/version")), toIRI("http://some/other/version"));
        statementStore.put(Pair.of(RefNodeConstants.HAS_PREVIOUS_VERSION, toIRI("http://some/other/version")), toIRI("http://some/version"));
        statementStore.put(Pair.of(RefNodeConstants.HAS_PREVIOUS_VERSION, toIRI("http://some/version")), toIRI("http://some/other/version"));


        IRI mostRecentVersion = VersionUtil.findMostRecentVersion(toIRI("http://some"), statementStore);

        assertThat(mostRecentVersion.toString(), is("<http://some/other/version>"));
    }

    @Ignore(value = "enable after implementing prov root selection")
    @Test
    public void versionPointingToItself2NonRoot() throws IOException {
        KeyValueStore testKeyValueStore = TestUtil.getTestPersistence();


        StatementStore statementStore = new StatementStoreImpl(testKeyValueStore);
        statementStore.put(Pair.of(toIRI("http://some"), RefNodeConstants.HAS_VERSION), toIRI("http://some/version"));
        statementStore.put(Pair.of(RefNodeConstants.HAS_PREVIOUS_VERSION, toIRI("http://some/version")), toIRI("http://some/other/version"));
        statementStore.put(Pair.of(RefNodeConstants.HAS_PREVIOUS_VERSION, toIRI("http://some/other/version")), toIRI("http://some/version"));
        statementStore.put(Pair.of(RefNodeConstants.HAS_PREVIOUS_VERSION, toIRI("http://some/version")), toIRI("http://some/other/version"));


        IRI mostRecentVersion = VersionUtil.findMostRecentVersion(toIRI("http://some/version"), statementStore);

        assertNotNull(mostRecentVersion);
        assertThat(mostRecentVersion.toString(), is("<http://some/other/version>"));
    }

    @Ignore(value = "enable after implementing prov root selection")
    @Test
    public void historyOfSpecificNonRootVersion() throws IOException {
        KeyValueStore testKeyValueStore = TestUtil.getTestPersistence();


        StatementStore statementStore = new StatementStoreImpl(testKeyValueStore);
        statementStore.put(Pair.of(toIRI("http://some"), RefNodeConstants.HAS_VERSION), toIRI("http://some/version"));
        statementStore.put(Pair.of(RefNodeConstants.HAS_PREVIOUS_VERSION, toIRI("http://some/version")), toIRI("http://some/other/version"));


        IRI mostRecentVersion = VersionUtil.findMostRecentVersion(toIRI("http://some/version"), statementStore);

        assertNotNull(mostRecentVersion);
        assertThat(mostRecentVersion.toString(), is("<http://some/other/version>"));
    }


}