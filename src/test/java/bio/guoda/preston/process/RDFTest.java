package bio.guoda.preston.process;

import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Literal;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.simple.SimpleRDF;
import org.apache.commons.rdf.simple.Types;
import org.hamcrest.core.Is;
import org.junit.Test;

import static org.junit.Assert.assertThat;

public class RDFTest {


    @Test
    public void doit() {
        SimpleRDF simpleRDF = new SimpleRDF();
        BlankNode subject = simpleRDF.createBlankNode();
        String reference = subject.uniqueReference();
        IRI predicate = simpleRDF.createIRI("bla");
        IRI objcet = simpleRDF.createIRI("boo");

        Triple triple = simpleRDF.createTriple(subject, predicate, objcet);

        Graph graph = simpleRDF.createGraph();
        graph.add(triple);

        Literal date = simpleRDF.createLiteral("2018-01-01", Types.XSD_DATETIME);
        assertThat(date.toString(), Is.is("\"2018-01-01\"^^<http://www.w3.org/2001/XMLSchema#dateTime>"));
        assertThat(triple.toString(), Is.is("_:" + reference + " <bla> <boo> ."));
    }
}
