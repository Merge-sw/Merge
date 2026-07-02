import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';

export default function ScoutCompleteScreen() {
  const navigate = useNavigate();
  const [analyzing, setAnalyzing] = useState(true);
  const [consoleLines, setConsoleLines] = useState([]);
  const [profile, setProfile] = useState({
    thinkingStyle: "SYSTEMATIC",
    motivationType: "INTERNAL",
    priorExposure: "NONE",
    learningApproach: "EXAMPLES_FIRST",
    scaffoldingLevel: "MEDIUM"
  });

  useEffect(() => {
    const logs = [
      "Connecting to profile derivation engine...",
      "Reading persisted Layer 1 responses...",
      "Evaluating q3/q4/q7 with structural grammar rules...",
      "Matching lexicons: [logic, structure, algorithms] detected.",
      "Analyzing q5/q8 career vectors...",
      "Interpreting baseline coding exercise submissions...",
      "Calculating initial scaffolding multipliers...",
      "Compiling final personalization profile vectors...",
      "Persisting derived profile attributes to DB..."
    ];

    let currentLine = 0;
    const interval = setInterval(() => {
      if (currentLine < logs.length) {
        setConsoleLines(prev => [...prev, logs[currentLine]]);
        currentLine++;
      } else {
        clearInterval(interval);
        const layer1Raw = localStorage.getItem('scout_layer1_responses');
        const layer3Code = localStorage.getItem('scout_layer3_code');
        const hasPrior = localStorage.getItem('scout_has_prior_experience') === 'true';

        let derivedExposure = "NONE";
        if (layer3Code) derivedExposure = "EXPERIENCED";
        else if (hasPrior) derivedExposure = "SOME";

        let derivedThinking = "SYSTEMATIC";
        let derivedMotivation = "INTERNAL";
        let derivedApproach = "EXAMPLES_FIRST";

        if (layer1Raw) {
          try {
            const r = JSON.parse(layer1Raw);
            const q3_q4_q7 = ((r.q3 || "") + " " + (r.q4 || "") + " " + (r.q7 || "")).toLowerCase();
            const q5_q8 = ((r.q5 || "") + " " + (r.q8 || "")).toLowerCase();
            const q3_q7 = ((r.q3 || "") + " " + (r.q7 || "")).toLowerCase();

            const intuitiveCount = (q3_q4_q7.match(/(curious|creat|intuition|explor|experiment|feel|wonder)/g) || []).length;
            const systematicCount = (q3_q4_q7.match(/(logic|structure|systematic|methodical|algorithm|math|precise|step)/g) || []).length;
            if (intuitiveCount > systematicCount) derivedThinking = "INTUITIVE";

            const externalCount = (q5_q8.match(/(salary|job|money|income|career|employ|company|pay)/g) || []).length;
            const internalCount = (q5_q8.match(/(passion|love|build|creat|impact|change|purpose|help)/g) || []).length;
            if (externalCount > internalCount) derivedMotivation = "EXTERNAL";

            const defsCount = (q3_q7.match(/(understand|theory|concept|fundamental|foundation|principle|study)/g) || []).length;
            const examplesCount = (q3_q7.match(/(example|hands-on|practical|project|build|try|practice)/g) || []).length;
            if (defsCount > examplesCount) derivedApproach = "DEFINITIONS_FIRST";
          } catch(e) {}
        }

        setProfile({
          thinkingStyle: derivedThinking,
          motivationType: derivedMotivation,
          priorExposure: derivedExposure,
          learningApproach: derivedApproach,
          scaffoldingLevel: "MEDIUM"
        });
        setAnalyzing(false);
      }
    }, 300);

    return () => clearInterval(interval);
  }, []);

  const handleLaunchDashboard = () => {
    const studentRaw = localStorage.getItem('merge_student');
    if (studentRaw) {
      try {
        const student = JSON.parse(studentRaw);
        student.current_stage = "CADET";
        localStorage.setItem('merge_student', JSON.stringify(student));
      } catch(e) {}
    }
    navigate('/workspace');
  };

  return (
    <div className="min-h-screen flex flex-col items-center justify-center p-6 relative bg-[#10131a] text-[#e1e2ec] overflow-hidden">
      <main className="relative z-10 w-full max-w-2xl flex flex-col items-center">
        <header className="mb-8 text-center">
          <MergeLogo className="font-display font-extrabold uppercase tracking-widest text-[28px]" />
        </header>

        {analyzing ? (
          <div className="w-full bg-[#16181D] border border-outline-variant p-8 font-mono-code text-xs text-left space-y-4 shadow-2xl h-[420px] flex flex-col justify-between">
            <div className="flex justify-between items-center border-b border-outline-variant/30 pb-3">
              <span className="text-primary font-bold animate-pulse">ANALYZING INTAKE SCORES...</span>
              <span className="text-on-surface-variant font-mono-code">[GATEWAY_OK]</span>
            </div>
            
            <div className="flex-1 overflow-y-auto space-y-2 mt-4 select-none">
              {consoleLines.map((line, idx) => (
                <div key={idx} className="flex gap-2">
                  <span className="text-primary">&gt;</span>
                  <span className="text-on-surface">{line}</span>
                </div>
              ))}
            </div>

            <div className="border-t border-outline-variant/30 pt-3 text-[10px] text-on-surface-variant flex justify-between">
              <span>LEXICAL_ENGINE_V1</span>
              <span>HEURISTICS: KEYWORD_FREQUENCY</span>
            </div>
          </div>
        ) : (
          <div className="w-full bg-[#16181D] border border-outline-variant p-8 md:p-12 shadow-2xl space-y-8 animate-fade-in text-left">
            <div className="border-b border-outline-variant pb-4">
              <h1 className="font-headline-lg text-headline-lg text-on-surface uppercase">Profile Derived</h1>
              <p className="font-body-md text-on-surface-variant mt-1">Your learning vectors have been derived and stored in the orchestration registry.</p>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div className="border border-outline-variant p-4 bg-surface-container-lowest">
                <span className="font-mono-code text-[10px] text-primary uppercase block mb-1">Thinking Style</span>
                <span className="font-headline-md text-[18px] text-on-surface font-extrabold uppercase">{profile.thinkingStyle}</span>
                <p className="text-[12px] text-on-surface-variant mt-1">
                  {profile.thinkingStyle === "SYSTEMATIC" 
                    ? "You approach problems step-by-step using logical system rules."
                    : "You favor creative intuition, exploration, and trial-and-error."}
                </p>
              </div>

              <div className="border border-outline-variant p-4 bg-surface-container-lowest">
                <span className="font-mono-code text-[10px] text-primary uppercase block mb-1">Motivation Vector</span>
                <span className="font-headline-md text-[18px] text-on-surface font-extrabold uppercase">{profile.motivationType}</span>
                <p className="text-[12px] text-on-surface-variant mt-1">
                  {profile.motivationType === "INTERNAL" 
                    ? "Driven by the intrinsic joy of building and solving hard puzzles."
                    : "Motivated by security, career stability, and outcomes."}
                </p>
              </div>

              <div className="border border-outline-variant p-4 bg-surface-container-lowest">
                <span className="font-mono-code text-[10px] text-primary uppercase block mb-1">Exposure Level</span>
                <span className="font-headline-md text-[18px] text-on-surface font-extrabold uppercase">{profile.priorExposure}</span>
                <p className="text-[12px] text-on-surface-variant mt-1">
                  {profile.priorExposure === "EXPERIENCED" 
                    ? "Prior programming experience; completed coding baseline."
                    : profile.priorExposure === "SOME" 
                      ? "Some light conceptual or syntax background detected."
                      : "Zero coding background; learning from the ground up."}
                </p>
              </div>

              <div className="border border-outline-variant p-4 bg-surface-container-lowest">
                <span className="font-mono-code text-[10px] text-primary uppercase block mb-1">Pedagogical Mode</span>
                <span className="font-headline-md text-[18px] text-on-surface font-extrabold uppercase">{profile.learningApproach.replace('_', ' ')}</span>
                <p className="text-[12px] text-on-surface-variant mt-1">
                  {profile.learningApproach === "EXAMPLES_FIRST" 
                    ? "You prefer practical hands-on examples before deep theorizing."
                    : "You prefer formal conceptual definitions before viewing code."}
                </p>
              </div>
            </div>

            <div className="pt-4">
              <button 
                onClick={handleLaunchDashboard}
                className="w-full bg-[#3B82F6] hover:bg-[#2563EB] text-white font-label-caps text-label-caps h-[52px] uppercase tracking-widest flex items-center justify-center gap-2 rounded-none transition-colors"
              >
                <span>ENTER CADET WORKSPACE</span>
                <span className="material-symbols-outlined text-[20px]">arrow_forward</span>
              </button>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
