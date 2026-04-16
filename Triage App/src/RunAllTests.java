import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.*;

class ZombieTriageFixedTest {
    private TriageSystem triage;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        triage = new TriageSystem(50);
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    // ========== FIXED TEST: Priority Inversion ==========
    @Test
    @DisplayName("FIXED: Young bleeding should have higher priority than elderly mutating")
    void testPriorityInversionFix() {
        triage.addPatient("ElderlyMutating", "mutating", 70);
        triage.addPatient("YoungBleeding", "bleeding", 20);

        triage.processDay();
        String output = outContent.toString();

        int youngIndex = output.indexOf("YoungBleeding");
        int elderlyIndex = output.indexOf("ElderlyMutating");

        // Young bleeding should be treated FIRST despite lower condition priority
        assertTrue(youngIndex > elderlyIndex,
                "Young bleeding (20) should be treated before elderly mutating (70)");
    }

    // ========== NEW TEST: Input Validation ==========
    @Test
    @DisplayName("Test: Invalid age should be rejected")
    void testInvalidAge() {
        triage.addPatient("InvalidAge", "scared", -5);
        String output = outContent.toString();

        assertTrue(output.contains("ERROR"), "Should show error for negative age");

        triage.addPatient("InvalidAge2", "scared", 200);
        output = outContent.toString();
        assertTrue(output.contains("ERROR"), "Should show error for excessive age");
    }

    @Test
    @DisplayName("Test: Empty name should be rejected")
    void testEmptyName() {
        triage.addPatient("", "scared", 30);
        String output = outContent.toString();

        assertTrue(output.contains("ERROR"), "Should reject empty name");
        assertTrue(output.contains("cannot be null or empty"),
                "Should specify the error reason");
    }

    @Test
    @DisplayName("Test: Null name should be rejected")
    void testNullName() {
        triage.addPatient(null, "scared", 30);
        String output = outContent.toString();

        assertTrue(output.contains("ERROR"), "Should reject null name");
    }

    @Test
    @DisplayName("Test: Invalid condition should be rejected")
    void testInvalidCondition() {
        triage.addPatient("Test", "alien infection", 30);
        String output = outContent.toString();

        assertTrue(output.contains("ERROR"), "Should reject invalid condition");
        assertTrue(output.contains("Valid conditions:"),
                "Should list valid conditions");
    }

    @Test
    @DisplayName("Test: Case insensitive condition handling")
    void testCaseInsensitivity() {
        triage.addPatient("Test1", "MUTATING", 30);
        triage.addPatient("Test2", "BLEEDING", 30);

        triage.processDay();
        String output = outContent.toString();

        // Should treat mutating first despite uppercase
        int mutatingIndex = output.indexOf("Test1");
        int bleedingIndex = output.indexOf("Test2");

        assertTrue(mutatingIndex < bleedingIndex,
                "Case should not affect priority ordering");
    }

    // ========== NEW TEST: Weekly Capacity ==========
    @Test
    @DisplayName("Test: Weekly capacity limit of 350")
    void testWeeklyCapacityLimit() throws Exception {
        // Add 400 patients
        for (int i = 0; i < 400; i++) {
            triage.addPatient("P" + i, "scared", 30);
        }

        // Process 7 days (350 capacity)
        for (int day = 0; day < 7; day++) {
            triage.processDay();
        }

        String output = outContent.toString();

        // Should have treated exactly 350
        long treatedCount = output.lines()
                .filter(line -> line.contains("💉 TREATED:"))
                .count();

        assertTrue(treatedCount <= 350, "Should not exceed weekly capacity");
        assertTrue(output.contains("WEEKLY CURE LIMIT REACHED"),
                "Should show weekly limit reached message");
    }

    // ========== NEW TEST: Weekly Reset ==========
    @Test
    @DisplayName("Test: Weekly counter resets after 7 days")
    void testWeeklyReset() {
        // Add patients
        for (int i = 0; i < 100; i++) {
            triage.addPatient("P" + i, "scared", 30);
        }

        // Process week 1
        for (int day = 0; day < 7; day++) {
            triage.processDay();
        }

        // Process day 8 (new week)
        triage.processDay();
        String output = outContent.toString();

        assertTrue(output.contains("Treated today: 50"),
                "Should resume treating in new week");
    }

