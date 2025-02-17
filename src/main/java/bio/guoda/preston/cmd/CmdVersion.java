package bio.guoda.preston.cmd;

import com.beust.jcommander.Parameters;
import bio.guoda.preston.Preston;

@Parameters(separators = "= ", commandDescription = "show version")
public class CmdVersion implements Runnable {

    @Override
    public void run() {
        System.out.println(Preston.getVersion());
    }

}
