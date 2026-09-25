package com.nightshift.tracker.ui.guides

// Urology day-shift learning content for a JMO on a urology term.
// Same rules as everything else in this app: memory aid, not a textbook.
// VERIFY = check eTG/local protocol before prescribing. Registrar > app.

data class Tutorial(
    val title: String,
    val oneLiner: String,
    val steps: List<String>,
    val pitfalls: List<String>, // rendered as the red section
)

const val URO_DISCLAIMER =
    "Learning aid for a urology JMO — not a substitute for eTG, local protocols or your registrar. " +
        "Doses marked VERIFY need checking before prescribing. Procedures described are for " +
        "consolidating teaching you've received, not a licence to do them unsupervised."

val uroTutorials =
    listOf(
        Tutorial(
            "Male IDC insertion (doing it well)",
            "Most 'difficult catheters' are technique, lignocaine and patience.",
            listOf(
                "Set up properly before you glove: everything opened, syringes drawn, bag connected to gravity. Scrambling mid-procedure is how sterility dies.",
                "Analgesia IS the technique: 10–20 mL lignocaine gel slowly into the urethra, hold the glans closed ~2–3 min. Rushing this step causes the spasm that blocks you at the sphincter.",
                "Penis on stretch, perpendicular to the body (corrects the penoscrotal angle). Advance gently — the catheter does the work, you just guide.",
                "Resistance at the external sphincter: ask the patient to breathe deeply / bear down gently, steady gentle pressure — do NOT push hard.",
                "Insert to the HILT before inflating the balloon. Urine flow confirms position; no urine but you're at the hilt in a retention patient — flush 20 mL saline in and expect it back.",
                "Inflate 10 mL water slowly. ANY pain on inflation = stop, deflate, advance further, try again.",
                "Document: size, type, balloon volume, residual drained, colour, and whether it was traumatic.",
                "After large-volume retention (>1 L): watch for post-obstructive diuresis — hourly urine output for a few hours; escalate if >200 mL/h sustained. VERIFY local threshold.",
            ),
            listOf(
                "Never inflate the balloon until fully inserted — a urethral balloon is a preventable disaster.",
                "Blood at the meatus / pelvic trauma / high-riding prostate: do NOT catheterise — urethral injury pathway, senior first.",
                "Recent radical prostatectomy or urethral surgery: the anastomosis is sacred — urology does the catheter, not you.",
                "Two gentle failed attempts = stop. More attempts = more trauma = harder salvage. Call the reg (coudé, guidewire, or flexible cysto is their call).",
                "Never force past resistance; false passages haunt the patient for years.",
            ),
        ),
        Tutorial(
            "Difficult catheter algorithm",
            "What to change between attempt one and attempt two.",
            listOf(
                "First: why is it hard? Stricture (young man, previous instrumentation) feels like a firm early stop; big prostate (older man) is a spongy distal obstruction; sphincter spasm grabs at ~16–20 cm.",
                "More lignocaine gel + more time is the highest-yield change.",
                "Big prostate: go UP a size (18 Fr has more stiffness to push through prostatic curves) and/or use a coudé tip — tip points UP (12 o'clock) the whole way.",
                "Suspected stricture: go DOWN a size (12–14 Fr). Do not repeatedly ram a 16.",
                "Still failing after one smart change: stop and call urology. Options above your pay grade: guidewire-assisted, dilators, flexible cystoscopy, SPC.",
                "Can't catheterise + painful retention: analgesia and a bladder scan while you wait; if bladder >1 L and urology is far away, ask about suprapubic aspiration/SPC — senior decision.",
            ),
            listOf(
                "Never dilate a stricture blind from the ward.",
                "Don't repeat the identical failed attempt with the identical catheter — change something or change operator.",
                "A traumatic failed attempt with frank bleeding: tell urology NOW, before the next person makes it worse.",
            ),
        ),
        Tutorial(
            "3-way catheter, washout & irrigation",
            "The core skill of the urology ward: clots kill catheters, washouts save nights.",
            listOf(
                "Indication: macroscopic haematuria with clots, clot retention, post-TURP. A standard 2-way blocks; the 22–24 Fr 3-way has a lumen big enough to wash.",
                "Manual bladder washout FIRST, irrigation second: 50–60 mL catheter-tip syringe, sterile saline, instil 50 mL and draw back firmly. Repeat until you get no more clot and free flow both ways. This is the treatment; irrigation only PREVENTS re-clotting.",
                "A washout that returns nothing but won't aspirate = catheter buried in clot: reposition, more vigorous cycles, senior if it stays stuck.",
                "Start continuous bladder irrigation (CBI) only once the catheter washes freely: titrate rate to keep outflow ROSÉ — pale pink. Darker → open it up; clear → slow down.",
                "Your hourly maths matters: outflow must exceed inflow. Falling behind = clot forming = impending clot retention. Nurses chart it; you check it every time you walk past.",
                "Painful bladder + inflow running + poor outflow = STOP the inflow (you're inflating a blocked bladder), washout, then restart.",
                "Persistent dark bleeding despite good CBI, or haemodynamic wobble: Hb, group & hold, IV access, urology reg — may need cystoscopy and diathermy.",
            ),
            listOf(
                "Never leave irrigation running against a blocked outflow — bladder rupture territory.",
                "Don't 'just increase the rate' for dark bleeding without checking the outflow is actually flowing.",
                "Clot retention is painful and undertreated — chart proper analgesia while you fix the plumbing.",
            ),
        ),
        Tutorial(
            "Blocked or bypassing catheter",
            "Before you replace it, work out why it failed.",
            listOf(
                "Bypassing (leaking around) with no drainage = blocked until proven otherwise: clot, sediment, kink, or balloon jammed in the urethra.",
                "Check the stupid stuff first: kinked tubing, bag above bladder height, clamp left on, balloon deflated.",
                "Flush 50 mL saline: goes in and comes back = fine; goes in but won't return = partial block/malposition; won't go in = fully blocked → change it.",
                "Sediment blockers on long-term IDCs need a plan (regular washouts or scheduled changes), not just tonight's fix.",
                "Bypassing WITH good drainage is usually bladder spasm, not blockage: check for constipation and UTI; anticholinergic per team. VERIFY choice/dose.",
                "Long-term IDC due for change anyway? Change rather than salvage.",
            ),
            listOf(
                "Never remove an SPC that won't drain without urology input — mature tracts close within hours.",
                "Recent urological surgery (TURP, prostatectomy) + blocked catheter = urology manages it; a ward re-catheterisation can wreck the surgical field.",
            ),
        ),
        Tutorial(
            "Trial of void (TOV)",
            "How retention patients earn their freedom.",
            listOf(
                "Usual timing: after acute retention, alpha-blocker on board (tamsulosin 400 microg daily, started ≥48 h before), then remove IDC early morning — VERIFY local protocol.",
                "Post-removal: measure each void + bladder scan post-void residual (PVR).",
                "Rough pass criteria (local rules win): voiding comfortably, decent volumes, PVR <~150–200 mL. Fail: no void by ~6–8 h with a full bladder, painful retention, or PVR climbing.",
                "Failed TOV: recatheterise (document volume), urology follow-up — usually repeat TOV in 1–2 weeks or surgical planning.",
                "Send them home with safety-netting: what retention feels like, come back immediately if it happens.",
            ),
            listOf(
                "Don't run a TOV on the day nobody can recatheterise (Friday 4 pm classic).",
                "Don't call scan-only 'retention' a fail if they're voiding well — treat numbers in context.",
                "Post-obstructive renal impairment patients need their creatinine rechecked, not just their bladder.",
            ),
        ),
        Tutorial(
            "Ureteric stents: what your patients feel",
            "Half the ward has a stent; know the normal so you can spot the abnormal.",
            listOf(
                "Expected symptoms: flank twinge on voiding (reflux), frequency, urgency, mild haematuria after activity. Reassure — it's the stent, not a stone.",
                "Stent colic can be genuinely painful: regular simple analgesia, alpha-blocker (tamsulosin 400 microg daily) helps stent symptoms. Anticholinergics per team for bladder spasm. VERIFY.",
                "Fever + stent = blocked/infected until proven otherwise: cultures, bloods, urgent senior review — an infected obstructed system is the urological emergency.",
                "Every stent needs a documented removal/exchange date. 'Forgotten stent' = encrusted stone factory = disaster. Check the plan is written before discharge.",
                "Post-stent-removal pain within 24 h ('stent removal colic') happens; severe or febrile = assess properly.",
            ),
            listOf(
                "Never let a stented patient leave without a removal plan in writing.",
                "Don't dismiss fever in a stented patient as 'probably viral'.",
            ),
        ),
        Tutorial(
            "Paraphimosis reduction",
            "A time-critical squeeze you can actually fix at the bedside.",
            listOf(
                "Recognise: retracted foreskin stuck behind the glans, swollen oedematous ring — often after catheter care where nobody pulled it back. Prevent it: ALWAYS reduce the foreskin after catheterising.",
                "Analgesia first: topical lignocaine gel ± procedural analgesia; penile block is a senior skill.",
                "Compress the glans firmly with your hand wrapped in gauze for several minutes — squeeze the oedema out. Slow and boring beats fast and traumatic.",
                "Then push the glans back while pulling the foreskin forward with both thumbs on the glans, fingers behind the ring.",
                "Failed manual reduction → urology urgently (dorsal slit under local). Don't sit on it overnight.",
            ),
            listOf(
                "This is time-critical — glans ischaemia is the endpoint. Escalate early if it won't reduce.",
                "Never leave the foreskin retracted after catheter insertion or cleaning — you cause the next one.",
            ),
        ),
        Tutorial(
            "Bladder scans: reading them honestly",
            "The most over-trusted machine on the ward.",
            listOf(
                "It's an estimate: ascites, obesity, pelvic cysts, pregnancy and a mid-scan wriggle all fool it. Odd number → repeat, reposition, or verify with in-out catheter if it matters.",
                "PVR: scan within 10 min of voiding or the kidneys refill the number.",
                "Anuric vs retention: scan says 50 mL and no urine output → the problem is upstream (AKI), not the bladder. Scan says 800 mL → it's retention; fix the outflow.",
                "Chronic retention (painless, huge volumes, often with overflow incontinence) is different from acute: don't panic-catheterise at midnight without thinking about post-obstructive diuresis and renal function.",
            ),
            listOf(
                "Don't document a scan number as gospel when the clinical picture disagrees — palpate the abdomen.",
                "A 'normal' scan doesn't exclude obstruction above the bladder.",
            ),
        ),
    )

