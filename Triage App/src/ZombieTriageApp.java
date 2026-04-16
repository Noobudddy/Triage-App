import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.*;
import java.util.*;

// ========== PATIENT CLASS ==========
class Patient implements Comparable<Patient> {
    private String name;
    private String condition;
    private int age;
    private int arrivalOrder;
    private LocalDate arrivalDate;
    private static int counter = 0;

    // Valid conditions
    private static final Set<String> VALID_CONDITIONS = new HashSet<>(Arrays.asList(
            "mutating", "bleeding", "fever", "infected", "scared", "full zombie"
    ));

    public Patient(String name, String condition, int age) throws IllegalArgumentException {
        // Input validation
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Patient name cannot be null or empty");
        }

        if (age < 0 || age > 150) {
            throw new IllegalArgumentException("Age must be between 0 and 150");
        }

        String normalizedCondition = condition.toLowerCase().trim();
        if (!VALID_CONDITIONS.contains(normalizedCondition)) {
            throw new IllegalArgumentException("Invalid condition: " + condition +
                    ". Valid conditions: " + VALID_CONDITIONS);
        }

        this.name = name.trim();
        this.condition = normalizedCondition;
        this.age = age;
        this.arrivalOrder = counter++;
        this.arrivalDate = LocalDate.now();
    }

    // Calculate priority score (LOWER score = HIGHER priority)
    private int getConditionPriority() {
        switch (condition) {
            case "mutating": return 1;      // Highest priority
            case "bleeding": return 2;       // Bleeding non-stop
            case "fever": return 3;          // Infection fever
            case "infected": return 4;       // Bite/infected
            case "scared": return 6;         // Not infected (least)
            default: return 5;
        }
    }

    // Age modifier: SUBTRACT from score for higher priority (lower score = better)
    private int getAgeModifier() {
        if (age < 25) return -1;      // Young adults get BETTER priority (subtract 1)
        if (age >= 25 && age <= 50) return 0;  // Neutral
        return 1;                     // Elderly get WORSE priority (add 1)
    }

    @Override
    public int compareTo(Patient other) {
        // Full zombies always go to the end (to be rejected)
        if (this.condition.equals("full zombie") && !other.condition.equals("full zombie")) {
            return 1;
        }
        if (!this.condition.equals("full zombie") && other.condition.equals("full zombie")) {
            return -1;
        }
        if (this.condition.equals("full zombie") && other.condition.equals("full zombie")) {
            return 0;
        }

        // Calculate final priority scores
        int thisScore = this.getConditionPriority() + this.getAgeModifier();
        int otherScore = other.getConditionPriority() + other.getAgeModifier();

        // FIXED: Properly handles cases where age overrides condition
        if (thisScore != otherScore) {
            return Integer.compare(thisScore, otherScore);
        }

        // If equal priority, FIFO by arrival order
        return Integer.compare(this.arrivalOrder, other.arrivalOrder);
    }

    public boolean isFullZombie() {
        return condition.equals("full zombie");
    }

    public String getName() { return name; }
    public String getCondition() { return condition; }
    public int getAge() { return age; }
    public LocalDate getArrivalDate() { return arrivalDate; }
    public int getArrivalOrder() { return arrivalOrder; }

    @Override
    public String toString() {
        String priorityMarker = getPriorityMarker();
        return String.format("%s%-15s | Age: %-3d | Condition: %-15s",
                priorityMarker, name, age, condition);
    }

    private String getPriorityMarker() {
        int score = getConditionPriority() + getAgeModifier();
        if (score <= 1) return "🔥 ";
        if (score <= 2) return "⚠️ ";
        if (score <= 3) return "📌 ";
        return "   ";
    }
}

// ========== TRIAGE SYSTEM CLASS ==========
class TriageSystem {
    private PriorityQueue<Patient> waitingQueue;
    private List<Patient> treatedToday;
    private List<Patient> treatedHistory;
    private List<Patient> turnedAway;
    private List<Patient> postponedToTomorrow;
    private int dailyCapacity;
    private int weeklyCapacity;
    private int treatedThisWeek;
    private LocalDate currentDate;
    private int totalTreated;
    private int totalRejected;
    private Map<String, Integer> conditionStats;
    private int currentWeek;
    private LocalDate weekStartDate;

