# Increment App Business Plan

**App name:** Increment  
**Positioning:** A listening, reading and writing practice app for university students, with optional speaking practice  

## 1. App Overview

Increment is designed for university students who need to take part in English-medium class discussions and presentations. It helps them turn English they already understand into language they can use independently. These students have some listening and reading proficiency and can follow content with subtitles, but struggle to retrieve suitable words and phrases when speaking or writing. They may also misuse familiar expressions or leave out important logical connections.

The app offers selected English audio and video materials and lets learners import their own content, including audio, video and timestamped transcripts. Interest is the best teacher: learners can practise with topics, courses and conversations they genuinely want to understand. Each session centres on a 60–90-second excerpt that offers a manageable challenge, following a core sequence: **listen, read, listen again, write and revise**. Once learners understand the content, they choose one phrase or sentence pattern they recognise but do not yet use confidently as their practice target. They then try to use it in their writing. Learners who want speaking practice can also record a retelling and make a new recording after reviewing their work. Subtitles, notes and sentence cues help keep the task manageable.

This sequence forms a repeatable practice cycle. The core cycle is expected to take 15–20 minutes, or approximately 20–25 minutes with speaking practice. Both paths support pausing and resuming. Completing the core cycle automatically adds one completion record and increments the completion count. Learners do not need to save drafts, expressions or reflections to finish. Continued practice comes from using the app each day and completing another cycle. Speaking remains optional; skipping it does not affect completion.

### 1.1 The Name and Design Concept

| Meaning | How it shapes the app |
|---|---|
| `i+1` | Choose material just beyond the learner's current level while keeping the cognitive demands manageable. |
| `Δ Ability` | Focus on making understood language available for active use, rather than simply increasing vocabulary size. |
| `i++` | Organise practice into repeatable cycles, making accumulated practice visible through a completion counter inspired by programming. |

### 1.2 The Practice Cycle

| Step | What the learner does |
|---|---|
| 1. Listen without subtitles | Choose a suitable excerpt from the built-in library or imported content, note its main point and supporting reasons, and mark unclear moments. Switch material or add support if needed. |
| 2. Read for meaning | Read the timestamped English subtitles, request AI analysis of sentence components and grammar as needed, consult essential notes, and write down three to five keywords. Choose one understood but underused expression as the session target. |
| 3. Listen again | Use subtitle timestamps to replay one or two difficult passages, then listen to the full excerpt again with subtitles off. Imitating the speaker aloud is optional. |
| 4. Retell aloud — optional | Use the selected keywords and cues to record a 45–60-second retelling, add a personal view with a reason, and try the target expression. Listen back and self-assess, or skip directly to writing. |
| 5. Write independently | With the source, keywords and previous responses hidden, write an 80–100-word summary in the learner's own words, preserving the speaker's main point, supporting reasons, conditions and qualifications. A separate personal opinion is not required. The web demonstration uses two to three sentences to illustrate the same process. |
| 6. Review and complete | Review the writing using the support available in the chosen tier: basic rule-based self-checks in Free or the large language model's (LLM's) evaluation in Pro. Revise or retain the response. Learners who choose speaking practice can also record another attempt based on their self-assessment. Once the cycle is complete, the app automatically records completion. |

During AI-assisted writing review, the LLM evaluates the learner's response for accurate meaning, information completeness, logical organisation, grammar and appropriate expression, using the subtitles, prepared key points and session target as references. Feedback is prioritised: address logical or meaning-related deviations first, then offer wording and grammar suggestions, with references to the relevant source passages. Suggestions should remain concise and manageable. For imported materials without prepared key points, the matching transcript serves as the content reference. Feedback focuses on accurate meaning and appropriate use of the target expression. Alternative wording is accepted when it preserves the meaning. Learners decide whether to revise or retain their wording after checking the feedback against the source. Free users follow the same review-and-complete sequence with basic rule-based self-checks; the proposed tiers are described in Section 4.3.

A cycle is complete when the learner finishes listening, reading and listening again, writes independently, and finishes reviewing and revising or retaining the response. The app automatically adds a dated completion record and increments the count once for that cycle. There is no separate save step or required reflection form. The completion history shows completed cycles and daily practice dates; it is not a collection of writing drafts or expression cards. Speaking and self-assessment are optional and do not determine completion.

