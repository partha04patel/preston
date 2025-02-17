package bio.guoda.preston.store;

import bio.guoda.preston.Hasher;
import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.hamcrest.core.Is;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static bio.guoda.preston.model.RefNodeFactory.toIRI;
import static bio.guoda.preston.store.TestUtil.toUTF8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;

public class DereferencerContentAddressedTest {

    @Test
    public void dereference() throws IOException {
        Dereferencer<InputStream> dereferencer = new DereferenceTest("derefData@");
        BlobStore blobStore = new BlobStoreAppendOnly(TestUtil.getTestPersistence());
        DereferencerContentAddressed dereferencerContentAddressed = new DereferencerContentAddressed(dereferencer, blobStore);

        IRI contentHash = dereferencerContentAddressed.dereference(toIRI(URI.create("http://some")));
        InputStream content = blobStore.get(contentHash);
        assertNotNull(contentHash);

        String expectedContent = "derefData@http://some";

        String actualContent = toUTF8(content);
        assertThat(actualContent, Is.is(expectedContent));
        assertThat(contentHash, Is.is(Hasher.calcSHA256(expectedContent)));

    }

    private class DereferenceTest implements Dereferencer<InputStream> {

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