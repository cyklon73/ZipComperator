package de.cyklon.zipcomperator;

import io.github.cyklon73.cytils.utils.FileHelper;
import io.github.cyklon73.cytils.utils.Util;
import me.tongfei.progressbar.ProgressBar;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Scanner;

public class Main {

    private final static String RESET = "\u001B[0m";
    private final static String RED = "\u001B[31m";
    private final static String GREEN = "\u001B[32m";
    private final static String YELLOW = "\u001B[33m";


    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        Scanner scanner = new Scanner(System.in);
        String z1 = "";
        while (z1.isBlank()) {
            System.out.print("Enter a path to a zip/jar file > ");
            z1 = scanner.nextLine().trim();
        }
        String z2 = "";
        while (z2.isBlank()) {
            System.out.print("Enter a path to a other zip/jar file > ");
            z2 = scanner.nextLine().trim();
        }

        File zf1 = new File(z1);
        File zf2 = new File(z2);

        ZipComparator comparator = new ZipComparator(zf1, zf2);
        ProgressBar progressBar = new ProgressBar(String.format("%s | %s", zf1.getName(), zf2.getName()), 100);
        boolean[] finished = {false};
        new Thread(() -> {
            while (!finished[0]) {
                long progress = Math.round(comparator.getProgress()*100);
                progressBar.setExtraMessage(progress>=50 ? "Comparing" : "Collecting");
                progressBar.stepTo(progress);
            }
            progressBar.close();
        }).start();
        List<String> dif = comparator.compare();
        finished[0] = true;

        System.out.printf("\n%s  Successfully compared %s and %s", GREEN, zf1.getName(), zf2.getName());
        if (dif.size()==0) System.out.println(GREEN + "\n  No Differences Found!" + RESET);
        else System.out.printf("\n  %s%s%s Differences Found!%s", YELLOW, dif.size(), RED, RESET);

        console(dif, scanner);
    }

    private static void console(List<String> dif, Scanner scanner) {
        System.out.println("\n\n\n");
        boolean running = true;
        while (running) {
            Util.tryCatch(() -> Thread.sleep(50));
            System.out.print("> ");
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("close") || input.equalsIgnoreCase("exit")) {
                running = false;
            } else if (input.startsWith("-file") || input.startsWith("--file")) {
                String[] tokens = input.split(" ");
                if (tokens.length == 2) {
                    String filename = tokens[1];
                    File file = new File(filename);
                    if (file.exists() && file.isFile()) {
                        if (file.getName().endsWith(".txt")) {
                            StringBuilder sb = new StringBuilder();
                            for (String s : dif) {
                                sb.append("- ").append(s).append("\n");
                            }
                            try {
                                FileHelper.writeFile(sb.toString(), file);
                            } catch (IOException e) {
                                System.err.println("Error: " + e);
                            }
                        } else System.err.println("Error: Invalid file type, only .txt files allowed");
                    } else {
                        System.err.println("Error: file not found or is not a regular file");
                    }
                } else {
                    System.err.println("Error: no filename specified");
                }
            } else if (input.isBlank()) {
            } else {
                System.err.println("Error: invalid command");
            }
        }
        scanner.close();
    }

}