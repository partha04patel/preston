package org.globalbioticinteractions.preston.store;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AppendOnlyBlobStoreTest {

    @Test
    public void put() throws IOException {
        BlobStore blobStore = new AppendOnlyBlobStore(getTestPersistence());
        URI key = blobStore.putBlob(IOUtils.toInputStream("testing123", StandardCharsets.UTF_8));
        InputStream inputStream = blobStore.get(key);
        assertThat(TestUtil.toUTF8(inputStream), is("testing123"));
    }

    @Test
    public void putURI() throws IOException {
        BlobStore blobStore = new AppendOnlyBlobStore(getTestPersistence());
        URI key = blobStore.putBlob(URI.create("pesto:123"));
        InputStream inputStream = blobStore.get(key);
        assertThat(TestUtil.toUTF8(inputStream), is("pesto:123"));
    }

    public static Persistence getTestPersistence() {
        return new Persistence() {
            private final Map<String, String> lookup = new TreeMap<>();

            @Override
            public void put(String key, String value) throws IOException {
                if (lookup.containsKey(key) && !value.equals(lookup.get(key))) {
                    throw new IOException("can't overwrite with value [" + value + "]");
                }
                lookup.putIfAbsent(key, value);
            }

            @Override
            public String put(KeyGeneratingStream keyGeneratingStream, InputStream is) throws IOException {
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                String key = keyGeneratingStream.generateKeyWhileStreaming(is, os);
                String value = TestUtil.toUTF8(new ByteArrayInputStream(os.toByteArray()));
                lookup.putIfAbsent(key, value);
                return key;
            }

            @Override
            public InputStream get(String key) throws IOException {
                String input = lookup.get(key);
                return input == null ? null : IOUtils.toInputStream(input, StandardCharsets.UTF_8);
            }
        };
    }


}
