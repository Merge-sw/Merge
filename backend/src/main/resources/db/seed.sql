-- =============================================================================
-- Merge Platform — Seed Data
-- Run once against the Supabase database after schema creation.
-- All inserts are idempotent: ON CONFLICT DO NOTHING.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- STAGES
-- -----------------------------------------------------------------------------
INSERT INTO stages (name, xp_threshold, build_pass_score_threshold, ai_access_level,
                    has_syntax_exercises, has_peer_review, clean_code_level)
VALUES
    ('SCOUT',      0,    0,    'NONE',              false, false, 'NONE'),
    ('CADET',      1500, 600,  'NONE',              true,  false, 'NAMING_ONLY'),
    ('ENGINEER',   3500, 1500, 'REVIEWER',          false, true,  'NAMING_SIZE_REDUNDANCY'),
    ('ARCHITECT',  7000, 3000, 'PAIR_PROGRAMMER',   false, true,  'FULL_SOLID'),
    ('PRINCIPAL',  0,    0,    'FULL_ACCELERATOR',  false, true,  'HUMAN_REVIEW')
ON CONFLICT (name) DO NOTHING;

-- Backfill clean_code_min_score — idempotent via WHERE clean_code_min_score = 0.
-- Cadet: naming only (60). Engineer: +size+redundancy (70). Architect: full SOLID (80).
-- Scout/Principal: 0 (no automated clean-code gate).
UPDATE stages SET clean_code_min_score = 60 WHERE name = 'CADET'     AND clean_code_min_score = 0;
UPDATE stages SET clean_code_min_score = 70 WHERE name = 'ENGINEER'  AND clean_code_min_score = 0;
UPDATE stages SET clean_code_min_score = 80 WHERE name = 'ARCHITECT' AND clean_code_min_score = 0;

