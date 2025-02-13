import database.DatabaseService;

import java.sql.*;
import java.util.Scanner;

public class Main {
    private static Connection connection;
    private static final String URL = "jdbc:sqlserver://localhost:1433;databaseName=JavaAdv;encrypt=true;trustServerCertificate=true";
    private static final String USER = "root";
    private static final String PASSWORD = "password";

    public static void main(String[] args) {

            connection = DatabaseService.createConnection();
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("\nOdaberite opciju:");
                System.out.println("1 - Unesi novog polaznika");
                System.out.println("2 - Unesi novi program obrazovanja");
                System.out.println("3 - Upisi polaznika na program obrazovanja");
                System.out.println("4 - Prebaci polaznika iz jednog u drugi program obrazovanja");
                System.out.println("5 - Prikazi polaznike određenog programa");
                System.out.println("0 - Izlaz");
                System.out.println("Unos: ");

                int opcija = scanner.nextInt();
                scanner.nextLine();

                switch (opcija) {
                    case 1 -> unesiNovogPolaznika(scanner);
                    case 2 -> unesiNoviProgram(scanner);
                    case 3 -> upisiPolaznikaNaProgram(scanner);
                    case 4 -> prebaciPolaznika(scanner);
                    case 5 -> prikaziPolaznikePrograma(scanner);
                    case 0 -> {
                        System.out.println("Izlaz iz programa.");
                        scanner.close();
                        return;
                    }
                    default -> System.out.println("Nepoznata opcija, pokušajte ponovno.");
                }
            }
        }


    private static void unesiNovogPolaznika(Scanner scanner) {
        System.out.print("Unesite ime polaznika: ");
        String ime = scanner.nextLine();
        System.out.print("Unesite prezime polaznika: ");
        String prezime = scanner.nextLine();

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             CallableStatement stmt = conn.prepareCall("{CALL UnesiNovogPolaznika(?, ?)}")) {
            stmt.setString(1, ime);
            stmt.setString(2, prezime);
            stmt.execute();
            System.out.println("Polaznik uspješno dodan.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static void unesiNoviProgram(Scanner scanner) {
        System.out.print("Unesite naziv programa: ");
        String naziv = scanner.nextLine();
        System.out.print("Unesite CSVET bodove: ");
        int csvet = scanner.nextInt();
        scanner.nextLine();

        String sql = "INSERT INTO ProgramObrazovanja (Naziv, CSVET) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, naziv);
            stmt.setInt(2, csvet);
            stmt.executeUpdate();
            System.out.println("Program obrazovanja uspješno dodan.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static void upisiPolaznikaNaProgram(Scanner scanner) {
        System.out.print("Unesite ID polaznika: ");
        int polaznikID = scanner.nextInt();
        System.out.print("Unesite ID programa obrazovanja: ");
        int programID = scanner.nextInt();
        scanner.nextLine();

        String sql = "INSERT INTO Upis (IDPolaznik, IDProgramObrazovanja) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, polaznikID);
            stmt.setInt(2, programID);
            stmt.executeUpdate();
            System.out.println("Polaznik uspješno upisan na program obrazovanja.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static void prebaciPolaznika(Scanner scanner) {
        System.out.print("Unesite ID polaznika: ");
        int polaznikID = scanner.nextInt();
        System.out.print("Unesite ID starog programa: ");
        int stariProgramID = scanner.nextInt();
        System.out.print("Unesite ID novog programa: ");
        int noviProgramID = scanner.nextInt();
        scanner.nextLine();

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             CallableStatement stmt = conn.prepareCall("{CALL PrebaciPolaznika(?, ?, ?)}")) {
            stmt.setInt(1, polaznikID);
            stmt.setInt(2, stariProgramID);
            stmt.setInt(3, noviProgramID);
            stmt.execute();
            System.out.println("Polaznik uspješno prebačen u novi program.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    private static void prikaziPolaznikePrograma(Scanner scanner) {
        System.out.print("Unesite ID programa obrazovanja: ");
        int programID = scanner.nextInt();
        scanner.nextLine();

        String sql = "SELECT p.Ime, p.Prezime, po.Naziv, po.CSVET " +
                "FROM Upis u " +
                "JOIN Polaznik p ON u.IDPolaznik = p.PolaznikID " +
                "JOIN ProgramObrazovanja po ON u.IDProgramObrazovanja = po.ProgramObrazovanjaID " +
                "WHERE po.ProgramObrazovanjaID = ?";

        try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, programID);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                System.out.println("Ime: " + rs.getString("Ime") +
                        ", Prezime: " + rs.getString("Prezime") +
                        ", Program: " + rs.getString("Naziv") +
                        ", CSVET: " + rs.getInt("CSVET"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}