    public TriageSystem(int dailyCapacity) {
        this.waitingQueue = new PriorityQueue<>();
        this.treatedToday = new ArrayList<>();
        this.treatedHistory = new ArrayList<>();
        this.turnedAway = new ArrayList<>();
        this.postponedToTomorrow = new ArrayList<>();
        this.dailyCapacity = dailyCapacity;
        this.weeklyCapacity = dailyCapacity * 7;
        this.treatedThisWeek = 0;
        this.currentDate = LocalDate.now();
        this.totalTreated = 0;
        this.totalRejected = 0;
        this.currentWeek = 1;
        this.weekStartDate = currentDate;
        this.conditionStats = new HashMap<>();

        // Initialize condition stats
        for (String condition : Arrays.asList("mutating", "bleeding", "fever", "infected", "scared", "full zombie")) {
            conditionStats.put(condition, 0);
        }
    }

    public void addPatient(String name, String condition, int age) {
        try {
            Patient patient = new Patient(name, condition, age);

            if (patient.isFullZombie()) {
                turnedAway.add(patient);
                totalRejected++;
                conditionStats.put("full zombie", conditionStats.get("full zombie") + 1);
                System.out.println("⚠️  " + name + " is a FULL ZOMBIE! Turned away/killed for safety.");
            } else {
                waitingQueue.add(patient);
                conditionStats.put(condition.toLowerCase(), conditionStats.get(condition.toLowerCase()) + 1);
                System.out.println("✅ " + name + " added to triage queue.");
            }
        } catch (IllegalArgumentException e) {
            System.out.println("❌ ERROR: " + e.getMessage());
        }
    }
    /*
    public void processDay() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📅 DAY " + currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        System.out.println("Week " + currentWeek + ", Day " + ((currentDate.toEpochDay() - weekStartDate.toEpochDay()) % 7 + 1));
        System.out.println("=".repeat(60));

        // Check and reset weekly capacity
        checkAndResetWeekly();

        if (waitingQueue.isEmpty()) {
            System.out.println("No patients waiting today.");
            currentDate = currentDate.plusDays(1);
            return;
        }

        treatedToday.clear();
        int treated = 0;
        int remainingDailyCapacity = dailyCapacity;
        int remainingWeeklyCapacity = weeklyCapacity - treatedThisWeek;
        int canTreatToday = Math.min(remainingDailyCapacity, remainingWeeklyCapacity);

        System.out.println("\n🏥 PROCESSING PATIENTS");
        System.out.println("  Daily capacity: " + dailyCapacity);
        System.out.println("  Weekly remaining: " + remainingWeeklyCapacity);
        System.out.println("  Can treat today: " + canTreatToday + "\n");

        // Process patients in correct priority order
        List<Patient> toProcess = new ArrayList<>();
        PriorityQueue<Patient> tempQueue = new PriorityQueue<>(waitingQueue);
        while (!tempQueue.isEmpty()) {
            toProcess.add(tempQueue.poll());
        }

        for (Patient p : toProcess) {
            if (treated < canTreatToday && treatedThisWeek < weeklyCapacity) {
                waitingQueue.remove(p);
                treatedToday.add(p);
                treatedHistory.add(p);
                treated++;
                totalTreated++;
                treatedThisWeek++;
                System.out.println("💉 TREATED: " + p);
            } else {
                // Patient will stay in waiting queue for tomorrow
                System.out.println("⏰ POSTPONED: " + p + " (capacity reached)");
            }
        }

        System.out.println("\n📊 DAY SUMMARY:");
        System.out.println("  - Treated today: " + treatedToday.size() + "/" + dailyCapacity);
        System.out.println("  - Total treated so far: " + totalTreated);
        System.out.println("  - Treated this week: " + treatedThisWeek + "/" + weeklyCapacity);
        System.out.println("  - Turned away (zombies): " + totalRejected);
        System.out.println("  - Waiting in queue: " + waitingQueue.size());

        currentDate = currentDate.plusDays(1);

        // Print weekly report at end of week or when capacity reached
        if (treatedThisWeek >= weeklyCapacity || isEndOfWeek()) {
            printWeeklyReport();
        }
    }
    */

