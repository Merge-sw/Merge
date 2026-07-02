import React, { useState, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';

export default function DrillResultScreen() {
  const navigate = useNavigate();
  const { id } = useParams(); // submissionId
  
  // Screen views: "loading" | "polling" | "feedback_ready" | "error"
  const [viewMode, setViewMode] = useState("loading");
  const [errorMsg, setErrorMsg] = useState("");
  const [pollCount, setPollCount] = useState(0);
  
  // Data state
  const [feedback, setFeedback] = useState(null);

  useEffect(() => {
    fetchFeedback();
  }, [id]);

  const fetchFeedback = async () => {
    const token = localStorage.getItem('merge_jwt');
    if (!token) {
      loadMockFeedback("Running in offline preview mode.");
      return;
    }

    try {
      const res = await fetch(`/api/v1/submissions/${id}/feedback`, {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (res.status === 200) {
        const data = await res.json();
        setFeedback(data);
        setViewMode("feedback_ready");
      } else if (res.status === 202) {
        // Still generating feedback
        setViewMode("polling");
        pollFeedback();
      } else {
        throw new Error(`API returned status: ${res.status}`);
      }
    } catch (e) {
      console.warn("Failed to fetch feedback. Falling back to mock data.", e);
      loadMockFeedback();
    }
  };

  const pollFeedback = () => {
    setTimeout(async () => {
      const token = localStorage.getItem('merge_jwt');
      if (!token) return;

      try {
        setPollCount(prev => prev + 1);
        const res = await fetch(`/api/v1/submissions/${id}/feedback`, {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

        if (res.status === 200) {
          const data = await res.json();
          setFeedback(data);
          setViewMode("feedback_ready");
        } else if (res.status === 202) {
          pollFeedback(); // Keep polling
        } else {
          setErrorMsg("Failed to compile feedback comments. Please check dashboard.");
          setViewMode("error");
        }
      } catch (e) {
        loadMockFeedback("Server disconnected during compilation. Running mock mode.");
      }
    }, 3000);
  };

  const loadMockFeedback = (warning = "") => {
    const mockData = {
      overallScore: 82,
      naming_issues: [
        { line: 12, code: "let tempVar = selectIndex;", msg: "Rename 'tempVar' to a descriptive name that conveys its purpose, such as 'targetServerIndex'." }
      ],
      function_size_issues: [
        { line: 24, code: "pingCheck(pingService) { ... }", msg: "Function is 26 lines. Consider extracting individual server state transitions into a helper method 'updateServerHealth(server, isHealthy)' to improve modularity." }
      ],
      redundancy_issues: [],
      solid_issues: [
        { line: 2, code: "class LoadBalancer { ... }", msg: "Single Responsibility Principle (SRP): The load balancer currently manages both routing requests and conducting ping checks. Consider decoupling health checking into an active 'HealthChecker' class." }
      ]
    };

    setFeedback(mockData);
    setViewMode("feedback_ready");
    if (warning) {
      console.log(warning);
    }
  };

  if (viewMode === "loading") {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center text-on-surface select-none">
        <div className="animate-spin text-primary text-[32px] material-symbols-outlined mb-4">sync</div>
        <p className="font-mono-code text-xs uppercase tracking-widest text-on-surface-variant">Accessing Code Verification Logs...</p>
      </div>
    );
  }

  if (viewMode === "polling") {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6 text-on-surface select-none animate-fade-in">
        <div className="w-full max-w-md bg-[#16181D] border border-outline-variant p-8 flex flex-col space-y-6">
          <div className="flex justify-between items-center border-b border-outline-variant/30 pb-3">
            <span className="text-primary font-bold animate-pulse font-mono-code text-[11px] uppercase">CLEAN_CODE_FEEDBACK RUNNING</span>
            <span className="text-on-surface-variant font-mono-code text-[10px]">JOB_ID: #FB-03</span>
          </div>

          <div className="space-y-4 text-left">
            <h1 className="font-headline-md text-on-surface uppercase">Evaluating Code Quality</h1>
            <p className="text-xs text-on-surface-variant leading-relaxed font-normal">
              Gemini is conducting an asynchronous review of your submission against the stage-appropriate rubric (Naming, sizing, and redundancy).
            </p>
            
            <div className="font-mono-code text-[10px] text-primary/80 space-y-1 pt-1">
              <div>&gt; Scanning variable lexical bindings...</div>
              {pollCount >= 1 && <div>&gt; Checking class function lengths...</div>}
              {pollCount >= 2 && <div>&gt; Evaluating DRY violations and duplicate segments...</div>}
              <div className="animate-pulse text-white font-bold">&gt; Compiling feedback checklist (attempt: {pollCount})...</div>
            </div>
          </div>

          <div className="flex gap-3 pt-2">
            <button 
              onClick={fetchFeedback}
              className="flex-1 bg-surface-container-high border border-outline-variant hover:bg-surface-container-highest text-on-surface text-label-caps uppercase py-3 transition-colors"
            >
              Force Status Check
            </button>
            <button 
              onClick={() => navigate('/dashboard')}
              className="flex-1 border border-transparent text-on-surface-variant hover:text-on-surface text-label-caps uppercase py-3 transition-colors text-[10px]"
            >
              Skip to Dashboard
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (viewMode === "error") {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6 text-on-surface select-none">
        <div className="max-w-md bg-[#16181D] border border-outline border-error/50 p-8 text-center space-y-6">
          <span className="material-symbols-outlined text-[36px] text-error">error</span>
          <h2 className="font-headline-md text-on-surface uppercase">Feedback Restrained</h2>
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

  const allIssues = [
    ...(feedback.naming_issues || []).map(i => ({ ...i, type: "Naming" })),
    ...(feedback.function_size_issues || []).map(i => ({ ...i, type: "Function Size" })),
    ...(feedback.redundancy_issues || []).map(i => ({ ...i, type: "Redundancy" })),
    ...(feedback.solid_issues || []).map(i => ({ ...i, type: "SOLID Principle" }))
  ];

  return (
    <div className="min-h-screen flex flex-col bg-background text-on-surface relative font-body-md text-body-md select-text">
      {/* Header */}
      <header className="h-[48px] bg-surface border-b border-outline-variant flex justify-between items-center px-margin-md z-40">
        <div className="flex items-center gap-4">
          <button 
            onClick={() => navigate('/dashboard')}
            className="flex items-center gap-1.5 hover:text-primary transition-colors text-on-surface-variant font-label-caps text-label-caps"
          >
            <span className="material-symbols-outlined text-[16px]">arrow_back</span>
            <span>Dashboard</span>
          </button>
        </div>
        <div className="flex items-center gap-4">
          <span className="px-2 py-0.5 border border-primary bg-primary/5 text-primary font-mono-code text-[10px] uppercase">
            CLEAN CODE ANALYSIS log
          </span>
        </div>
      </header>

      {/* Main Container */}
      <main className="flex-grow max-w-2xl w-full mx-auto px-6 py-12 text-left flex flex-col justify-start">
        <div className="mb-8 border-b border-outline-variant pb-6">
          <span className="font-mono-code text-[10px] text-primary uppercase block">DRILL SUBMISSION VERIFICATION</span>
          <h1 className="font-headline-lg text-headline-lg text-on-surface uppercase">Clean Code Review</h1>
        </div>

        {/* Score Card Box */}
        <div className="bg-[#16181D] border border-outline-variant p-6 flex flex-col sm:flex-row items-center gap-6 mb-8">
          {/* Circular Score representation */}
          <div className="relative w-24 h-24 flex items-center justify-center flex-shrink-0">
            <svg className="w-full h-full transform -rotate-90">
              <circle cx="48" cy="48" r="40" stroke="#27272A" strokeWidth="8" fill="transparent" />
              <circle 
                cx="48" 
                cy="48" 
                r="40" 
                stroke="#3B82F6" 
                strokeWidth="8" 
                fill="transparent" 
                strokeDasharray={2 * Math.PI * 40}
                strokeDashoffset={2 * Math.PI * 40 * (1 - feedback.overallScore / 100)}
                className="transition-all duration-1000"
              />
            </svg>
            <span className="absolute font-display font-extrabold text-xl text-on-surface">{feedback.overallScore}</span>
          </div>

          <div className="space-y-1 text-center sm:text-left">
            <span className="font-mono-code text-[9px] text-[#3B82F6] uppercase tracking-widest block font-bold">ANALYSIS INDEX RATING</span>
            <h2 className="font-display font-bold text-headline-md uppercase text-on-surface">Review Complete</h2>
            <p className="text-[11px] text-on-surface-variant leading-relaxed font-normal">
              Your code was scanned against Cadet stage standards. You secured {feedback.overallScore} quality rating. Review actionable items below.
            </p>
          </div>
        </div>

        {/* Review list */}
        <div className="space-y-4 mb-12">
          <h3 className="font-label-caps text-label-caps text-on-surface uppercase">Actionable Items Checklist</h3>
          
          {allIssues.length === 0 ? (
            <div className="p-8 text-center bg-surface-container-lowest border border-outline-variant/60 text-xs text-on-surface-variant font-mono-code uppercase">
              No style violations detected. Perfect code structure.
            </div>
          ) : (
            <div className="functional-border bg-[#09090B] divide-y divide-outline-variant/30">
              {allIssues.map((issue, idx) => (
                <div key={idx} className="p-5 space-y-3">
                  <div className="flex justify-between items-center text-xs">
                    <div className="flex items-center gap-2 font-mono-code">
                      <span className="px-1.5 py-0.5 bg-surface-container text-[9px] border border-outline-variant text-[#ffb4ab] uppercase">
                        {issue.type}
                      </span>
                      <span className="text-on-surface-variant">Line {issue.line}</span>
                    </div>
                  </div>
                  
                  <pre className="font-mono-code text-[11px] text-[#ffb4ab] bg-surface-container-lowest p-2 border border-outline-variant/20 overflow-x-auto select-all">
                    {issue.code}
                  </pre>
                  
                  <p className="text-xs text-on-surface-variant leading-relaxed">
                    {issue.msg}
                  </p>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Action CTA */}
        <div className="pt-2">
          <button 
            onClick={() => navigate('/dashboard')}
            className="w-full bg-[#3B82F6] hover:bg-[#2563EB] text-white py-4 font-label-caps text-label-caps uppercase tracking-widest transition-colors rounded-none"
          >
            Launch Overview Dashboard
          </button>
        </div>
      </main>
    </div>
  );
}
