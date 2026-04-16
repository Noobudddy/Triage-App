# 🧟 Zombie Apocalypse Triage System

## 📌 Overview

The **Zombie Apocalypse Triage System** is a Java-based simulation of a field hospital managing patients during a zombie outbreak. The system prioritizes patients based on **condition severity** and **age**, ensuring that limited medical resources are allocated efficiently.

This project includes:

* A fully functional triage system (`TriageSystem`)
* A patient priority model (`Patient`)
* A command-line interface (`ZombieTriageApp`)
* A comprehensive JUnit test suite (`ZombieTriageFixedTest`)

---

## ⚙️ Features

### 🏥 Patient Management

* Add patients with:

  * Name
  * Condition
  * Age
* Input validation:

  * Rejects null or empty names
  * Rejects invalid ages (<0 or >150)
  * Rejects invalid conditions
* Supported conditions:

  * `mutating` (highest priority)
  * `bleeding`
  * `fever`
  * `infected`
  * `scared` (lowest priority)
  * `full zombie` (rejected immediately)

---

### 📊 Priority System

Each patient is assigned a **priority score**:

```
Priority Score = Condition Priority + Age Modifier
```

#### Condition Priority:

| Condition   | Score    |
| ----------- | -------- |
| mutating    | 1        |
| bleeding    | 2        |
| fever       | 3        |
| infected    | 4        |
| scared      | 6        |
| full zombie | rejected |

#### Age Modifier:

| Age Range | Modifier |
| --------- | -------- |
| < 25      | -1       |
| 25 – 50   | 0        |
| > 50      | +1       |

👉 **Lower score = higher priority**

#### Example:

* Young bleeding (20): `2 + (-1) = 1`
* Elderly mutating (70): `1 + 1 = 2`

➡️ Young bleeding is treated **first** (priority inversion fix)

---

### ⏱️ Capacity Limits

* **Daily capacity:** 50 patients
* **Weekly capacity:** 350 patients
* System automatically:

  * Stops treatment when weekly limit is reached
  * Resets weekly counter after 7 days

---

### 🔄 Queue Processing

* Uses a **PriorityQueue** for correct ordering
* Patients are treated based on:

  1. Priority score
  2. FIFO (arrival order) if tied
* Remaining patients are **postponed** to the next day

---

### 📈 Statistics & Reporting

* Tracks:

  * Total treated patients
  * Rejected (full zombies)
  * Weekly progress
  * Condition distribution
* Weekly report includes:

  * Remaining cures
  * Queue size
  * Condition stats

---

### 💾 Data Persistence

* Save system state to file:

```
triage.saveData("filename.txt");
```

---

### 🖥️ Command-Line Interface

Menu options:

1. Add patient
2. Process day
3. View queue
4. View statistics
5. Show priority rules
6. Load sample patients
7. Save data
8. Exit

---

## 🧪 Testing (JUnit)

The project includes a **comprehensive test suite** covering:

### ✅ Core Functionality

* Priority ordering correctness
* Priority inversion fix (age vs condition)
* FIFO behavior for equal priority

### ✅ Input Validation

* Invalid age
* Null/empty name
* Invalid condition
* Case-insensitive handling

### ✅ System Constraints

* Weekly capacity limit (350)
* Weekly reset behavior

### ✅ Edge Cases

* Age boundaries (24, 25, 50, 51)
* Scared patient rollover
* Complex multi-factor ordering

### 🚀 Performance Tests

* Handles **5000 patients under 3 seconds**

### 💥 Stress Tests

* Multi-day simulation with 1000+ patients
* Ensures no crashes or memory issues

---

## 🛠️ How to Run

### 1. Compile

```
javac ZombieTriageApp.java
```

### 2. Run Application

```
java ZombieTriageApp
```

### 3. Run Tests (JUnit 5)

Make sure JUnit 5 is installed, then run:

```
ConsoleLauncher --select-class ZombieTriageFixedTest
```

---

## 🧩 Project Structure

```
├── Patient.java              
├── TriageSystem.java        
├── ZombieTriageApp.java     
├── ZombieTriageFixedTest.java 
```

---

## ⚠️ Key Fixes Implemented

### ✅ Priority Queue Bug Fix

* Switched to direct `poll()` from `PriorityQueue`
* Ensures correct real-time priority ordering

### ✅ Priority Inversion Fix

* Age modifier properly affects final score
* Young patients can outrank worse conditions

### ✅ Input Validation

* Prevents invalid data from entering system

---

## 🚀 Future Improvements

* GUI interface (JavaFX or Swing)
* Persistent database storage
* Real-time monitoring dashboard
* Multiplayer simulation mode

---

## 🧟 Final Note

In a zombie apocalypse, **every decision matters**.
This system ensures that limited resources are used **logically, fairly, and efficiently**.

Stay safe. 🏥
