package de.cyklon.zipcomperator;

import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipComparator {

    private final File first_zip;
    private final File second_zip;

    private final Progress progress;

    public ZipComparator(File first_zip, File second_zip) {
        this.first_zip = first_zip;
        this.second_zip = second_zip;
        this.progress = Progress.create();
    }

    public double getProgress() {
        return progress.calculate();
    }

    public List<String> compare() throws IOException, NoSuchAlgorithmException {
        List<String> z1_contents, z2_contents;
        try (ZipFile zip = new ZipFile(first_zip)) {
            z1_contents = getContents(zip);
        }
        try (ZipFile zip = new ZipFile(second_zip)) {
            z2_contents = getContents(zip);
        }

        List<String> dif;
        try (ZipFile zip1 = new ZipFile(first_zip); ZipFile zip2 = new ZipFile(second_zip)) {
            dif = compareContents(zip1, zip2, z1_contents, z2_contents);
        }
        return dif;
    }

    private List<String> compareContents(ZipFile zip1, ZipFile zip2, List<String> j1_contents, List<String> j2_contents) throws NoSuchAlgorithmException, IOException {
        final String HASH_ALGORITHM = "SHA-256";
        MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
        int max = j1_contents.size();
        int current = 0;
        for (String s : j1_contents) {
            current++;
            if (j2_contents.contains(s)) {
                ZipEntry j1_entry = zip1.getEntry(s);
                ZipEntry j2_entry = zip2.getEntry(s);
                if (j1_entry!=null && j2_entry!=null) {
                    byte[] j1_hash = hashZipEntry(zip1, j1_entry, md);
                    byte[] j2_hash = hashZipEntry(zip2, j2_entry, md);
                    if (Arrays.equals(j1_hash, j2_hash)) {
                        j2_contents.remove(s);
                    }
                }
            }
            progress.setComparing((double) current/max);
        }
        return j2_contents;
    }

    private byte[] hashZipEntry(ZipFile zip, ZipEntry entry, MessageDigest md) throws IOException {
        try (InputStream in = zip.getInputStream(entry); DigestInputStream din = new DigestInputStream(in, md)) {
            while (din.read()!=-1) {}
            return md.digest();
        }
    }

    private List<String> getContents(ZipFile zip) {
        List<String> contents = new ArrayList<>();
        int size = zip.size();
        int curr = 0;
        double pr = 0d;
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            curr++;
            contents.add(entries.nextElement().getName());
            pr = (double) curr/size;
            progress.setCollecting(pr);
        }
        if (pr!=1) progress.setCollecting(1);
        return contents
                .stream()
                .filter(s -> !s.endsWith("/"))
                .collect(Collectors.toList());
    }

    @Setter
    static class Progress {

        private Progress() {

        }

        public static Progress create() {
            return new Progress();
        }


        private double collecting = 0;
        private double collecting_first = 0;
        private double collecting_second = 0;
        private double comparing = 0;

        public void setCollecting(double collecting) {
            if (collecting==1) {
                if (collecting_first==0) {
                    collecting_first = 1;
                    collecting = 0;
                } else if (collecting_first==1 && collecting_second==0) {
                    collecting_second = 1;
                    collecting = 0;
                }
            }
            this.collecting = collecting;
        }

        private double getCollecting() {
            return ((collecting_first==0 ? collecting : collecting_first) + ((collecting_second==0 && collecting_first==1) ? collecting : collecting_second)) / 2;
        }

        public double calculate() {
            return (getCollecting() + comparing) / 2;
        }

    }

}
