package org.globalbioticinteractions.preston;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class HasherTest {

    @Test
    public void testSHA256() throws IOException {
        assertSHA(Hasher.calcSHA256(IOUtils.toInputStream("something", StandardCharsets.UTF_8), new ByteArrayOutputStream()));
    }

    private void assertSHA(String calculated) {
        assertThat(calculated, is("3fc9b689459d738f8c88a3a48aa9e33542016b7a4052e001aaa536fca74813cb"));
        assertThat(calculated.length(), is(64));
    }


}