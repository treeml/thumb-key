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
 *
 * [aliases] are the words people actually say at 3 am — "SOB", "temp", "BNO",
 * "found on floor" — so the search box finds the template without anyone
 * having to guess the label it was filed under.
 */
data class ReviewTemplate(
    val label: String,
    val reason: String,
    val priority: Int,
    val workupPrompt: String,
    val aliases: List<String> = emptyList(),
)

private val generalTemplates =
    listOf(
        ReviewTemplate(
            "Chest pain", "Chest pain", 1,
            "Consider: ECG within 10 min (compare old), troponin per pathway, CXR, FBC/UEC, " +
                "obs trend. Delete what you don't do.",
            listOf("acs", "angina", "cardiac", "crushing", "heart"),
        ),
        ReviewTemplate(
            "Short of breath", "Shortness of breath / hypoxia", 1,
            "Consider: RR counted, sats + O2 requirement, ECG, CXR, ABG/VBG if unwell, " +
                "FBC/UEC. Delete what you don't do.",
            listOf("sob", "dyspnoea", "dyspnea", "desat", "desaturating", "hypoxia", "breathless", "sats", "oxygen"),
        ),
        ReviewTemplate(
            "Hypotension", "Hypotension", 1,
            "Consider: manual BP recheck, fluid balance + UO, lactate, FBC/UEC, cultures if " +
                "febrile, medication chart review. Delete what you don't do.",
            listOf("low bp", "hypotensive", "soft bp", "shocked", "map"),
        ),
        ReviewTemplate(
            "Tachycardia", "Tachycardia", 1,
            "Consider: 12-lead ECG, obs trend, FBC/UEC/Mg, TFT if new AF, cultures if febrile. " +
                "Delete what you don't do.",
            listOf("fast heart rate", "tachy", "af", "svt", "palpitations", "high hr"),
        ),
        ReviewTemplate(
            "Fever / ?sepsis", "Fever", 1,
            "Consider: blood cultures x2 before antibiotics, lactate, FBC/UEC/CRP, urine MCS, " +
                "CXR, line/wound review. Delete what you don't do.",
            listOf("temp", "temperature", "febrile", "sepsis", "septic", "rigors", "hot", "infection"),
        ),
        ReviewTemplate(
            "Fall", "Fall", 2,
            "Consider: injury survey incl. head and hips, neuro obs if head strike, " +
                "anticoagulant check, postural BP, ECG, BSL, imaging as indicated, " +
                "incident report. Delete what you don't do.",
            listOf("fell", "found on floor", "slipped", "head strike", "tumble"),
        ),
        ReviewTemplate(
            "Low urine output", "Oliguria", 2,
            "Consider: catheter patency / bladder scan, fluid balance, UEC incl. K+, " +
                "nephrotoxin review, BP trend. Delete what you don't do.",
            listOf("oliguria", "anuria", "not passing urine", "low uo", "no urine", "aki", "kidney"),
        ),
        ReviewTemplate(
            "Hypoglycaemia", "Hypoglycaemia", 1,
            "Consider: BSL now and at 15 min, treatment given, insulin/sulfonylurea chart " +
                "review, cause, repeat risk. Delete what you don't do.",
            listOf("hypo", "hypoglycemia", "low bsl", "low sugar", "bsl", "glucose", "diabetes"),
        ),
        ReviewTemplate(
            "Confusion / delirium", "Acute confusion", 2,
            "Consider: BSL, obs, bladder scan for retention, bowels, infection screen, " +
                "medication review, pain, hypoxia. Delete what you don't do.",
            listOf("delirium", "delirious", "confused", "muddled", "wandering", "disorientated", "sundowning"),
        ),
        ReviewTemplate(
            "Uncontrolled pain", "Pain not controlled", 2,
            "Consider: current chart incl. patches/PCA, renal function for opioid choice, " +
                "sedation score, aperients and antiemetic charted. Delete what you don't do.",
            listOf("analgesia", "sore", "breakthrough", "pain relief", "opioid", "prn"),
        ),
        ReviewTemplate(
            "Abnormal bloods", "Abnormal result", 2,
            "Consider: repeat/confirm sample, ECG if K+ or Ca abnormal, medication review, " +
                "trend against previous. Delete what you don't do.",
            listOf(
                "result", "electrolytes", "potassium", "hyperkalaemia", "hyperkalemia", "sodium",
                "hyponatraemia", "magnesium", "calcium", "inr", "hb", "anaemia", "creatinine",
            ),
        ),
        ReviewTemplate(
            "Nausea / vomiting", "Nausea and vomiting", 2,
            "Consider: cause (obstruction, ileus, drugs, infection, raised ICP), abdominal " +
                "exam and bowel sounds, antiemetic charted and by what route, hydration and " +
                "UEC, NG if obstructed. Delete what you don't do.",
            listOf("vomiting", "vomit", "nausea", "sick", "emesis", "throwing up", "retching", "n+v"),
        ),
        ReviewTemplate(
            "Constipation / BNO", "Bowels not open", 3,
            "Consider: last bowel motion, abdominal exam and PR if indicated, opioid and " +
                "anticholinergic review, aperients already charted, obstruction excluded " +
                "before stimulants, oral intake. Delete what you don't do.",
            listOf("bno", "constipated", "bowels", "aperient", "laxative", "not opened", "impaction"),
        ),
        ReviewTemplate(
            "Diarrhoea", "Diarrhoea", 2,
            "Consider: stool chart and frequency, isolation and infection control, C. diff " +
                "risk after antibiotics, UEC incl. K+, fluid balance, overflow from " +
                "constipation, medication review. Delete what you don't do.",
            listOf("diarrhea", "loose stool", "runny", "c diff", "cdiff", "gastro", "bowels open"),
        ),
        ReviewTemplate(
            "Seizure", "Seizure", 1,
            "Consider: airway and recovery position, duration and description, BSL, obs, " +
                "injury survey, antiepileptic chart and levels, alcohol withdrawal, " +
                "electrolytes incl. Na and Ca, post-ictal state, senior review. " +
                "Delete what you don't do.",
            listOf("fit", "fitting", "convulsion", "epilepsy", "status", "jerking"),
        ),
        ReviewTemplate(
            "Reduced GCS / unresponsive", "Reduced level of consciousness", 1,
            "Consider: A-B-C first, BSL, GCS and pupils, opioid or benzodiazepine given, obs " +
                "and O2, sepsis screen, neuro obs frequency, imaging decision with senior, " +
                "MET/code criteria. Delete what you don't do.",
            listOf("unresponsive", "drowsy", "gcs", "unconscious", "not waking", "obtunded", "collapsed", "floppy"),
        ),
        ReviewTemplate(
            "Hypertension", "Hypertension", 2,
            "Consider: manual recheck with the right cuff, symptoms (chest pain, headache, " +
                "visual, neuro deficit), pain / retention / anxiety as causes, missed usual " +
                "antihypertensives, and that a rapid drop does harm. Delete what you don't do.",
            listOf("high bp", "hypertensive", "bp up", "elevated bp", "raised bp"),
        ),
        ReviewTemplate(
            "Bleeding", "Bleeding", 1,
            "Consider: site and volume, obs and Hb trend, coags, anticoagulant/antiplatelet " +
                "chart and last dose, group and hold, pressure or packing applied, surgical " +
                "or endoscopy escalation. Delete what you don't do.",
            listOf(
                "bleed", "haemorrhage", "hemorrhage", "blood loss", "haematemesis", "melaena",
                "pr bleed", "epistaxis", "nose bleed", "ooze",
            ),
        ),
        ReviewTemplate(
            "Rash / allergic reaction", "Rash / allergic reaction", 1,
            "Airway, breathing and BP first: anaphylaxis is a clinical diagnosis, treated " +
                "before it is investigated — follow your hospital's anaphylaxis protocol, " +
                "and call for help early. Then consider: timing against recent drugs, " +
                "contrast and blood products, distribution, mucosal or airway involvement, " +
                "drug chart stopped and the allergy documented. Delete what you don't do.",
            listOf("rash", "itch", "itchy", "urticaria", "hives", "allergy", "allergic", "anaphylaxis", "reaction", "swelling"),
        ),
        ReviewTemplate(
            "Agitation / aggression", "Agitation", 1,
            "Your safety and the staff's comes first — get help before you get close. Then " +
                "consider: reversible causes (hypoxia, hypoglycaemia, retention, pain, " +
                "withdrawal, delirium), de-escalation before any medication, sedation only " +
                "per your hospital's policy and with monitoring afterwards, and clear " +
                "documentation of what was tried. Delete what you don't do.",
            listOf("aggressive", "violent", "agitated", "combative", "security", "code black", "restless", "hitting"),
        ),
        ReviewTemplate(
            "Wanting to leave (DAMA)", "Requesting discharge against advice", 2,
            "Consider: capacity assessed and documented, risks explained in their own " +
                "language and understood, senior and after-hours notified, family / NOK, " +
                "discharge medications and follow-up still offered, your hospital's DAMA " +
                "form completed. Delete what you don't do.",
            listOf("dama", "self discharge", "against advice", "wants to leave", "absconding", "going home"),
        ),
        ReviewTemplate(
            "Death verification", "Verification of death", 2,
            "Consider: identity confirmed, no response to stimulus, absent central pulse and " +
                "absent heart and breath sounds over the observed period, pupils fixed, time " +
                "of verification recorded, family and treating team notified, coroner " +
                "criteria considered. VERIFY: verification and certification requirements " +
                "differ by state and by hospital — follow your local policy and forms.",
            listOf("died", "deceased", "death", "verify", "certify", "passed away", "palliative", "rip"),
        ),
        ReviewTemplate(
            "Wound / drain concern", "Wound or drain concern", 2,
            "Consider: dressing taken down and the wound actually looked at, discharge " +
                "character, dehiscence or collection, drain volume and character over 24 h, " +
                "obs and inflammatory markers, photograph per policy, surgical team " +
                "notified. Delete what you don't do.",
            listOf("wound", "drain", "dressing", "dehiscence", "sutures", "staples", "leaking", "pus", "collection"),
        ),
        ReviewTemplate(
            "Not sleeping / sedation request", "Request for night sedation", 3,
            "Consider: the reason (pain, nocturia, delirium, environment, withdrawal), " +
                "non-drug measures first, that sedatives cause delirium and falls in the " +
                "elderly, what is already charted and their regular medications, falls " +
                "risk. Delete what you don't do.",
            listOf("sleep", "insomnia", "sedation", "temazepam", "awake", "night sedation", "cant sleep", "restless"),
        ),
        ReviewTemplate(
            "IV access / cannula", "IV access required", 3,
            "Consider: whether access is genuinely needed tonight or can wait for daylight, " +
                "previous sites and how difficult they were, ultrasound-guided or a more " +
                "experienced pair of hands, alternative routes (oral, subcut, IM), and that " +
                "central access is a senior decision. Delete what you don't do.",
            listOf("cannula", "ivc", "iv access", "drip", "tissued", "extravasation", "line", "bloods"),
        ),
        ReviewTemplate(
            "Post-op check", "Post-operative review", 3,
            "Consider: obs trend, pain, wound/drains, urine output, bloods, VTE prophylaxis, " +
                "operative plan from the op note. Delete what you don't do.",
            listOf("post op", "postop", "surgical review", "day 1", "theatre", "operation"),
        ),
    )