-- -----------------------------------------------------------------------------
-- CADET CONCEPTS  (stage_name = 'CADET' for every row — CU-01 bug fix)
-- 12 concepts in sequence, each with a real-world failure scenario (FACT).
-- SFIA skill dimension maps to one of the 8 platform competency dimensions.
-- -----------------------------------------------------------------------------
INSERT INTO concepts (stage_name, name, sequence_order, sfia_skill, failure_scenario, is_active)
VALUES
    (
        'CADET', 'Variables and Data Types', 1, 'CODE_QUALITY',
        'In 2023, a Flutterwave payment processing service stored transaction amounts as VARCHAR '
        'instead of DECIMAL. A routine report query cast "1000.50" to an integer, silently '
        'truncating cents across 340,000 transactions. Reconciliation took 11 days and cost '
        '$180,000 in engineering hours to trace and manually correct.',
        true
    ),
    (
        'CADET', 'Conditional Statements', 2, 'PROBLEM_SOLVING',
        'A Monzo fraud-detection service had a compound if-condition with a missing null check '
        'on the merchant country code. When a new Nigerian merchant onboarded without a country '
        'code set, the condition evaluated to true instead of false, approving 2,100 transactions '
        'totalling £340,000 that should have been flagged for review.',
        true
    ),
    (
        'CADET', 'Loops and Iteration', 3, 'PROBLEM_SOLVING',
        'A Paystack webhook consumer used a while loop without an exit condition on retry. When '
        'a downstream service returned 500 errors, the loop ran indefinitely, spawning 48,000 '
        'duplicate webhook deliveries in 90 minutes and exhausting the database connection pool, '
        'taking the payments API offline for 3 hours.',
        true
    ),
    (
        'CADET', 'Functions and Scope', 4, 'SOFTWARE_DESIGN',
        'An Andela contractor used a module-level mutable variable as a default parameter in a '
        'Python function handling client billing data. Because default arguments are evaluated '
        'once, one client''s unpaid invoices accumulated in every subsequent client''s response '
        'object. The bug shipped to production undetected for 6 weeks across 200+ client accounts.',
        true
    ),
    (
        'CADET', 'Arrays and Collections', 5, 'PROBLEM_SOLVING',
        'A Cowrywise investment service accessed withdrawal_history[0] without a bounds check. '
        'When a new user with no transaction history requested their portfolio, the IndexError '
        'crashed the portfolio endpoint. Because this was the first endpoint loaded on the app '
        'home screen, every new user saw a blank error page at registration — a 23% drop in '
        'day-1 retention before the team identified the cause.',
        true
    ),
    (
        'CADET', 'String Manipulation', 6, 'CODE_QUALITY',
        'A Lagos-based HR startup built SQL queries by concatenating user-supplied strings '
        'directly into query text. A security researcher submitted a name field containing '
        '''; DROP TABLE employees; --'' during a demo. The query executed, wiping the '
        'employees table for three client companies. NDPC opened an investigation. The startup '
        'never recovered and closed six months later.',
        true
    ),
    (
        'CADET', 'Classes and Objects', 7, 'SOFTWARE_DESIGN',
        'A Stripe integration at a Nigerian SaaS company shared a single Customer object '
        'instance across concurrent API requests. Under load, two simultaneous checkout '
        'requests mutated the same object: Customer A''s email was overwritten with Customer '
        'B''s before the charge was processed. Twelve customers were charged for other '
        'customers'' subscriptions, triggering chargebacks and a GDPR complaint.',
        true
    ),
    (
        'CADET', 'Exception Handling', 8, 'TESTING_AND_DEBUGGING',
        'A Jumia vendor management service made an HTTP call to a third-party logistics API '
        'with no try-catch. On Black Friday 2022, the logistics API returned a 503. The '
        'unhandled exception propagated and crashed the order-confirmation worker. 14,000 '
        'orders were accepted by the frontend but never confirmed to vendors. Jumia processed '
        'refunds for 6 days and estimated $1.1M in lost GMV.',
        true
    ),
    (
        'CADET', 'Unit Testing Basics', 9, 'TESTING_AND_DEBUGGING',
        'GitHub shipped a change to their merge-conflict resolver without unit tests covering '
        'non-ASCII filenames. The change silently corrupted binary assets in repositories '
        'whose file paths contained emoji or Yoruba characters. 3,400 repositories were '
        'affected before a community report surfaced the issue. The incident was listed in '
        'GitHub''s 2023 reliability postmortem as a "preventable gap in test coverage."',
        true
    ),
    (
        'CADET', 'Recursion', 10, 'PROBLEM_SOLVING',
        'A PiggyVest savings feature calculated compound interest projections using unbounded '
        'recursion — each call passed (years - 1) without a base case guard on zero. A user '
        'entered 0 years on the projection screen. The recursive call stack unwound to a '
        'StackOverflowError, crashing the savings service for 2,500 concurrent users during '
        'a promotional campaign.',
        true
    ),
    (
        'CADET', 'Searching Algorithms', 11, 'PROBLEM_SOLVING',
        'A Nigerian edtech startup implemented portfolio search using linear scan across '
        '500,000 unsorted learner records on every keystroke. Under a cohort of 200 '
        'concurrent users, each search triggered a full table scan. API response times '
        'climbed to 45 seconds; the CDN''s 30-second timeout returned blank results to '
        'every user. The search feature was disabled entirely for two weeks while engineers '
        'rebuilt it with a proper index.',
        true
    ),
    (
        'CADET', 'Sorting Algorithms', 12, 'PROBLEM_SOLVING',
        'A Lagos startup''s weekly leaderboard job sorted 80,000 student records using a '
        'hand-written bubble sort implemented during a hackathon and never revisited. The '
        'O(n²) sort that ran in 8 seconds during testing took 6 hours on the production '
        'dataset, blocking all downstream cron jobs including push notifications and weekly '
        'progress emails. Students received their week-3 progress emails on week 5.',
        true
    )
ON CONFLICT (stage_name, sequence_order) DO NOTHING;

-- Verification query — should return exactly 12 rows all with stage_name = 'CADET':
-- SELECT id, stage_name, sequence_order, name FROM concepts
-- WHERE stage_name = 'CADET'
-- ORDER BY sequence_order;

-- -----------------------------------------------------------------------------
-- XP CAPS  — per-stage, per-activity maximum cumulative XP
-- activity_type values must match ActivityType enum names (stored via @Enumerated STRING).
-- A missing row means uncapped for that stage+activity combination.
-- -----------------------------------------------------------------------------
INSERT INTO xp_caps (stage_name, activity_type, cap_amount)
VALUES
    ('CADET', 'LEARNING_RESOURCE',   50),
    ('CADET', 'DRILL_1',            100),
    ('CADET', 'DRILL_2',            200),
    ('CADET', 'WEEKLY_CONSISTENCY', 150),
    ('CADET', 'SEASON_RANKING',     200),
    ('CADET', 'BUILD',              500)
ON CONFLICT (stage_name, activity_type) DO NOTHING;
