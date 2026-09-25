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
    /** Why they might be presenting like this — the differential, not a finding. */
    val thinkAbout: String = "",
    val aliases: List<String> = emptyList(),
)

private val generalTemplates =
    listOf(
        ReviewTemplate(
            "Chest pain", "Chest pain", 1,
            "Consider: ECG within 10 min (compare old), troponin per pathway, CXR, FBC/UEC, " +
                "obs trend. Delete what you don't do.",
            "ACS (atypical or silent in diabetics, women and the elderly), PE, aortic dissection, " +
                "pericarditis, pneumothorax, pneumonia or pleurisy, oesophageal spasm and reflux, " +
                "musculoskeletal, anxiety. The ones that kill quickly: ACS, PE, dissection, tension " +
                "pneumothorax.",
            listOf("acs", "angina", "cardiac", "crushing", "heart"),
        ),
        ReviewTemplate(
            "Short of breath", "Shortness of breath / hypoxia", 1,
            "Consider: RR counted, sats + O2 requirement, ECG, CXR, ABG/VBG if unwell, " +
                "FBC/UEC. Delete what you don't do.",
            "APO, pneumonia, PE, asthma or COPD exacerbation, pneumothorax, effusion, mucus " +
                "plugging, aspiration, anaemia, metabolic acidosis (deep sighing breathing), anxiety. " +
                "Post-op, think PE and atelectasis; on a medical ward, think APO and infection. New " +
                "oxygen requirement is the finding that matters, not the number alone.",
            listOf("sob", "dyspnoea", "dyspnea", "desat", "desaturating", "hypoxia", "breathless", "sats", "oxygen"),
        ),
        ReviewTemplate(
            "Hypotension", "Hypotension", 1,
            "Consider: manual BP recheck, fluid balance + UO, lactate, FBC/UEC, cultures if " +
                "febrile, medication chart review. Delete what you don't do.",
            "Sepsis, hypovolaemia (bleeding, third-spacing, poor intake), cardiogenic (MI, " +
                "arrhythmia, tamponade), anaphylaxis, drugs (antihypertensives, opioids, sedation, " +
                "epidural), adrenal insufficiency, PE. First ask whether it is real: wrong cuff size, " +
                "a damped arterial line, an arm above the heart.",
            listOf("low bp", "hypotensive", "soft bp", "shocked", "map"),
        ),
        ReviewTemplate(
            "Tachycardia", "Tachycardia", 1,
            "Consider: 12-lead ECG, obs trend, FBC/UEC/Mg, TFT if new AF, cultures if febrile. " +
                "Delete what you don't do.",
            "Pain, fever and sepsis, hypovolaemia or bleeding, AF and other arrhythmias, PE, " +
                "hypoxia, alcohol or benzodiazepine withdrawal, thyrotoxicosis, drugs (salbutamol, " +
                "inotropes, anticholinergics), anxiety. Sinus tachycardia is a symptom with a cause, " +
                "not a diagnosis.",
            listOf("fast heart rate", "tachy", "af", "svt", "palpitations", "high hr"),
        ),
        ReviewTemplate(
            "Fever / ?sepsis", "Fever", 1,
            "Consider: blood cultures x2 before antibiotics, lactate, FBC/UEC/CRP, urine MCS, " +
                "CXR, line/wound review. Delete what you don't do.",
            "Sources: chest, urine, line, wound or surgical site, abdomen, skin, CNS, joint. " +
                "Non-infective: DVT or PE, drug fever, transfusion reaction, malignancy, and early " +
                "post-op atelectasis. In the neutropenic or immunosuppressed, treat first and " +
                "investigate second.",
            listOf("temp", "temperature", "febrile", "sepsis", "septic", "rigors", "hot", "infection"),
        ),
        ReviewTemplate(
            "Fall", "Fall", 2,
            "Consider: injury survey incl. head and hips, neuro obs if head strike, " +
                "anticoagulant check, postural BP, ECG, BSL, imaging as indicated, " +
                "incident report. Delete what you don't do.",
            "Mechanical or environmental, postural hypotension, arrhythmia or syncope, " +
                "hypoglycaemia, sepsis, delirium, stroke or seizure, alcohol and withdrawal, " +
                "sedatives and antihypertensives, poor vision or footwear. Ask whether they blacked " +
                "out — a syncope and a trip are different problems with different workups.",
            listOf("fell", "found on floor", "slipped", "head strike", "tumble"),
        ),
        ReviewTemplate(
            "Low urine output", "Oliguria", 2,
            "Consider: catheter patency / bladder scan, fluid balance, UEC incl. K+, " +
                "nephrotoxin review, BP trend. Delete what you don't do.",
            "Pre-renal: hypovolaemia, sepsis, cardiac failure, hepatorenal. Renal: ATN, " +
                "nephrotoxins (NSAIDs, contrast, aminoglycosides, ACEi/ARB), rhabdomyolysis. " +
                "Post-renal: blocked catheter, retention, obstruction. Exclude the blocked catheter " +
                "first — it is the commonest cause on a ward and the quickest to fix.",
            listOf("oliguria", "anuria", "not passing urine", "low uo", "no urine", "aki", "kidney"),
        ),
        ReviewTemplate(
            "Hypoglycaemia", "Hypoglycaemia", 1,
            "Consider: BSL now and at 15 min, treatment given, insulin/sulfonylurea chart " +
                "review, cause, repeat risk. Delete what you don't do.",
            "Insulin or sulfonylurea (dose, timing, wrong insulin given), missed or delayed meal, " +
                "NBM without an insulin plan, steroids reduced or stopped, renal impairment " +
                "prolonging insulin, liver disease, sepsis, alcohol, adrenal insufficiency. A " +
                "sulfonylurea hypo will come back — it needs prolonged observation, not one juice.",
            listOf("hypo", "hypoglycemia", "low bsl", "low sugar", "bsl", "glucose", "diabetes"),
        ),
        ReviewTemplate(
            "Confusion / delirium", "Acute confusion", 2,
            "Consider: BSL, obs, bladder scan for retention, bowels, infection screen, " +
                "medication review, pain, hypoxia. Delete what you don't do.",
            "Infection (urine, chest), hypoxia, hypoglycaemia, urinary retention, constipation, " +
                "pain, medications (opioids, anticholinergics, benzodiazepines, steroids), withdrawal " +
                "(alcohol, nicotine, benzodiazepines), electrolytes (Na, Ca), post-ictal, stroke, " +
                "dehydration, sleep and environment. Quiet hypoactive delirium is missed far more " +
                "often than the agitated kind.",
            listOf("delirium", "delirious", "confused", "muddled", "wandering", "disorientated", "sundowning"),
        ),
        ReviewTemplate(
            "Uncontrolled pain", "Pain not controlled", 2,
            "Consider: current chart incl. patches/PCA, renal function for opioid choice, " +
                "sedation score, aperients and antiemetic charted. Delete what you don't do.",
            "Is this the expected pain getting worse, or a new pain? New or escalating " +
                "post-operative pain: compartment syndrome, ischaemia, anastomotic leak, perforation, " +
                "obstruction, urinary retention, wound infection or haematoma, DVT. Otherwise: " +
                "inadequate charting, wrong route, opioid tolerance, or neuropathic pain that will " +
                "not answer to opioids.",
            listOf("analgesia", "sore", "breakthrough", "pain relief", "opioid", "prn"),
        ),
        ReviewTemplate(
            "Abnormal bloods", "Abnormal result", 2,
            "Consider: repeat/confirm sample, ECG if K+ or Ca abnormal, medication review, " +
                "trend against previous. Delete what you don't do.",
            "First — is it real? Haemolysed sample, drip-arm or line contamination, wrong tube, " +
                "wrong patient, or an old result being read as new. Then the cause and, more " +
                "importantly, the trend. Potassium, sodium and glucose each have their own guide in " +
                "the Guides tab.",
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
            "Bowel obstruction or ileus, drugs (opioids, antibiotics, chemotherapy), infection or " +
                "gastroenteritis, constipation, post-operative nausea, raised intracranial pressure, " +
                "hypercalcaemia, uraemia, DKA, vestibular causes, pregnancy, anxiety. Vomiting with " +
                "distension and no flatus is obstruction until proven otherwise.",
            listOf("vomiting", "vomit", "nausea", "sick", "emesis", "throwing up", "retching", "n+v"),
        ),
        ReviewTemplate(
            "Constipation / BNO", "Bowels not open", 3,
            "Consider: last bowel motion, abdominal exam and PR if indicated, opioid and " +
                "anticholinergic review, aperients already charted, obstruction excluded " +
                "before stimulants, oral intake. Delete what you don't do.",
            "Opioids and anticholinergics, immobility, dehydration and poor intake, " +
                "hypercalcaemia, hypothyroidism, hypokalaemia, obstruction, spinal cord compression " +
                "(new back pain or neurology — an emergency), and impaction with overflow. Exclude " +
                "obstruction before giving a stimulant laxative.",
            listOf("bno", "constipated", "bowels", "aperient", "laxative", "not opened", "impaction"),
        ),
        ReviewTemplate(
            "Diarrhoea", "Diarrhoea", 2,
            "Consider: stool chart and frequency, isolation and infection control, C. diff " +
                "risk after antibiotics, UEC incl. K+, fluid balance, overflow from " +
                "constipation, medication review. Delete what you don't do.",
            "Antibiotic-associated and C. difficile, overflow around impaction, enteral feed, " +
                "laxatives, infective gastroenteritis, IBD flare, ischaemic colitis, post-operative. " +
                "New diarrhoea after antibiotics is C. difficile until tested, and isolation should " +
                "not wait for the result.",
            listOf("diarrhea", "loose stool", "runny", "c diff", "cdiff", "gastro", "bowels open"),
        ),
        ReviewTemplate(
            "Seizure", "Seizure", 1,
            "Consider: airway and recovery position, duration and description, BSL, obs, " +
                "injury survey, antiepileptic chart and levels, alcohol withdrawal, " +
                "electrolytes incl. Na and Ca, post-ictal state, senior review. " +
                "Delete what you don't do.",
            "Known epilepsy with missed doses or low levels, alcohol or benzodiazepine " +
                "withdrawal, hypoglycaemia, hyponatraemia, hypocalcaemia, sepsis or meningitis, " +
                "stroke or bleed, head injury, drugs that lower the threshold (tramadol, some " +
                "antibiotics), eclampsia in pregnancy, hypoxia. A seizure that does not stop is " +
                "status — call for help early.",
            listOf("fit", "fitting", "convulsion", "epilepsy", "status", "jerking"),
        ),
        ReviewTemplate(
            "Reduced GCS / unresponsive", "Reduced level of consciousness", 1,
            "Consider: A-B-C first, BSL, GCS and pupils, opioid or benzodiazepine given, obs " +
                "and O2, sepsis screen, neuro obs frequency, imaging decision with senior, " +
                "MET/code criteria. Delete what you don't do.",
            "Hypoglycaemia, opioid or benzodiazepine effect, hypoxia or hypercapnia, sepsis, " +
                "stroke or bleed, post-ictal state, hyponatraemia, hypercalcaemia, uraemia, hepatic " +
                "encephalopathy, hypothermia, alcohol and drugs. A BSL and the drug chart come before " +
                "anything clever.",
            listOf("unresponsive", "drowsy", "gcs", "unconscious", "not waking", "obtunded", "collapsed", "floppy"),
        ),
        ReviewTemplate(
            "Hypertension", "Hypertension", 2,
            "Consider: manual recheck with the right cuff, symptoms (chest pain, headache, " +
                "visual, neuro deficit), pain / retention / anxiety as causes, missed usual " +
                "antihypertensives, and that a rapid drop does harm. Delete what you don't do.",
            "Pain, urinary retention, anxiety, missed usual antihypertensives, withdrawal " +
                "(alcohol, clonidine, beta-blocker), raised intracranial pressure, pre-eclampsia in " +
                "pregnancy, and the wrong cuff. Asymptomatic high BP overnight is rarely the " +
                "emergency; end-organ symptoms are. VERIFY: dropping it fast causes harm — follow " +
                "your hospital's protocol and involve a senior.",
            listOf("high bp", "hypertensive", "bp up", "elevated bp", "raised bp"),
        ),
        ReviewTemplate(
            "Bleeding", "Bleeding", 1,
            "Consider: site and volume, obs and Hb trend, coags, anticoagulant/antiplatelet " +
                "chart and last dose, group and hold, pressure or packing applied, surgical " +
                "or endoscopy escalation. Delete what you don't do.",
            "Surgical or procedural site, GI (upper vs lower), anticoagulants and antiplatelets, " +
                "thrombocytopenia, liver disease, DIC, line or drain site, epistaxis, urinary tract. " +
                "Estimate the rate, not just what is in the bag — a young patient holds their obs " +
                "until they do not.",
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
            "Anaphylaxis (airway, breathing, circulation — treated before it is investigated), " +
                "drug eruption, contrast or blood product reaction, urticaria, cellulitis, viral " +
                "exanthem, and severe drug reactions with fever, mucosal involvement, blistering or " +
                "skin pain — those are emergencies, not 'just a rash'.",
            listOf("rash", "itch", "itchy", "urticaria", "hives", "allergy", "allergic", "anaphylaxis", "reaction", "swelling"),
        ),
        ReviewTemplate(
            "Agitation / aggression", "Agitation", 1,
            "Your safety and the staff's comes first — get help before you get close. Then " +
                "consider: reversible causes (hypoxia, hypoglycaemia, retention, pain, " +
                "withdrawal, delirium), de-escalation before any medication, sedation only " +
                "per your hospital's policy and with monitoring afterwards, and clear " +
                "documentation of what was tried. Delete what you don't do.",
            "Delirium from any cause, hypoxia, hypoglycaemia, urinary retention, pain, alcohol or " +
                "benzodiazepine withdrawal, intoxication, head injury, dementia with an acute " +
                "stressor, psychiatric illness, and plain fear and disorientation. Look for the " +
                "reversible cause before reaching for sedation.",
            listOf("aggressive", "violent", "agitated", "combative", "security", "code black", "restless", "hitting"),
        ),
        ReviewTemplate(
            "Wanting to leave (DAMA)", "Requesting discharge against advice", 2,
            "Consider: capacity assessed and documented, risks explained in their own " +
                "language and understood, senior and after-hours notified, family / NOK, " +
                "discharge medications and follow-up still offered, your hospital's DAMA " +
                "form completed. Delete what you don't do.",
            "Capacity is decision-specific and fluctuates — delirium, intoxication, hypoxia and " +
                "pain all affect it. Ask why they want to go: uncontrolled pain, withdrawal, a carer " +
                "or a pet at home, feeling unheard. Fixing that often ends the conversation. If they " +
                "lack capacity this is not a DAMA — escalate.",
            listOf("dama", "self discharge", "against advice", "wants to leave", "absconding", "going home"),
        ),
        ReviewTemplate(
            "Death verification", "Verification of death", 2,
            "Consider: identity confirmed, no response to stimulus, absent central pulse and " +
                "absent heart and breath sounds over the observed period, pupils fixed, time " +
                "of verification recorded, family and treating team notified, coroner " +
                "criteria considered. VERIFY: verification and certification requirements " +
                "differ by state and by hospital — follow your local policy and forms.",
            "Confirm identity. Check for an implanted device (pacemaker or ICD) and for anything " +
                "that makes the death reportable: unexpected, within 24 hours of a procedure or " +
                "anaesthetic, after injury or a fall, in care, or cause unknown. VERIFY: reportable " +
                "criteria, who may verify, and the paperwork all differ by state and by hospital.",
            listOf("died", "deceased", "death", "verify", "certify", "passed away", "palliative", "rip"),
        ),
        ReviewTemplate(
            "Wound / drain concern", "Wound or drain concern", 2,
            "Consider: dressing taken down and the wound actually looked at, discharge " +
                "character, dehiscence or collection, drain volume and character over 24 h, " +
                "obs and inflammatory markers, photograph per policy, surgical team " +
                "notified. Delete what you don't do.",
            "Infection, haematoma, seroma, dehiscence, anastomotic leak, pressure injury, and " +
                "necrotising infection — pain out of proportion, rapid change and systemic upset make " +
                "that a surgical emergency. A sudden rise in serous drain output after abdominal " +
                "surgery can be dehiscence.",
            listOf("wound", "drain", "dressing", "dehiscence", "sutures", "staples", "leaking", "pus", "collection"),
        ),
        ReviewTemplate(
            "Not sleeping / sedation request", "Request for night sedation", 3,
            "Consider: the reason (pain, nocturia, delirium, environment, withdrawal), " +
                "non-drug measures first, that sedatives cause delirium and falls in the " +
                "elderly, what is already charted and their regular medications, falls " +
                "risk. Delete what you don't do.",
            "Pain, nocturia, breathlessness, delirium, alcohol or nicotine withdrawal, restless " +
                "legs, anxiety, steroids given late, and simple ward noise and light. Sedatives " +
                "increase falls, delirium and aspiration in the elderly — treat the cause where there " +
                "is one.",
            listOf("sleep", "insomnia", "sedation", "temazepam", "awake", "night sedation", "cant sleep", "restless"),
        ),
        ReviewTemplate(
            "IV access / cannula", "IV access required", 3,
            "Consider: whether access is genuinely needed tonight or can wait for daylight, " +
                "previous sites and how difficult they were, ultrasound-guided or a more " +
                "experienced pair of hands, alternative routes (oral, subcut, IM), and that " +
                "central access is a senior decision. Delete what you don't do.",
            "First ask what it is for: if it is for something that can wait for daylight, that is " +
                "an answer. Consider oral, subcutaneous or IM alternatives, how difficult access has " +
                "been before, dehydration, and whether a midline or PICC is the real answer — a " +
                "daytime senior decision, not a 3 am one.",
            listOf("cannula", "ivc", "iv access", "drip", "tissued", "extravasation", "line", "bloods"),
        ),
        ReviewTemplate(
            "Post-op check", "Post-operative review", 3,
            "Consider: obs trend, pain, wound/drains, urine output, bloods, VTE prophylaxis, " +
                "operative plan from the op note. Delete what you don't do.",
            "By day: 1-2 atelectasis and pain, 3-5 pneumonia, UTI and DVT, 5-7 anastomotic leak, " +
                "collection and wound infection. At any point: bleeding, MI, PE, ileus, urinary " +
                "retention, delirium. Read the op note — what you are looking for is usually named in " +
                "it.",
            listOf("post op", "postop", "surgical review", "day 1", "theatre", "operation"),
        ),
    )

