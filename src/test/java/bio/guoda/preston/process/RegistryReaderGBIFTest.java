package bio.guoda.preston.process;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import bio.guoda.preston.Seeds;
import bio.guoda.preston.store.TestUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static bio.guoda.preston.RefNodeConstants.*;
import static bio.guoda.preston.model.RefNodeFactory.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.hamcrest.core.StringStartsWith.startsWith;

public class RegistryReaderGBIFTest {

    public static final String GBIFDATASETS_JSON = "gbifdatasets.json";

    @Test
    public void onSeed() {
        ArrayList<Triple> nodes = new ArrayList<>();
        RegistryReaderGBIF registryReaderGBIF = new RegistryReaderGBIF(TestUtil.getTestBlobStore(), nodes::add);
        registryReaderGBIF.on(toStatement(Seeds.GBIF, WAS_ASSOCIATED_WITH, toIRI("http://example.org/someActivity")));
        Assert.assertThat(nodes.size(), is(5));
        assertThat(getVersionSource(nodes.get(4)).getIRIString(), is("https://api.gbif.org/v1/dataset"));
    }

    @Test
    public void onEmptyPage() {
        ArrayList<Triple> nodes = new ArrayList<>();
        RegistryReaderGBIF registryReaderGBIF = new RegistryReaderGBIF(TestUtil.getTestBlobStore(), nodes::add);

        registryReaderGBIF.on(toStatement(toIRI("https://api.gbif.org/v1/dataset"),
                HAS_VERSION,
                toIRI("https://some")));
        assertThat(nodes.size(), is(0));
    }

    @Test
    public void onNotSeed() {
        ArrayList<Triple> nodes = new ArrayList<>();
        RegistryReaderGBIF registryReaderGBIF = new RegistryReaderGBIF(TestUtil.getTestBlobStore(), nodes::add);
        RDFTerm bla = toLiteral("bla");
        registryReaderGBIF.on(toStatement(Seeds.GBIF, toIRI("http://example.org"), bla));
        assertThat(nodes.size(), is(0));
    }

    @Test
    public void onContinuation() {
        ArrayList<Triple> nodes = new ArrayList<>();
        BlobStoreReadOnly blobStore = new BlobStoreReadOnly() {
            @Override
            public InputStream get(IRI key) throws IOException {
                return getClass().getResourceAsStream(GBIFDATASETS_JSON);
            }
        };
        RegistryReaderGBIF registryReaderGBIF = new RegistryReaderGBIF(blobStore, nodes::add);


        Triple firstPage = toStatement(toIRI("https://api.gbif.org/v1/dataset"), HAS_VERSION, createTestNode());

        registryReaderGBIF.on(firstPage);

        Assert.assertThat(nodes.size(), is(40654));
        Triple secondPage = nodes.get(18 - 1);
        assertThat(getVersionSource(secondPage).toString(), is("<https://api.gbif.org/v1/dataset?offset=2&limit=2>"));
        Triple lastPage = nodes.get(nodes.size() - 1);
        assertThat(getVersionSource(lastPage).toString(), is("<https://api.gbif.org/v1/dataset?offset=40638&limit=2>"));
    }

    @Test
    public void onContinuationSearchOrSuggestion() {
        ArrayList<Triple> nodes = new ArrayList<>();
        BlobStoreReadOnly blobStore = new BlobStoreReadOnly() {
            @Override
            public InputStream get(IRI key) throws IOException {
                return getClass().getResourceAsStream("gbif-dataset-search-results.json");
            }
        };
        RegistryReaderGBIF registryReaderGBIF = new RegistryReaderGBIF(blobStore, nodes::add);


        Triple firstPage = toStatement(toIRI("https://api.gbif.org/v1/dataset/suggest"), HAS_VERSION, createTestNode());

        registryReaderGBIF.on(firstPage);

        Assert.assertThat(nodes.size(), is(4));
        Triple lastItem = nodes.get(nodes.size() - 1);
        assertThat(getVersionSource(lastItem).toString(), is("<https://api.gbif.org/v1/dataset/b7010c1b-8013-4a3c-a43b-4309a91f9629>"));
    }

    @Test
    public void onContinuationSuggestion() {
        ArrayList<Triple> nodes = new ArrayList<>();
        BlobStoreReadOnly blobStore = new BlobStoreReadOnly() {
            @Override
            public InputStream get(IRI key) throws IOException {
                return getClass().getResourceAsStream("gbif-suggest.json");
            }
        };
        RegistryReaderGBIF registryReaderGBIF = new RegistryReaderGBIF(blobStore, nodes::add);


        Triple firstPage = toStatement(toIRI("http://api.gbif.org/v1/dataset/suggest?q=Amazon&amp;type=OCCURRENCE"), HAS_VERSION, createTestNode());

        registryReaderGBIF.on(firstPage);

        Assert.assertThat(nodes.size(), is(40));
        Triple lastItem = nodes.get(nodes.size() - 1);
        assertThat(getVersionSource(lastItem).toString(), is("<https://api.gbif.org/v1/dataset/663199f1-3528-4289-8069-d27552f62f10>"));
    }

