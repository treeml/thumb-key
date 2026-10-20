package com.nightshift.tracker.ui.guides

// Second block of urology teaching content. Same rules as UroContent.kt:
// memory aid, not a prescribing reference; VERIFY = check eTG/local protocol.

val moreUroTutorials =
    listOf(
        Tutorial(
            "Choosing the right catheter",
            "Size, tip and lumens — the three decisions that make it work first time.",
            listOf(
                "Size: 12–14 Fr for routine drainage or suspected stricture; 16 Fr standard male; 18–20 Fr if debris; 22–24 Fr THREE-way for haematuria with clots.",
                "Bigger is not gentler, but bigger IS stiffer — a 16 often passes a big prostate where a 12 coils.",
                "Tip: standard (Foley) for most; COUDÉ (curved) tip for a big prostate or a bladder neck you can't get past — the tip must point UP (12 o'clock) the whole way in.",
                "Lumens: 2-way = drainage; 3-way = drainage + irrigation channel (you cannot irrigate through a 2-way).",
                "Material: latex/silicone-coated for short term; 100% silicone for long term, allergy, or planned 12-week changes.",
                "Balloon: 10 mL for standard adults. Do NOT use a 30 mL balloon 'for traction' unless a urologist asked for it.",
                "Document what you used — the next person's attempt starts from your note.",
            ),
            listOf(
                "Never use a 3-way small-lumen catheter to manage brisk clots — the drainage channel blocks and the bladder fills.",
                "Latex allergy: check before you open the pack, not after.",
                "Don't upsize repeatedly in a suspected stricture — that's how false passages are made.",
            ),
        ),
        Tutorial(
            "Suprapubic catheter (SPC) care",
            "A mature tract is a privilege — protect it, never gamble with it.",
            listOf(
                "First change is a urology job (usually ~6 weeks post-insertion, once the tract has matured). Ward staff do routine changes after that.",
                "If an SPC falls out: the tract starts closing within HOURS. Re-catheterise the tract promptly with the same or nearest size — if you can't, tell urology immediately and consider a urethral catheter to protect the bladder.",
                "Blocked SPC: flush as you would a urethral catheter (50 mL saline). If it won't drain and won't flush, it needs changing, not more flushing.",
                "Site care: clean and dry, look for overgranulation, discharge or cellulitis at the stoma.",
                "Always document balloon volume and catheter size used — the next change depends on it.",
            ),
            listOf(
                "NEVER remove an SPC that isn't draining without urology input — you may not get it back in.",
                "Never force a new SPC blindly into a tract you cannot pass easily; a false track into the peritoneum is a disaster.",
                "A newly inserted (immature) SPC that comes out is an emergency — urology now.",
            ),
        ),
        Tutorial(
            "Nephrostomy care",
            "The kidney's only exit in an obstructed system. Treat the tube as life-saving.",
            listOf(
                "Indications you'll see: infected obstructed kidney (with a stone), malignant ureteric obstruction, urinary leak.",
                "Chart the output from EACH nephrostomy separately — a sudden drop means blocked or displaced, and in a solitary/obstructed system that's urgent.",
                "Expect blood-stained urine for the first 24–48 h; frank bleeding or clots is a call to interventional radiology/urology.",
                "Never clamp a nephrostomy in an infected or obstructed system unless a senior specifically instructs it.",
                "Secure the tube properly — most 'blocked' nephrostomies are dislodged ones. Check the dressing and the marked position.",
                "Post-insertion for infection: watch for sepsis in the following hours — decompressing pus can bacteraemically spike them.",
            ),
            listOf(
                "Do not flush a nephrostomy on ward initiative — it's an IR/urology procedure with a small volume (5 mL) and strict asepsis.",
                "Falling output + fever + flank pain = blocked and infected until proven otherwise. Escalate, image, don't wait for the morning.",
            ),
        ),
        Tutorial(
            "Urine tests: what they actually tell you",
            "The dipstick is a screening tool with a bad reputation for a reason.",
            listOf(
                "Nitrites: fairly specific for Enterobacterales, poor sensitivity (needs hours in the bladder; Enterococcus and Pseudomonas don't make them).",
                "Leucocytes: sensitive, not specific — inflammation of any kind, including a catheter sitting there.",
                "Blood on dipstick without symptoms: needs a proper haematuria work-up in the right age group, not an antibiotic.",
                "In a catheterised patient, a positive dipstick means almost nothing. Treat the patient's physiology, not the strip.",
                "MSU technique matters: contaminated samples create fake diagnoses and real antibiotic courses. Send a proper sample.",
                "Culture BEFORE antibiotics whenever you can — after the first dose, you may never learn the organism.",
                "Sterile pyuria: think prior antibiotics, stones, TB, tumour, interstitial nephritis.",
            ),
            listOf(
                "Do not treat asymptomatic bacteriuria — except in pregnancy or before urological instrumentation (VERIFY local policy).",
                "A negative dipstick does not exclude pyelonephritis in an unwell patient.",
            ),
        ),
        Tutorial(
            "Choosing imaging in urology",
            "Match the question to the test before you fill in the form.",
            listOf(
                "Suspected stone / renal colic: non-contrast CT KUB. Fast, no contrast, finds the stone and the alternatives.",
                "Young or pregnant patient with colic: ultrasound first (radiation), accepting lower sensitivity.",
                "Hydronephrosis screening or 'is it obstructed at the bedside': renal tract ultrasound.",
                "Painless visible haematuria work-up: CT urogram (upper tract) PLUS cystoscopy (bladder) — imaging alone misses bladder tumours.",
                "Suspected prostate cancer: multiparametric MRI prostate BEFORE biopsy (PI-RADS reporting), then targeted biopsy.",
                "Suspected testicular pathology: scrotal ultrasound with Doppler — but never delay theatre for suspected torsion to get it.",
                "Staging: CT chest/abdomen/pelvis for urological cancers; bone scan / PSMA PET where prostate cancer staging requires it (specialist-directed).",
                "Always write the clinical question on the request, not just 'renal colic?'. The radiologist answers the question you ask.",
            ),
            listOf(
                "Contrast: check eGFR and allergy history; discuss with radiology in renal impairment rather than cancelling on your own.",
                "Never let imaging delay decompression in an infected obstructed system, or theatre in torsion or Fournier's.",
            ),
        ),
        Tutorial(
            "Pre-op prep for the urology list",
            "The JMO's job the night before is where the list runs on time or falls apart.",
            listOf(
                "Consent is signed by someone who can perform the operation — your job is to make sure it exists, is legible and matches the planned side/site.",
                "Anticoagulants: know each patient's plan and when it was actually last taken. This is the single commonest cause of a cancelled urology list.",
                "MSU before instrumentation: a positive pre-op culture matters — untreated bacteriuria + instrumentation = urosepsis. VERIFY who needs pre-op treatment locally.",
                "Group & hold for TURP, nephrectomy, big open cases, or anyone bleeding. Check it's valid and in-date.",
                "Bloods: FBC, UEC, coags where indicated; ECG per anaesthetic policy.",
                "Fasting instructions and diabetes plans documented; check the morning insulin/metformin decision.",
                "Mark the side with the surgeon for anything lateralised — stones, nephrectomy, orchidectomy.",
                "Antibiotic prophylaxis charted per the local protocol, timed for induction. VERIFY the agent — it varies by procedure.",
            ),
            listOf(
                "Never let a patient go to theatre with an unresolved positive urine culture without the surgeon knowing.",
                "Never guess the anticoagulation plan — get it from the surgeon or the pre-admission note in writing.",
            ),
        ),
        Tutorial(
            "Consent conversations: what patients actually ask",
            "You're often not the consenter — but you'll be asked these at 8 pm. Know the honest answers.",
            listOf(
                "TURP: 'Will I still be able to…?' — retrograde ejaculation is common (majority), erectile dysfunction less so; incontinence uncommon; re-operation may be needed years later. Bleeding and TUR syndrome are the acute risks.",
                "Ureteroscopy + stent: stent symptoms are near-universal (frequency, urgency, flank twinge on voiding, blood); risk of infection, ureteric injury, need for further procedures.",
                "TURBT: haematuria, bladder perforation, need for repeat procedures and surveillance cystoscopies; single-dose intravesical chemotherapy may be given after.",
                "Prostate biopsy: bleeding (urine, semen for weeks — warn them, it's alarming and normal), infection/sepsis (the serious one), retention.",
                "Circumcision: bleeding, infection, cosmetic result, change in sensation.",
                "Nephrectomy: bleeding, transfusion, injury to nearby organs, reduced renal function in the remaining kidney.",
                "If you don't know the answer, say so and get the registrar — inventing a risk figure is worse than a short wait.",
            ),
            listOf(
                "Never obtain consent for a procedure you cannot perform and do not fully understand.",
                "Never reassure about a functional outcome (potency, continence) you haven't checked with the operating surgeon.",
            ),
        ),
        Tutorial(
            "Post-op complications by day",
            "When something goes wrong, the day number narrows the differential fast.",
            listOf(
                "Day 0–1: bleeding (primary haemorrhage), anaesthetic issues, pain control failure, urinary retention after catheter removal, TUR syndrome.",
                "Day 1–2: atelectasis and fever from the chest, ongoing haematuria, clot retention, ileus starting.",
                "Day 3–5: infection — chest, wound, urine, lines. This is the classic window for a fever that means something.",
                "Day 5–7: collections and anastomotic problems, urine leak (check drain fluid creatinine), deep abscess.",
                "Day 7–14: secondary haemorrhage post-TURP as sloughs separate; wound problems; late DVT/PE.",
                "Any day: PE. Sudden hypoxia or tachycardia in a post-op urology patient is a PE until you've thought about it properly.",
                "Fever + urological instrumentation at any point = urine source until proven otherwise, but examine everything.",
            ),
            listOf(
                "Never attribute post-op oliguria to 'normal post-op' without excluding a blocked catheter and hypovolaemia.",
                "A patient who looks worse than their numbers is the one to escalate. Trust that instinct.",
            ),
        ),
        Tutorial(
            "Intermittent self-catheterisation (ISC) & neurogenic bladder",
            "The long-game skill that keeps kidneys alive.",
            listOf(
                "ISC is the gold standard for chronic retention/incomplete emptying — lower infection risk than an indwelling catheter, better independence and sexual function.",
                "Typical regimen: 3–5 times a day, aiming to keep bladder volumes under ~400–500 mL. Frequency is titrated to residuals.",
                "Clean (not sterile) technique at home is standard practice; single-use hydrophilic catheters are commonly supplied.",
                "Neurogenic bladder types matter: suprapontine/suprasacral lesions tend to cause overactivity with high pressures; sacral/peripheral lesions cause a flaccid, poorly emptying bladder.",
                "The kidneys are the priority: high storage pressures, not incontinence, are what destroy them. Urodynamics guides management.",
                "Spinal cord injury patients often manage their own bladders better than the ward does — ask them what their routine is and support it.",
            ),
            listOf(
                "AUTONOMIC DYSREFLEXIA (lesion at/above T6): sudden severe hypertension, pounding headache, sweating and flushing above the lesion, bradycardia. Sit them UP, find and remove the trigger — a blocked catheter or full bladder is the commonest — and treat the BP urgently. This kills; treat it as an emergency.",
                "Never leave a spinal patient's catheter blocked 'until the morning'.",
                "Don't cancel someone's home ISC routine because it's inconvenient for the ward.",
            ),
        ),
        Tutorial(
            "Urostomy / ileal conduit basics",
            "New stoma, new physiology — and the notes people forget to write.",
            listOf(
                "An ileal conduit is NOT a continent reservoir: urine flows continuously into the bag. There's no such thing as 'no output but they're comfortable'.",
                "Mucus in the urine is normal — the conduit is bowel. Warn the patient; don't investigate it as debris.",
                "Ureteric stents are often left in through the conduit post-op; note the planned removal date and which side each is on.",
                "Falling or absent output: think blocked stents, dehydration, or a kinked/dislodged tube — escalate early, it's the same emergency as an obstructed kidney.",
                "Watch the electrolytes: bowel in the urinary tract can cause a hyperchloraemic metabolic acidosis; check the gas and UEC if unwell.",
                "Stomal appearance: pink and moist is healthy. Dusky or black is ischaemia — surgical review now.",
                "Involve the stoma nurse early; they change outcomes and length of stay more than any of your notes.",
            ),
            listOf(
                "Never catheterise a urostomy or manipulate stents without the surgical team's instruction.",
                "Dusky stoma, no output, or peristomal sepsis: escalate immediately — not a morning-round problem.",
            ),
        ),
    )