    public void processDay() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("📅 DAY " + currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        System.out.println("Week " + currentWeek + ", Day " +
                ((currentDate.toEpochDay() - weekStartDate.toEpochDay()) % 7 + 1));
        System.out.println("=".repeat(60));

        // Check and reset weekly capacity
        checkAndResetWeekly();

        if (waitingQueue.isEmpty()) {
            System.out.println("No patients waiting today.");
            currentDate = currentDate.plusDays(1);
            return;
        }

        treatedToday.clear();
        int treated = 0;

        int remainingDailyCapacity = dailyCapacity;
        int remainingWeeklyCapacity = weeklyCapacity - treatedThisWeek;
        int canTreatToday = Math.min(remainingDailyCapacity, remainingWeeklyCapacity);

        System.out.println("\n🏥 PROCESSING PATIENTS");
        System.out.println("  Daily capacity: " + dailyCapacity);
        System.out.println("  Weekly remaining: " + remainingWeeklyCapacity);
        System.out.println("  Can treat today: " + canTreatToday + "\n");

        // ✅ FIX: Always poll directly from PriorityQueue
        while (!waitingQueue.isEmpty()
                && treated < canTreatToday
                && treatedThisWeek < weeklyCapacity) {

            Patient p = waitingQueue.poll();  // ⭐ KEY FIX

            treatedToday.add(p);
            treatedHistory.add(p);

            treated++;
            totalTreated++;
            treatedThisWeek++;

            System.out.println("💉 TREATED: " + p);
        }

        // ✅ Remaining patients stay in queue (postponed)
        if (!waitingQueue.isEmpty()) {
            for (Patient p : waitingQueue) {
                System.out.println("⏰ POSTPONED: " + p + " (capacity reached)");
            }
        }

        System.out.println("\n📊 DAY SUMMARY:");
        System.out.println("  - Treated today: " + treatedToday.size() + "/" + dailyCapacity);
        System.out.println("  - Total treated so far: " + totalTreated);
        System.out.println("  - Treated this week: " + treatedThisWeek + "/" + weeklyCapacity);
        System.out.println("  - Turned away (zombies): " + totalRejected);
        System.out.println("  - Waiting in queue: " + waitingQueue.size());

        currentDate = currentDate.plusDays(1);

        // Print weekly report if needed
        if (treatedThisWeek >= weeklyCapacity || isEndOfWeek()) {
            printWeeklyReport();
        }
    }


    private void checkAndResetWeekly() {
        if (isEndOfWeek()) {
            System.out.println("\n🔄 NEW WEEK STARTING! Resetting weekly counter.");
            treatedThisWeek = 0;
            currentWeek++;
            weekStartDate = currentDate;
        }
    }

    private boolean isEndOfWeek() {
        long daysSinceWeekStart = currentDate.toEpochDay() - weekStartDate.toEpochDay();
        return daysSinceWeekStart >= 7;
    }