### 1.3 Learning Methods and Features

| Module | Practice method | Main features |
|---|---|---|
| Content import | Bring personally relevant material into the same practice cycle. | Local audio/video import, subtitle and transcript parsing, media-text pairing, timing preview and excerpt selection. |
| Listening and reading | Understand moderately challenging material with support as needed, then listen again without subtitles. | Difficulty descriptions, video/audio access, timestamped subtitles, sentence replay, notes, subtitle controls and AI-assisted “Sentence Focus” analysis of sentence components and grammar. |
| Speaking — optional | Retell from keywords, try the target expression, and listen back to assess the attempt. | Enable or skip speaking, target expression, keyword notes, structural cues, recording, playback, rerecording, a self-assessment checklist and local recordings for playback. |
| Writing | Summarise the material independently to check whether understood language can be used appropriately. | Writing workspace, word count, basic rule-based self-checks in Free, AI evaluation of meaning, organisation and language use in Pro, source references, and comparison of initial and revised responses within the current cycle. |
| Completion history | Make daily use and completed cycles visible, encouraging learners to return for another session. | Automatically recorded completion dates, cycle count and an entry point for daily practice. |

“Sentence Focus” presents AI-assisted analysis of a selected sentence: subject, predicate, object or complement, modifiers, clause relationships and relevant grammar. It links each explanation to the corresponding words in the original sentence, helping learners grasp the main idea before working through the full sentence. Negation, conditions and limits to a claim remain clearly marked so that essential meaning is not lost. Each cycle concentrates on one expression target, with accurate and clear use as the goal.

### 1.4 Learning Materials and Initial Development Scope

Each learning package includes the following resources:

| Resource | Preparation and use |
|---|---|
| Video or audio | Record the speaker, title, official playback link, and excerpt start and end times. Use it for listening, replay and optional spoken imitation. Classroom playback uses the official page. |
| Timestamped English subtitles | Each subtitle entry includes a start time, end time and matching text, aligned with the same version of the video or audio. These support targeted replay and comparison with the source. |

The first version will include four practice excerpts and two reserved assessment excerpts. The team will manually check the existing subtitle text and timing, and prepare difficulty descriptions, essential background, optional notes and candidate expressions. Learners choose a target after understanding it. All materials will identify their sources.

Material suitability depends on whether learners can understand the main content with limited help. If an excerpt remains too difficult, they can change the material or add support before moving to writing and optional speaking. The first version combines a curated selection with user-imported materials and learner choice. Difficulty descriptions for the curated library are prepared by the team; learners check whether their imported content is manageable with available support. Automatic level assessment and difficulty progression are outside the initial scope.

**AI use is limited to two areas: reading support that analyses sentence components, grammatical structures and their meaning in context; and writing support that evaluates and analyses the learner's own response. Speaking practice remains optional, with recording, playback, rerecording and learner self-assessment.** Recordings stay on the device and are not uploaded to the model. The app does not provide speech recognition, automatic transcription or speaking scores. It requests microphone access only when the learner chooses to record. Skipping or stopping speaking practice does not interrupt the core cycle.

The first version supports local content import, timeline parsing, audio-text pairing, automatic completion records and temporary draft recovery for interrupted cycles. For both built-in and imported materials, learners can select a sentence and request AI analysis using the surrounding transcript as context. The analysis identifies the main clause, sentence components, modifiers and relevant grammar while preserving negation, conditions and qualifications. If the LLM service is unavailable, learners can continue reading the original text and review their writing with a checklist; recording and playback remain available. Automatic material recommendations and cloud synchronisation are outside the initial scope.

The two AI tasks have separate inputs and outputs. Reading analysis is requested on demand for a selected sentence and explains how the original language works. Writing evaluation follows the learner's independent response, gives a brief assessment and prioritises logical and meaning-related issues before wording and grammar suggestions. The depth of support follows the proposed offer in Section 4.3, with basic rule-based writing self-checks in Free and deeper AI evaluation in Pro. AI does not produce the learner's initial answer. Material selection, media import, timeline alignment, listening playback, speaking tools and completion counting remain app functions.

### 1.5 Bring Your Own Content

**Interest is the best teacher.** Learners can bring content they want to understand into Increment: a favourite podcast, a course recording, an interview or a video about a personal interest. This gives daily practice a clear personal purpose while retaining the same listening, reading, writing and review cycle.