    @Test
    public void onContinuationWithQuery() {
        ArrayList<Triple> nodes = new ArrayList<>();
        BlobStoreReadOnly blobStore = new BlobStoreReadOnly() {
            @Override
            public InputStream get(IRI key) throws IOException {
                return getClass().getResourceAsStream(GBIFDATASETS_JSON);
            }
        };
        RegistryReaderGBIF registryReaderGBIF = new RegistryReaderGBIF(blobStore, nodes::add);


        Triple firstPage = toStatement(toIRI("https://api.gbif.org/v1/dataset/search?q=plant&amp;publishingCountry=AR"), HAS_VERSION, createTestNode());

        registryReaderGBIF.on(firstPage);

        Assert.assertThat(nodes.size(), is(40654));
        Triple secondPage = nodes.get(18 - 1);
        assertThat(getVersionSource(secondPage).toString(), is("<https://api.gbif.org/v1/dataset/search?q=plant&amp;publishingCountry=AR&offset=2&limit=2>"));
        Triple lastPage = nodes.get(nodes.size() - 1);
        assertThat(getVersionSource(lastPage).toString(), is("<https://api.gbif.org/v1/dataset/search?q=plant&amp;publishingCountry=AR&offset=40638&limit=2>"));
    }

    @Test
    public void onSingle() {
        ArrayList<Triple> nodes = new ArrayList<>();
        BlobStoreReadOnly blobStore = new BlobStoreReadOnly() {
            @Override
            public InputStream get(IRI key) throws IOException {
                return getClass().getResourceAsStream("gbif-dataset-single.json");
            }
        };
        RegistryReaderGBIF registryReaderGBIF = new RegistryReaderGBIF(blobStore, nodes::add);


        Triple firstPage = toStatement(toIRI("https://api.gbif.org/v1/dataset"), HAS_VERSION, createTestNode());

        registryReaderGBIF.on(firstPage);

        Assert.assertThat(nodes.size(), is(5));
        Triple secondPage = nodes.get(nodes.size() - 1);
        assertThat(getVersionSource(secondPage).toString(), is("<http://plazi.cs.umb.edu/GgServer/dwca/2924FFB8FFC7C76B4B0B503BFFD8D973.zip>"));
    }

    @Test
    public void nextPage() {
        List<Triple> nodes = new ArrayList<Triple>();
        RegistryReaderGBIF.emitNextPage(0, 10, nodes::add, "https://bla/?limit=2&offset=8");
        assertThat(nodes.size(), is(2));
        assertThat(nodes.get(1).getSubject().toString(), is("<https://bla/?limit=10&offset=0>"));
    }

    @Test
    public void parseDatasets() throws IOException {

        final List<Triple> refNodes = new ArrayList<>();

        IRI testNode = createTestNode();

        RegistryReaderGBIF.parse(testNode, refNodes::add, getClass().getResourceAsStream(GBIFDATASETS_JSON), toIRI("http://example.org/"));

        assertThat(refNodes.size(), is(40654));

        Triple refNode = refNodes.get(0);
        assertThat(refNode.toString(), endsWith("<http://www.w3.org/ns/prov#hadMember> <6555005d-4594-4a3e-be33-c70e587b63d7> ."));

        refNode = refNodes.get(1);
        assertThat(refNode.toString(), is("<6555005d-4594-4a3e-be33-c70e587b63d7> <http://www.w3.org/1999/02/22-rdf-syntax-ns#seeAlso> <https://doi.org/10.15468/orx3mk> ."));

        refNode = refNodes.get(2);
        assertThat(refNode.toString(), is("<6555005d-4594-4a3e-be33-c70e587b63d7> <http://www.w3.org/ns/prov#hadMember> <http://www.snib.mx/iptconabio/archive.do?r=SNIB-ME006-ME0061704F-ictioplancton-CH-SIB.2017.06.06> ."));

        refNode = refNodes.get(3);
        assertThat(refNode.toString(), is("<http://www.snib.mx/iptconabio/archive.do?r=SNIB-ME006-ME0061704F-ictioplancton-CH-SIB.2017.06.06> <http://purl.org/dc/elements/1.1/format> \"application/dwca\" ."));

        refNode = refNodes.get(4);
        assertThat(refNode.toString(), startsWith("<http://www.snib.mx/iptconabio/archive.do?r=SNIB-ME006-ME0061704F-ictioplancton-CH-SIB.2017.06.06> <http://purl.org/pav/hasVersion> "));

        refNode = refNodes.get(5);
        assertThat(refNode.toString(), is("<6555005d-4594-4a3e-be33-c70e587b63d7> <http://www.w3.org/ns/prov#hadMember> <http://www.snib.mx/iptconabio/eml.do?r=SNIB-ME006-ME0061704F-ictioplancton-CH-SIB.2017.06.06> ."));

        refNode = refNodes.get(6);
        assertThat(refNode.toString(), is("<http://www.snib.mx/iptconabio/eml.do?r=SNIB-ME006-ME0061704F-ictioplancton-CH-SIB.2017.06.06> <http://purl.org/dc/elements/1.1/format> \"application/eml\" ."));

        refNode = refNodes.get(7);
        assertThat(refNode.toString(), startsWith("<http://www.snib.mx/iptconabio/eml.do?r=SNIB-ME006-ME0061704F-ictioplancton-CH-SIB.2017.06.06> <http://purl.org/pav/hasVersion> "));

        refNode = refNodes.get(8);
        assertThat(refNode.toString(), endsWith("<http://www.w3.org/ns/prov#hadMember> <d0df772d-78f4-4602-acf2-7d768798f632> ."));

        Triple lastRefNode = refNodes.get(refNodes.size() - 2);
        assertThat(lastRefNode.toString(), is("<http://example.org/?offset=40638&limit=2> <http://purl.org/dc/elements/1.1/format> \"application/json\" ."));

        lastRefNode = refNodes.get(refNodes.size() - 1);
        assertThat(lastRefNode.toString(), startsWith("<http://example.org/?offset=40638&limit=2> <http://purl.org/pav/hasVersion> "));

    }

    private IRI createTestNode() {
        try {
            return toIRI(getClass().getResource("gbifdatasets.json").toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }


}