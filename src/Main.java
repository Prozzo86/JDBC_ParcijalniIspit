import database.DatabaseService;

import java.sql.*;
import java.util.Scanner;

public class Main {
    private static Connection connection;

    public static void main(String[] args) {
        try {
           
            connection = DatabaseService.createConnection();
            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.println("Ovo su opcije školskog sustava:\n");
                System.out.println("1. Unesi novog učenika");
                System.out.println("2. Pretraži razred po imenu");
                System.out.println("3. Prebaci učenika u drugi razred");
                System.out.println("4. Ispiši sve učenike za određeni razred");
                System.out.println("5. Obriši učenika");
                System.out.println("6. Update imena i prezimena nastavnika");
                System.out.println("7. Izlaz");
                System.out.println("Odaberite opciju:\n");

                int odabir = scanner.nextInt();
                scanner.nextLine();

                switch (odabir) {
                    case 1:
                        unesiNovogUcenika(scanner);
                        break;
                    case 2:
                        pretraziRazredPoImenu(scanner);
                        break;
                    case 3:
                        prebaciUcenikaUDrugiRazred(scanner);
                        break;
                    case 4:
                        ispisiSveUcenikeZaRazred(scanner);
                        break;
                    case 5:
                        obrisiUcenika(scanner);
                        break;
                    case 6:
                        updateNastavnika(scanner);
                        break;
                    case 7:
                        System.out.println("Izlaz iz aplikacije.");
                        connection.close();
                        return;
                    default:
                        System.out.println("Nevažeći odabir.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void unesiNovogUcenika(Scanner scanner) throws SQLException {
        System.out.println("Unesite ime učenika:");
        String ime = scanner.nextLine();
        System.out.println("Unesite prezime učenika:");
        String prezime = scanner.nextLine();
        System.out.println("Unesite ID razreda:");
        int razredId = scanner.nextInt();
        scanner.nextLine();


        if (!postojiRazred(razredId)) {
            System.out.println("Razred s ID-om " + razredId + " ne postoji. Unesite naziv novog razreda:");
            String nazivRazreda = scanner.nextLine();
            System.out.println("Unesite ID nastavnika za ovaj razred:");
            int nastavnikId = scanner.nextInt();
            scanner.nextLine(); // Očisti buffer
            razredId = unesiNoviRazred(nazivRazreda, nastavnikId);
        }

        String sql = "INSERT INTO Ucenik (Ime, Prezime, RazredID) VALUES (?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ime);
            statement.setString(2, prezime);
            statement.setInt(3, razredId);
            statement.executeUpdate();
            System.out.println("Učenik uspješno unesen.");
        }
    }


    private static boolean postojiRazred(int razredId) throws SQLException {
        String sql = "SELECT id FROM Razred WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, razredId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private static int unesiNoviRazred(String nazivRazreda, int nastavnikId) throws SQLException {
        String sql = "INSERT INTO Razred (Naziv, NastavnikId) OUTPUT INSERTED.id VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, nazivRazreda);
            statement.setInt(2, nastavnikId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt("id");
                }
            }
        }
        throw new SQLException("Neuspješan unos razreda.");
    }

    private static int dohvatiZadnjiUneseniRazredId() throws SQLException {
        String sql = "SELECT SCOPE_IDENTITY() AS lastId";
        try (Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery(sql)) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
            }
        }
        return -1;
    }

    private static void pretraziRazredPoImenu(Scanner scanner) throws SQLException {
        System.out.println("Unesite naziv razreda:");
        String naziv = scanner.nextLine();

        String sql = "SELECT * FROM Razred WHERE naziv = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, naziv);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    System.out.println("Razred pronađen: ID = " + resultSet.getInt("id") + ", Naziv = " + resultSet.getString("naziv"));
                } else {
                    System.out.println("Razred nije pronađen.");
                }
            }
        }
    }

    private static void prebaciUcenikaUDrugiRazred(Scanner scanner) throws SQLException {
        System.out.println("Unesite ID učenika:");
        int ucenikId = scanner.nextInt();
        System.out.println("Unesite trenutni ID razreda:");
        int trenutniRazredId = scanner.nextInt();
        System.out.println("Unesite novi ID razreda:");
        int noviRazredId = scanner.nextInt();
        scanner.nextLine();

        connection.setAutoCommit(false);

        try {
            String sql = "UPDATE Ucenik SET RazredID = ? WHERE id = ? AND RazredID = ?";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setInt(1, noviRazredId);
                statement.setInt(2, ucenikId);
                statement.setInt(3, trenutniRazredId);
                int affectedRows = statement.executeUpdate();

                if (affectedRows == 0) {
                    System.out.println("Učenik nije pronađen ili je već u traženom razredu.");
                    connection.rollback();
                } else {
                    connection.commit();
                    System.out.println("Učenik uspješno prebačen u novi razred.");
                }
            }
        } catch (SQLException e) {
            connection.rollback();
            e.printStackTrace();
        } finally {
            connection.setAutoCommit(true);
        }
    }

    private static void ispisiSveUcenikeZaRazred(Scanner scanner) throws SQLException {
        System.out.println("Unesite ID razreda:");
        int razredId = scanner.nextInt();
        scanner.nextLine();

        String sql = "SELECT Ucenik.ime, Ucenik.prezime, Razred.naziv, Nastavnik.ime AS nastavnik_ime, Nastavnik.prezime AS nastavnik_prezime " +
                "FROM Ucenik " +
                "JOIN Razred ON Ucenik.RazredID = Razred.id " +
                "JOIN Nastavnik ON Razred.NastavnikID = Nastavnik.id " +
                "WHERE Razred.id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, razredId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    System.out.println("Učenik: " + resultSet.getString("ime") + " " + resultSet.getString("prezime") +
                            ", Razred: " + resultSet.getString("naziv") +
                            ", Nastavnik: " + resultSet.getString("nastavnik_ime") + " " + resultSet.getString("nastavnik_prezime"));
                }
            }
        }
    }

    private static void obrisiUcenika(Scanner scanner) throws SQLException {
        ispisiSveUcenikeZaRazred(scanner);
        System.out.println("Unesite ID učenika kojeg želite obrisati:");
        int ucenikId = scanner.nextInt();
        scanner.nextLine(); // Očisti buffer

        String sql = "DELETE FROM Ucenik WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, ucenikId);
            int affectedRows = statement.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Učenik uspješno obrisan.");
            } else {
                System.out.println("Učenik nije pronađen.");
            }
        }
    }

    private static void updateNastavnika(Scanner scanner) throws SQLException {
        System.out.println("Unesite ID nastavnika:");
        int nastavnikId = scanner.nextInt();
        scanner.nextLine(); // Očisti buffer
        System.out.println("Unesite novo ime nastavnika:");
        String novoIme = scanner.nextLine();
        System.out.println("Unesite novo prezime nastavnika:");
        String novoPrezime = scanner.nextLine();

        String sql = "UPDATE Nastavnik SET ime = ?, prezime = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, novoIme);
            statement.setString(2, novoPrezime);
            statement.setInt(3, nastavnikId);
            int affectedRows = statement.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Nastavnik uspješno ažuriran.");
            } else {
                System.out.println("Nastavnik nije pronađen.");
            }
        }
    }
}