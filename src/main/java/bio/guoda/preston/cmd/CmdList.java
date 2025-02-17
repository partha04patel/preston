package bio.guoda.preston.cmd;

import bio.guoda.preston.StatementLogFactory;
import bio.guoda.preston.store.BlobStoreAppendOnly;
import bio.guoda.preston.store.BlobStore;
import bio.guoda.preston.store.StatementStore;
import bio.guoda.preston.store.StatementStoreImpl;
import com.beust.jcommander.Parameters;

import static bio.guoda.preston.cmd.ReplayUtil.attemptReplay;

@Parameters(separators = "= ", commandDescription = "show biodiversity dataset provenance logs")
public class CmdList extends LoggingPersisting implements Runnable {

    @Override
    public void run() {
        run(new LogErrorHandler() {
            @Override
            public void handleError() {
                System.exit(0);
            }
        });
    }

    public void run(LogErrorHandler handler) {
        attemptReplay(
                new BlobStoreAppendOnly(getKeyValueStore()),
                new StatementStoreImpl(getKeyValueStore()),
                StatementLogFactory.createPrintingLogger(getLogMode(), System.out, handler));
    }

}
