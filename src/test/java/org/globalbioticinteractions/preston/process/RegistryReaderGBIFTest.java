package org.globalbioticinteractions.preston.process;

import org.globalbioticinteractions.preston.Seeds;
import org.globalbioticinteractions.preston.model.RefNode;
import org.globalbioticinteractions.preston.model.RefNodeRelation;
import org.globalbioticinteractions.preston.model.RefNodeString;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class RegistryReaderGBIFTest {

    @Test
    public void onSeed() {
        ArrayList<RefNodeRelation> nodes = new ArrayList<>();
        RegistryReaderGBIF registryReaderGBIF = new RegistryReaderGBIF(nodes::add);
        RefNodeString bla = new RefNodeString("bla");
        registryReaderGBIF.on(new RefNodeRelation(bla, bla, Seeds.SEED_NODE_GBIF));
        Assert.assertThat(nodes.size(), is(2));
    }


    @Test
    public void parseDatasets() throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream("gbifdatasets.json");

        final List<RefNodeRelation> refNodes = new ArrayList<>();

        RegistryReaderGBIF.parse(resourceAsStream, refNodes::add, new RefNodeString("description"));

        assertThat(refNodes.size(), is(11));

        RefNodeRelation refNode = refNodes.get(0);
        assertThat(refNode.getLabel(), is("[description]-[:http://example.org/hasPart]->[6555005d-4594-4a3e-be33-c70e587b63d7]"));

        refNode = refNodes.get(1);
        assertThat(refNode.getLabel(), is("[6555005d-4594-4a3e-be33-c70e587b63d7]-[:http://example.org/hasPart]->[http://www.snib.mx/iptconabio/archive.do?r=SNIB-ME006-ME0061704F-ictioplancton-CH-SIB.2017.06.06]"));

        refNode = refNodes.get(2);
        assertThat(refNode.getLabel(), is("[http://www.snib.mx/iptconabio/archive.do?r=SNIB-ME006-ME0061704F-ictioplancton-CH-SIB.2017.06.06]-[:http://example.com/hasContent]->[?]"));

        refNode = refNodes.get(3);
        assertThat(refNode.getLabel(), is("[6555005d-4594-4a3e-be33-c70e587b63d7]-[:http://example.org/hasPart]->[http://www.snib.mx/iptconabio/eml.do?r=SNIB-ME006-ME0061704F-ictioplancton-CH-SIB.2017.06.06]"));

        RefNodeRelation thirdRefNode = refNodes.get(4);
        assertThat(thirdRefNode.getLabel(), is("[http://www.snib.mx/iptconabio/eml.do?r=SNIB-ME006-ME0061704F-ictioplancton-CH-SIB.2017.06.06]-[:http://example.com/hasContent]->[?]"));

        thirdRefNode = refNodes.get(5);
        assertThat(thirdRefNode.getLabel(), is("[description]-[:http://example.org/hasPart]->[d0df772d-78f4-4602-acf2-7d768798f632]"));

        RefNodeRelation lastRefNode = refNodes.get(refNodes.size() - 1);
        assertThat(lastRefNode.getLabel(), is("[description]-[:http://example.org/hasPart]->[https://api.gbif.org/v1/dataset?offset=2&limit=2]"));

    }


}