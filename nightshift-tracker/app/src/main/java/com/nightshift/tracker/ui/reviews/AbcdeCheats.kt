package com.nightshift.tracker.ui.reviews

import androidx.compose.ui.graphics.Color
import com.nightshift.tracker.BuildConfig

// Written for a stressed intern at 4 am, not for an exam.
// Letter colours have light-background variants for the UroDay flavor.
private val URO = BuildConfig.URO
private val ColA = if (URO) Color(0xFFD92D20) else Color(0xFFFF6B6B)
private val ColB = if (URO) Color(0xFFC2410C) else Color(0xFFFFA94D)
private val ColC = if (URO) Color(0xFFB45309) else Color(0xFFFFD166)
private val ColD = if (URO) Color(0xFF0E7490) else Color(0xFF7EB6FF)
private val ColE = if (URO) Color(0xFF15803D) else Color(0xFF6BCB77)
data class AbcdeCheat(
    val letter: String,
    val title: String,
    val color: Color,
    val lines: List<String>,
)

val abcdeCheats =
    listOf(
        AbcdeCheat(
            "A", "Airway", ColA,
            listOf(
                "Talking in full sentences = airway patent. Document that and move on.",
                "Stridor, gurgling, see-saw breathing = badness. Call for help NOW, don't finesse it.",
                "Reduced GCS + vomiting? Left lateral, suction, anaesthetics early.",
                "Look in the mouth: dentures, blood, secretions, swollen tongue (anaphylaxis? angioedema on ACEi?).",
            ),
        ),
        AbcdeCheat(
            "B", "Breathing", ColB,
            listOf(
                "RR is the single best sick-patient number and the one nurses chart least reliably — count it yourself for 30 seconds.",
                "Sats target 92–96%, or 88–92% if CO2 retainer (check old ABGs / COPD history).",
                "Listen: silent chest in asthma is peri-arrest, not improvement.",
                "Crackles + JVP up + new O2 need = think APO. Wheeze isn't always asthma (cardiac wheeze exists).",
                "Sudden SOB + clear chest: think PE. Recent surgery? Immobile? Cancer?",
            ),
        ),
        AbcdeCheat(
            "C", "Circulation", ColC,
            listOf(
                "Trend beats number: SBP 105 is fine unless they've been 150 all week.",
                "Recheck weird BPs yourself with a manual cuff — half of overnight 'hypotension' is a cuff problem.",
                "Cap refill, cool peripheries, mottling: sicker than the numbers say.",
                "Urine output is the honest organ-perfusion gauge — check the fluid balance chart.",
                "Tachycardia is never 'normal for them' until proven: pain? sepsis? bleeding? AF? PE? dry?",
                "Two large-bore IVs early if you're worried. You can't cannulate a shocked patient later.",
            ),
        ),
        AbcdeCheat(
            "D", "Disability", ColD,
            listOf(
                "BSL: ALWAYS CHECK. Hypo kills and is fixable in 90 seconds.",
                "GCS drop: compute it properly, chart it, and compare to baseline — 'drowsy' isn't a number.",
                "Pupils: size, symmetry, reaction. Pinpoint = opioids: check the chart, naloxone if RR down.",
                "New confusion in an old person = delirium until proven otherwise: infection, retention, constipation, drugs.",
                "Head injury on anticoagulant/antiplatelet = low threshold for CT and escalation.",
            ),
        ),
        AbcdeCheat(
            "E", "Exposure", ColE,
            listOf(
                "Temp — actually look at it, including the trend. Hypothermia is a sepsis sign too.",
                "Look at the whole patient: rashes (petechiae?), calves (DVT?), wounds, drains, catheter bag colour.",
                "Check the back and pressure areas — the source is often where nobody looked.",
                "Lines and sites: any IV that's red or old could be the septic source.",
                "Keep them warm and covered when you're done. Dignity matters at 4 am too.",
            ),
        ),
    )
