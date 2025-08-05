# 🐛 Silk Farming Bot Plan (Closed Loop, No Silk Production)

## Overview

This bot automates the full silk farming loop in **Haven & Hearth**, starting from silkworm eggs and ending with more silkworm eggs via moth breeding. The goal is to maintain a continuous, self-sustaining silk lifecycle.

❗ **This bot does NOT produce silk filaments, thread, or cloth.**  
❗ **Quality and timing are ignored.**

---

## 🔄 Key Processes

1. **Hatching Silkworm Eggs**
2. **Feeding Silkworms in Cabinets**
3. **Waiting for Cocoons to Hatch into Moths**
4. **Breeding Moths to Produce More Eggs**
5. **Collecting New Eggs and Restarting the Loop**

---

## 🗂 Required Areas

| Area Name           | Purpose                                                                 |
|---------------------|-------------------------------------------------------------------------|
| **Egg Storage**      | Container holding silkworm eggs (input/output of the loop)              |
| **Hatching Area**    | Herbalist tables for egg hatching                                       |
| **Cabinet Area**     | Cabinets for silkworms, cocoons, moths, and breeding (all shared)       |
| **Leaf Supply Area** | Source of mulberry leaves used to feed silkworms                        |

---

## 📦 Bot Flow

1. **Collect Eggs from Egg Storage**
    - Withdraw silkworm eggs.

2. **Place Eggs onto Herbalist Tables**
    - Distribute across tables in the hatching area.

3. **Wait for Eggs to Hatch**
    - No timing checks; periodically scan for hatched silkworms.

4. **Transfer Hatched Silkworms to Cabinets**
    - Move worms into cabinets.
    - Add mulberry leaves from leaf supply.
    - 56 silkworms (unstacked) 28 Mulberry Leaves (stacked) per cupboard is optimal.

5. **Let Silkworms Feed and Form Cocoons**
    - Silkworms eat and spin cocoons automatically.

6. **Wait for Cocoons to Hatch into Moths**
    - Cocoons remain in cabinets and hatch.

7. **Allow Moths to Breed**
    - Moths breed inside the same cabinet.
    - Silkworm eggs are produced.

8. **Collect New Eggs from Cabinets**
    - Gather eggs and move them to **Egg Storage**.

9. **Repeat the Loop**

---

## 🧠 Bot Logic & Capabilities

- Recognize and handle the following item types:
    - `Silkworm Egg`
    - `Silkworm`
    - `Mulberry Leaf`
    - `Silkworm Cocoon`
    - `Silkmoth`
- Identify areas by specialization or name:
    - `SilkEggStorage` - this is simply a PUT/TAKE area.
    - `SilkHerbalistTables` - specialization htable
    - `SilkCabinets` - 
    - `MulberryLeaves` - TAKE area with mulberry leaves.
- Move items between containers and objects (e.g., tables, cabinets).
- No complex state machines—use direct condition checks (e.g., herbalist table now holds a silkworm).
- Fully repeatable with minimal logic.

---

## ✅ Simplifications & Constraints

- ❌ No silk filament, thread, or cloth production.
- ❌ No quality tracking.
- ❌ No timing or delay logic.
- ✅ Cabinet area handles **all stages**: silkworm feeding, cocoon hatching, and moth breeding.
- ✅ Hardcoded or fixed area naming is acceptable.

---

## 🔁 Loop Summary

```text
EGGS
 ↓
HATCH → SILKWORMS
 ↓
FEED → COCOONS
 ↓
HATCH IN CABINET → MOTHS
 ↓
BREED → EGGS
 ↺ (repeat)