| Content type | Initial import support | Use in practice |
|---|---|---|
| Audio | MP3, M4A and WAV files supported by the device's media decoder. | Listen, select a short excerpt, replay passages and optionally retell. |
| Video | MP4 and WebM files supported by the device's media decoder. | Watch or listen to a chosen passage, then practise with its matching text. |
| Timestamped text | SRT and WebVTT subtitles, plus TXT transcripts using a documented timestamp format such as `[00:12.500] Sentence text`. | Match sentences to playback times for reading, navigation and replay. |

The import flow is **choose files → pair media and text → preview and check timing → select a practice excerpt**. A learner can import either component first and add the matching file later. Before starting a complete cycle, the learner confirms that the excerpt has both playable media and matching text. The app checks timestamp order, valid time ranges and empty entries; learners can adjust a uniform timing offset after previewing. A timestamped TXT entry ends at the next entry's start, with the final entry ending at the selected excerpt's end.

Imported files remain local. The learner chooses the excerpt and expression target; the written-feedback request uses the selected transcript, target and response. Prepared key points are included for built-in materials; for imported content, the transcript is the reference and learner notes are optional. The model cites that text when giving feedback. Reading-analysis requests include the selected sentence and its surrounding text; writing-evaluation requests include the transcript, target and learner response. File import, timestamp parsing, playback and completion recording are handled by the app.

The completion rule is the same for both material sources: finishing the core cycle automatically creates one completion record. Daily use can start with another personally relevant excerpt, with the built-in library available whenever learners prefer a ready-made task.

### 1.6 Planned Product Extensions

The commercial roadmap extends the individual practice cycle into academic content, voluntary sharing and institutional use. These are proposed additions rather than completed features or confirmed partnerships.

| Extension | Proposed use and timing |
|---|---|
| Dedicated presentation-material library | Provide material for academic presentations and reports as part of the proposed Pro Academic offer. |
| Initial-versus-final writing cards | Let learners choose to create and share a comparison of their own drafts during the months 3–8 content-marketing phase. Sharing is optional and is not required to complete a cycle; completion history continues to record dates and cycle counts. |
| Class learning dashboard | Explore a dashboard for university English for Academic Purposes (EAP) and English for Specific Purposes (ESP) teaching teams during the months 8–18 institutional phase. This requires separate development beyond the initial local practice scope. |
| Joint academic practice libraries | Seek university and test-preparation partners to develop IELTS/TOEFL academic micro-practice materials during the months 8–18 phase. |

## 2. Unique Selling Proposition

**Increment turns a short, moderately challenging English audio or video excerpt into a repeatable listening, reading, writing and revision cycle built around one understood but underused expression, with speaking practice available when the learner wants it.**

Material selection, comprehension, writing, review and further practice are organised within a single cycle. Speaking can be added as needed. The app automatically records each completed cycle. Learners build repeated practice through daily use, choosing suitable material and an expression target for each session.

> Put the English you understand to work. One expression at a time.

| Product feature | Value to the learner |
|---|---|
| Personally relevant content | Import audio, video and timestamped text about topics the learner wants to understand, giving daily practice a personal purpose. |
| Manageable challenge and optional support | Material choice, notes and sentence cues help control the demands of comprehension. |
| A bridge from understanding to active use | Learners select one understood expression, use it in a short written response, and optionally practise it in a spoken retelling. |
| Daily practice with a visible completion history | Automatically recorded cycles and practice dates make regular use visible and encourage learners to continue each day. |

| Common practice approach | What Increment adds |
|---|---|
| Watching videos and reading subtitles | A task that asks learners to use understood language in their own writing, with an optional spoken retelling. |
| Using separate playback, note-taking and recording tools | A coordinated sequence with pause-and-resume support and automatic completion records. |
| Asking a general-purpose AI tool to correct writing | Prioritised feedback tied to the session target, followed by learner-led revision and further practice. |

### 2.1 Competitive Positioning

The Chinese pitch compares three common ways of using existing products. The comparison below concerns those workflows rather than a comprehensive assessment of every product's capabilities.

