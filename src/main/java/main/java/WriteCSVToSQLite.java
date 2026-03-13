package main.java;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class WriteCSVToSQLite {

    private static final String CSV_SPLIT_REGEX = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

    private Connection connection;
    private String[] headers;

    public static void main(String[] args) {
        WriteCSVToSQLite writer = new WriteCSVToSQLite();
        String csvFilePath = "messages.csv"; // Change to your CSV file path

        try {
            writer.initDatabase(csvFilePath);
            writer.readCSVAndWriteToDatabase(csvFilePath);
            writer.closeConnection();
            System.out.println("CSV data successfully written to SQLite database.");
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    private String cleanHeader(String raw) {
        String h = raw.trim();
        if (h.startsWith("\"") && h.endsWith("\"")) {
            h = h.substring(1, h.length() - 1).replace("\"\"", "\"");
        }
        return h;
    }

    private void initDatabase(String csvFilePath) throws SQLException, IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                throw new IOException("CSV file is empty");
            }
            String[] rawHeaders = headerLine.split(CSV_SPLIT_REGEX);
            headers = new String[rawHeaders.length];
            for (int i = 0; i < rawHeaders.length; i++) {
                headers[i] = cleanHeader(rawHeaders[i]);
            }
        }

        StringBuilder sql = new StringBuilder("CREATE TABLE IF NOT EXISTS csv_data (\n");
        sql.append("  id INTEGER PRIMARY KEY AUTOINCREMENT");
        for (String header : headers) {
            sql.append(",\n  \"").append(header).append("\" TEXT");
        }
        sql.append("\n);");

        connection = DriverManager.getConnection("jdbc:sqlite:messages.db");
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql.toString());
        }
    }

    private void readCSVAndWriteToDatabase(String csvFilePath) throws IOException, SQLException {
        StringBuilder sql = new StringBuilder("INSERT INTO csv_data(");
        for (int i = 0; i < headers.length; i++) {
            sql.append("\"").append(headers[i]).append("\"");
            if (i < headers.length - 1) sql.append(", ");
        }
        sql.append(") VALUES(");
        for (int i = 0; i < headers.length; i++) {
            sql.append("?");
            if (i < headers.length - 1) sql.append(", ");
        }
        sql.append(")");

        try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath));
             PreparedStatement pstmt = connection.prepareStatement(sql.toString())) {

            br.readLine(); // skip header row

            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(CSV_SPLIT_REGEX);
                for (int i = 0; i < headers.length; i++) {
                    if (i < values.length) {
                        pstmt.setString(i + 1, cleanHeader(values[i]));
                    } else {
                        pstmt.setString(i + 1, null);
                    }
                }
                pstmt.executeUpdate();
            }
        }
    }

    private void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

}