    public void printWeeklyReport() {
        System.out.println("\n📊 === WEEKLY REPORT (Week " + currentWeek + ") ===");
        System.out.println("Week ending: " + currentDate.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
        System.out.println("Total treated this week: " + treatedThisWeek);
        System.out.println("Cures remaining this week: " + (weeklyCapacity - treatedThisWeek));
        System.out.println("Patients waiting in queue: " + waitingQueue.size());
        System.out.println("Total zombies eliminated: " + totalRejected);

        if (treatedThisWeek >= weeklyCapacity) {
            System.out.println("\n⚠️  WEEKLY CURE LIMIT REACHED! No more treatments until next week.");
        }

        System.out.println("\nCondition Statistics (All Time):");
        for (Map.Entry<String, Integer> entry : conditionStats.entrySet()) {
            if (entry.getValue() > 0) {
                System.out.println("  - " + entry.getKey() + ": " + entry.getValue());
            }
        }
    }

    public void showQueue() {
        if (waitingQueue.isEmpty()) {
            System.out.println("\n📋 No patients waiting.");
            return;
        }

        System.out.println("\n📋 CURRENT TRIAGE QUEUE (by priority):");
        System.out.println("🔥 = Highest Priority  ⚠️ = High Priority  📌 = Normal Priority");
        System.out.println("-".repeat(60));

        PriorityQueue<Patient> tempQueue = new PriorityQueue<>(waitingQueue);
        int position = 1;
        while (!tempQueue.isEmpty()) {
            Patient p = tempQueue.poll();
            System.out.printf("%2d. %s%n", position++, p);
        }
        System.out.println("-".repeat(60));
        System.out.println("Total waiting: " + waitingQueue.size());
    }

    public void showStatistics() {
        System.out.println("\n📈 === HOSPITAL STATISTICS ===");
        System.out.println("Total treated patients: " + totalTreated);
        System.out.println("Total zombies turned away: " + totalRejected);
        System.out.println("Current waiting queue size: " + waitingQueue.size());
        System.out.println("Daily cure capacity: " + dailyCapacity);
        System.out.println("Weekly cure capacity: " + weeklyCapacity);
        System.out.println("Treated this week: " + treatedThisWeek);
        System.out.println("Cures remaining this week: " + (weeklyCapacity - treatedThisWeek));
        System.out.println("Current week: " + currentWeek);

        System.out.println("\nCondition Breakdown:");
        for (Map.Entry<String, Integer> entry : conditionStats.entrySet()) {
            if (entry.getValue() > 0) {
                System.out.printf("  - %-12s: %d%n", entry.getKey(), entry.getValue());
            }
        }
    }

    public void saveData(String filename) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(filename))) {
            writer.println("ZOMBIE TRIAGE SYSTEM DATA");
            writer.println("Date: " + LocalDate.now());
            writer.println("Week: " + currentWeek);
            writer.println("Total Treated: " + totalTreated);
            writer.println("Total Rejected: " + totalRejected);
            writer.println("Treated This Week: " + treatedThisWeek);
            writer.println("Waiting Queue Size: " + waitingQueue.size());
            writer.println("\nTreated Patients History:");
            for (Patient p : treatedHistory) {
                writer.printf("%s,%d,%s,%d%n", p.getName(), p.getAge(), p.getCondition(), p.getArrivalOrder());
            }
            System.out.println("✅ Data saved to " + filename);
        } catch (IOException e) {
            System.out.println("❌ Error saving data: " + e.getMessage());
        }
    }

    // For testing purposes
    public int getWaitingQueueSize() {
        return waitingQueue.size();
    }

    public int getTreatedThisWeek() {
        return treatedThisWeek;
    }
}

// ========== MAIN APPLICATION CLASS ==========
public class ZombieTriageApp {
    private static Scanner scanner = new Scanner(System.in);
    private static TriageSystem triage;

