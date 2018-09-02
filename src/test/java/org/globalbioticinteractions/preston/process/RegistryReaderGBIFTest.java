package org.globalbioticinteractions.preston.process;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFTerm;
import org.apache.commons.rdf.api.Triple;
import org.globalbioticinteractions.preston.RefNodeConstants;
import org.globalbioticinteractions.preston.Seeds;
import org.globalbioticinteractions.preston.model.RefNodeFactory;
import org.globalbioticinteractions.preston.store.TestUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

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
        RDFTerm bla = RefNodeFactory.toLiteral("bla");
        registryReaderGBIF.on(RefNodeFactory.toStatement(Seeds.SEED_NODE_GBIF, RefNodeConstants.HAD_MEMBER, RefNodeConstants.SOFTWARE_AGENT));
        Assert.assertThat(nodes.size(), is(2));
        assertThat(nodes.get(1).getObject().toString(), is("<https://api.gbif.org/v1/dataset>"));
    }

    @Test
    public void onPage() {
        ArrayList<Triple> nodes = new ArrayList<>();
        RegistryReaderGBIF registryReaderGBIF = new RegistryReaderGBIF(TestUtil.getTestBlobStore(), nodes::add);

        registryReaderGBIF.on(RefNodeFactory.toStatement(RefNodeFactory.toIRI("https://api.gbif.org/v1/dataset"),
                RefNodeConstants.HAS_VERSION,
                RefNodeFactory.toIRI("https://some")));
        assertThat(nodes.size(), is(1));
        assertThat(nodes.get(0).getObject().toString(), is("<https://some>"));
    }

    @Test
    public void onNotSeed() {
        ArrayList<Triple> nodes = new ArrayList<>();
        RegistryReaderGBIF registryReaderGBIF = new RegistryReaderGBIF(TestUtil.getTestBlobStore(), nodes::add);
        RDFTerm bla = RefNodeFactory.toLiteral("bla");
        registryReaderGBIF.on(RefNodeFactory.toStatement(Seeds.SEED_NODE_GBIF, RefNodeFactory.toIRI("http://example.org"), bla));
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


        Triple firstPage = RefNodeFactory.toStatement(createTestNode(), RefNodeConstants.WAS_DERIVED_FROM, RefNodeFactory.toIRI("https://api.gbif.org/v1/dataset"));

        registryReaderGBIF.on(firstPage);

        Assert.assertThat(nodes.size(), is(17));
        Triple secondPage = nodes.get(nodes.size() - 1);
        assertThat(secondPage.getObject().toString(), is("<https://api.gbif.org/v1/dataset?offset=2&limit=2>"));
    }

    @Test
    public void parseDatasets() throws IOException {

        final List<Triple> refNodes = new ArrayList<>();

        IRI testNode = createTestNode();

        RegistryReaderGBIF.parse(testNode, refNodes::add, getClass().getResourceAsStream(GBIFDATASETS_JSON));

        assertThat(refNodes.size(), is(17));

        Triple refNode = refNodes.get(0);
        assertThat(refNode.toString(), startsWith("<https://gbif.org> <http://www.w3.org/ns/prov#hadMember> "));

        refNode = refNodes.get(1);
        assertThat(refNode.toString(), endsWith("<http://www.w3.org/ns/prov#hadMember> <6555005d-4594-4a3e-be33-c70e587b63d7> ."));

        refNode = refNodes.get(2);
        assertThat(refNode.toString(), is("<6555005d-4594-4a3e-be33-c70e587b63d7> <http://www.w3.org/ns/prov#hadMember> <http://www.snib.mx/iptconabio/archive.do?r=SNIB-ME006-ME0061704F-ictioplancton-CH-SIB.2017.06.06> ."));

        refNode = refNodes.get(3);
        assertThat(refNode.toString(), is("<http://www.snib.mx/iptconabio/archive.do?r=SNIB-ME006-ME0061704F-ictioplancton-CH-SIB.2017.06.06> <http://purl.org/dc/elements/1.1/format> \"application/dwca\" ."));

        refNode = refNodes.get(4);
        assertThat(refNode.toString(), endsWith("<http://www.w3.org/ns/prov#wasDerivedFrom> <http://www.snib.mx/iptconabio/archive.do?r=SNIB-ME006-ME0061704F-ictioplancton-CH-SIB.2017.06.06> ."));

        refNode = refNodes.get(5);
        assertThat(refNode.toString(), is("<6555005d-4594-4a3e-be33-c70e587b63d7> <http://www.w3.org/ns/prov#hadMember> <http://www.snib.mx/iptconabio/eml.do?r=SNIB-ME006-ME0061704F-ictioplancton-CH-SIB.2017.06.06> ."));

        refNode = refNodes.get(6);
        assertThat(refNode.toString(), is("<http://www.snib.mx/iptconabio/eml.do?r=SNIB-ME006-ME0061704F-ictioplancton-CH-SIB.2017.06.06> <http://purl.org/dc/elements/1.1/format> \"application/eml\" ."));

        refNode = refNodes.get(7);
        assertThat(refNode.toString(), endsWith("<http://www.w3.org/ns/prov#wasDerivedFrom> <http://www.snib.mx/iptconabio/eml.do?r=SNIB-ME006-ME0061704F-ictioplancton-CH-SIB.2017.06.06> ."));

        refNode = refNodes.get(8);
        assertThat(refNode.toString(), endsWith("<http://www.w3.org/ns/prov#hadMember> <d0df772d-78f4-4602-acf2-7d768798f632> ."));

        Triple lastRefNode = refNodes.get(refNodes.size() - 2);
        assertThat(lastRefNode.toString(), is("<https://api.gbif.org/v1/dataset?offset=2&limit=2> <http://purl.org/dc/elements/1.1/format> \"application/json\" ."));

        lastRefNode = refNodes.get(refNodes.size() - 1);
        assertThat(lastRefNode.toString(), endsWith("<http://www.w3.org/ns/prov#wasDerivedFrom> <https://api.gbif.org/v1/dataset?offset=2&limit=2> ."));

    }

    private IRI createTestNode() {
        try {
            return RefNodeFactory.toIRI(getClass().getResource("gbifdatasets.json").toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }


}