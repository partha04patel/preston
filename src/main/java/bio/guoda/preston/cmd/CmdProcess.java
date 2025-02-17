package bio.guoda.preston.cmd;

import bio.guoda.preston.process.EmittingStreamRDF;
import bio.guoda.preston.process.StatementEmitter;
import bio.guoda.preston.process.StatementListener;
import bio.guoda.preston.store.BlobStore;
import com.beust.jcommander.Parameters;
import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.Triple;

import java.util.Queue;

/**
 * Command to (re-) process biodiversity dataset graph by providing some existing provenance logs.
 * <p>
 * Only considers already tracked datasets and their provenance.
 * <p>
 * See https://github.com/bio-guoda/preston/issues/15 .
 */

@Parameters(separators = "= ", commandDescription = "(re-)process tracked biodiversity dataset graph using stdin")
public class CmdProcess extends CmdActivity {

    @Override
    void initQueue(Queue<Triple> statementQueue, ActivityContext ctx) {
        // no initial seeds
    }

    @Override
    void processQueue(Queue<Triple> statementQueue, BlobStore blobStore, ActivityContext ctx, StatementListener[] listeners) {
        handleQueuedMessages(statementQueue, listeners);

        StatementEmitter emitter = new StatementEmitter() {
            @Override
            public void emit(Triple statement) {
                handleStatement(statement, listeners);
                handleQueuedMessages(statementQueue, listeners);
            }
        };
        new EmittingStreamRDF(emitter).parseAndEmit(System.in);

    }

    @Override
    String getActivityDescription() {
        return "An event that (re-) processes existing biodiversity datasets graphs and their provenance.";
    }


    private void handleStatement(Triple statement, StatementListener[] listeners) {
        if (!(statement.getSubject() instanceof BlankNode) && !(statement.getObject() instanceof BlankNode)) {
            for (StatementListener listener : listeners) {
                listener.on(statement);
            }
        }
    }

    private void handleQueuedMessages(Queue<Triple> statementQueue1, StatementListener[] listeners) {
        while (!statementQueue1.isEmpty()) {
            Triple polled = statementQueue1.poll();
            handleStatement(polled, listeners);
        }
    }

}