val moreUroGuides =
    listOf(
        Guide(
            title = "Testicular lump & testicular cancer",
            oneLiner = "A young man with a painless hard lump gets an ultrasound this week, not eventually.",
            sections =
                listOf(
                    GuideSection(
                        "Assessment",
                        listOf(
                            "Painless, hard, irregular, arising FROM the testis and doesn't transilluminate = cancer until excluded.",
                            "Separate from testis and cystic = epididymal cyst; transilluminates and surrounds the testis = hydrocele (but a new hydrocele in a young man may hide a tumour — image it).",
                            "Ultrasound scrotum is the test. Tumour markers BEFORE orchidectomy: AFP, beta-hCG, LDH.",
                            "Ask about back pain, cough/dyspnoea, gynaecomastia — metastatic presentations.",
                        ),
                    ),
                    GuideSection(
                        "Management pathway",
                        listOf(
                            "Radical inguinal orchidectomy is both diagnostic and therapeutic — never a scrotal approach or a biopsy through the scrotum.",
                            "Offer sperm banking BEFORE treatment. This conversation is your responsibility to prompt if nobody has.",
                            "Staging CT chest/abdomen/pelvis; markers repeated after orchidectomy.",
                            "Prosthesis discussion at the time of surgery — patients appreciate being asked.",
                            "Outcomes are excellent even with metastatic disease — say so, because they're often terrified.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Never biopsy a suspected testicular tumour through the scrotum — it seeds a new lymphatic field.",
                    "Never dismiss a hard testicular lump as 'probably infection' without imaging; treated 'epididymo-orchitis' that doesn't settle is cancer until scanned.",
                    "Don't delay markers until after surgery — pre-op values are prognostically vital.",
                ),
        ),
        Guide(
            title = "Bladder cancer & TURBT",
            oneLiner = "Painless visible haematuria is bladder cancer until cystoscopy says otherwise.",
            sections =
                listOf(
                    GuideSection(
                        "Presentation and work-up",
                        listOf(
                            "Painless visible haematuria in an adult over 45 (and smokers/occupational exposures at any age): urgent haematuria clinic — CT urogram plus flexible cystoscopy.",
                            "Irritative symptoms with sterile urine can be carcinoma in situ — don't keep treating 'recurrent UTI' in a smoker.",
                            "Risk factors worth documenting: smoking (the big one), aromatic amines, prior pelvic radiotherapy, chronic catheter.",
                        ),
                    ),
                    GuideSection(
                        "Around TURBT",
                        listOf(
                            "Post-op: expect haematuria; CBI may run. Manage clots as per the washout tutorial — gently, the resection bed is raw.",
                            "Single-dose intravesical chemotherapy (commonly mitomycin) may be instilled early post-op for non-muscle-invasive disease. Handle per protocol; it is a cytotoxic.",
                            "If perforation is suspected (severe pain, distension, low output, peritonism) — stop irrigation, tell the surgeon now, keep the catheter draining.",
                            "Follow-up is surveillance cystoscopy — make sure the plan and the histology chase are in the discharge summary.",
                            "Muscle-invasive disease means a different pathway entirely (neoadjuvant chemo, cystectomy or chemoradiation) — that's a consultant conversation.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Never dismiss a single episode of painless visible haematuria because it stopped.",
                    "No forceful bladder washout immediately post-TURBT without the surgeon's OK — perforation risk.",
                    "Cytotoxic instillation: follow the protocol for handling and for contraindications (recent perforation or heavy bleeding).",
                ),
        ),
        Guide(
            title = "Advanced prostate cancer on the ward",
            oneLiner = "Bone pain, cord compression and hormone therapy problems.",
            sections =
                listOf(
                    GuideSection(
                        "What you'll be called about",
                        listOf(
                            "Bone pain: prostate cancer metastasises to bone, classically axial skeleton. New, severe or progressive back pain is imaged, not just given analgesia.",
                            "Hormone therapy (ADT) effects: hot flushes, fatigue, sarcopenia, osteoporosis, metabolic and cardiovascular risk, mood change.",
                            "Anaemia and marrow infiltration in advanced disease; check the film and iron studies before assuming bleeding.",
                            "Renal impairment from ureteric obstruction by local disease — think of it, scan for hydronephrosis.",
                        ),
                    ),
                    GuideSection(
                        "Practical points",
                        listOf(
                            "Any new neurology (leg weakness, sensory level, bladder/bowel change) = urgent MRI whole spine + high-dose steroids per protocol + urgent oncology/spinal referral. VERIFY dexamethasone dose locally.",
                            "Hypercalcaemia of malignancy: confusion, constipation, thirst — check corrected calcium in the unwell patient.",
                            "Bone protection (calcium/vitamin D, bone-targeted agents) is part of good ADT care — flag if missing.",
                            "Palliative and supportive care involvement early improves symptoms and quality of life; it isn't giving up.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "SPINAL CORD COMPRESSION IS AN EMERGENCY: hours matter for preserving walking. Never wait for a morning MRI slot without escalating.",
                    "Don't attribute new back pain in prostate cancer to 'mechanical' without a considered assessment.",
                    "Don't stop ADT abruptly without oncology input.",
                ),
        ),
        Guide(
            title = "Urethral stricture & failed catheterisation",
            oneLiner = "The reason your catheter won't pass — and why forcing it makes tomorrow worse.",
            sections =
                listOf(
                    GuideSection(
                        "Recognise",
                        listOf(
                            "Suggestive history: previous catheters or instrumentation, STI, trauma (straddle injury), lichen sclerosus, prior hypospadias surgery.",
                            "Symptoms: slow stream for a long time, spraying, straining, incomplete emptying, recurrent UTI.",
                            "On attempted catheterisation: a firm stop well before the prostate, often at the bulbar urethra.",
                        ),
                    ),
                    GuideSection(
                        "Management",
                        listOf(
                            "Acute retention with known/suspected stricture: try a SMALLER catheter (12–14 Fr) once, with plenty of gel and time.",
                            "Failed: urology for flexible cystoscopy-guided placement, guidewire technique, or suprapubic catheter. Not a ward dilatation.",
                            "Definitive options (dilatation, optical urethrotomy, urethroplasty) are outpatient/urology decisions — urethroplasty has the best durable outcomes for suitable strictures.",
                            "Document every attempt, the size used, and any bleeding — it changes what the next clinician does.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Never force a catheter past resistance — false passages and bleeding make the definitive repair harder.",
                    "No blind ward dilatation with bougies or repeated stiff catheters.",
                    "Blood at the meatus after trauma: stop, think urethral injury, get urology before any further attempt.",
                ),
        ),
        Guide(
            title = "Urinary incontinence: sorting the type",
            oneLiner = "The type dictates the treatment — get the history right and the rest follows.",
            sections =
                listOf(
                    GuideSection(
                        "Types",
                        listOf(
                            "Stress: leak on cough/laugh/lift. Post-prostatectomy in men; pelvic floor weakness in women.",
                            "Urgency: sudden desperate need, often with frequency and nocturia — overactive bladder.",
                            "Mixed: both, and common. Treat the more bothersome first.",
                            "Overflow: chronic retention with dribbling — bladder scan finds it, and it's the one that damages kidneys.",
                            "Functional/continence-of-opportunity: they can't get to the toilet in time (mobility, delirium, restraints, no call bell answered). Extremely common in hospital and frequently missed.",
                            "Continuous leak post-op or post-radiotherapy: think fistula — needs specialist assessment.",
                        ),
                    ),
                    GuideSection(
                        "First moves on the ward",
                        listOf(
                            "Bladder scan every new 'incontinent' patient — you're excluding overflow retention.",
                            "Look for reversible causes: UTI, constipation, delirium, diuretic timing, sedatives, mobility, oedema.",
                            "Bladder diary and pelvic floor referral are the genuinely effective first-line interventions for stress and urgency.",
                            "Overactive bladder drugs: antimuscarinics (caution in the elderly — cognitive load, falls) vs beta-3 agonists. Specialist or GP-led choice. VERIFY.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Never insert a catheter for convenience of nursing incontinence — it's an infection and dignity harm; treat the cause.",
                    "Don't start antimuscarinics in a delirious or cognitively impaired elderly patient without senior review.",
                    "Don't miss overflow: 'incontinence' with a full bladder is retention.",
                ),
        ),
        Guide(
            title = "Genital & renal trauma",
            oneLiner = "Rare, time-critical, and you only need to recognise them.",
            sections =
                listOf(
                    GuideSection(
                        "Penile fracture",
                        listOf(
                            "History is diagnostic: a crack or pop during intercourse, immediate detumescence, pain, swelling and 'aubergine' deformity.",
                            "It is a surgical emergency — early repair gives far better functional outcomes.",
                            "Ask about blood at the meatus or difficulty voiding: associated urethral injury changes the operation.",
                        ),
                    ),
                    GuideSection(
                        "Testicular / scrotal trauma",
                        listOf(
                            "Ultrasound to assess for rupture; testicular rupture needs exploration and repair, early salvage is far better.",
                            "Large haematocele usually needs drainage/exploration rather than conservative management.",
                        ),
                    ),
                    GuideSection(
                        "Renal trauma",
                        listOf(
                            "Suspect with flank trauma, rib fractures, visible haematuria after injury.",
                            "Haemodynamically stable: CT with contrast including a delayed (urographic) phase — otherwise you miss collecting system injury.",
                            "Most blunt renal injuries are managed conservatively; instability means IR embolisation or theatre.",
                            "Ureteric injury is often iatrogenic (pelvic surgery) — a post-op patient with flank pain, fever, urine leak or rising creatinine needs it excluded.",
                        ),
                    ),
                ),
            contraindications =
                listOf(
                    "Blood at the meatus, high-riding prostate or pelvic fracture: DO NOT catheterise — retrograde urethrogram and urology first.",
                    "Never 'watch and wait' a suspected penile fracture or testicular rupture overnight.",
                    "Don't accept a CT without delayed phase to exclude collecting system or ureteric injury.",
                ),
        ),
    )

/** Everything the Learn tab renders, in order. */
val allUroTutorials = uroTutorials + moreUroTutorials
val allUroGuides = uroGuides + moreUroGuides

data class Flashcard(
    val topic: String,
    val question: String,
    val answer: String,
)

/** Self-test deck — tap a card to reveal. Deliberately answers, not essays. */
val uroFlashcards =
    listOf(
        Flashcard(
            "Emergencies",
            "Fever + an obstructing ureteric stone. What is the ONE thing that must happen tonight?",
            "Urgent decompression (ureteric stent or nephrostomy) plus IV antibiotics and resuscitation. " +
                "An infected obstructed system is pus under pressure — antibiotics alone do not treat it. Call urology immediately.",
        ),
        Flashcard(
            "Emergencies",
            "Sudden severe unilateral testicular pain in a 16-year-old, absent cremasteric reflex. Next step?",
            "Immediate surgical exploration. Do NOT wait for an ultrasound if the story is convincing — " +
                "the salvage window is roughly 6 hours. Keep NBM, analgesia, IV access, call the reg.",
        ),
        Flashcard(
            "Emergencies",
            "Perineal pain far out of proportion to appearance, in a septic diabetic. Diagnosis and action?",
            "Fournier's gangrene. Emergency surgical debridement + broad-spectrum IV antibiotics + resuscitation. " +
                "Imaging must not delay theatre.",
        ),
        Flashcard(
            "Emergencies",
            "Spinal cord injury at T5: sudden pounding headache, BP 210/110, flushing, bradycardia. What is this and what do you do first?",
            "Autonomic dysreflexia. Sit the patient UP, then find and remove the trigger — most often a blocked catheter " +
                "or full bladder. Treat the hypertension urgently. It is life-threatening.",
        ),
        Flashcard(
            "Emergencies",
            "Ischaemic priapism: how long before permanent damage becomes likely, and what's the definitive step?",
            "Beyond ~4 hours it is a compartment syndrome; by 24–48 h permanent damage is likely. " +
                "Definitive management is corporal aspiration ± irrigation ± intracavernosal phenylephrine, done by seniors with monitoring.",
        ),
        Flashcard(
            "Catheters",
            "You cannot pass a 16 Fr in a 78-year-old with a big prostate. What do you change?",
            "More lignocaine gel and more time, then go UP a size (18 Fr) and/or use a coudé tip pointing up. " +
                "Two failed gentle attempts = stop and call urology.",
        ),
        Flashcard(
            "Catheters",
            "You suspect a urethral stricture. Bigger or smaller catheter?",
            "SMALLER (12–14 Fr), once, gently. Repeatedly ramming a larger catheter creates false passages.",
        ),
        Flashcard(
            "Catheters",
            "When must you never inflate the balloon?",
            "Until the catheter is inserted to the hilt and you have urine (or have flushed and got it back). " +
                "Any pain on inflation: stop, deflate, advance further.",
        ),
        Flashcard(
            "Catheters",
            "After catheterising an uncircumcised man, what must you always do?",
            "Reduce the foreskin. Leaving it retracted causes paraphimosis — a preventable emergency.",
        ),
        Flashcard(
            "Catheters",
            "Catheter bypassing and not draining. First three checks?",
            "Kinked tubing / bag above bladder / clamp left on; then flush 50 mL saline; then consider blockage by clot or sediment and change it.",
        ),
        Flashcard(
            "Haematuria",
            "Clot retention: what is the treatment, and what is irrigation for?",
            "Manual bladder washout via a 3-way (22–24 Fr) until it runs clear is the TREATMENT. " +
                "Continuous irrigation only PREVENTS re-clotting — titrate it to keep the outflow rosé.",
        ),
        Flashcard(
            "Haematuria",
            "Painless visible haematuria in a 62-year-old smoker. What must be arranged?",
            "Urgent haematuria work-up: CT urogram AND flexible cystoscopy. It is bladder cancer until excluded — " +
                "imaging alone is not enough.",
        ),
        Flashcard(
            "Retention",
            "Drained 2.2 L for acute-on-chronic retention. What do you watch for next?",
            "Post-obstructive diuresis — hourly urine output, replace per protocol if sustained >200 mL/h, recheck UEC. " +
                "Also expect mild ex vacuo haematuria.",
        ),
        Flashcard(
            "Retention",
            "New urinary retention with back pain and saddle numbness. What have you got until proven otherwise?",
            "Cauda equina syndrome. Emergency MRI and senior escalation — not a urology clinic referral.",
        ),
        Flashcard(
            "Retention",
            "What must every retention patient leave the ward with?",
            "A documented catheter plan: TOV date, urology follow-up, or an explicit long-term catheter decision. " +
                "'IDC inserted' alone is an incomplete note.",
        ),
        Flashcard(
            "Stones",
            "First-line analgesia for renal colic and why?",
            "NSAIDs (e.g. indomethacin PR or ibuprofen PO) if renal function and gut allow — genuinely superior to opioids for colic. " +
                "Add opioid for breakthrough.",
        ),
        Flashcard(
            "Stones",
            "Which renal colic patients can usually go home?",
            "Stone <5 mm and distal, pain controlled, normal renal function, no infection, able to return — " +
                "with analgesia, follow-up and clear return precautions.",
        ),
        Flashcard(
            "Stones",
            "A 58-year-old with 'first episode of renal colic'. What must you consider?",
            "Ruptured/leaking abdominal aortic aneurysm — it mimics colic. Examine the abdomen and think aorta before anchoring.",
        ),
        Flashcard(
            "Stents",
            "Which stent symptoms are expected, and which are an emergency?",
            "Expected: flank twinge on voiding, frequency, urgency, mild haematuria. " +
                "Emergency: fever — a stented patient with fever is blocked/infected until proven otherwise.",
        ),
        Flashcard(
            "Stents",
            "What is the single most important thing to document about any stent?",
            "The removal or exchange date. A forgotten stent encrusts and becomes a major surgical problem.",
        ),
        Flashcard(
            "Post-op",
            "Confused, nauseated, bradycardic and hypertensive after a long TURP. Diagnosis?",
            "TUR syndrome — dilutional hyponatraemia from irrigation absorption. Urgent UEC/VBG, senior and anaesthetics/ICU. " +
                "Never blame post-op confusion on 'the anaesthetic' before seeing a sodium.",
        ),
        Flashcard(
            "Post-op",
            "Post-TURP patient re-presents at day 10 with heavy haematuria. Is this expected?",
            "Secondary haemorrhage as sloughs separate is a recognised day 7–14 event — warn patients at discharge. " +
                "It still needs assessment: washout, Hb, and surgeon involvement if brisk.",
        ),
        Flashcard(
            "Post-op",
            "Day 1 post-nephrectomy: tachycardic, Hb down 30, drain filling. What is your working diagnosis?",
            "Post-operative bleeding — the retroperitoneum hides litres. Resuscitate, crossmatch, call the surgeon now; don't watch it fall twice.",
        ),
        Flashcard(
            "Post-op",
            "Rising creatinine and flank pain a few days after pelvic surgery. What must be excluded?",
            "Iatrogenic ureteric injury or obstruction — image the renal tract (and check drain fluid creatinine if a drain is in).",
        ),
        Flashcard(
            "Infection",
            "Catheterised nursing-home patient, cloudy smelly urine, otherwise well. Antibiotics?",
            "No. Asymptomatic bacteriuria in a catheterised patient should not be treated — you breed resistance and C. diff. " +
                "Treat systemic features, not the smell or the dipstick.",
        ),
        Flashcard(
            "Infection",
            "If you do treat a catheter-associated UTI, what else should happen?",
            "Change the catheter (biofilm) and culture from the NEW catheter, not the old one.",
        ),
        Flashcard(
            "Infection",
            "Which UTI patients need further investigation rather than just a script?",
            "Men, children, recurrent infections, stone-formers, pyelonephritis, and anyone with obstruction or a device — " +
                "they need a reason found.",
        ),
        Flashcard(
            "Pharmacology",
            "How does finasteride change PSA interpretation?",
            "It roughly HALVES the measured PSA — double the value to interpret it, and use the trend.",
        ),
        Flashcard(
            "Pharmacology",
            "Key counselling points for tamsulosin?",
            "Postural hypotension, retrograde ejaculation, and floppy iris syndrome — flag it before cataract surgery. " +
                "It also improves stent symptoms and TOV success.",
        ),
        Flashcard(
            "Pharmacology",
            "Which analgesics need special care at eGFR <30?",
            "Avoid NSAIDs entirely; avoid codeine and tramadol; reduce or avoid routine oxycodone (metabolite accumulation) — " +
                "renal/pain team input for alternatives. VERIFY locally.",
        ),
        Flashcard(
            "PSA",
            "Name four non-cancer causes of a raised PSA.",
            "BPH, prostatitis/UTI, urinary retention, recent catheter/instrumentation (also ejaculation and cycling). " +
                "Don't interpret a PSA taken during any of these — repeat in 4–6 weeks.",
        ),
        Flashcard(
            "Assessment",
            "Bladder scan says 60 mL in an anuric patient. Where is the problem?",
            "Upstream — this is AKI or obstruction above the bladder, not retention. Catheterising will not fix it.",
        ),
        Flashcard(
            "Assessment",
            "What single number best predicts what happens next in retention?",
            "The drained residual volume. 600 mL acute and 2.5 L chronic are different diseases with different risks " +
                "(diuresis, renal impairment, long-term drainage).",
        ),
        Flashcard(
            "Documentation",
            "What makes a ward round plan actually happen?",
            "An owner and a trigger for every line: what, who, and by when — plus explicit escalation thresholds.",
        ),
    )