| Product examples and usage pattern | Limitation addressed by Increment | Increment's proposed response |
|---|---|---|
| Duolingo / Baicizhan (百词斩): fragmented vocabulary memorisation and word-selection exercises | Isolated word practice does not by itself provide a complete task for organising a logical paragraph in a complex context. | Start from a short, coherent source and require an independent summary that preserves its reasons and conditions. |
| Grammarly / DeepL: passive correction or one-click rewriting | When the tool does the rewriting, the learner may have little opportunity to retrieve and use the language independently. | Require an initial response before review, then let the learner revise or retain their own wording. |
| ChatGPT / Claude: direct generation through a general-purpose model | Learners must construct their own learning prompts and support sequence; accepting a generated full answer can encourage dependence. | Provide a guided input-to-expression cycle and prioritised review without generating the learner's initial answer. |

Increment's intended niche combines short source materials, required independent expression, and review of logic and conditions. Whether learners value this combination remains to be tested.

## 3. Target Users and Persona

### 3.1 Target Users

The initial target audience is Chinese university students from their second undergraduate year to their first postgraduate year who need to participate in English-medium instruction (EMI) discussions and deliver presentations in English. Initial trials will focus on students interested in technology and learning who have basic listening and reading proficiency but find it difficult to use familiar language independently.

The wider market includes university learners with advanced CET-4/CET-6 preparation needs, postgraduate entrance-examination needs, English academic-reading tasks, and IELTS/TOEFL preparation needs. These are potential expansion groups; the initial focus remains students with frequent English presentation and report tasks, particularly in business and computing-related programmes, joint Chinese–international programmes and international colleges.

The Chinese pitch uses the following preliminary population estimates:

| Market layer | Preliminary size | Population and needs |
|---|---|---|
| Total addressable market (TAM) | 40 million learners | University English learners in China, including advanced CET-4/CET-6 preparation, postgraduate entrance-examination preparation and English academic-reading needs. |
| Serviceable available market (SAM) | 1.2 million learners | The academic and EMI core group, including joint Chinese–international programmes, English-medium majors and IELTS/TOEFL candidates. |
| Serviceable obtainable market (SOM) | 80,000–100,000 learners | The early core target population: business and computing-related students who frequently face English presentations and reports. |

These figures are preliminary project estimates awaiting research and checks for overlap between groups. They are not verified demand or observed customer numbers. SOM denotes the early target-population size used in the pitch, not a first-year acquisition commitment. The first-year operating target is stated separately in Section 4.4.

**Illustrative persona: Xiaolin, 21, a third-year computing student in a joint Chinese–international degree programme.**

| Dimension | Description |
|---|---|
| Immediate tasks | Participate in class discussions in English and prepare a group presentation. |
| English proficiency | Understands the gist with English subtitles and recognises common conditional and causal expressions, but struggles to identify some familiar language in connected speech. |
| Main difficulties | Loses supporting reasons without subtitles, pauses frequently when speaking, and sometimes omits conditions or confuses the speaker's view with her own in writing. |
| Current habits | Rereads subtitles, saves videos and copies useful sentences, but does relatively little independent speaking or writing. Sometimes uses AI-generated answers directly. |
| Learning goals | Use understood language in her own responses, explain views and reasons clearly, and rely less on translating sentence by sentence. |
| Practice conditions | Can spend 15–20 minutes on listening, reading and writing with headphones. Adds speaking when circumstances allow, bringing the total to about 20–25 minutes. Needs to pause and resume interrupted sessions. |
| Reasons to return | Keep a daily practice routine and prepare for upcoming discussions and presentations. |
| Reasons to stop | Material is too difficult, the sequence feels cumbersome, recordings are lost, feedback is unclear, or practice feels disconnected from real coursework. |

> “With subtitles on, I feel like I understand. When I try to explain it to someone else, I can only think of a few keywords. I can't put them into complete sentences.”

### 3.2 User Needs and Feature Responses

| User difficulty | Proposed feature response |
|---|---|
| Recognises language in print but cannot identify it in speech | Replay the relevant passage using subtitle timestamps, then listen again without subtitles. Spoken imitation is optional. |
| Struggles with complex sentence structure | Request AI analysis through Sentence Focus to identify sentence components, clause relationships and grammatical usage, with negation and conditions kept visible. |
| Understands the content but cannot express it independently | Choose one understood but underused expression, practise with keywords and structural cues, then gradually reduce support. |
| Does not clearly connect a view with its reasons | Use a short written response and feedback to check logical connections. Speaking participants can also listen back and self-assess. |
| Worries about evaluation or cannot speak aloud | Complete the core cycle without speaking. Optional speaking uses private recordings and self-assessment; written feedback prioritises the most useful changes and keeps explanations manageable. |
| Finds available material unrelated to personal interests or coursework | Import personally relevant audio or video with matching timestamped text and select a short practice excerpt. |
| Interrupts practice or struggles to practise regularly | Resume an unfinished cycle, then see completed cycles and practice dates in the completion history. Return each day for another session. |

