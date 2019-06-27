package bio.guoda.preston.process;

import bio.guoda.preston.cmd.ReplayUtil;
import org.apache.commons.rdf.api.Triple;
import bio.guoda.preston.RDFUtil;

import java.io.PrintStream;

public class StatementLoggerTSV implements StatementListener {

    private final PrintStream out;

    public StatementLoggerTSV(PrintStream printWriter) {
        this.out = printWriter;
    }

    @Override
    public void on(Triple statement) {
        ReplayUtil.throwOnError(out);
        String subject = RDFUtil.getValueFor(statement.getSubject());
        String predicate = RDFUtil.getValueFor(statement.getPredicate());
        String object = RDFUtil.getValueFor(statement.getObject());
        out.println(subject + "\t" + predicate + "\t" + object);
    }

}
