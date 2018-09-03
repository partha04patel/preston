package org.globalbioticinteractions.preston.process;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFTerm;
import org.globalbioticinteractions.preston.model.RefNodeFactory;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class StatementLoggerTSVTest {

    @Test
    public void relation() {
        IRI source = RefNodeFactory.toIRI("source");
        IRI relation = RefNodeFactory.toIRI("relation");
        RDFTerm target = RefNodeFactory.toDateTime("2018-01-01");


        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new StatementLoggerTSV(new PrintStream(out)).on(RefNodeFactory.toStatement(source, relation, target));


        assertThat(StringUtils.toEncodedString(out.toByteArray(), StandardCharsets.UTF_8),
                is("source\trelation\t2018-01-01\n"));
    }


}