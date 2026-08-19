package com.nightshift.tracker.ui.reviews

import com.nightshift.tracker.BuildConfig

/**
 * A starting point for the calls that come in over and over.
 *
 * A template fills the reason, a sensible starting priority, and a PROMPT list
 * of the workup worth considering. It deliberately does not pre-fill any
 * finding, observation or plan: a note that arrives pre-written with clinical
 * content is a lie waiting to be signed, and the whole app is built on the
 * opposite principle.
 */
data class ReviewTemplate(
    val label: String,
    val reason: String,
    val priority: Int,
    val workupPrompt: String,
)

private val generalTemplates =
    listOf(
        ReviewTemplate(
            "Chest pain", "Chest pain", 1,
            "Consider: ECG within 10 min (compare old), troponin per pathway, CXR, FBC/UEC, " +
                "obs trend. Delete what you don't do.",
        ),
        ReviewTemplate(
            "Short of breath", "Shortness of breath / hypoxia", 1,
            "Consider: RR counted, sats + O2 requirement, ECG, CXR, ABG/VBG if unwell, " +
                "FBC/UEC. Delete what you don't do.",
        ),
        ReviewTemplate(
            "Hypotension", "Hypotension", 1,
            "Consider: manual BP recheck, fluid balance + UO, lactate, FBC/UEC, cultures if " +
                "febrile, medication chart review. Delete what you don't do.",
        ),
        ReviewTemplate(
            "Tachycardia", "Tachycardia", 1,
            "Consider: 12-lead ECG, obs trend, FBC/UEC/Mg, TFT if new AF, cultures if febrile. " +
                "Delete what you don't do.",
        ),
        ReviewTemplate(
            "Fever / ?sepsis", "Fever", 1,
            "Consider: blood cultures x2 before antibiotics, lactate, FBC/UEC/CRP, urine MCS, " +
                "CXR, line/wound review. Delete what you don't do.",
        ),
        ReviewTemplate(
            "Fall", "Fall", 2,
            "Consider: injury survey incl. head and hips, neuro obs if head strike, " +
                "anticoagulant check, postural BP, ECG, BSL, imaging as indicated, " +
                "incident report. Delete what you don't do.",
        ),
        ReviewTemplate(
            "Low urine output", "Oliguria", 2,
            "Consider: catheter patency / bladder scan, fluid balance, UEC incl. K+, " +
                "nephrotoxin review, BP trend. Delete what you don't do.",
        ),
        ReviewTemplate(
            "Hypoglycaemia", "Hypoglycaemia", 1,
            "Consider: BSL now and at 15 min, treatment given, insulin/sulfonylurea chart " +
                "review, cause, repeat risk. Delete what you don't do.",
        ),
        ReviewTemplate(
            "Confusion / delirium", "Acute confusion", 2,
            "Consider: BSL, obs, bladder scan for retention, bowels, infection screen, " +
                "medication review, pain, hypoxia. Delete what you don't do.",
        ),
        ReviewTemplate(
            "Uncontrolled pain", "Pain not controlled", 2,
            "Consider: current chart incl. patches/PCA, renal function for opioid choice, " +
                "sedation score, aperients and antiemetic charted. Delete what you don't do.",
        ),
        ReviewTemplate(
            "Abnormal bloods", "Abnormal result", 2,
            "Consider: repeat/confirm sample, ECG if K+ or Ca abnormal, medication review, " +
                "trend against previous. Delete what you don't do.",
        ),
        ReviewTemplate(
            "Post-op check", "Post-operative review", 3,
            "Consider: obs trend, pain, wound/drains, urine output, bloods, VTE prophylaxis, " +
                "operative plan from the op note. Delete what you don't do.",
        ),
    )

private val uroTemplates =
    listOf(
        ReviewTemplate(
            "Haematuria / clots", "Haematuria", 1,
            "Consider: catheter size and washout done, FBC (Hb trend), coags, group & hold, " +
                "anticoagulant review, CBI running. Delete what you don't do.",
        ),
        ReviewTemplate(
            "Blocked catheter", "Blocked / bypassing catheter", 1,
            "Consider: tubing and bag check, 50 mL flush result, bladder scan, catheter " +
                "change, retention symptoms. Delete what you don't do.",
        ),
        ReviewTemplate(
            "Urinary retention", "Acute urinary retention", 1,
            "Consider: bladder scan, residual drained, cause (BPH, constipation, drugs, " +
                "neuro), UEC, alpha-blocker started, TOV plan. Delete what you don't do.",
        ),
        ReviewTemplate(
            "Stent pain", "Ureteric stent symptoms", 2,
            "Consider: temp and infection screen, analgesia, alpha-blocker, stent removal " +
                "date documented. Delete what you don't do.",
        ),
        ReviewTemplate(
            "Fever with stent/catheter", "Fever with urological device", 1,
            "Consider: cultures before antibiotics, lactate, imaging for obstruction, " +
                "urgent senior review — infected obstruction is an emergency. " +
                "Delete what you don't do.",
        ),
    )

/** Urology templates come first in UroDay; the general set follows. */
val reviewTemplates: List<ReviewTemplate> =
    if (BuildConfig.URO) uroTemplates + generalTemplates else generalTemplates