### 3.3 Practice Schedule

| Task | Activity | Estimated time |
|---|---|---|
| Daily practice cycle | Choose an excerpt and target expression; listen, read, listen again, write and review. Add speaking and self-assessment if desired. Completion is recorded automatically. | 15–20 minutes for the core cycle; approximately 20–25 minutes with speaking. |
| Pause and resume | Resume an unfinished cycle from the last step when interrupted. | Complete the remaining steps at a convenient time. |
| Continue on another day | Open the app as part of a daily routine and complete another cycle with suitable material. Each session provides another opportunity to understand and use English. | The same core or speaking-inclusive duration. |

**Illustrative example, created by the team:** A learner understands conditions introduced by `when ...` but rarely uses them, so chooses “stating the condition under which a claim holds” as the target. The material suggests that flexible working may improve job satisfaction when teams coordinate effectively. The learner writes, “Flexible working is always better.” After an LLM prompt, the learner adds the missing condition and finishes the review. The app automatically records the completed cycle. If speaking is selected, the learner can also record a retelling, listen back and decide whether to rerecord.

The completion record captures the cycle identifier and completion date and time. For example, completing today's cycle adds one to the count and marks today as a practice day. On the next day, the learner opens the app and starts another cycle. Within a session, learners can compare their initial and revised writing or listen back to their own recordings.

### 3.4 Optional Speaking and Support Settings

Learners can choose a spoken retelling before writing and further spoken practice after revision. Both are optional. If speaking aloud is inconvenient or microphone permission is unavailable, learners proceed with the listening, reading and writing cycle. Those who choose speaking can select a support level; they formulate the actual sentences themselves.

| Setting | What is visible |
|---|---|
| Structural cues | A sequence such as “point → reason or example → condition → personal view,” optional linking words, and up to five learner-written keywords. |
| Keywords only | Up to three learner-written keywords, with no structural cues or linking words. |
| No cues | The task instructions only; source text, notes and previous responses are hidden. |

Learners adjust support based on comprehension and self-assessment. If the source meaning is uncertain, they return to it first. If organising a response is difficult, they can enable structural cues. Writing hides cues by default; reopening them is recorded. These records describe the assistance used and are not used to score speaking.

Daily practice follows the same core cycle. Learners choose suitable material and a target for that session; they may encounter and reuse familiar expressions naturally. Each completed cycle is recorded once, including when optional speaking is skipped.

## 4. Business Model and Marketing Strategy

### 4.1 Marketing Channels

Promotion will prioritise students with upcoming English discussion and presentation tasks. It will explain the app's purpose, practice method and time requirements, and help students start their first session. Outreach will begin with course groups and expand to relevant communities based on the response.

| Channel | Why it fits | Proposed activity |
|---|---|---|
| EMI course groups and English presentation teams | Students have immediate discussion or presentation needs that connect directly to the practice task. | Share an approximately 60-second demonstration and prototype link, with material related to class topics to make the first session easy to start. |
| Campus English communities and learning centres | Reach students who want to practise consistently and improve their expression. | Show a session target, a written revision and optional recordings. Explain both session lengths and the pause-and-resume feature. |
| Technology interest groups and reading clubs | Technology and learning topics provide a natural basis for discussing ideas. | Introduce an excerpt through a discussion question and practice link. Invite students to summarise the material independently and optionally retell it aloud. |
| Referrals from trial participants | Students on similar courses often share the same tasks and practice needs. | Provide a short introduction and prototype link that participants can share with classmates. |

Promotional materials will include a demonstration, a complete worked practice example and an accessible prototype link. Invitations will state that this is a course project, explain the expected time for the core and speaking-inclusive paths, and make clear that recording is optional.

The team will track prototype visits and first-session starts by channel to assess how well each channel reaches the intended users. User experience and improvement suggestions will be collected through the methods in Section 5.