    public static void main(String[] args) {
        System.out.println("🧟‍♂️ ZOMBIE APOCALYPSE FIELD HOSPITAL TRIAGE SYSTEM 🧟‍♀️");
        System.out.println("=".repeat(60));
        System.out.println("NOTE: Limited cure - 50 people per day, 350 per week");
        System.out.println("=".repeat(60));

        // Initialize with 50 cures per day
        triage = new TriageSystem(50);

        boolean running = true;
        while (running) {
            showMenu();
            int choice = getIntInput("Choose option: ");
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    addNewPatient();
                    break;
                case 2:
                    triage.processDay();
                    break;
                case 3:
                    triage.showQueue();
                    break;
                case 4:
                    triage.showStatistics();
                    break;
                case 5:
                    printPriorityRules();
                    break;
                case 6:
                    addSamplePatients();
                    break;
                case 7:
                    triage.saveData("triage_backup.txt");
                    break;
                case 8:
                    running = false;
                    System.out.println("\n🏥 Field hospital closing. Stay safe!");
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
        scanner.close();
    }

    private static void showMenu() {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("MAIN MENU");
        System.out.println("=".repeat(60));
        System.out.println("1. 🚑 Add new patient");
        System.out.println("2. 💉 Process day (treat up to 50 patients)");
        System.out.println("3. 📋 View current triage queue");
        System.out.println("4. 📊 View statistics");
        System.out.println("5. 📖 Show priority rules");
        System.out.println("6. 🧪 Load sample patients");
        System.out.println("7. 💾 Save data to file");
        System.out.println("8. 🚪 Exit");
        System.out.println("=".repeat(60));
    }

    private static void addNewPatient() {
        System.out.println("\n🚑 ADD NEW PATIENT");
        System.out.println("-".repeat(40));

        System.out.print("Patient name: ");
        String name = scanner.nextLine();

        System.out.println("\nConditions:");
        System.out.println("  - bleeding (non-stop bleeding)");
        System.out.println("  - infected (bite/infected - lowest priority)");
        System.out.println("  - fever (infection fever)");
        System.out.println("  - mutating (HIGHEST priority)");
        System.out.println("  - full zombie (turn away/kill)");
        System.out.println("  - scared (not infected - least priority)");
        System.out.print("Condition: ");
        String condition = scanner.nextLine();

        int age = getIntInput("Age: ");
        scanner.nextLine(); // Consume newline

        triage.addPatient(name, condition, age);
    }

    private static void printPriorityRules() {
        System.out.println("\n📖 TRIAGE PRIORITY RULES");
        System.out.println("=".repeat(60));
        System.out.println("PRIORITY ORDER (highest to lowest):");
        System.out.println("  1. 🧬 MUTATING - Base priority 1");
        System.out.println("  2. 🩸 BLEEDING NON-STOP - Base priority 2");
        System.out.println("  3. 🌡️ FEVER - Base priority 3");
        System.out.println("  4. 🦷 INFECTED/BITE - Base priority 4");
        System.out.println("  5. 😨 SCARED (not infected) - Base priority 6");
        System.out.println("  6. 🧟 FULL ZOMBIE - Turn away/kill");
        System.out.println("\nAGE MODIFIERS:");
        System.out.println("  - Young adults (<25): -1 (HIGHER priority)");
        System.out.println("  - Adults (25-50): 0 (Normal priority)");
        System.out.println("  - Elderly (>50): +1 (LOWER priority)");
        System.out.println("\nFINAL PRIORITY SCORE = Condition Priority + Age Modifier");
        System.out.println("(LOWER score = HIGHER priority)");
        System.out.println("\nEXAMPLE:");
        System.out.println("  - Young bleeding (20yo): 2 + (-1) = 1 (VERY HIGH)");
        System.out.println("  - Elderly mutating (70yo): 1 + 1 = 2 (HIGH)");
        System.out.println("  → Young bleeding gets treated FIRST despite condition!");
        System.out.println("\n🏥 CAPACITY:");
        System.out.println("  - 50 cures per day");
        System.out.println("  - 350 cures per week");
        System.out.println("  - Treatment stops when weekly limit reached");
    }

    private static int getIntInput(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.print("Invalid input. " + prompt);
            scanner.next();
        }
        int input = scanner.nextInt();
        return input;
    }

    private static void addSamplePatients() {
        System.out.println("\n📝 Loading sample patients for demonstration...");

        // Test case 1: Priority inversion fix
        triage.addPatient("Sarah (Young Bleeding)", "bleeding", 20);
        triage.addPatient("George (Elderly Mutating)", "mutating", 70);

        // Test case 2: Normal priority
        triage.addPatient("John (Adult Fever)", "fever", 35);
        triage.addPatient("Emma (Young Infected)", "infected", 18);

        // Test case 3: Edge cases
        triage.addPatient("Mike (Adult Bleeding)", "bleeding", 40);
        triage.addPatient("Lisa (Elderly Scared)", "scared", 65);
        triage.addPatient("Tom (Full Zombie)", "full zombie", 45);

        // Test case 4: Boundary ages
        triage.addPatient("Boundary1 (Age 24)", "scared", 24);
        triage.addPatient("Boundary2 (Age 25)", "scared", 25);
        triage.addPatient("Boundary3 (Age 50)", "scared", 50);
        triage.addPatient("Boundary4 (Age 51)", "scared", 51);

        System.out.println("\n✓ Sample patients loaded!");
        System.out.println("\n🔍 NOTE: Watch how 'Sarah (Young Bleeding)' gets treated BEFORE");
        System.out.println("   'George (Elderly Mutating)' despite mutating being higher");
        System.out.println("   priority - this demonstrates the age modifier fix!\n");
    }
}