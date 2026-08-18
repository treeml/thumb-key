package com.nightshift.tracker.ui.rounds

import androidx.compose.ui.graphics.Color

/**
 * Teaching prompts that sit behind the "?" on each ward-round field —
 * what a good urology JMO is actually looking for at that line, written the
 * way a registrar would say it on the round.
 */
data class RoundCheat(
    val field: String,
    val title: String,
    val color: Color,
    val lines: List<String>,
)

private val TealTeal = Color(0xFF0F766E)
private val Amber = Color(0xFFB45309)
private val Blue = Color(0xFF0E7490)
private val Green = Color(0xFF15803D)
private val Plum = Color(0xFF7E22CE)

val roundCheats =
    mapOf(
        "dx" to
            RoundCheat(
                "dx", "Dx / operation — what to capture", TealTeal,
                listOf(
                    "Name the operation AND the day: 'TURP, POD 1' tells the reader more than 'post-op'.",
                    "Note the approach where it matters (open vs lap vs robotic; rigid vs flexible).",
                    "Side matters in urology — always write left/right for stones, nephrectomy, orchidectomy.",
                    "Flag the drivers of everything else: catheter in situ? stent in situ? drain? These decide the plan.",
                    "For cancer patients, note the working stage/grade if known — it frames every discussion.",
                ),
            ),
        "overnight" to
            RoundCheat(
                "overnight", "Overnight — ask the right five", Amber,
                listOf(
                    "Pain: controlled? What did they actually need overnight? Escalating analgesia is a red flag, not a nursing detail.",
                    "Fever/rigors: any spike changes the whole plan (stent? catheter? recent instrumentation?).",
                    "Bleeding: haematuria colour trend — rosé, cranberry, or clots? Any washouts done overnight, and how many?",
                    "Voiding: did they pass urine, how much, and did they need a catheter or a bladder scan?",
                    "Read the obs chart yourself before you write 'stable overnight' — nurses' handover plus the chart, not one or the other.",
                ),
            ),
        "exam" to
            RoundCheat(
                "exam", "O/E — the urology-specific look", Blue,
                listOf(
                    "Obs trend first: HR and temp trends over the night beat a single set at 0800.",
                    "Abdomen: distended? Palpable bladder? Tender flank (obstruction/pyelo)?",
                    "Catheter: colour of the urine, what's actually draining, and whether the bag has been emptied — check the bag, don't take it on trust.",
                    "For CBI: is outflow keeping ahead of inflow? Is the outflow rosé or getting darker?",
                    "Wounds and drains: type of fluid, volume trend. A sudden change in drain character is the note that saves the day.",
                    "Legs and calves: post-op urology patients get DVTs; look while you're there.",
                ),
            ),
        "results" to
            RoundCheat(
                "results", "Results — what actually changes management", Green,
                listOf(
                    "Hb trend, not one value — post-op urological bleeding shows in the slope.",
                    "Creatinine/eGFR: drives every drug dose you'll write, especially opioids, gentamicin and LMWH.",
                    "K+ before anyone writes fluids or an ACEi restart.",
                    "Cultures: chase what's pending and write the sensitivities down — the antibiotic plan is only as good as the last MCS.",
                    "Imaging: read the images/report yourself where you can, and write the conclusion, not just 'CT done'.",
                    "Histology pending? Note the expected date — it's the thing everyone forgets to chase.",
                ),
            ),
        "plan" to
            RoundCheat(
                "plan", "Plan — make it executable", Plum,
                listOf(
                    "Every line should have an owner and a trigger: what, who, and by when.",
                    "Catheter plan is a plan: TOV date, or 'IDC to stay, urology clinic in 2 weeks' — never leave it blank.",
                    "Write escalation thresholds explicitly: 'call if outflow slows, SBP <100, or urine output <30 mL/h'.",
                    "Antibiotics need a stop or review date on the day they're started.",
                    "Discharge criteria written early ('home once voiding and pain controlled on oral') shortens the stay.",
                    "Stents: removal/exchange date in writing, every time. A forgotten stent is a preventable disaster.",
                    "If a consultant decision is pending, say so and name who is asking — an unowned plan doesn't happen.",
                ),
            ),
    )