### 4.2 Phased Market Entry

| Phase | Proposed activity | Target or intended result |
|---|---|---|
| Months 0–3: campus launch | Run a 14-day challenge for joint Chinese–international programmes and international colleges, positioning the app as supplementary practice outside class. Recruit through course and campus communities. | Acquire the first 500 seed users with near-zero expected cash advertising expenditure. This is a recruitment target, not existing traction; team time and operating costs still need to be recorded. |
| Months 3–8: focused content marketing | Publish “three-minute academic expression improvement” tutorials on Xiaohongshu and Bilibili. Let learners voluntarily share initial-versus-final writing cards to encourage peer referrals. | Expand acquisition through relevant content, practice check-ins and participant sharing. |
| Months 8–18: institutional and test-preparation expansion | Approach university EAP/ESP teaching teams with a proposed class learning dashboard, and seek test-preparation partners to co-develop IELTS/TOEFL academic micro-practice libraries. | Test institutional adoption and conversion from relevant test-preparation use cases. Partnerships and dashboard delivery remain planned. |

The 14-day challenge is a marketing campaign. The seven-day follow-up and validation sample sizes in Section 5 remain the product-testing plan.

### 4.3 Proposed Free and Pro Offers

The proposed model uses accessible daily practice to acquire learners and a paid academic offer for deeper support in presentation and report tasks.

| Offer | Proposed price and access | Main features |
|---|---|---|
| Free Basic | Free; one standard practice cycle per day, presented as approximately 15 minutes in the commercial pitch. | Basic rule-based writing self-checks and personal content import, supporting a daily practice habit. The core-cycle planning range remains 15–20 minutes. |
| Pro Academic | RMB 19 per month or RMB 69 per semester. | Unlimited practice cycles, deeper AI evaluation of academic logic and rigour, and a dedicated presentation-material library. |

The commercial assumptions are an AI inference cost below RMB 0.03 per cycle and an expected gross margin above 75%. Pricing, inference cost and margin are hypotheses to validate rather than measured operating results. Actual gross margin depends on average practice frequency, AI requests per cycle, free-tier subsidy and other service costs; the per-cycle inference assumption alone does not establish the margin. Unlimited practice must therefore be assessed against observed usage and service costs.

### 4.4 First-Year Operating Targets

| Metric | Proposed target | Measurement basis |
|---|---|---|
| User acquisition | 15,000 users in the first year. | Track acquired users by channel and distinguish them from prototype visits and first-session starts. |
| Following-month retention | At least 35%. | The share of newly acquired users who remain active in the following month. |
| Paid conversion | 4–6%. | The share of newly acquired users who become paying users. |

These are operating targets, not achieved results. The pricing, partnerships and growth assumptions will be examined through pilots and subsequent operating data. They are separate from the usability and return-use acceptance criteria in Section 5.

## 5. App Validation

Validation will focus on material difficulty, use of target expressions and continued practice. Interviews, prototype trials, questionnaires and follow-ups will inform changes to the app. Comparisons with learners' current practice methods will help assess its practical value.

### 5.1 Validation Methods

| Method | Plan | How the findings will be used |
|---|---|---|
| User interviews | Interview eight students who need to join discussions or give presentations in English, focusing on recent real tasks. | Identify understood but hard-to-use language, suitable material difficulty and acceptable session length; adjust materials and target selection. |
| Prototype trials | Invite at least 12 target users to complete the core cycle. Speaking is voluntary. | Record speaking choices and reasons, material selection, support use, time and dropouts. Invite willing participants to test the recording tools. Include learners who import their own media and timestamped text; observe pairing, timing checks and excerpt selection. |
| Post-practice questionnaire and brief interview | Collect views from both completers and participants who stop early. | Check instructions, difficulty, sentence-analysis explanations and writing evaluations. Ask speaking participants separately about recording, playback and self-assessment. |
| Material and feedback review | Check video/audio, subtitles and timing for all six excerpts. Have human reviewers assess AI writing evaluations on 30 fixed written responses and separately review reading analyses of sentences covering main clauses, subordinate clauses, negation and modality. | Check subtitle alignment, key points, sentence-component labels, grammar explanations and writing-evaluation accuracy; correct problems and repeat the checks. |
| Follow-up | Observe voluntary daily use over seven days and ask participants about their routine and reasons for returning or stopping. | Record active practice days and completed cycles; identify friction in maintaining daily practice. |
| Comparison with existing methods | Ask a subset of users to try both Increment and their usual tools. Match difficulty, speaking choice, time allowance and task requirements as closely as possible, and alternate the order. | Compare task time, assistance needed, written performance and preference. Collect speaking self-assessments separately where applicable. |

