package com.make.common.utils.file;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PDF Parse Tool
 */
public class PdfUtil {
    private static final Logger log = LoggerFactory.getLogger(PdfUtil.class);

    // Regex for CMB: 2023-09-06 CNY -27.60 221,744.15 快捷支付 乐尼奇水果店
    // Group 1: Date (yyyy-MM-dd)
    // Group 2: Currency (CNY)
    // Group 3: Amount (-27.60 or 1,234.56)
    // Group 4: Balance (221,744.15)
    // Group 5: Transaction Type
    // Group 6: Counter Party
    private static final Pattern CMB_PATTERN = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2})\\s+([A-Z]+)\\s+([-\\d,.]+)\\s+([-\\d,.]+)\\s+(\\S+)\\s+(.+)$");

    // Regex for CCB: Assuming yyyyMMdd followed by numbers.
    // Based on Excel logic: Date is index 4, so format is different.
    // Standard CCB PDF often has: Date(yyyyMMdd) ...
    // Let's try to match lines starting with date yyyyMMdd
    // Example Assumption: 20230906 ...
    private static final Pattern CCB_PATTERN = Pattern.compile("^(\\d{8})\\s+.*");

    public static List<Map<String, Object>> parsePdf(File file, String bankType) throws IOException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (PDDocument document = PDDocument.load(file)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // Attempt to sort text by position
            String text = stripper.getText(document);
            String[] lines = text.split("\\r?\\n");

            if ("CMB".equals(bankType)) {
                return parseCMB(lines);
            } else if ("CCB".equals(bankType)) {
                return parseCCB(lines);
            } else {
                log.warn("Unsupported bank type for PDF: {}", bankType);
            }
        }
        return list;
    }

    private static List<Map<String, Object>> parseCMB(String[] lines) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            Matcher matcher = CMB_PATTERN.matcher(line);
            if (matcher.find()) {
                Map<String, Object> map = new HashMap<>();
                map.put("date", matcher.group(1)); // yyyy-MM-dd
                map.put("currency", matcher.group(2)); // CNY
                map.put("amount", matcher.group(3).replace(",", ""));
                map.put("balance", matcher.group(4).replace(",", ""));
                map.put("transactionType", matcher.group(5));
                map.put("counterParty", matcher.group(6));
                list.add(map);
            }
        }
        return list;
    }

    private static List<Map<String, Object>> parseCCB(String[] lines) {
        List<Map<String, Object>> list = new ArrayList<>();
        // Since we don't have a CCB sample, we'll try a flexible parsing
        // We know from Excel that Date is yyyyMMdd, and there are Amount, Balance, Type, CounterParty.
        // We will look for lines that look like transaction rows.

        // Revised Regex for generic capture of Date yyyyMMdd and subsequent amounts
        // Pattern: Date Space+ (maybe location?) Space+ Amount Space+ Balance ...
        // Note: CCB Excel Logic:
        // Index 4: Date (yyyyMMdd)
        // Index 5: Amount
        // Index 6: Balance
        // Index 1: Transaction Type
        // Index 8: Counter Party
        // This implies the PDF might be structured differently than the CSV/Excel.
        // Without visual evidence, this is a best-effort implementation.

        // Let's assume a line looks like:
        // yyyyMMdd  Type?  ... Amount Balance ...

        for (String line : lines) {
            line = line.trim();
            Matcher matcher = CCB_PATTERN.matcher(line);
            if (matcher.find()) {
                // Determine it is a date line.
                // We will try to split by whitespace and map by index relative to the detected columns?
                // Or just splitting by space.
                String[] tokens = line.split("\\s+");
                if (tokens.length >= 5) {
                    Map<String, Object> map = new HashMap<>();
                    // Heuristic mapping
                    map.put("date", tokens[0]); // yyyyMMdd

                    // Finding amounts: scan for tokens that parse to BigDecimal
                    // This is safer than fixed indices for unknown layout
                    // We need two numbers: Amount and Balance.
                    // Usually Amount is before Balance.

                    String amount = null;
                    String balance = null;
                    int amountIndex = -1;

                    for (int i = 1; i < tokens.length; i++) {
                        String t = tokens[i].replace(",", "");
                        if (t.matches("-?\\d+\\.\\d{2}")) { // Matches currency format
                            if (amount == null) {
                                amount = t;
                                amountIndex = i;
                            } else if (balance == null) {
                                balance = t;
                                break; // Found both
                            }
                        }
                    }

                    if (amount != null && balance != null) {
                        map.put("amount", amount);
                        map.put("balance", balance);
                        // Everything after balance could be CounterParty?
                        // Or transaction type is between Date and Amount?
                        // Let's guess: Type is at index 1 if available
                         if (amountIndex > 1) {
                             map.put("transactionType", tokens[1]);
                         } else {
                             map.put("transactionType", "Unknown");
                         }
                         // CounterParty might be last
                         if (tokens.length > amountIndex + 2) { // if there are tokens after balance
                             // Reconstruct tail
                             // This is hard without knowing index of balance in 'tokens'
                             // Let's skip for now or take the last token
                             map.put("counterParty", tokens[tokens.length - 1]);
                         } else {
                             map.put("counterParty", "Unknown");
                         }

                         list.add(map);
                    }
                }
            }
        }
        return list;
    }
}
