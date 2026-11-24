import java.io.*;
import java.util.*;


public class MovieTicketBooking {

    static final int ROWS = 5;
    static final int COLS = 5;
    static final String FILE_NAME = "tickets.txt";
    static final int FIRST_ID = 101;

    // ---------- Data classes ----------
    static class Booking {
        String id, name, type;
        int row, col;

        Booking(String id, String name, String type, int row, int col) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.row = row;
            this.col = col;
        }

        String seat() {
            return "" + (char) ('A' + row) + (col + 1);
        }
    }

    static class Waiting {
        String name, type;
        long order;                         // arrival number, keeps FIFO inside a type

        Waiting(String name, String type, long order) {
            this.name = name;
            this.type = type;
            this.order = order;
        }

        int priority() {
            return type.equals("Gold") ? 0 : 1;   // 0 = highest priority
        }
    }

    static final Comparator<Waiting> WAIT_ORDER = (a, b) -> {
        if (a.priority() != b.priority()) return Integer.compare(a.priority(), b.priority());
        return Long.compare(a.order, b.order);
    };

    // ---------- Data structures ----------
    static Booking[][] seats = new Booking[ROWS][COLS];                 // 2D array
    static HashMap<String, Booking> bookings = new HashMap<>();         // ID -> booking
    static PriorityQueue<Waiting> waitingList = new PriorityQueue<>(WAIT_ORDER);
    static int nextId = FIRST_ID;
    static long arrivalCounter = 0;
    static Scanner sc = new Scanner(System.in);

    // ---------- Main ----------
    public static void main(String[] args) {
        loadData();
        while (true) {
            System.out.println("\n===== MOVIE TICKET BOOKING =====");
            System.out.println("1. View seats");
            System.out.println("2. Book seat");
            System.out.println("3. Cancel booking");
            System.out.println("4. View waiting list");
            System.out.println("5. Exit");
            System.out.print("Enter choice: ");
            String line = readLine();
            if (line == null) { saveData(); break; }       // end of input
            switch (line.trim()) {
                case "1": viewSeats(); break;
                case "2": bookSeat(); break;
                case "3": cancelBooking(); break;
                case "4": viewWaitingList(); break;
                case "5":
                    saveData();
                    System.out.println("Data saved to " + FILE_NAME + ". Goodbye!");
                    return;
                default: System.out.println("Invalid choice. Enter 1-5.");
            }
        }
    }

    // ---------- Features ----------
    static void viewSeats() {
        System.out.println("\n        SCREEN THIS WAY");
        System.out.println("     1   2   3   4   5");
        for (int r = 0; r < ROWS; r++) {
            StringBuilder sb = new StringBuilder("  " + (char) ('A' + r) + "  ");
            for (int c = 0; c < COLS; c++) {
                sb.append(seats[r][c] == null ? "[ ] " : "[X] ");
            }
            System.out.println(sb);
        }
        System.out.println("[ ] free   [X] booked   Free seats: " + freeSeats());
    }

    static void bookSeat() {
        System.out.print("Name: ");
        String name = readLine();
        if (name == null || name.trim().isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }
        name = name.trim();

        System.out.print("Customer type (Gold/Regular): ");
        String type = normaliseType(readLine());
        if (type == null) {
            System.out.println("Invalid type. Choose Gold or Regular.");
            return;
        }

        if (freeSeats() == 0) {                            // hall full -> waiting list
            waitingList.add(new Waiting(name, type, ++arrivalCounter));
            System.out.println("Hall full. " + name + " added to waiting list (" + type + ").");
            saveData();
            return;
        }

        System.out.print("Seat (e.g. B3): ");
        int[] pos = parseSeat(readLine());
        if (pos == null) {
            System.out.println("Invalid seat. Choose rows A-E and seats 1-5.");
            return;
        }
        if (seats[pos[0]][pos[1]] != null) {
            System.out.println("Seat already booked. Choose another seat.");
            return;
        }

        Booking b = confirm(name, type, pos[0], pos[1]);
        System.out.println("Booking confirmed. ID: " + b.id + " | " + b.name + " | Seat " + b.seat());
        if (freeSeats() == 0) System.out.println("Hall is now full.");
        saveData();
    }

    static void cancelBooking() {
        System.out.print("Enter booking ID: ");
        String id = readLine();
        Booking b = (id == null) ? null : bookings.get(id.trim().toUpperCase());   // HashMap lookup
        if (b == null) {
            System.out.println("Booking ID not found.");
            return;
        }
        bookings.remove(b.id);
        seats[b.row][b.col] = null;
        System.out.println("Booking " + b.id + " cancelled. Seat " + b.seat() + " is free.");

        Waiting w = waitingList.poll();                    // highest priority customer
        if (w != null) {
            Booking nb = confirm(w.name, w.type, b.row, b.col);
            System.out.println("Seat " + nb.seat() + " given to " + w.name + " (" + w.type
                    + "). New ID: " + nb.id);
        }
        saveData();
    }

    static void viewWaitingList() {
        if (waitingList.isEmpty()) {
            System.out.println("Waiting list is empty.");
            return;
        }
        List<Waiting> list = new ArrayList<>(waitingList);
        Collections.sort(list, WAIT_ORDER);                // sorting: Gold first, then arrival
        System.out.println("Waiting list (in serving order):");
        int n = 1;
        for (Waiting w : list) {
            System.out.println(n++ + ". " + w.name + " (" + w.type + ")");
        }
    }

    // ---------- Helper methods ----------
    static Booking confirm(String name, String type, int r, int c) {
        Booking b = new Booking("T" + nextId++, name, type, r, c);
        seats[r][c] = b;
        bookings.put(b.id, b);
        return b;
    }

    static int freeSeats() {
        int count = 0;
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                if (seats[r][c] == null) count++;
        return count;
    }

    static int[] parseSeat(String s) {
        if (s == null) return null;
        s = s.trim().toUpperCase();
        if (s.length() != 2) return null;
        int r = s.charAt(0) - 'A';
        int c = s.charAt(1) - '1';
        if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return null;
        return new int[]{r, c};
    }

    static String normaliseType(String s) {
        if (s == null) return null;
        s = s.trim().toLowerCase();
        if (s.equals("gold") || s.equals("g")) return "Gold";
        if (s.equals("regular") || s.equals("r")) return "Regular";
        return null;
    }

    static String readLine() {
        return sc.hasNextLine() ? sc.nextLine() : null;
    }

    // ---------- File handling ----------
    // BOOKING,id,name,type,seat   |   WAIT,name,type,order
    static void saveData() {
        List<Booking> list = new ArrayList<>(bookings.values());
        Collections.sort(list, Comparator.comparingInt(b -> Integer.parseInt(b.id.substring(1))));
        List<Waiting> wl = new ArrayList<>(waitingList);
        Collections.sort(wl, WAIT_ORDER);
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (Booking b : list) pw.println("BOOKING," + b.id + "," + b.name + "," + b.type + "," + b.seat());
            for (Waiting w : wl) pw.println("WAIT," + w.name + "," + w.type + "," + w.order);
        } catch (IOException e) {
            System.out.println("Could not save file: " + e.getMessage());
        }
    }

    static void loadData() {
        File f = new File(FILE_NAME);
        if (!f.exists()) return;
        int maxId = FIRST_ID - 1;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split(",");
                if (p[0].equals("BOOKING") && p.length == 5) {
                    int[] pos = parseSeat(p[4]);
                    if (pos == null) continue;
                    Booking b = new Booking(p[1], p[2], p[3], pos[0], pos[1]);
                    seats[pos[0]][pos[1]] = b;
                    bookings.put(b.id, b);
                    maxId = Math.max(maxId, Integer.parseInt(p[1].substring(1)));
                } else if (p[0].equals("WAIT") && p.length == 4) {
                    long order = Long.parseLong(p[3]);
                    waitingList.add(new Waiting(p[1], p[2], order));
                    arrivalCounter = Math.max(arrivalCounter, order);
                }
            }
            nextId = maxId + 1;
        } catch (IOException | NumberFormatException e) {
            System.out.println("Could not read " + FILE_NAME + ": " + e.getMessage());
        }
    }
}