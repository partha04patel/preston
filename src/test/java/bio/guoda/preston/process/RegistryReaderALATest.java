package bio.guoda.preston.process;

import bio.guoda.preston.Seeds;
import bio.guoda.preston.store.TestUtil;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;

import static bio.guoda.preston.RefNodeConstants.HAS_VERSION;
import static bio.guoda.preston.RefNodeConstants.WAS_ASSOCIATED_WITH;
import static bio.guoda.preston.model.RefNodeFactory.getVersionSource;
import static bio.guoda.preston.model.RefNodeFactory.toIRI;
import static bio.guoda.preston.model.RefNodeFactory.toLiteral;
import static bio.guoda.preston.model.RefNodeFactory.toStatement;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@Ignore
public class RegistryReaderALATest {

    @Test
    public void onSeed() {
        ArrayList<Triple> nodes = new ArrayList<>();
        RegistryReaderALA registryReader = new RegistryReaderALA(TestUtil.getTestBlobStore(), nodes::add);
        registryReader.on(toStatement(Seeds.ALA, WAS_ASSOCIATED_WITH, toIRI("http://example.org/someActivity")));
        Assert.assertThat(nodes.size(), is(5));
        assertThat(getVersionSource(nodes.get(4)).getIRIString(), is("https://collections.ala.org.au/ws/dataResource?status=dataAvailable"));
    }

    @Test
    public void onEmptyPage() {
        ArrayList<Triple> nodes = new ArrayList<>();
        RegistryReaderALA registryReader = new RegistryReaderALA(TestUtil.getTestBlobStore(), nodes::add);

        registryReader.on(toStatement(toIRI("https://collections.ala.org.au/ws/dataResource?status=dataAvailable&resourceType=records"),
                HAS_VERSION,
                toIRI("https://some")));
        assertThat(nodes.size(), is(0));
    }

    @Test
    public void onNotSeed() {
        ArrayList<Triple> nodes = new ArrayList<>();
        RegistryReaderALA registryReader = new RegistryReaderALA(TestUtil.getTestBlobStore(), nodes::add);
        RDFTerm bla = toLiteral("bla");
        registryReader.on(toStatement(Seeds.GBIF, toIRI("http://example.org"), bla));
        assertThat(nodes.size(), is(0));
    }

    @Test
    public void onRegistry() {
        ArrayList<Triple> nodes = new ArrayList<>();
        BlobStoreReadOnly blobStore = new BlobStoreReadOnly() {
            @Override
            public InputStream get(IRI key) throws IOException {
                return getClass().getResourceAsStream("ala-dataresource-reg.json");
            }
        };
        RegistryReaderALA registryReader = new RegistryReaderALA(blobStore, nodes::add);


        Triple firstPage = toStatement(toIRI("https://collections.ala.org.au/ws/dataResource?status=dataAvailable"), HAS_VERSION, createTestNode());

        registryReader.on(firstPage);

        Assert.assertThat(nodes.size(), is(2654));
        Triple secondPage = nodes.get(nodes.size() - 2);
        assertThat(secondPage.toString(), is("<https://collections.ala.org.au/ws/dataResource/dr8052> <http://purl.org/dc/elements/1.1/format> \"application/json\" ."));
        Triple thirdPage = nodes.get(nodes.size() - 1);
        assertThat(getVersionSource(thirdPage).toString(), is("<https://collections.ala.org.au/ws/dataResource/dr8052>"));
    }

    @Test
    public void onSingle() {
        ArrayList<Triple> nodes = new ArrayList<>();
        BlobStoreReadOnly blobStore = new BlobStoreReadOnly() {
            @Override
            public InputStream get(IRI key) throws IOException {
                return getClass().getResourceAsStream("ala-dataresource.json");
            }
        };
        RegistryReaderALA registryReader = new RegistryReaderALA(blobStore, nodes::add);


        Triple firstPage = toStatement(toIRI("https://collections.ala.org.au/ws/dataResource/dr6504"), HAS_VERSION, createTestNode());

        registryReader.on(firstPage);

        Assert.assertThat(nodes.size(), is(2));
        Triple first = nodes.get(0);
        assertThat(getVersionSource(first).toString(), is("<http://biocache.ala.org.au/archives/dr6504/dr6504_ror_dwca.zip>"));
        Triple second = nodes.get(1);
        assertThat(getVersionSource(second).toString(), is("<https://biocache.ala.org.au/archives/dr6504/dr6504_ror_dwca.zip>"));
    }

    @Test
    public void onSingleDwCA() {
        ArrayList<Triple> nodes = new ArrayList<>();
        BlobStoreReadOnly blobStore = new BlobStoreReadOnly() {
            @Override
            public InputStream get(IRI key) throws IOException {
                return getClass().getResourceAsStream("ala-dataresource-dwca.json");
            }
        };
        RegistryReaderALA registryReader = new RegistryReaderALA(blobStore, nodes::add);


        Triple firstPage = toStatement(toIRI("https://collections.ala.org.au/ws/dataResource/dr6504"), HAS_VERSION, createTestNode());

        registryReader.on(firstPage);

        Assert.assertThat(nodes.size(), is(2));
        Triple first = nodes.get(0);
        assertThat(first.toString(), is("<https://biocache.ala.org.au/archives/gbif/dr382/dr382.zip> <http://purl.org/dc/elements/1.1/format> \"application/dwca\" ."));
        Triple second = nodes.get(1);
        assertThat(getVersionSource(second).toString(), is("<https://biocache.ala.org.au/archives/gbif/dr382/dr382.zip>"));
    }

    private IRI createTestNode() {
        try {
            return toIRI(getClass().getResource("ala-dataresource-reg.json").toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }


}