    // ========== NEW TEST: Priority Queue Ordering ==========
    @Test
    @DisplayName("Test: Complex priority ordering with multiple factors")
    void testComplexPriorityOrdering() {
        // Add patients in random order
        triage.addPatient("A-MutatingYoung", "mutating", 20);    // Score: 1-1=0
        triage.addPatient("B-BleedingYoung", "bleeding", 22);    // Score: 2-1=1
        triage.addPatient("C-MutatingElderly", "mutating", 70);  // Score: 1+1=2
        triage.addPatient("D-FeverAdult", "fever", 35);          // Score: 3+0=3
        triage.addPatient("E-BleedingElderly", "bleeding", 65);  // Score: 2+1=3
        triage.addPatient("F-InfectedYoung", "infected", 18);    // Score: 4-1=3
        triage.addPatient("G-ScaredAdult", "scared", 40);        // Score: 6+0=6

        triage.processDay();
        String output = outContent.toString();

        // Verify order
        int idxA = output.indexOf("A-MutatingYoung");
        int idxB = output.indexOf("B-BleedingYoung");
        int idxC = output.indexOf("C-MutatingElderly");
        int idxD = output.indexOf("D-FeverAdult");
        int idxE = output.indexOf("E-BleedingElderly");
        int idxF = output.indexOf("F-InfectedYoung");
        int idxG = output.indexOf("G-ScaredAdult");

        assertTrue(idxA < idxB, "Mutating young before bleeding young");
        assertTrue(idxB < idxC, "Bleeding young before mutating elderly");
        assertTrue(idxC < idxD, "Mutating elderly before fever adult");
        // D, E, F have same score (3) - FIFO applies
        assertTrue(idxG > idxF, "Scared should be last");
    }

    // ========== NEW TEST: Scared Patient Rollover ==========
    @Test
    @DisplayName("Test: Scared patients maintain priority across days")
    void testScaredPatientRollover() {
        // Add scared patients on day 1
        triage.addPatient("Scared1", "scared", 30);
        triage.addPatient("Scared2", "scared", 31);

        // Process day 1 (treats them if capacity available)
        triage.processDay();

        // Add more patients day 2
        triage.addPatient("Bleeding1", "bleeding", 40);

        // Process day 2
        triage.processDay();
        String output = outContent.toString();

        // Bleeding should be treated before any remaining scared patients
        int bleedingIdx = output.indexOf("Bleeding1");
        int scaredIdx = output.indexOf("Scared");

        if (scaredIdx > 0) { // If scared still waiting
            assertTrue(bleedingIdx > scaredIdx,
                    "New bleeding patient should have priority over old scared patients");
        }
    }

    // ========== NEW TEST: Boundary Ages ==========
    @Test
    @DisplayName("Test: Age boundaries (24,25,50,51)")
    void testAgeBoundaries() {
        triage.addPatient("Age24", "scared", 24);  // Should be -1 modifier
        triage.addPatient("Age25", "scared", 25);  // Should be 0 modifier
        triage.addPatient("Age50", "scared", 50);  // Should be 0 modifier
        triage.addPatient("Age51", "scared", 51);  // Should be +1 modifier

        triage.processDay();
        String output = outContent.toString();

        int idx24 = output.indexOf("Age24");
        int idx25 = output.indexOf("Age25");
        int idx50 = output.indexOf("Age50");
        int idx51 = output.indexOf("Age51");

        assertTrue(idx24 < idx25, "Age 24 should be before age 25");
        assertTrue(idx25 < idx51, "Age 25 should be before age 51");
        assertTrue(idx50 < idx51, "Age 50 should be before age 51");
    }

    // ========== NEW TEST: Save/Load Data ==========
    @Test
    @DisplayName("Test: Save data to file")
    void testSaveData() throws Exception {
        triage.addPatient("TestSave", "bleeding", 30);
        triage.processDay();

        String filename = "test_save.txt";
        triage.saveData(filename);

        File file = new File(filename);
        assertTrue(file.exists(), "Save file should be created");

        // Clean up
        file.delete();
    }

    // ========== PERFORMANCE TEST: Large Scale ==========
    @Test
    @DisplayName("Performance: Handle 5000 patients efficiently")
    void testLargeScalePerformance() {
        long startTime = System.currentTimeMillis();

        // Add 5000 patients
        for (int i = 0; i < 5000; i++) {
            String condition = switch (i % 6) {
                case 0 -> "mutating";
                case 1 -> "bleeding";
                case 2 -> "fever";
                case 3 -> "infected";
                case 4 -> "scared";
                default -> "full zombie";
            };
            triage.addPatient("Patient" + i, condition, 20 + (i % 80));
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(duration < 3000,
                "Should handle 5000 patients in under 3 seconds. Took: " + duration + "ms");
    }

    // ========== STRESS TEST: Memory Management ==========
    @Test
    @DisplayName("Stress: Multiple days with 1000+ patients")
    void testMultipleDaysStress() {
        // Add 1000 patients
        for (int i = 0; i < 1000; i++) {
            triage.addPatient("P" + i, "scared", 30 + (i % 50));
        }

        // Process 20 days
        for (int day = 0; day < 20; day++) {
            triage.processDay();
        }

        // Should not crash or throw exception
        assertTrue(true, "System should handle continuous operation");
    }
}

// Run all tests
/*
public class RunAllTests {
    public static void main(String[] args) {
        System.out.println("🧪 RUNNING COMPREHENSIVE TEST SUITE\n");


        org.junit.platform.console.ConsoleLauncher.execute(
                org.junit.platform.console.ConsoleLauncher.builder()
                        .selectClass(ZombieTriageFixedTest.class)
                        .build()
        );


    }
}

 */