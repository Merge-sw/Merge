import React, { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';

export default function BuildComprehensionScreen() {
  const navigate = useNavigate();
  const { id } = useParams();
  
  // Screen views: "loading" | "timed_questions" | "evaluation_results" | "error"
  const [viewMode, setViewMode] = useState("loading");
  const [errorMsg, setErrorMsg] = useState("");
  const [timerExpired, setTimerExpired] = useState(false);
  
  // Data states
  const [checkData, setCheckData] = useState(null);
  const [answers, setAnswers] = useState([]);
  const [submitting, setSubmitting] = useState(false);
  const [resultsData, setResultsData] = useState(null);
  
  // Timer states
  const [secondsRemaining, setSecondsRemaining] = useState(60);
  const timerIntervalRef = useRef(null);

  // Load check details on mount
  useEffect(() => {
    fetchComprehensionCheck();
    return () => {
      if (timerIntervalRef.current) clearInterval(timerIntervalRef.current);
    };
  }, [id]);

  const fetchComprehensionCheck = async () => {
    const token = localStorage.getItem('merge_jwt');
    if (!token) {
      loadMockCheck("No auth token. Running in offline preview mode.");
      return;
    }

    try {
      setViewMode("loading");
      const res = await fetch(`/api/v1/build-comprehension/${id}`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (res.status === 200) {
        const data = await res.json();
        setCheckData(data);
        setAnswers(new Array(data.questions.length).fill(""));
        
        if (data.status === "COMPLETED") {
          // If already completed, show mock/cached results or fetch submission details
          setErrorMsg("This comprehension check has already been completed.");
          setViewMode("error");
        } else {
          setViewMode("timed_questions");
          startTimer(data.serverDeadline);
        }
      } else if (res.status === 403) {
        setErrorMsg("Access Restrained. You are not authorized to view this build comprehension check.");
        setViewMode("error");
      } else {
        throw new Error(`API returned status: ${res.status}`);
      }
    } catch (e) {
      console.warn("Failed to fetch check. Running mock preview.", e);
      loadMockCheck();
    }
  };

  const startTimer = (deadlineIso) => {
    if (timerIntervalRef.current) clearInterval(timerIntervalRef.current);
    
    const deadline = new Date(deadlineIso).getTime();
    
    const updateTimer = () => {
      const now = Date.now();
      const remaining = Math.max(0, Math.floor((deadline - now) / 1000));
      setSecondsRemaining(remaining);
      
      if (remaining <= 0) {
        clearInterval(timerIntervalRef.current);
        setTimerExpired(true);
        // Auto-submit when timer expires
        autoSubmitAnswers();
      }
    };
    
    updateTimer();
    timerIntervalRef.current = setInterval(updateTimer, 1000);
  };

  const loadMockCheck = (warning = "") => {
    const mockCheck = {
      id: parseInt(id) || 42,
      buildSubmissionId: 101,
      questions: [
        "Explain why you chose to use an index pointer in LoadBalancer.selectServer() instead of shifting elements.",
        "How does your implementation handle exceptions when the PingService ping call throws a TimeoutException?",
        "Under what scenario would your health checker fail to transition a server to OFFLINE?"
      ],
      triggeredAt: new Date().toISOString(),
      serverDeadline: new Date(Date.now() + 60 * 1000).toISOString(),
      status: "PENDING"
    };

    setCheckData(mockCheck);
    setAnswers(new Array(mockCheck.questions.length).fill(""));
    setViewMode("timed_questions");
    startTimer(mockCheck.serverDeadline);
    if (warning) {
      console.log(warning);
    }
  };

  const handleAnswerChange = (index, value) => {
    const updated = [...answers];
    updated[index] = value;
    setAnswers(updated);
  };

  const handleSubmit = async (e) => {
    if (e) e.preventDefault();
    if (submitting) return;

    if (timerIntervalRef.current) clearInterval(timerIntervalRef.current);
    setSubmitting(true);

    const token = localStorage.getItem('merge_jwt');
    const payload = { answers };

    if (!token) {
      // Simulate results evaluation in preview mode
      setTimeout(() => {
        setSubmitting(false);
        const mockResponse = {
          comprehensionPassed: true,
          xpAwarded: 300,
          tier: "STANDARD",
          gates: [
            { gate: "JUDGE0", status: "PASSED", feedback: "All 18 hidden test suites passed successfully." },
            { gate: "ARCHITECTURE", status: "PASSED", feedback: "Architecture document maps to round-robin constraints." },
            { gate: "CLEAN_CODE", status: "PASSED", feedback: "Clean code minimum score reached: 78/100." },
            { gate: "TEST_QUALITY", status: "PASSED", feedback: "Test suite covers 88% happy path and 2 edge cases." },
            { gate: "COMPETENCY_SIGNAL", status: "PASSED", feedback: "Demonstrated METC-3 and TEST-3 competence metrics." }
          ]
        };
        setResultsData(mockResponse);
        setViewMode("evaluation_results");
      }, 1500);
      return;
    }

    try {
      const res = await fetch(`/api/v1/build-comprehension/${checkData.id}/submit`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(payload)
      });

      if (res.status === 200) {
        const result = await res.json();
        setResultsData(result);
        
        // Update user XP locally for display consistency
        const studentRaw = localStorage.getItem('merge_student');
        if (studentRaw && result.xpAwarded) {
          try {
            const student = JSON.parse(studentRaw);
            student.total_xp = (student.total_xp || 0) + result.xpAwarded;
            localStorage.setItem('merge_student', JSON.stringify(student));
          } catch(e) {}
        }
        
        setViewMode("evaluation_results");
      } else if (res.status === 400) {
        const errorData = await res.json();
        if (errorData.timerExpired) {
          setTimerExpired(true);
        } else {
          alert("Submission failed validation checks.");
        }
      } else {
        alert("An error occurred during build compilation verification.");
      }
    } catch(e) {
      console.error(e);
      alert("Network timeout exception submitting comprehension data.");
    } finally {
      setSubmitting(false);
    }
  };

  const autoSubmitAnswers = () => {
    // If timer expired, submit immediately without waiting for form submission
    handleSubmit();
  };

  if (viewMode === "loading") {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center text-on-surface select-none">
        <div className="animate-spin text-primary text-[32px] material-symbols-outlined mb-4">sync</div>
        <p className="font-mono-code text-xs uppercase tracking-widest text-on-surface-variant">Locking Workspace Gates...</p>
      </div>
    );
  }

  if (viewMode === "error") {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6 text-on-surface select-none">
        <div className="max-w-md bg-[#16181D] border border-outline border-error/50 p-8 text-center space-y-6">
          <span className="material-symbols-outlined text-[36px] text-error">lock</span>
          <h2 className="font-headline-md text-on-surface uppercase">Check Restrained</h2>
          <p className="text-xs text-on-surface-variant leading-relaxed">{errorMsg}</p>
          <button 
            onClick={() => navigate('/dashboard')}
            className="w-full bg-[#3B82F6] hover:bg-[#2563EB] text-white py-3.5 text-label-caps uppercase tracking-wider transition-colors"
          >
            Launch Overview Dashboard
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex flex-col bg-background text-on-surface relative font-body-md text-body-md select-text">
      {/* Header */}
      <header className="h-[48px] bg-surface border-b border-outline-variant flex justify-between items-center px-margin-md z-40">
        <div className="flex items-center gap-4">
          <span className="font-display font-extrabold uppercase tracking-widest text-[16px]">
            <MergeLogo className="text-[18px]" />
          </span>
        </div>
        <div className="flex items-center gap-4">
          <span className="px-2 py-0.5 border border-error bg-error/5 text-error font-mono-code text-[10px] uppercase">
            GATED COMPREHENSION review
          </span>
        </div>
      </header>

      {/* Main Content Area */}
      {viewMode === "timed_questions" && (
        <main className="flex-1 max-w-2xl mx-auto w-full px-6 py-12 flex flex-col justify-start">
          {/* Visual Timer Progress Bar */}
          <div className="mb-8 space-y-2 text-left">
            <div className="flex justify-between items-end">
              <div>
                <span className="font-mono-code text-[10px] text-primary uppercase block">GATE 4 / BUILD COMPREHENSION</span>
                <h1 className="font-headline-lg text-headline-lg text-on-surface uppercase">Design Integrity Verification</h1>
              </div>
              <div className="text-right">
                <span className="font-mono-code text-[10px] text-on-surface-variant block">SERVER DEADLINE</span>
                <span className={`font-mono-code text-sm font-bold ${secondsRemaining <= 15 ? 'text-error animate-pulse' : 'text-primary'}`}>
                  {secondsRemaining}s remaining
                </span>
              </div>
            </div>
            
            {/* Visual Timer Line */}
            <div className="h-1 w-full bg-surface-container-highest border border-outline-variant/35 overflow-hidden">
              <div 
                className={`h-full transition-all duration-1000 ${secondsRemaining <= 15 ? 'bg-error' : 'bg-primary'}`}
                style={{ width: `${Math.min(100, (secondsRemaining / 60) * 100)}%` }}
              ></div>
            </div>
          </div>

          {timerExpired ? (
            <div className="bg-error/5 border border-error p-6 text-center space-y-4 mb-8">
              <span className="material-symbols-outlined text-[32px] text-error animate-pulse">timer_off</span>
              <h4 className="font-headline-md text-on-surface uppercase text-sm">Server Timeout Expired</h4>
              <p className="text-xs text-on-surface-variant leading-relaxed">
                The 10-second per question deadline was exceeded. Answers have been locked and submitted for compilation verification.
              </p>
            </div>
          ) : (
            <div className="bg-surface-container border border-outline-variant p-5 mb-8 text-left text-xs text-on-surface-variant leading-relaxed">
              <p>
                Explain your design decisions in the text areas below. Questions target your actual variable names, function layouts, and architecture choices. <strong>Timer is enforced server-side.</strong>
              </p>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-8 text-left mb-16">
            {checkData.questions.map((q, idx) => (
              <div key={idx} className="space-y-3">
                <div className="flex gap-3">
                  <span className="font-mono-code text-primary font-bold">{idx + 1}.</span>
                  <label className="block text-xs text-on-surface font-semibold leading-relaxed">
                    {q}
                  </label>
                </div>
                <textarea
                  disabled={timerExpired || submitting}
                  required
                  value={answers[idx] || ""}
                  onChange={e => handleAnswerChange(idx, e.target.value)}
                  placeholder="Enter detailed architectural justification..."
                  rows={3}
                  className="w-full bg-[#09090B] border border-outline-variant p-4 font-mono-code text-xs focus:border-primary outline-none transition-colors rounded-none resize-none disabled:opacity-40"
                />
              </div>
            ))}

            {!timerExpired && (
              <div className="pt-4 flex justify-end">
                <button
                  type="submit"
                  disabled={submitting}
                  className="bg-[#3B82F6] hover:bg-[#2563EB] disabled:opacity-50 disabled:cursor-wait text-white px-8 py-4 font-label-caps text-label-caps uppercase tracking-widest rounded-none transition-colors"
                >
                  {submitting ? "Reviewing Answers..." : "Submit Verification Gate"}
                </button>
              </div>
            )}
          </form>
        </main>
      )}

      {viewMode === "evaluation_results" && (
        <main className="flex-1 max-w-2xl mx-auto w-full px-6 py-12 flex flex-col justify-start">
          {/* Header */}
          <div className="mb-8 border-b border-outline-variant pb-6 text-left">
            <span className="font-mono-code text-[10px] text-primary uppercase block">BUILD COMPILATION REVIEWED</span>
            <h1 className="font-headline-lg text-headline-lg text-on-surface uppercase">Evaluation Results</h1>
          </div>

          {/* Results Summary Box */}
          <div className="bg-[#16181D] border border-outline-variant p-8 text-center space-y-6 mb-8">
            <div className="space-y-1">
              <span className="font-mono-code text-[10px] text-on-surface-variant uppercase tracking-widest block">
                EARNED PERFORMANCE LEVEL
              </span>
              <h2 className="font-display text-[32px] font-extrabold text-primary tracking-wide uppercase">
                {resultsData.tier} TIER
              </h2>
            </div>

            <div className="space-y-2 max-w-sm mx-auto">
              <span className="font-display text-[64px] font-extrabold text-green-400 leading-none block">
                +{resultsData.xpAwarded} XP
              </span>
              <span className="font-label-caps text-[10px] text-on-surface-variant block uppercase">
                AWARDED TO STAGE TOTAL
              </span>
            </div>

            <div className="pt-2">
              <span className="px-3 py-1.5 bg-green-500/10 border border-green-500 text-green-400 font-mono-code text-[11px] uppercase inline-flex items-center gap-2">
                <span className="material-symbols-outlined text-sm">check_circle</span>
                <span>
                  {resultsData.comprehensionPassed 
                    ? "Comprehension check passed successfully" 
                    : "Comprehension check failed — minimum pass tier secured"}
                </span>
              </span>
            </div>
          </div>

          {/* Gates Review Details */}
          <div className="space-y-4 text-left mb-16">
            <h3 className="font-label-caps text-label-caps text-on-surface uppercase">Build Gate Review Checklist</h3>
            
            <div className="functional-border bg-[#09090B] divide-y divide-outline-variant/30">
              {resultsData.gates.map((g, idx) => (
                <div key={idx} className="p-5 flex flex-col gap-2">
                  <div className="flex justify-between items-center">
                    <span className="font-mono-code text-[11px] text-on-surface uppercase tracking-wide">
                      {g.gate.replace('_', ' ')}
                    </span>
                    <span className={`px-2 py-0.5 font-mono-code text-[10px] uppercase ${
                      g.status === 'PASSED' 
                        ? 'bg-green-500/10 border border-green-500/30 text-green-400' 
                        : 'bg-error/10 border border-error/30 text-error'
                    }`}>
                      {g.status}
                    </span>
                  </div>
                  <p className="text-xs text-on-surface-variant leading-relaxed">
                    {g.feedback || "Gate completed successfully."}
                  </p>
                </div>
              ))}
            </div>

            <div className="pt-8">
              <button
                onClick={() => navigate('/dashboard')}
                className="w-full bg-[#3B82F6] hover:bg-[#2563EB] text-white py-4 font-label-caps text-label-caps uppercase tracking-widest transition-colors rounded-none"
              >
                Return to Overview Dashboard
              </button>
            </div>
          </div>
        </main>
      )}
    </div>
  );
}
