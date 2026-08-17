package com.nightshift.tracker.ui.guides

// Quick-reference cheat sheets for common overnight calls, written for an
// Australian JMO. Aligned with Therapeutic Guidelines (eTG) style practice
// where relevant, BUT: this is a memory aid, not a prescribing reference.
// Anything marked VERIFY means: confirm against current eTG / your hospital
// guideline before prescribing — dosing changes, and local protocols win.

data class GuideSection(
    val heading: String,
    val lines: List<String>,
)

data class Guide(
    val title: String,
    val oneLiner: String,
    val sections: List<GuideSection>,
    val contraindications: List<String>, // rendered as the red section
)

const val GUIDES_DISCLAIMER =
    "Memory aid only — not a substitute for eTG, MIMS or your hospital's protocols. " +
        "Doses marked VERIFY need checking before you prescribe. When in doubt, call your registrar. " +
        "Content current to early 2026; guidelines change."

val guides =
    listOf(
        Guide(
            title = "IV fluids",
            oneLiner = "Maintenance vs replacement vs resus — decide which one you're doing first.",
            sections =
                listOf(
                    GuideSection(
                        "Maintenance (patient simply nil by mouth)",
                        listOf(
                            "25–30 mL/kg/day water ≈ 2–2.5 L/day for most adults. Less (20–25) if elderly, frail, cardiac or renal failure.",
                            "≈1 mmol/kg/day each of Na+ and K+, and 50–100 g/day glucose to prevent starvation ketosis.",
                            "A reasonable adult recipe: sodium chloride 0.9% or Plasma-Lyte alternating with glucose 5%, with KCl added per K+ — but check today's UEC first, not yesterday's.",
                            "Reassess daily. 'Maintenance' running for 4 days without review is how people drown or drop their sodium.",
                        ),
                    ),
                    GuideSection(
                        "Resuscitation (shocked / hypotensive)",
                        listOf(
                            "Balanced crystalloid (Plasma-Lyte, Hartmann's) or sodium chloride 0.9%: 250–500 mL bolus, run fast, REASSESS after every bag: BP, HR, lung bases, urine output.",
                            "Smaller boluses (250 mL) in heart failure, elderly, renal failure.",
                            "If 1–2 L in and still shocked: that's an escalation, not a third litre. Think sepsis, bleeding, cardiogenic.",
                        ),
                    ),
                    GuideSection(
                        "Choosing the bag",
                        listOf(
                            "Glucose 5% is water, not volume — never for resuscitation.",
                            "Large volumes of 0.9% saline cause hyperchloraemic acidosis; balanced solutions preferred for big-volume resus.",
                            "Hartmann's contains K+ ~5 mmol/L — usually still fine in hyperkalaemia, but many hospitals prefer saline there; follow local practice.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Do NOT write a K+-containing bag without today's potassium result and urine output.",
                    "Never give K+ faster than 10 mmol/hr on a general ward (local policies vary — check yours).",
                    "Glucose 5% alone in hyponatraemia will make it worse.",
                    "Caution boluses in severe aortic stenosis, dialysis patients and APO — small volumes, early senior help.",
                ),
        ),
        Guide(
            title = "Hypotension",
            oneLiner = "Trend + context + perfusion, not the number alone.",
            sections =
                listOf(
                    GuideSection(
                        "First 5 minutes",
                        listOf(
                            "Recheck yourself: manual cuff, right size, both arms if odd.",
                            "Compare with their normal — SBP 100 in a young woman is baseline; in a hypertensive 80-year-old it's shock.",
                            "Perfusion check: conscious state, cap refill, peripheries, urine output, lactate if gas available.",
                            "MET criteria (commonly SBP <90, but know your hospital's): if met, call — that's what the system is for.",
                        ),
                    ),
                    GuideSection(
                        "Common overnight causes",
                        listOf(
                            "Drugs: night-time antihypertensives, tamsulosin, opioids, epidural running. Hold and document.",
                            "Hypovolaemia: poor intake, diuretics, drains, diarrhoea, bleeding (check the drain, the PR history, the Hb trend).",
                            "Sepsis: fever OR hypothermia + new hypotension = septic until proven otherwise. Cultures, lactate, antibiotics early.",
                            "Cardiogenic: new AF, ischaemia, APO — get an ECG.",
                            "Post-op: bleeding until proven otherwise. Look at the wound and drains, trend the Hb.",
                        ),
                    ),
                    GuideSection(
                        "Action",
                        listOf(
                            "IV access, bloods if none recent (FBC, UEC, ± lactate, ± cultures), 250–500 mL crystalloid bolus, reassess.",
                            "Chart hold on antihypertensives for the morning round to review.",
                            "Document your review and your escalation threshold: 'if SBP <X or not responding to Y, call'.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Don't fluid-slam APO / decompensated heart failure — small bolus, sit up, senior help.",
                    "Don't sit on hypotension + fever waiting for the morning: sepsis kills overnight. Antibiotics within the hour.",
                    "GTN, epidural top-ups and more opioid all lower BP further — review the chart before the next dose.",
                ),
        ),
        Guide(
            title = "Tachycardia",
            oneLiner = "ECG first. Unstable = call. Sinus tachy has a cause — find it.",
            sections =
                listOf(
                    GuideSection(
                        "Unstable? (any of these → MET/arrest call now)",
                        listOf(
                            "Hypotension, chest pain, syncope/pre-syncope, APO, GCS drop.",
                            "Unstable tachyarrhythmia is managed with synchronised cardioversion by seniors — your job is recognition and the phone call.",
                        ),
                    ),
                    GuideSection(
                        "Stable: get a 12-lead and classify",
                        listOf(
                            "Narrow + regular: sinus tachy (rate usually <150, p-waves) vs SVT (often ~150–220, sudden onset).",
                            "Narrow + irregular: AF. New AF overnight is commonly driven by something — sepsis, hypoxia, PE, bleeding, dry, electrolytes.",
                            "Broad complex: VT until proven otherwise. Senior review even if 'stable'.",
                            "Sinus tachycardia is a symptom, not a diagnosis: pain, fever, hypovolaemia, bleeding, PE, withdrawal, retention. Treat the cause, not the rate.",
                        ),
                    ),
                    GuideSection(
                        "SVT (regular narrow complex)",
                        listOf(
                            "Vagal manoeuvres first: modified Valsalva (blow into 10 mL syringe, then legs up) — decent success rate, zero drugs.",
                            "Adenosine (if protocolised at your site and you're supported): 6 mg rapid IV push + flush via a big proximal vein; then 12 mg, then 18 mg if needed. VERIFY against local protocol — and warn the patient about the impending-doom feeling.",
                            "Continuous ECG running during adenosine — the strip is diagnostic gold.",
                        ),
                    ),
                    GuideSection(
                        "Fast AF (stable)",
                        listOf(
                            "Fix the driver first: fluids if dry, treat sepsis, correct K+ (aim >4.0) and Mg2+ (aim >0.8–1.0).",
                            "Rate control choice (beta-blocker vs digoxin vs amiodarone) is a registrar conversation overnight, not a solo intern decision.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Adenosine: avoid in severe asthma/bronchospasm; contraindicated in heart transplant patients (profound response); ineffective/risky diagnosis-dependent — senior support first.",
                    "Never give verapamil or other AV blockers in broad-complex tachycardia — VT + verapamil can be fatal.",
                    "AF + pre-excitation (WPW, delta waves): AV-nodal blockers (adenosine, digoxin, verapamil) are dangerous — senior help.",
                    "Don't beta-block the compensatory tachycardia of sepsis or hypovolaemia.",
                ),
        ),
        Guide(
            title = "Chest pain",
            oneLiner = "ECG within 10 minutes, eyes on it, compare with old.",
            sections =
                listOf(
                    GuideSection(
                        "Immediate",
                        listOf(
                            "12-lead ECG within 10 min of the call, and YOU look at it (with the old one side by side).",
                            "Obs, IV access, troponin + repeat per your pathway (high-sensitivity assays commonly 0 and 2 h — follow local).",
                            "Quick differential sweep: ACS, PE, dissection (tearing, BP differential), pneumothorax, oesophageal, musculoskeletal, shingles.",
                        ),
                    ),
                    GuideSection(
                        "If ACS suspected",
                        listOf(
                            "Aspirin 300 mg chewed, unless true allergy or active bleeding.",
                            "GTN 400 microg sublingual if SBP >100 and no contraindication (below); may repeat per chart.",
                            "STEMI criteria on ECG = code/cath-lab activation per your hospital — that call is immediate, not after the troponin.",
                            "Ongoing pain, dynamic ECG changes, or positive troponin: registrar/cardiology now, continuous monitoring.",
                        ),
                    ),
                    GuideSection(
                        "Don't miss",
                        listOf(
                            "PE: pleuritic pain + tachycardia + hypoxia + risk factors; Wells/PERC thinking, don't anchor on 'anxiety'.",
                            "Dissection: severe tearing pain to the back, syncope, pulse/BP differential — this changes everything (no anticoagulation).",
                            "New oxygen requirement with chest pain is never reassuring.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "NO GTN if PDE5 inhibitor taken (sildenafil/vardenafil within ~24 h, tadalafil within ~48 h) — profound hypotension.",
                    "NO GTN in hypotension or suspected RV infarct (inferior STEMI with V4R changes).",
                    "Hold anticoagulation thoughts if dissection is on the table.",
                    "Don't give repeated opioids to make undiagnosed chest pain 'settle' without escalation.",
                ),
        ),
        Guide(
            title = "Breathlessness / hypoxia",
            oneLiner = "Sit them up, oxygen titrated to target, then find the cause.",
            sections =
                listOf(
                    GuideSection(
                        "Immediate",
                        listOf(
                            "Sit upright, count RR yourself, sats + work of breathing.",
                            "O2 targets: 92–96% for most; 88–92% if chronic CO2 retention (COPD, obesity hypoventilation) — titrate, don't blast.",
                            "Focused exam: trachea, air entry both sides, crackles vs wheeze vs silence, calves, JVP.",
                            "ECG + CXR for almost everyone; ABG/VBG if sick or retainer.",
                        ),
                    ),
                    GuideSection(
                        "Pattern recognition",
                        listOf(
                            "APO: crackles, pink frothy sputum, JVP up, history of HF or big fluid balance. Sit up, GTN if SBP allows, furosemide (frusemide) 40 mg IV (higher if already on it — VERIFY dose vs their usual), escalate early — NIV decisions are senior ones.",
                            "Pneumonia: fever, purulent sputum, focal crackles → cultures, antibiotics (see antibiotics card).",
                            "PE: sudden, clear chest, pleuritic, risk factors — don't wait for the perfect CTPA at 3 am to escalate.",
                            "Asthma/COPD: wheeze → salbutamol 4–12 puffs via spacer or 5 mg neb, ipratropium, steroids; silent chest = peri-arrest.",
                            "Pneumothorax: sudden pleuritic pain + reduced air entry; tension (tracheal shift, shock) = immediate senior + decompression, don't go to X-ray.",
                            "Anxiety/hyperventilation is a diagnosis of exclusion at night. Normal sats + normal RR trend + reproducible story, and even then — document your negative findings.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Don't cap oxygen in a genuinely hypoxic retainer — hypoxia kills first; titrate with a gas, get help.",
                    "Furosemide in a dry, hypotensive patient with crackles that are actually pneumonia makes them worse — perfusion check first.",
                    "High-flow O2 masks a deteriorating patient: rising O2 requirement is an escalation trigger, not a fix.",
                ),
        ),
        Guide(
            title = "Hypoglycaemia",
            oneLiner = "BSL <4.0 with symptoms (or <3.0 regardless): treat now, find the cause after.",
            sections =
                listOf(
                    GuideSection(
                        "Conscious and can swallow",
                        listOf(
                            "15 g fast-acting carbohydrate: ~150 mL juice or soft drink (not diet), or glucose tablets/jellybeans per ward stock.",
                            "Recheck BSL in 15 min; repeat 15 g if still <4.0.",
                            "Once >4.0: give longer-acting carbs (sandwich, biscuits + milk) unless a meal is imminent.",
                        ),
                    ),
                    GuideSection(
                        "Unconscious, unsafe swallow, or NBM",
                        listOf(
                            "IV access: glucose 10% 150 mL IV over ~15 min, or glucose 50% 15–20 mL via a good vein (irritant) — local protocols pick one; VERIFY yours.",
                            "No IV access: glucagon 1 mg IM (less effective if depleted glycogen — liver disease, alcohol, repeated hypos).",
                            "Recheck BSL at 15 min, then regularly — treat again as needed.",
                        ),
                    ),
                    GuideSection(
                        "Then think",
                        listOf(
                            "Why? Missed meal + usual insulin, sulfonylurea, renal decline, sepsis, alcohol, wrong dose charted.",
                            "Sulfonylurea or long-acting insulin hypos RECUR — ongoing glucose, frequent BSLs, consider infusion, tell your registrar.",
                            "Review tonight's remaining insulin doses before you walk away, and document the episode.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Never give 50% glucose fast through a tiny peripheral vein — extravasation causes necrosis; dilute options preferred.",
                    "Don't withhold ALL insulin in type 1 diabetes after a hypo — they always need basal insulin; adjust, don't stop (ask if unsure).",
                    "Glucagon is unreliable in alcohol-related or hepatic hypoglycaemia — get IV access.",
                ),
        ),
        Guide(
            title = "Hyperkalaemia",
            oneLiner = "ECG + calcium first if changes; shift K+ in; stop the source; recheck.",
            sections =
                listOf(
                    GuideSection(
                        "Severity & first moves",
                        listOf(
                            "Mild 5.5–5.9, moderate 6.0–6.4, severe ≥6.5 or any ECG changes (peaked T, wide QRS, flat P, bradycardia).",
                            "K+ ≥6.0 or any symptoms/ECG changes: continuous monitoring, urgent 12-lead, escalate.",
                            "Is it real? Haemolysed sample is common — but never assume: repeat urgently WHILE treating if the ECG is abnormal or K+ severe.",
                        ),
                    ),
                    GuideSection(
                        "Treatment ladder (severe or ECG changes)",
                        listOf(
                            "1. Protect the heart: calcium gluconate 10% 10 mL IV over 5–10 min (monitored); repeat in 5–10 min if ECG unchanged.",
                            "2. Shift: insulin (short-acting, e.g. Actrapid) 10 units IV WITH glucose 25 g (e.g. 50 mL of 50%) — then BSL checks q30–60 min for hours; hypos are common and late. VERIFY exact recipe on your hospital's hyperkalaemia protocol.",
                            "3. Shift more: salbutamol 10–20 mg nebulised (caution tachyarrhythmia).",
                            "4. Remove: treat cause, potassium binder per protocol (e.g. sodium zirconium cyclosilicate or resonium — availability varies; VERIFY), dialysis if refractory (renal team).",
                            "5. Stop the source: K+-sparing diuretics, ACEi/ARB, K+ supplements/IV bags, trimethoprim, NSAIDs.",
                        ),
                    ),
                    GuideSection(
                        "Recheck",
                        listOf(
                            "Repeat K+ 1–2 h after treatment (shifting is temporary — it comes back).",
                            "Look for the cause: AKI, rhabdo, tumour lysis, addisonian, drugs.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Calcium and digoxin toxicity: caution — discuss before giving calcium if dig toxicity suspected.",
                    "Insulin without glucose (or without BSL follow-up) causes dangerous delayed hypos.",
                    "Don't dump Hartmann's/K+-containing maintenance into a hyperkalaemic patient without thinking.",
                    "Resonium alone is NOT acute treatment — hours to work.",
                ),
        ),
        Guide(
            title = "Hyponatraemia",
            oneLiner = "Symptoms + speed matter more than the number. Slow correction, always.",
            sections =
                listOf(
                    GuideSection(
                        "Triage",
                        listOf(
                            "Severe symptoms (seizure, coma, GCS drop): this is an emergency — MET/ICU + registrar now. Treatment is hypertonic (3%) saline in small aliquots (commonly 100–150 mL over 10–20 min, repeated to a 4–6 mmol/L rise) — SENIOR-LED, monitored setting only. VERIFY protocol; do not run this solo.",
                            "Moderate symptoms (nausea, confusion, headache): urgent senior discussion tonight.",
                            "Asymptomatic mild (128–134): careful workup can usually wait for the day team, but stop the obvious causes now.",
                        ),
                    ),
                    GuideSection(
                        "Workup you can do at 3 am",
                        listOf(
                            "Repeat UEC (confirm it's real), glucose (hyperglycaemia dilutes Na+), paired serum + urine osmolality and urine sodium BEFORE fluids if possible — sample first, treat after (unless severe symptoms).",
                            "Volume status: dry (vomiting, diuretics), euvolaemic (SIADH — drugs, lungs, brain, malignancy), overloaded (HF, cirrhosis, nephrosis).",
                            "Drug list: thiazides, SSRIs, carbamazepine, PPIs, desmopressin, recent glucose 5% litres.",
                        ),
                    ),
                    GuideSection(
                        "Principles",
                        listOf(
                            "Correction limit: commonly quoted max 8 mmol/L in any 24 h (lower if high-risk: malnourished, alcohol, liver disease, hypokalaemia). Overcorrection → osmotic demyelination — irreversible. VERIFY your hospital's target.",
                            "Hypovolaemic: cautious isotonic fluid; watch for brisk auto-correction once the ADH stimulus stops — recheck Na+ 4–6 hourly.",
                            "SIADH-pattern: fluid restrict, stop offending drugs, day-team workup.",
                            "Overloaded: fluid restrict, treat the underlying failure.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Never correct faster than the 24 h limit — if Na+ is rising quickly, that's also an emergency (senior call, consider slowing/reversing per protocol).",
                    "No hypertonic saline outside a monitored, senior-supervised setting.",
                    "Don't hang glucose 5% 'maintenance' on a hyponatraemic patient.",
                    "Fluid-restricting a hypovolaemic patient makes them worse — volume status first.",
                ),
        ),
        Guide(
            title = "Oliguria / AKI",
            oneLiner = "Catheter/obstruction, perfusion, nephrotoxins, potassium — in that order.",
            sections =
                listOf(
                    GuideSection(
                        "Definitions",
                        listOf(
                            "Oliguria: <0.5 mL/kg/h for 6+ h (≈ <30–35 mL/h in most adults).",
                            "AKI: creatinine rise ≥26.5 micromol/L in 48 h, or ≥1.5× baseline in 7 days, or that urine output criterion.",
                        ),
                    ),
                    GuideSection(
                        "The 4 am checklist",
                        listOf(
                            "1. Post-renal: is there a catheter, and is it actually draining? Flush it. No catheter → bladder scan (retention is common, fixable, and missed).",
                            "2. Pre-renal: BP trend, fluid balance, losses. If dry: 250–500 mL crystalloid, reassess, repeat once — then escalate rather than blind litres.",
                            "3. Nephrotoxins: hold NSAIDs; discuss ACEi/ARB and diuretics; check gentamicin/vancomycin doses and levels; recent contrast?",
                            "4. Dangerous chemistry: urgent K+ (see hyperkalaemia card), acid-base, and check for a creatinine trend.",
                            "5. Sepsis? AKI + fever = source hunt + cultures + antibiotics.",
                        ),
                    ),
                    GuideSection(
                        "Escalate when",
                        listOf(
                            "Anuria, K+ ≥6.0, pulmonary oedema, uraemic symptoms, pH <7.2, or no response to your fluid challenge — these are renal/ICU conversations tonight.",
                            "Renally-cleared drugs need review: opioids (see pain card), gentamicin, vancomycin, LMWH, metformin, DOACs.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Don't give furosemide just to 'make urine' in an undiagnosed oliguric patient — it treats the chart, not the kidney, and masks the trigger.",
                    "No NSAIDs. None. Also check the chart for the NSAID someone else wrote.",
                    "Blind fluid loading in oliguric heart failure = pulmonary oedema; examine first.",
                ),
        ),
        Guide(
            title = "Falls",
            oneLiner = "Injury first, cause second, paperwork third — and anticoagulants change everything.",
            sections =
                listOf(
                    GuideSection(
                        "At the bedside",
                        listOf(
                            "Obs + BSL + full set of injuries: head, C-spine tenderness, hips (pain on log roll / shortened externally rotated leg), wrists, skin tears.",
                            "Witnessed? Loss of consciousness? Seizure activity? Chest pain or palpitations before? Mechanical trip vs syncope matters.",
                            "Head strike or can't exclude one + anticoagulant/antiplatelet: escalate now, low threshold for CT head per local policy, neuro obs regardless.",
                            "Hip pain or won't weight-bear: analgesia, keep NWB, X-ray pelvis/hip ± femur.",
                        ),
                    ),
                    GuideSection(
                        "Cause sweep",
                        listOf(
                            "Postural BP, ECG (arrhythmia, long QT), medications (sedatives charted last night? new antihypertensive?), delirium screen, infection.",
                            "Environment: bed height, cot sides confusion, footwear, lighting — mention it in the note; falls recur the same night.",
                        ),
                    ),
                    GuideSection(
                        "Documentation & follow-through",
                        listOf(
                            "Document: time found, witnessed/unwitnessed, injuries found AND injuries excluded, neuro obs plan, escalation made.",
                            "Incident report (Riskman or local equivalent) — it protects the patient and you.",
                            "Neuro obs schedule per policy if any head-strike possibility; make sure nursing staff know the plan and the trigger to call you back.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Never 'observe overnight' a head-struck anticoagulated patient without senior discussion — delayed bleeds kill.",
                    "Don't sedate the agitated post-fall patient before excluding head injury and pain.",
                    "Don't let a normal first CT fully reassure you if GCS later drops — rescan territory.",
                ),
        ),
        Guide(
            title = "Pain management",
            oneLiner = "Ladder up properly; renal function decides your opioid and your dose.",
            sections =
                listOf(
                    GuideSection(
                        "Ladder",
                        listOf(
                            "Paracetamol 1 g QID regular (max 4 g/day; consider 3 g/day if <50 kg, frail, or hepatic concerns) — actually chart it regularly, it's opioid-sparing.",
                            "NSAIDs only in the young-ish with good kidneys, no bleeding/ulcer risk, no HF — and even then, shortest course (e.g. ibuprofen 400 mg TDS PO).",
                            "Then opioids — as below, by renal function.",
                        ),
                    ),
                    GuideSection(
                        "Oxycodone (immediate release, opioid-naive) by eGFR",
                        listOf(
                            "eGFR >60: oxycodone IR 2.5–5 mg PO q4–6h PRN (start 2.5 mg in elderly/frail).",
                            "eGFR 30–60: reduce — 2.5 mg PO q6–8h PRN and review response; metabolite accumulation is real. VERIFY against local renal dosing guide.",
                            "eGFR <30 / dialysis: avoid routine oxycodone if you can; preferred options are usually reduced-dose hydromorphone, fentanyl, or buprenorphine — these choices belong with your registrar/pain or renal team overnight. VERIFY locally.",
                            "Always co-chart: naloxone availability, aperients (opioids constipate everyone), and antiemetic PRN.",
                        ),
                    ),
                    GuideSection(
                        "Before you write it",
                        listOf(
                            "Check what's already on board tonight: patches (fentanyl/buprenorphine), slow-release opioids, PCA — additive sedation is the killer.",
                            "Sedation score matters more than pain score for safety: a drowsy patient does not get another opioid dose without review.",
                            "Neuropathic pain doesn't respond well to opioids — flag for the day team rather than escalating doses overnight.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "RR <10 or sedation score high: no further opioid; consider naloxone per protocol and stay with the patient.",
                    "NSAIDs: NO in AKI/CKD, HF, GI bleeding history, on anticoagulants, or peri-operative surgical instruction against.",
                    "Avoid codeine and tramadol in renal impairment and the elderly (unpredictable metabolism, delirium).",
                    "Pethidine has essentially no place — seizure-genic metabolite, worse in renal failure.",
                ),
        ),
        Guide(
            title = "Antibiotics (empirical, adult)",
            oneLiner = "Site + severity + allergies + local protocol. Cultures BEFORE the first dose when possible.",
            sections =
                listOf(
                    GuideSection(
                        "Before any of these",
                        listOf(
                            "These are eTG-style ADULT EMPIRICAL starting points — your hospital's guideline and the patient's allergies/renal function override them. VERIFY before prescribing; doses shift between eTG editions.",
                            "Blood cultures (×2 if febrile/septic) + source cultures first, unless that delays antibiotics in sepsis — then antibiotics win.",
                            "Check: true penicillin allergy vs 'nausea', renal function (gentamicin, vancomycin), pregnancy.",
                        ),
                    ),
                    GuideSection(
                        "Sepsis / septic shock, source unclear",
                        listOf(
                            "This is a MET-call context: within the hour — commonly gentamicin 7 mg/kg IV (adjust for renal function; max doses per protocol) PLUS flucloxacillin 2 g IV q6h; many sites use piperacillin–tazobactam 4.5 g IV instead. FOLLOW YOUR SEPSIS PATHWAY — VERIFY.",
                            "Fluids + lactate + urine output alongside, not after.",
                        ),
                    ),
                    GuideSection(
                        "Community-acquired pneumonia",
                        listOf(
                            "Mild (ward, low CORB/SMART-COP): amoxicillin 1 g PO q8h PLUS doxycycline 100 mg PO q12h. VERIFY durations (usually 5–7 days total).",
                            "Moderate: benzylpenicillin 1.2 g IV q6h PLUS doxycycline 100 mg PO q12h.",
                            "Severe/ICU-bound: ceftriaxone 1 g IV daily (some sites 2 g) PLUS azithromycin 500 mg IV daily. VERIFY.",
                        ),
                    ),
                    GuideSection(
                        "Hospital-acquired pneumonia",
                        listOf(
                            "Non-severe: amoxicillin–clavulanate 875/125 mg PO q12h (or IV 1.2 g q8h if not tolerating oral). VERIFY.",
                            "Severe or MRO risk: piperacillin–tazobactam 4.5 g IV q6–8h (site-dependent) ± MRSA cover (vancomycin) if colonised. VERIFY with your antimicrobial team.",
                        ),
                    ),
                    GuideSection(
                        "Urinary tract",
                        listOf(
                            "Simple cystitis (women): trimethoprim 300 mg PO nocte for 3 nights (avoid if eGFR low/K+ high) or nitrofurantoin 100 mg PO q6h 5 days (avoid eGFR <45 — commonly quoted; VERIFY).",
                            "Pyelonephritis, ward-level: gentamicin IV (dose per renal function) PLUS amoxicillin (or ampicillin) 2 g IV q6h; step down per cultures. VERIFY.",
                            "Catheter-associated: change/remove the catheter if you can; treat as complicated.",
                        ),
                    ),
                    GuideSection(
                        "Skin & soft tissue",
                        listOf(
                            "Mild cellulitis: flucloxacillin 500 mg PO q6h. Non-severe penicillin allergy: cefalexin 500 mg PO q6h. VERIFY duration (usually 5 days, review).",
                            "Systemically unwell / spreading fast: flucloxacillin 2 g IV q6h.",
                            "Mark the edge with a pen + time. Crepitus, pain out of proportion, rapid spread, grey skin = necrotising fasciitis concern — surgical emergency, not more antibiotics.",
                        ),
                    ),
                    GuideSection(
                        "Intra-abdominal (perforation, cholangitis, peritonitis)",
                        listOf(
                            "Classic eTG triple: amoxicillin/ampicillin 2 g IV q6h + gentamicin (renal-dosed) + metronidazole 500 mg IV q12h. Some sites use amoxicillin–clavulanate or pip–taz mono. VERIFY.",
                            "Source control (surgery/drainage) is the real treatment — surgical registrar early.",
                        ),
                    ),
                    GuideSection(
                        "Meningitis (suspected bacterial)",
                        listOf(
                            "Do NOT wait for CT/LP if it delays: ceftriaxone 2 g IV q12h (or 4 g daily) + dexamethasone 10 mg IV q6h (ideally with/before first dose).",
                            "Add benzylpenicillin 2.4 g IV q4h for Listeria cover if >50 y, immunocompromised, pregnant. VERIFY.",
                            "Blood cultures first (they're fast); this is simultaneous with the registrar phone call, not after.",
                        ),
                    ),
                    GuideSection(
                        "C. difficile",
                        listOf(
                            "Stop the offending antibiotic if possible; stop PPIs where reasonable; NO antimotility drugs.",
                            "First episode, non-severe to severe: vancomycin 125 mg PO q6h for 10 days (oral — IV vancomycin does nothing in the gut). Metronidazole 400 mg PO q8h only if vancomycin unavailable. VERIFY (fidaxomicin appears in newer guidance at some sites).",
                            "Fulminant (ileus, megacolon, shock): senior + surgical + ID involvement now.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "True penicillin anaphylaxis: avoid ALL penicillins; cephalosporin cross-reactivity is low but policy-dependent — check the allergy record, ask if unclear.",
                    "Gentamicin: avoid/adjust in significant renal impairment, and avoid repeated empirical doses without levels; document dose, time and weight.",
                    "Trimethoprim raises K+ — bad combo with ACEi/ARB/spironolactone or existing hyperkalaemia.",
                    "Nitrofurantoin ineffective in pyelonephritis and in poor renal function.",
                    "Don't start antibiotics for asymptomatic bacteriuria in catheterised elderly patients — treat the patient, not the dipstick.",
                ),
        ),
        Guide(
            title = "Anticoagulant reversal",
            oneLiner = "How bad is the bleed × what drug × when was the last dose. Haematology is your friend.",
            sections =
                listOf(
                    GuideSection(
                        "First principles (any bleeding on anticoagulation)",
                        listOf(
                            "Stop the drug, note the LAST DOSE TIME and renal function (drives DOAC clearance).",
                            "Assess severity: haemodynamics, Hb trend, site (intracranial/GI/retroperitoneal are the big three).",
                            "Group & hold ± crossmatch, FBC, coags (note: normal APTT/INR does NOT exclude DOAC effect), UEC.",
                            "Major bleeding = registrar + haematology now. Reversal agents below are senior/haematology-guided at most sites — know they exist, don't fly solo.",
                        ),
                    ),
                    GuideSection(
                        "Warfarin",
                        listOf(
                            "Life-threatening bleed: vitamin K 5–10 mg slow IV PLUS Prothrombinex-VF 25–50 units/kg IV, plus FFP if Prothrombinex unavailable (or per protocol alongside). VERIFY exact dosing on your hospital/haematology protocol.",
                            "Elevated INR without bleeding: managed per the standard warfarin-reversal table (withhold ± small oral vitamin K depending on INR) — chart tonight, recheck INR.",
                            "Vitamin K IV works in ~6–12 h; Prothrombinex is immediate — that's why bad bleeds get both.",
                        ),
                    ),
                    GuideSection(
                        "Dabigatran",
                        listOf(
                            "Specific antidote: idarucizumab (Praxbind) 5 g IV — for life-threatening bleeding or emergency surgery. Haematology/ED senior call.",
                            "Dialysable in extremis (renally cleared) — another reason renal function matters.",
                        ),
                    ),
                    GuideSection(
                        "Rivaroxaban / apixaban (Xa inhibitors)",
                        listOf(
                            "Andexanet alfa has limited availability in Australia; most centres use Prothrombinex-VF ~25–50 units/kg off-label for life-threatening bleeding. VERIFY — this is strictly a haematology-guided decision.",
                            "Activated charcoal only if ingestion within ~2 h and airway safe.",
                        ),
                    ),
                    GuideSection(
                        "Heparins",
                        listOf(
                            "Unfractionated heparin: stop infusion (short half-life ~1 h); protamine 1 mg per 100 units heparin given in the last 2–3 h (max ~50 mg, slow IV). VERIFY.",
                            "Enoxaparin: protamine partially reverses (~60%) — dose depends on timing; haematology call.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Prothrombinex is prothrombotic — not for 'high INR, no bleeding' or minor bruising; respect the indication.",
                    "Protamine: risk of severe reactions (esp. prior NPH insulin exposure, fish allergy per older data) — slow IV, monitored.",
                    "Don't give platelets/FFP reflexively for DOAC bleeding — wrong tool; get advice.",
                    "Never restart anticoagulation post-bleed without a documented senior plan.",
                ),
        ),
    )