val uroGuides =
    listOf(
        Guide(
            title = "Haematuria & clot retention",
            oneLiner = "Pink is a symptom; clots are a job. Wash first, diagnose second.",
            sections =
                listOf(
                    GuideSection(
                        "Assess",
                        listOf(
                            "Anticoagulated? On antiplatelets? Recent TURP/biopsy? Known cancer? Trauma? — the cause list writes itself.",
                            "Bloods: FBC (Hb trend), UEC, coags, group & hold if brisk.",
                            "Painless frank haematuria in an older patient = bladder cancer until investigated: make sure the day team books cystoscopy + upper tract imaging. Your note is where that referral starts.",
                        ),
                    ),
                    GuideSection(
                        "Clot retention",
                        listOf(
                            "Diagnosis: retention + clots ± a tense miserable patient. Treatment: 22–24 Fr 3-way, manual washout until clear, then CBI (see tutorial).",
                            "Analgesia properly — clot retention hurts like labour.",
                            "Anticoagulation: discuss holding/reversing with the team — bleeding source + anticoagulant needs an actual decision, documented.",
                        ),
                    ),
                    GuideSection(
                        "Escalate when",
                        listOf(
                            "Washout won't clear, Hb falling, haemodynamically wobbly, or bleeding post-op day 0–2 — cystoscopy territory.",
                            "Transfusion threshold per local policy; crossmatch early, not when the Hb is 62.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Never run CBI into a catheter that doesn't wash freely.",
                    "Don't discharge painless frank haematuria without a documented investigation plan.",
                    "Don't restart anticoagulation post-bleed without a senior's name next to the decision.",
                ),
        ),
        Guide(
            title = "Acute urinary retention",
            oneLiner = "Catheterise, measure, then find the cause and plan the exit.",
            sections =
                listOf(
                    GuideSection(
                        "Fix",
                        listOf(
                            "IDC (see tutorial), document residual volume — it predicts everything (a 600 mL retention and a 2.5 L chronic retention are different diseases).",
                            "Causes sweep: BPH, constipation (huge and fixable), UTI, drugs (anticholinergics, opioids, antihistamines), neuro (cauda equina — ASK about saddle anaesthesia, leg weakness, bowel function), post-op, clot.",
                            "Start tamsulosin 400 microg daily for men with presumed BPH retention (helps the TOV succeed). VERIFY.",
                            "Rectal exam: prostate assessment + constipation + tone (cauda equina).",
                        ),
                    ),
                    GuideSection(
                        "Watch after drainage",
                        listOf(
                            "Post-obstructive diuresis after large/chronic retention: hourly UO; replace per protocol if >200 mL/h sustained; recheck UEC — these kidneys have been under pressure.",
                            "Haematuria after decompression (ex vacuo) is usually mild and settles; brisk bleeding is not 'ex vacuo'.",
                        ),
                    ),
                    GuideSection(
                        "Plan the exit",
                        listOf(
                            "Every retention needs a documented plan: TOV date, urology follow-up, or long-term catheter decision — never just 'IDC inserted'.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "New retention + back pain / leg neurology / saddle change = cauda equina until MRI says otherwise — emergency, not a urology-clinic letter.",
                    "Don't clamp-decompress 'slowly' — old myth; drain freely, just watch for diuresis.",
                    "Retention with fever: think infected obstruction — bloods, cultures, antibiotics, senior.",
                ),
        ),
        Guide(
            title = "Renal colic",
            oneLiner = "The stone is rarely the emergency; the infected obstructed kidney always is.",
            sections =
                listOf(
                    GuideSection(
                        "Workup",
                        listOf(
                            "Classic: loin-to-groin colic, can't lie still, ± haematuria on dipstick. But AAA mimics renal colic in over-50s — examine the abdomen, think aorta.",
                            "CT KUB (non-contrast) is the answer test; ultrasound first-line if young/pregnant.",
                            "Bloods: UEC (solitary kidney? creatinine up?), FBC, CRP; urine MCS; lipase if the story is odd.",
                        ),
                    ),
                    GuideSection(
                        "Analgesia (the actual treatment tonight)",
                        listOf(
                            "NSAIDs are first-line and genuinely superior for colic: e.g. indomethacin 100 mg PR or ibuprofen 400 mg PO — if renal function and gut allow. VERIFY dose/route locally.",
                            "Add opioid for breakthrough (see renal-adjusted dosing).",
                            "Antiemetic, IV fluids only if dry — flooding doesn't push stones out.",
                            "Medical expulsive therapy: tamsulosin 400 microg daily is commonly used for distal stones 5–10 mm. VERIFY current stance — evidence has wobbled.",
                        ),
                    ),
                    GuideSection(
                        "Disposition",
                        listOf(
                            "Likely to pass: <5 mm distal, pain controlled, normal renal function, no infection → home with analgesia, strainer advice, urology follow-up + return precautions.",
                            "Admit: uncontrolled pain, vomiting, solitary kidney, renal impairment, large/proximal stone, social reasons.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "FEVER + OBSTRUCTING STONE = pus under pressure = urological emergency: IV antibiotics, resus, and urgent decompression (stent or nephrostomy) — call urology NOW, not after the CT report is typed.",
                    "NSAIDs: not in AKI/CKD, dehydration, or the usual GI/cardiac contraindications.",
                    "Don't anchor on 'known stone-former' — the over-50 first presentation needs the aorta considered.",
                ),
        ),
        Guide(
            title = "UTI, pyelonephritis & urosepsis",
            oneLiner = "Treat the patient, not the dipstick — and never trust a catheter dipstick at all.",
            sections =
                listOf(
                    GuideSection(
                        "Empirical therapy (adults; VERIFY against eTG/local)",
                        listOf(
                            "Cystitis (women): trimethoprim 300 mg PO nocte ×3/7, or nitrofurantoin 100 mg PO q6h ×5/7 (avoid poor renal function).",
                            "Pyelonephritis (ward): gentamicin IV (renal-dosed) + amoxicillin/ampicillin 2 g IV q6h; step down on cultures.",
                            "Urosepsis: sepsis pathway — cultures, lactate, gentamicin + amoxicillin (or pip-taz per local), fluids, hourly UO. Source control question: is anything obstructed or is there a device?",
                            "Men with UTI, and anyone with recurrent UTIs or stones: think 'why' — needs follow-up imaging/urology, not just a script.",
                        ),
                    ),
                    GuideSection(
                        "Catheter-associated",
                        listOf(
                            "Every long-term catheter is colonised: bacteriuria alone ≠ infection. Treat symptoms/systemic features, not smell or dipstick.",
                            "If treating: change the catheter (biofilm lives on the old one), culture from the NEW catheter.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Fever + hydronephrosis/stent/stone = obstructed infected system → decompression discussion tonight (see renal colic card).",
                    "Trimethoprim: raises K+ (bad with ACEi/spiro), avoid in significant renal impairment.",
                    "Nitrofurantoin: useless for pyelonephritis and in poor renal function.",
                    "Don't treat asymptomatic bacteriuria in the catheterised elderly — you breed resistance and C. diff.",
                ),
        ),
        Guide(
            title = "Acute scrotum: torsion first",
            oneLiner = "Torsion is a countdown, not a differential to savour.",
            sections =
                listOf(
                    GuideSection(
                        "Torsion",
                        listOf(
                            "Sudden severe unilateral pain ± nausea/vomiting; high-riding transverse testis, absent cremasteric reflex; any age but classically <25.",
                            "Time is testis: ~6 h for excellent salvage. The answer is surgical exploration — do NOT wait for an ultrasound to 'confirm' a good clinical story.",
                            "Your job: NBM, analgesia, IV access, consent conversation started, urology/surg reg called immediately, theatre paperwork moving.",
                        ),
                    ),
                    GuideSection(
                        "The others",
                        listOf(
                            "Epididymo-orchitis: gradual onset, fever, dysuria; <35 y think STI (ceftriaxone + doxycycline per eTG), >35 y think coliforms (trimethoprim or per culture). VERIFY doses.",
                            "Torted appendix testis: 'blue dot', tender upper pole, cremasteric intact — but if in doubt, it's torsion.",
                            "Incarcerated hernia, trauma, Fournier's (see below) round out the list.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Never let imaging delay theatre when the story screams torsion.",
                    "Don't diagnose epididymo-orchitis in a teenager without a consultant-level reason it isn't torsion.",
                    "Perineal/scrotal pain OUT OF PROPORTION with sepsis ± crepitus = Fournier's gangrene: theatre + broad-spectrum antibiotics + every senior you know. Minutes matter.",
                ),
        ),
        Guide(
            title = "Priapism",
            oneLiner = "Ischaemic priapism >4 h is a compartment syndrome of the penis.",
            sections =
                listOf(
                    GuideSection(
                        "Sort the type",
                        listOf(
                            "Ischaemic (low-flow): rigid, PAINFUL, often drugs (intracavernosal injections, antipsychotics, cocaine) or haematological (sickle cell, CML). This is the emergency.",
                            "Non-ischaemic (high-flow): partial, not painful, usually post-trauma — urgent-ish, not tonight's crisis.",
                            "Cavernosal blood gas differentiates when unclear (ischaemic: dark, acidotic, hypoxic).",
                        ),
                    ),
                    GuideSection(
                        "Ischaemic pathway",
                        listOf(
                            "Analgesia, treat the cause (sickle: your haem protocols), urology urgently.",
                            "Definitive: corporal aspiration ± irrigation ± intracavernosal phenylephrine — done by urology/ED senior with monitoring, not solo on the ward.",
                            ">24–48 h = permanent damage likely; that conversation belongs to the consultant, but timestamps in YOUR note.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Don't give the patient 'a few more hours to see' — duration is the prognosis.",
                    "Intracavernosal sympathomimetics need cardiac monitoring — not a corridor procedure.",
                ),
        ),
        Guide(
            title = "Post-TURP care",
            oneLiner = "Rosé outflow, sodium watched, patient warned about week-2 bleeding.",
            sections =
                listOf(
                    GuideSection(
                        "Normal course",
                        listOf(
                            "CBI overnight titrated to rosé; usually clear enough to stop day 1, TOV day 1–2 per surgeon.",
                            "Expect: mild haematuria, dysuria, urgency for days–weeks; secondary bleed around day 7–14 as sloughs separate — warn them at discharge.",
                            "Catheter removal per operative plan — the surgeon's note trumps the usual routine.",
                        ),
                    ),
                    GuideSection(
                        "TUR syndrome (rare now, still examined and still real)",
                        listOf(
                            "Dilutional hyponatraemia from irrigation absorption (mainly glycine/monopolar): confusion, nausea, visual disturbance, bradycardia, hypertension then hypotension.",
                            "Your move: stop irrigation absorption question, urgent UEC + VBG, senior + anaesthetics/ICU early. Managed like severe symptomatic hyponatraemia — senior-led (see Nightshift hyponatraemia rules: slow, monitored).",
                        ),
                    ),
                    GuideSection(
                        "Trouble",
                        listOf(
                            "Clot retention post-TURP: washout via the existing catheter, gently — the prostatic fossa is raw. Persistent bleeding = surgeon, possibly back to theatre.",
                            "Fever: cultures, antibiotics per local post-TURP protocol. VERIFY.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Don't blame post-op confusion on 'anaesthetic' before you've seen a sodium.",
                    "Don't do a forceful washout on a fresh prostatic fossa without knowing the operative plan — ask.",
                    "No aspirin/anticoagulant restart without the surgeon's explicit OK.",
                ),
        ),
        Guide(
            title = "Post-op: URS, stents, nephrectomy",
            oneLiner = "Know what today's op was, and what its specific day-1 disaster looks like.",
            sections =
                listOf(
                    GuideSection(
                        "Ureteroscopy ± laser ± stent",
                        listOf(
                            "Usually day cases. Expect stent symptoms (see tutorial), mild haematuria.",
                            "Day-1 fever or severe pain: think infection or (rarely) ureteric injury — bloods, cultures, senior; imaging per reg.",
                        ),
                    ),
                    GuideSection(
                        "Nephrectomy (open/lap, partial/radical)",
                        listOf(
                            "Watch: bleeding (drain output, Hb, tachycardia — retroperitoneum hides litres), ileus, pain control, and the REMAINING kidney's output.",
                            "Oliguria post-nephrectomy is never 'expected': hourly UO, UEC, fluids review, escalate early.",
                            "Partial nephrectomy specifics: delayed bleed and urine leak (drain fluid creatinine tells you) — reg-level calls, your job is noticing.",
                        ),
                    ),
                    GuideSection(
                        "Any urological post-op",
                        listOf(
                            "The operation note is your bible: rails ('leave IDC 7 days') are written there. Read it before changing anything.",
                            "VTE prophylaxis per surgeon — urological bleeding risk makes this a deliberate decision, not an autopilot chart.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Never remove or exchange a surgically-placed catheter/stent/drain without the operating team's say-so.",
                    "Tachycardia + falling Hb post-nephrectomy is theatre-grade bleeding until proven otherwise — don't watch it fall twice.",
                ),
        ),
        Guide(
            title = "BPH, PSA & clinic-letter literacy",
            oneLiner = "The knowledge that makes ward round conversations make sense.",
            sections =
                listOf(
                    GuideSection(
                        "BPH medical therapy",
                        listOf(
                            "Alpha-blocker (tamsulosin 400 microg daily): works in days, relaxes smooth muscle; side-effects postural hypotension, retrograde ejaculation. Hold ~2 weeks pre-cataract surgery conversation (floppy iris) — flag it.",
                            "5-alpha-reductase inhibitor (finasteride 5 mg / dutasteride): shrinks gland over months, HALVES the PSA (double the measured PSA to interpret it), sexual side-effects.",
                            "IPSS score quantifies symptoms; bother score drives treatment more than gland size.",
                        ),
                    ),
                    GuideSection(
                        "PSA in five lines",
                        listOf(
                            "Raised by: cancer, BPH, prostatitis/UTI, retention, recent catheter/instrumentation, ejaculation, cycling. Don't send one during/straight after a UTI or retention episode — it's uninterpretable.",
                            "Age-related reference ranges + trend (velocity) matter more than one number.",
                            "Pathway now runs through MRI (PI-RADS) → targeted biopsy; Gleason/ISUP grade groups 1–5 describe aggressiveness.",
                            "Metastatic disease loves bone: new back pain in a prostate cancer patient gets imaged, and cord compression symptoms are an emergency (dexamethasone + urgent MRI — VERIFY dose).",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Prostate cancer + new leg weakness/saddle change = spinal cord compression pathway NOW.",
                    "Don't interpret a PSA taken during retention/UTI/instrumentation — repeat 4–6 weeks later.",
                    "Don't stop alpha-blockers abruptly in a TOV-pending patient without a reason.",
                ),
        ),
        Guide(
            title = "Fournier's gangrene",
            oneLiner = "The one perineal diagnosis you are never allowed to miss.",
            sections =
                listOf(
                    GuideSection(
                        "Recognise",
                        listOf(
                            "Necrotising fasciitis of perineum/genitals: pain OUT OF PROPORTION, rapidly spreading erythema/dusky patches, crepitus, sepsis; diabetics, immunosuppressed, elderly at highest risk.",
                            "Early Fournier's can look like 'cellulitis' or 'scrotal abscess' — the disproportion of pain and the sick patient are the tells.",
                        ),
                    ),
                    GuideSection(
                        "Act",
                        listOf(
                            "Simultaneous: MET/sepsis response, broad-spectrum IV antibiotics per necrotising-infection protocol (e.g. meropenem + clindamycin + vancomycin — VERIFY local), and the emergency theatre phone call.",
                            "The treatment is urgent surgical debridement — antibiotics alone lose. Marking the edge and 'review in the morning' is how it's fatally missed.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "No imaging delay for a clinically evident case — CT is for equivocal, stable patients only.",
                    "Never 'observe overnight'. Escalate on suspicion, be pleased to be wrong.",
                ),
        ),
    )
