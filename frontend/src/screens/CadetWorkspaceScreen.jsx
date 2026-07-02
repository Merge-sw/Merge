import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';

export default function CadetWorkspaceScreen() {
  const navigate = useNavigate();
  
  const [workspaceState, setWorkspaceState] = useState("explanation");
  const [gateAnswer, setGateAnswer] = useState("");
  const [selectedLanguage, setSelectedLanguage] = useState("javascript");
  
  const [drill1Code, setDrill1Code] = useState(
`function checkCircuitState(failures, threshold) {
  // Write implementation here
  return "";
}`
  );
  const [drill1Console, setDrill1Console] = useState([]);
  const [drill1Passed, setDrill1Passed] = useState(false);

  const [drill2CodeReadingAnswer, setDrill2CodeReadingAnswer] = useState("");
  const [drill2ReadingUnlocked, setDrill2ReadingUnlocked] = useState(false);
  const [drill2Code, setDrill2Code] = useState(
`function isHalfOpen(lastFailureTime, resetTimeoutSec) {
  // Write implementation here
  return false;
}`
  );
  const [drill2Console, setDrill2Console] = useState([]);
  const [drill2Passed, setDrill2Passed] = useState(false);

  const [showExitModal, setShowExitModal] = useState(false);

  const handleExitTrigger = () => {
    if (workspaceState === "drill1" || workspaceState === "drill2") {
      setShowExitModal(true);
    } else {
      navigate('/dashboard');
    }
  };

  const confirmDiscardAndExit = () => {
    setShowExitModal(false);
    navigate('/dashboard');
  };

  const runDrill1Tests = () => {
    setDrill1Console(["Running test suite for Drill 1...", "Evaluating test cases..."]);
    setTimeout(() => {
      setDrill1Console(prev => [
        ...prev,
        "Testing failures=5, threshold=3 -> EXPECTED: OPEN, GOT: OPEN. [PASS]",
        "Testing failures=2, threshold=4 -> EXPECTED: CLOSED, GOT: CLOSED. [PASS]",
        "All tests passed successfully. Ready for submission."
      ]);
      setDrill1Passed(true);
    }, 1000);
  };

  const submitDrill1 = () => {
    setWorkspaceState("drill2");
  };

  const submitCodeReadingGate = (e) => {
    e.preventDefault();
    if (drill2CodeReadingAnswer.trim().length < 5) return;
    setDrill2ReadingUnlocked(true);
  };

  const runDrill2Tests = () => {
    setDrill2Console(["Running test suite for Drill 2...", "Evaluating execution intervals..."]);
    setTimeout(() => {
      setDrill2Console(prev => [
        ...prev,
        "Testing lastFailure=Date.now()-6000, timeout=5 -> EXPECTED: true, GOT: true. [PASS]",
        "Testing lastFailure=Date.now()-2000, timeout=5 -> EXPECTED: false, GOT: false. [PASS]",
        "Comprehension test vectors passed. Ready to close Concept."
      ]);
      setDrill2Passed(true);
    }, 1000);
  };

  const submitDrill2 = () => {
    const studentRaw = localStorage.getItem('merge_student');
    if (studentRaw) {
      try {
        const student = JSON.parse(studentRaw);
        student.total_xp = 1350;
        localStorage.setItem('merge_student', JSON.stringify(student));
      } catch(e) {}
    }
    setWorkspaceState("xp_screen");
  };

  return (
    <div className="min-h-screen flex flex-col bg-[#10131a] text-[#e1e2ec] relative select-text">
      <header className="h-[48px] bg-[#16181D] border-b border-outline-variant flex justify-between items-center px-margin-md z-40">
        <div className="flex items-center gap-4">
          <button 
            onClick={handleExitTrigger}
            className="flex items-center gap-1 hover:text-primary transition-colors text-on-surface-variant font-label-caps text-label-caps"
          >
            <span className="material-symbols-outlined text-[16px]">arrow_back</span>
            <span>Exit Workspace</span>
          </button>
        </div>
        <div className="flex items-center gap-4">
          <span className="px-2 py-0.5 border border-primary/40 bg-primary/5 text-primary font-mono-code text-[10px] uppercase">
            STAGE: CADET
          </span>
          <button onClick={() => navigate('/dashboard')} className="hover:text-primary transition-colors font-label-caps text-label-caps uppercase">
            Overview Dashboard
          </button>
        </div>
      </header>

      {workspaceState === "explanation" && (
        <main className="flex-1 max-w-3xl mx-auto w-full px-6 py-12 flex flex-col justify-start">
          <div className="mb-8 border-b border-outline-variant pb-4 text-left">
            <span className="font-mono-code text-[10px] text-primary uppercase block">Concept Explanation</span>
            <h1 className="font-headline-lg text-headline-lg text-on-surface uppercase">Circuit Breaker Mechanism</h1>
          </div>

          <article className="text-left font-body-lg text-body-lg text-on-surface-variant space-y-6 leading-relaxed mb-12">
            <p>
              In distributed systems, microservices make remote calls that can fail due to network timeouts, slow databases, or service crashes. If a calling service continuously threads connections to a failing remote node, it will exhaust its thread pool, causing cascading failures across the entire system.
            </p>
            <p>
              The **Circuit Breaker** pattern solves this. By wrapping dangerous remote operations, it intercepts calls and tracks failure rates. The breaker operates as a state machine with three core states:
            </p>
            <ul className="list-disc pl-6 space-y-2 font-body-md text-body-md">
              <li>
                <strong className="text-on-surface">CLOSED:</strong> Operations execute normally. If consecutive failures cross a configured threshold, the breaker transitions to the **OPEN** state.
              </li>
              <li>
                <strong className="text-on-surface">OPEN:</strong> Remote operations are blocked. Calls fail fast immediately with a local fallback response, sparing the network and preventing remote node overload.
              </li>
              <li>
                <strong className="text-on-surface">HALF-OPEN:</strong> After a cooldown reset timeout, the breaker tries a small number of test requests. If any test request fails, it swings back to **OPEN**. If all test calls succeed, the breaker returns to **CLOSED**.
              </li>
            </ul>
            <p>
              By decoupling system operations from failing components, circuit breakers convert system crashes into clean fallback pathways, assuring service degradation is graceful rather than catastrophic.
            </p>
          </article>

          <div className="border-t border-outline-variant pt-8 text-left space-y-4 mb-16">
            <div className="space-y-1">
              <span className="font-mono-code text-[10px] text-primary uppercase block">Mandatory Compression Gate</span>
              <h3 className="font-headline-md text-on-surface uppercase">Articulate Concept</h3>
              <p className="text-xs text-on-surface-variant leading-relaxed">
                David, explain this mechanism in your own words before unlocking the workspace drills.
              </p>
            </div>

            <form 
              onSubmit={(e) => {
                e.preventDefault();
                if (gateAnswer.trim().length > 5) {
                  setWorkspaceState("drill1");
                }
              }}
              className="space-y-4"
            >
              <textarea
                required
                value={gateAnswer}
                onChange={e => setGateAnswer(e.target.value)}
                placeholder="In one sentence, what does this concept actually do?"
                rows={3}
                className="w-full bg-[#09090B] border border-outline-variant p-4 font-mono-code text-mono-code focus:border-primary outline-none transition-colors rounded-none resize-none"
              />
              <button 
                type="submit"
                className="bg-[#3B82F6] hover:bg-[#2563EB] text-white px-6 py-3 font-label-caps text-label-caps uppercase tracking-wider rounded-none transition-colors"
              >
                SUBMIT COMPRESSION GATE & START DRILLS
              </button>
            </form>
          </div>
        </main>
      )}

      {workspaceState === "drill1" && (
        <main className="flex-grow grid grid-cols-12 overflow-hidden h-[calc(100vh-48px)]">
          <section className="col-span-5 border-r border-[#27272A] p-6 flex flex-col gap-6 text-left overflow-y-auto bg-surface-container-lowest">
            <div>
              <span className="font-mono-code text-[10px] text-primary uppercase tracking-widest block mb-1">DRILL 1 / CLOSED STATE THRESHOLD</span>
              <h2 className="font-headline-lg text-headline-lg text-on-surface uppercase">Closed to Open Transition</h2>
            </div>

            <div className="space-y-4 font-body-md text-body-md text-on-surface-variant leading-relaxed">
              <p>
                Implement a basic state change controller inside the circuit breaker. 
              </p>
              <p>
                Your function `checkCircuitState(failures, threshold)` accepts two integers. If `failures` is strictly greater than `threshold`, return string `'OPEN'`. Otherwise, return string `'CLOSED'`.
              </p>
            </div>

            <div className="space-y-4 border-t border-outline-variant/30 pt-4">
              <div className="space-y-1">
                <span className="font-label-caps text-[10px] text-on-surface-variant uppercase">Example Parameters</span>
                <pre className="font-mono-code text-xs bg-[#09090B] border border-outline-variant/20 p-2 text-primary">checkCircuitState(5, 3) =&gt; "OPEN"</pre>
              </div>
            </div>
          </section>

          <section className="col-span-7 flex flex-col overflow-hidden">
            <div className="flex justify-between items-center border-b border-[#27272A] px-4 py-2 bg-surface-container">
              <span className="font-mono-code text-[11px] text-on-surface-variant uppercase">workspace.src</span>
              <span className="font-mono-code text-[11px] text-primary uppercase">[DRILL_1_ACTIVE]</span>
            </div>

            <textarea
              value={drill1Code}
              onChange={e => setDrill1Code(e.target.value)}
              className="w-full flex-1 bg-[#09090B] font-mono-code text-xs text-[#a78bfa] p-4 outline-none border-none resize-none leading-relaxed select-all"
            />

            <div className="h-32 border-t border-[#27272A] bg-[#0b0e15] font-mono-code text-[11px] p-3 text-left overflow-y-auto space-y-1 select-none">
              {drill1Console.map((line, idx) => (
                <div key={idx} className="flex gap-2">
                  <span className="text-primary">&gt;</span>
                  <span className="text-on-surface">{line}</span>
                </div>
              ))}
            </div>

            <div className="border-t border-[#27272A] p-4 bg-surface-container flex justify-between">
              <button 
                onClick={runDrill1Tests}
                className="border border-[#27272A] hover:bg-surface-container-high text-on-surface px-6 py-3 font-label-caps text-label-caps uppercase tracking-wider transition-colors"
              >
                Run Test Suite
              </button>
              <button 
                disabled={!drill1Passed}
                onClick={submitDrill1}
                className="bg-[#3B82F6] hover:bg-[#2563EB] disabled:opacity-50 disabled:cursor-not-allowed text-white px-6 py-3 font-label-caps text-label-caps uppercase tracking-wider transition-colors"
              >
                Submit Closed Check
              </button>
            </div>
          </section>
        </main>
      )}

      {workspaceState === "drill2" && (
        <main className="flex-grow grid grid-cols-12 overflow-hidden h-[calc(100vh-48px)]">
          <section className="col-span-5 border-r border-[#27272A] p-6 flex flex-col gap-6 text-left overflow-y-auto bg-surface-container-lowest">
            <div>
              <span className="font-mono-code text-[10px] text-primary uppercase tracking-widest block mb-1">DRILL 2 / HALFPEN COOLDOWN CHECK</span>
              <h2 className="font-headline-lg text-headline-lg text-on-surface uppercase">Reset Timeout Verification</h2>
            </div>

            <div className="space-y-4 font-body-md text-body-md text-on-surface-variant leading-relaxed">
              <p>
                To avoid testing too early, the circuit breaker compares current time against the last failure time + timeout.
              </p>
            </div>

            <div className="border-t border-outline-variant/30 pt-4 space-y-4">
              <span className="font-mono-code text-[10px] text-primary uppercase block">Gate: Code Reading Review</span>
              <pre className="font-mono-code text-[11px] bg-[#09090B] border border-outline-variant/20 p-3 text-[#a78bfa] leading-relaxed">
{`function isHalfOpen(lastFailureTime, resetTimeoutSec) {
  return (Date.now() - lastFailureTime) > (resetTimeoutSec * 1000);
}`}
              </pre>

              {drill2ReadingUnlocked ? (
                <div className="p-3 bg-green-950/20 border border-green-500 text-green-400 font-mono-code text-[11px] uppercase flex items-center gap-2">
                  <span className="material-symbols-outlined text-[16px]">check_circle</span>
                  <span>Code reading accepted. Editor Unlocked.</span>
                </div>
              ) : (
                <form onSubmit={submitCodeReadingGate} className="space-y-3">
                  <label className="block text-xs text-on-surface-variant">
                    Explain: what does the comparison check `(Date.now() - lastFailureTime) &gt; (resetTimeoutSec * 1000)` verify?
                  </label>
                  <input
                    required
                    type="text"
                    value={drill2CodeReadingAnswer}
                    onChange={e => setDrill2CodeReadingAnswer(e.target.value)}
                    placeholder="Enter answer..."
                    className="w-full bg-[#09090B] border border-[#27272a] focus:border-primary p-2.5 font-mono-code text-xs outline-none rounded-none"
                  />
                  <button 
                    type="submit"
                    className="w-full bg-primary text-on-primary font-label-caps text-label-caps py-2.5 uppercase tracking-wider rounded-none transition-colors"
                  >
                    SUBMIT CODE READING GATE
                  </button>
                </form>
              )}
            </div>
          </section>

          <section className="col-span-7 flex flex-col overflow-hidden relative">
            <div className="flex justify-between items-center border-b border-[#27272A] px-4 py-2 bg-surface-container">
              <span className="font-mono-code text-[11px] text-on-surface-variant uppercase">workspace.src</span>
              <span className="font-mono-code text-[11px] text-primary uppercase">[DRILL_2_ACTIVE]</span>
            </div>

            <div className="flex-1 flex flex-col relative overflow-hidden">
              <textarea
                disabled={!drill2ReadingUnlocked}
                value={drill2Code}
                onChange={e => setDrill2Code(e.target.value)}
                className={`w-full flex-1 bg-[#09090B] font-mono-code text-xs text-[#a78bfa] p-4 outline-none border-none resize-none leading-relaxed select-all ${
                  !drill2ReadingUnlocked ? "opacity-35 select-none pointer-events-none filter blur-[2px]" : ""
                }`}
              />

              {!drill2ReadingUnlocked && (
                <div className="absolute inset-0 bg-black/60 flex items-center justify-center pointer-events-none select-none z-10">
                  <div className="border border-outline-variant p-6 bg-[#16181D] max-w-xs text-center space-y-2">
                    <span className="material-symbols-outlined text-primary text-[28px]">lock</span>
                    <h4 className="font-headline-md text-on-surface uppercase text-sm">Editor Locked</h4>
                    <p className="text-[11px] text-on-surface-variant">David, complete the Code Reading question on the left panel to unlock editing privileges.</p>
                  </div>
                </div>
              )}
            </div>

            <div className="h-32 border-t border-[#27272A] bg-[#0b0e15] font-mono-code text-[11px] p-3 text-left overflow-y-auto space-y-1 select-none">
              {drill2Console.map((line, idx) => (
                <div key={idx} className="flex gap-2">
                  <span className="text-primary">&gt;</span>
                  <span className="text-on-surface">{line}</span>
                </div>
              ))}
            </div>

            <div className="border-t border-[#27272A] p-4 bg-surface-container flex justify-between">
              <button 
                disabled={!drill2ReadingUnlocked}
                onClick={runDrill2Tests}
                className="border border-[#27272A] hover:bg-surface-container-high disabled:opacity-50 disabled:cursor-not-allowed text-on-surface px-6 py-3 font-label-caps text-label-caps uppercase tracking-wider transition-colors"
              >
                Run Test Suite
              </button>
              <button 
                disabled={!drill2Passed}
                onClick={submitDrill2}
                className="bg-[#3B82F6] hover:bg-[#2563EB] disabled:opacity-50 disabled:cursor-not-allowed text-white px-6 py-3 font-label-caps text-label-caps uppercase tracking-wider transition-colors"
              >
                Submit Timeout Check
              </button>
            </div>
          </section>
        </main>
      )}

      {workspaceState === "xp_screen" && (
        <main className="fixed inset-0 bg-[#0D0F12] flex items-center justify-center p-6 z-50 animate-fade-in select-none">
          <div className="max-w-md w-full border border-outline-variant bg-[#16181D] p-12 text-center space-y-8">
            <div className="space-y-2">
              <span className="font-mono-code text-[10px] text-primary uppercase tracking-widest block">Formation Milestone</span>
              <h1 className="font-display text-[72px] font-extrabold text-[#3B82F6] leading-none tracking-tight">+100 XP</h1>
              <span className="font-label-caps text-label-caps text-on-surface-variant block">AWARDED TO PROFILE</span>
            </div>

            <div className="space-y-2">
              <div className="flex justify-between font-mono-code text-xs text-on-surface-variant">
                <span>STAGE PROGRESSION</span>
                <span>1,350 / 1,500 XP</span>
              </div>
              <div className="h-1.5 w-full bg-surface-container-highest border border-outline-variant/35">
                <div className="h-full bg-[#3B82F6]" style={{ width: '90%' }}></div>
              </div>
              <span className="font-mono-code text-[10px] text-on-surface-variant block uppercase text-left pt-1">
                150 XP remaining to stage Scout I
              </span>
            </div>

            <div className="pt-4 flex flex-col gap-2">
              <button 
                onClick={() => navigate('/drill-result/101')}
                className="w-full bg-gradient-to-r from-blue-700 to-blue-500 hover:from-blue-600 hover:to-blue-400 text-white py-4 font-label-caps text-label-caps uppercase tracking-widest transition-colors rounded-none border-none"
              >
                View Clean Code Review
              </button>
              <button 
                onClick={() => navigate('/dashboard')}
                className="w-full border border-outline-variant hover:bg-surface-container-high text-on-surface-variant py-3 font-label-caps text-[10px] uppercase tracking-wider rounded-none transition-colors"
              >
                Return to Dashboard
              </button>
            </div>

          </div>
        </main>
      )}

      {showExitModal && (
        <div className="fixed inset-0 bg-black/75 flex items-center justify-center z-50 p-6 select-none animate-fade-in">
          <div className="bg-[#16181D] border border-error max-w-sm w-full p-8 text-center space-y-6 shadow-2xl">
            <div className="flex flex-col items-center space-y-2 text-error">
              <span className="material-symbols-outlined text-[32px]">warning</span>
              <h3 className="font-headline-md text-on-surface uppercase text-sm">Abort Active Workspace</h3>
            </div>
            
            <p className="text-xs text-on-surface-variant leading-relaxed">
              Exiting now will discard your current code changes and clear the active terminal state. Confirm abort action?
            </p>

            <div className="flex flex-col gap-2 pt-2">
              <button 
                onClick={() => setShowExitModal(false)}
                className="w-full bg-[#3B82F6] hover:bg-[#2563EB] text-white py-3 font-label-caps text-label-caps uppercase tracking-wider rounded-none"
              >
                Cancel / Return to Code
              </button>
              <button 
                onClick={confirmDiscardAndExit}
                className="w-full border border-[#27272A] hover:bg-surface-container-high text-on-surface-variant py-2.5 font-label-caps text-[10px] uppercase tracking-wider rounded-none"
              >
                Discard Work and Exit
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
