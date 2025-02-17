package bio.guoda.preston;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.apache.commons.rdf.api.IRI;
import bio.guoda.preston.model.RefNodeFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Hasher {

    public static IRI calcSHA256(String content) {
        try {
            return calcSHA256(IOUtils.toInputStream(content, StandardCharsets.UTF_8), new NullOutputStream());
        } catch (IOException e) {
            throw new IllegalStateException("unexpected failure of hash calculation", e);
        }
    }

    public static IRI calcSHA256(InputStream is, OutputStream os) throws IOException {
        return calcSHA256(is, os, true);
    }

    public static IRI calcSHA256(InputStream is, OutputStream os, boolean shouldCloseInputStream) throws IOException {
        try {
            MessageDigest md = createDigest(is, os, shouldCloseInputStream);
            String format = String.format("%064x", new BigInteger(1, md.digest()));
            return toHashURI(format);
        } catch (IOException | NoSuchAlgorithmException var9) {
            throw new IOException("failed to cache dataset", var9);
        }
    }

    private static MessageDigest createDigest(InputStream is, OutputStream os, boolean shouldCloseInputStream) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        DigestInputStream digestInputStream = new DigestInputStream(is, md);
        IOUtils.copy(digestInputStream, os);
        if (shouldCloseInputStream) {
            digestInputStream.close();
        }
        os.flush();
        os.close();
        return md;
    }

    public static IRI toHashURI(String sha256Hash) {
        return RefNodeFactory.toIRI(URI.create(getHashPrefix() + sha256Hash));
    }

    public static String getHashPrefix() {
        return "hash://sha256/";
    }
}