private val uroTemplates =
    listOf(
        ReviewTemplate(
            "Haematuria / clots", "Haematuria", 1,
            "Consider: catheter size and washout done, FBC (Hb trend), coags, group & hold, " +
                "anticoagulant review, CBI running. Delete what you don't do.",
            "Clot retention, recent TURP or TURBT, catheter trauma, UTI, stones, malignancy " +
                "(bladder, renal, prostate), anticoagulation, radiation cystitis. Frank haematuria " +
                "with clots is a plumbing problem first: wash it out, then investigate it.",
            listOf("hematuria", "blood in urine", "clots", "frank haematuria", "pink urine", "cbi", "washout"),
        ),
        ReviewTemplate(
            "Blocked catheter", "Blocked / bypassing catheter", 1,
            "Consider: tubing and bag check, 50 mL flush result, bladder scan, catheter " +
                "change, retention symptoms. Delete what you don't do.",
            "Kinked tubing, bag above bladder height, a clamp left on, balloon sitting in the " +
                "urethra, clot, sediment or debris, constipation compressing the outlet, or a " +
                "catheter too small for what it is draining. Bypassing with no drainage means blocked " +
                "— not 'needs more water in the balloon'.",
            listOf("idc", "catheter", "bypassing", "not draining", "blocked", "leaking around", "flush"),
        ),
        ReviewTemplate(
            "Urinary retention", "Acute urinary retention", 1,
            "Consider: bladder scan, residual drained, cause (BPH, constipation, drugs, " +
                "neuro), UEC, alpha-blocker started, TOV plan. Delete what you don't do.",
            "BPH, constipation, UTI, drugs (anticholinergics, opioids, alpha-agonists), " +
                "post-operative and post-anaesthetic, clot retention, urethral stricture, and " +
                "neurological causes — cauda equina with new back pain or saddle numbness is an " +
                "emergency. Painless massive retention is chronic: beware post-obstructive diuresis.",
            listOf("retention", "cant pass urine", "bladder scan", "residual", "tov", "aur", "distended"),
        ),
        ReviewTemplate(
            "Stent pain", "Ureteric stent symptoms", 2,
            "Consider: temp and infection screen, analgesia, alpha-blocker, stent removal " +
                "date documented. Delete what you don't do.",
            "Expected stent symptoms (frequency, urgency, loin pain on voiding, haematuria), " +
                "stent migration, infection above an obstruction, a stone elsewhere, or encrustation " +
                "in a forgotten stent. Fever with a stent is not stent pain.",
            listOf("stent", "jj stent", "ureteric", "loin pain", "colic"),
        ),
        ReviewTemplate(
            "Fever with stent/catheter", "Fever with urological device", 1,
            "Consider: cultures before antibiotics, lactate, imaging for obstruction, " +
                "urgent senior review — infected obstruction is an emergency. " +
                "Delete what you don't do.",
            "Infected obstruction — pyonephrosis — is the one to exclude tonight. Also catheter- " +
                "associated UTI, prostatitis, epididymo-orchitis, or a source that has nothing to do " +
                "with the device. Obstruction plus infection needs drainage, not just antibiotics.",
            listOf("infected obstruction", "urosepsis", "septic stent", "pyonephrosis", "febrile stent"),
        ),
    )

/** Urology templates come first in UroDay; the general set follows. */
val reviewTemplates: List<ReviewTemplate> =
    if (BuildConfig.URO) uroTemplates + generalTemplates else generalTemplates

/**
 * The template a review was started from, if it is still one we ship.
 *
 * Guidance is looked up rather than copied into the row, so a review started
 * last week shows today's version of the differential.
 */
fun templateFor(key: String): ReviewTemplate? =
    if (key.isBlank()) null else reviewTemplates.firstOrNull { it.label == key }