### 5.2 Validation Measures

| Area | Target or check |
|---|---|
| Material difficulty (`i+1`) | Record comprehension, note and cue use, and perceived difficulty. Adjust material if it remains hard to understand. |
| Active expression (`Δ Ability`) | Compare the target, first draft and revised draft for appropriate use and accurate meaning. Speaking relies on learner self-assessment. Progress is not measured by the number of new words. |
| Repeated practice (`i++`) | Check whether automatic completion records, practice dates and the daily practice entry point help learners maintain regular use. |
| Completion | At least 10 of 12 participants complete the core cycle without team guidance, confirmed by completion of the required steps and one automatically created completion record. No separate save or reflection step is required. Skipping speaking does not affect the count. |
| Session length | At least nine participants finish within the limit for their chosen path: 20 minutes for the core cycle or 25 minutes with speaking. Record the two paths separately. |
| Reading-analysis accuracy | Human reviewers check that sentence components match the original text, grammatical explanations are correct and concise, and negation, modality and conditions retain their meaning. Review built-in and imported text samples separately and correct misleading analyses before retesting. |
| Written feedback accuracy | Human reviewers accept at least 27 of 30 fixed cases, with no critical errors such as fabricated source text, reversed meaning or an accurate paraphrase incorrectly flagged as wrong. |
| Return use | Within seven days, at least six participants voluntarily return on another day and start a full cycle, and at least five complete it. Also report the number of active practice days and completed cycles per participant to describe daily use. |
| Import usability | Observe users importing audio/video and timestamped text, pairing them and starting a cycle. Test supported formats, malformed timestamps, missing text, unsupported codecs and timing-offset adjustments; keep existing practice accessible after an import error. |
| Feature reliability | Check subtitle navigation, draft recovery, and enabling or skipping speaking. Recording failure or denied permission must not block the core cycle. Also test playback, rerecording and behaviour when the LLM is unavailable. Verify that completing a cycle creates exactly one completion record, including after repeated clicks or a page reload. |

Listening and reading checks cover understanding of the main point, reasons and conditions. Writing checks compare first and revised drafts. Speaking participants listen back, compare their recordings with the source or a checklist, and complete a self-assessment. Validation collects only their self-assessments and experience of the tools for this component. Skipping speaking is not treated as non-completion. The system, LLM and project team do not score speaking quality.

For a before-and-after comparison, the team can use two excerpts of similar difficulty, R1 and R2, reserved from practice, three to seven days apart. Their order will be swapped across participants. On each occasion, learners first listen twice without subtitles and save their comprehension responses, then read the transcript and write. Playback speed, transcript-reading time and the number of cues remain consistent. Speaking is voluntary, with the same setting used on both occasions; any change is recorded separately. Related written feedback, extra cues and sample answers are withheld until both assessments are complete. The team records prior familiarity with the material and the support actually used.

For evaluation, same-excerpt revision and performance on new material are analysed separately from daily-use and completion counts. Evaluation responses are collected for the study rather than required as a product save step. Written comparisons examine whether learners can use understood expressions independently in suitable contexts, while accepting alternative wording that preserves the meaning. Speaking continues to rely on learner self-assessment.

### 5.3 Improvement and Retesting

Each trial round will produce an issue log recording where a problem occurred, its impact, the proposed change and the retest result. Lost recordings, missing or duplicate completion records and interrupted workflows take priority, followed by instructions and written feedback, then interface details.

Recurring problems will lead to specific changes. If learners struggle to start speaking, the team will add a preview of the task and optional cues. If listening back is cumbersome, recording controls will be revised. If LLM comments are too long or difficult to follow, explanations will be shortened and the relevant source sentence made more prominent.

After changes, selected users will repeat the affected steps to check whether the problems have been resolved. Each round will retain user feedback, prototype changes and retest records, organised around material difficulty, use of expressions and completion of the practice cycle.