private val uroTemplates =
    listOf(
        ReviewTemplate(
            "Haematuria / clots", "Haematuria", 1,
            "Consider: catheter size and washout done, FBC (Hb trend), coags, group & hold, " +
                "anticoagulant review, CBI running. Delete what you don't do.",
            listOf("hematuria", "blood in urine", "clots", "frank haematuria", "pink urine", "cbi", "washout"),
        ),
        ReviewTemplate(
            "Blocked catheter", "Blocked / bypassing catheter", 1,
            "Consider: tubing and bag check, 50 mL flush result, bladder scan, catheter " +
                "change, retention symptoms. Delete what you don't do.",
            listOf("idc", "catheter", "bypassing", "not draining", "blocked", "leaking around", "flush"),
        ),
        ReviewTemplate(
            "Urinary retention", "Acute urinary retention", 1,
            "Consider: bladder scan, residual drained, cause (BPH, constipation, drugs, " +
                "neuro), UEC, alpha-blocker started, TOV plan. Delete what you don't do.",
            listOf("retention", "cant pass urine", "bladder scan", "residual", "tov", "aur", "distended"),
        ),
        ReviewTemplate(
            "Stent pain", "Ureteric stent symptoms", 2,
            "Consider: temp and infection screen, analgesia, alpha-blocker, stent removal " +
                "date documented. Delete what you don't do.",
            listOf("stent", "jj stent", "ureteric", "loin pain", "colic"),
        ),
        ReviewTemplate(
            "Fever with stent/catheter", "Fever with urological device", 1,
            "Consider: cultures before antibiotics, lactate, imaging for obstruction, " +
                "urgent senior review — infected obstruction is an emergency. " +
                "Delete what you don't do.",
            listOf("infected obstruction", "urosepsis", "septic stent", "pyonephrosis", "febrile stent"),
        ),
    )

/** Urology templates come first in UroDay; the general set follows. */
val reviewTemplates: List<ReviewTemplate> =
    if (BuildConfig.URO) uroTemplates + generalTemplates else generalTemplates
