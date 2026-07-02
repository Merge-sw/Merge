import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';

export default function StagePromotionScreen() {
  const navigate = useNavigate();
  
  // Screen views: "loading" | "promotion_check" | "celebration" | "error"
  const [viewMode, setViewMode] = useState("loading");
  const [errorMsg, setErrorMsg] = useState("");
  
  // Data states
  const [student, setStudent] = useState(null);
  const [status, setStatus] = useState({ eligible: false, missingXp: 0, missingBuildScore: 0 });
  const [executing, setExecuting] = useState(false);
  const [promotionResult, setPromotionResult] = useState(null);

  // Simulation controls (offline preview mode only)
  const [isOffline, setIsOffline] = useState(false);
  const [simulateEligible, setSimulateEligible] = useState(false);

  useEffect(() => {
    fetchPromotionStatus();
  }, [simulateEligible]);

  const fetchPromotionStatus = async () => {
    const studentRaw = localStorage.getItem('merge_student');
    let localStudent = null;
    if (studentRaw) {
      try {
        localStudent = JSON.parse(studentRaw);
        setStudent(localStudent);
      } catch(e) {}
    }

    const token = localStorage.getItem('merge_jwt');
    if (!token) {
      setIsOffline(true);
      loadMockStatus(localStudent);
      return;
    }

    try {
      setViewMode("loading");
      const res = await fetch('/api/v1/students/me/stage/promotion-status', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (res.status === 200) {
        const data = await res.json();
        setStatus(data);
        setViewMode("promotion_check");
      } else {
        throw new Error(`API returned status: ${res.status}`);
      }
    } catch (e) {
      console.warn("Failed to fetch promotion status. Using mock data.", e);
      setIsOffline(true);
      loadMockStatus(localStudent);
    }
  };

  const loadMockStatus = (localStudent) => {
    // Generate mocked progression status depending on local simulated XP or simulation toggle
    const currentXp = localStudent ? localStudent.total_xp || 1250 : 1250;
    
    if (simulateEligible) {
      setStatus({
        eligible: true,
        missingXp: 0,
        missingBuildScore: 0
      });
    } else {
      setStatus({
        eligible: currentXp >= 1500,
        missingXp: Math.max(0, 1500 - currentXp),
        missingBuildScore: currentXp >= 1350 ? 50 : 200
      });
    }
    setViewMode("promotion_check");
  };

  const handleExecutePromotion = async () => {
    setExecuting(true);
    const token = localStorage.getItem('merge_jwt');

    if (!token || isOffline) {
      // Simulate success in offline preview mode
      setTimeout(() => {
        setExecuting(false);
        const fromStage = student ? student.current_stage || "SCOUT" : "SCOUT";
        const toStage = fromStage === "SCOUT" ? "CADET" : fromStage === "CADET" ? "ENGINEER" : "ARCHITECT";
        const result = {
          fromStage,
          toStage,
          xpAtPromotion: student ? student.total_xp || 1500 : 1500,
          buildScoreAtPromotion: 650
        };
        
        // Update local storage representation
        if (student) {
          const updated = { ...student, current_stage: toStage };
          localStorage.setItem('merge_student', JSON.stringify(updated));
        }

        setPromotionResult(result);
        setViewMode("celebration");
      }, 1500);
      return;
    }

    try {
      const res = await fetch('/api/v1/students/me/stage/promote', {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (res.status === 200) {
        const result = await res.json();
        
        // Backfill local storage student profile
        if (student) {
          const updated = { ...student, current_stage: result.toStage };
          localStorage.setItem('merge_student', JSON.stringify(updated));
        }

        setPromotionResult(result);
        setViewMode("celebration");
      } else if (res.status === 403) {
        const errorData = await res.json();
        alert(`Promotion Denied: Deficits remaining. Missing XP: ${errorData.missingXp || 0}, Missing Build Score: ${errorData.missingBuildScore || 0}`);
        fetchPromotionStatus(); // Reload
      } else {
        alert("An error occurred executing the promotion routine.");
      }
    } catch(e) {
      console.error(e);
      alert("Network exception occurred connecting to promotion gateway.");
    } finally {
      setExecuting(false);
    }
  };

  const getNextStage = (curr) => {
    if (!curr) return "CADET";
    const stages = ["SCOUT", "CADET", "ENGINEER", "ARCHITECT", "PRINCIPAL"];
    const idx = stages.indexOf(curr.toUpperCase());
    if (idx === -1 || idx === stages.length - 1) return "CADET";
    return stages[idx + 1];
  };

  const currentStage = student ? student.current_stage || "SCOUT" : "SCOUT";
  const nextStage = getNextStage(currentStage);

  if (viewMode === "loading") {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center text-on-surface select-none">
        <div className="animate-spin text-primary text-[32px] material-symbols-outlined mb-4">sync</div>
        <p className="font-mono-code text-xs uppercase tracking-widest text-on-surface-variant">Checking Eligibility Gates...</p>
      </div>
    );
  }

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
            REGISTRY GATEWAY
          </span>
        </div>
      </header>

      {/* Main Content Area */}
      {viewMode === "promotion_check" && (
        <main className="flex-1 max-w-xl mx-auto w-full px-6 py-12 flex flex-col justify-start">
          <div className="mb-8 border-b border-outline-variant pb-6 text-left">
            <span className="font-mono-code text-[10px] text-primary uppercase block">PROMOTION GATEWAY</span>
            <h1 className="font-headline-lg text-headline-lg text-on-surface uppercase">Stage Promotion</h1>
            <p className="text-xs text-on-surface-variant mt-1">Review validation requirements to transition out of stage {currentStage}.</p>
          </div>

          {/* Target Transition Box */}
          <div className="bg-[#16181D] border border-outline-variant p-6 flex justify-around items-center mb-8">
            <div className="text-center">
              <span className="font-mono-code text-[9px] text-on-surface-variant uppercase block mb-1">Current Stage</span>
              <span className="font-display font-extrabold text-lg text-on-surface tracking-wider">{currentStage}</span>
            </div>
            <span className="material-symbols-outlined text-primary text-[28px] animate-pulse">double_arrow</span>
            <div className="text-center">
              <span className="font-mono-code text-[9px] text-on-surface-variant uppercase block mb-1">Target Stage</span>
              <span className="font-display font-extrabold text-lg text-primary tracking-wider">{nextStage}</span>
            </div>
          </div>

          {/* Validation Checklist */}
          <div className="space-y-4 text-left mb-8">
            <h3 className="font-label-caps text-label-caps text-on-surface uppercase">Verification Credentials</h3>
            
            <div className="functional-border bg-[#09090B] divide-y divide-outline-variant/30">
              {/* Gate 1: XP */}
              <div className="p-5 flex items-start gap-4">
                <span className={`material-symbols-outlined text-[20px] ${status.missingXp === 0 ? 'text-green-400' : 'text-on-surface-variant'}`}>
                  {status.missingXp === 0 ? 'check_circle' : 'pending'}
                </span>
                <div className="flex-1 space-y-1">
                  <div className="flex justify-between items-center text-xs font-semibold text-on-surface">
                    <span>STAGE XP ACCUMULATION</span>
                    <span className="font-mono-code text-[11px]">
                      {status.missingXp === 0 ? 'CLEARED' : `-${status.missingXp} XP`}
                    </span>
                  </div>
                  <p className="text-[11px] text-on-surface-variant leading-relaxed">
                    Student must reach the stage XP threshold to satisfy basic formation volume criteria.
                  </p>
                </div>
              </div>

              {/* Gate 2: Build pass score */}
              <div className="p-5 flex items-start gap-4">
                <span className={`material-symbols-outlined text-[20px] ${status.missingBuildScore === 0 ? 'text-green-400' : 'text-on-surface-variant'}`}>
                  {status.missingBuildScore === 0 ? 'check_circle' : 'pending'}
                </span>
                <div className="flex-1 space-y-1">
                  <div className="flex justify-between items-center text-xs font-semibold text-on-surface">
                    <span>BUILD PASS QUALITY RATING</span>
                    <span className="font-mono-code text-[11px]">
                      {status.missingBuildScore === 0 ? 'CLEARED' : `-${status.missingBuildScore} POINTS`}
                    </span>
                  </div>
                  <p className="text-[11px] text-on-surface-variant leading-relaxed">
                    Student must secure a cumulative rating threshold across stage Builds to verify structural code quality.
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Promotion Button Action */}
          <div className="space-y-4">
            <button
              disabled={!status.eligible || executing}
              onClick={handleExecutePromotion}
              className={`w-full py-4 font-label-caps text-label-caps uppercase tracking-widest transition-all rounded-none flex items-center justify-center gap-2 ${
                status.eligible 
                  ? 'bg-gradient-to-r from-blue-700 via-blue-600 to-blue-500 hover:from-blue-600 hover:to-blue-400 text-white shadow-lg active:scale-[0.99] cursor-pointer' 
                  : 'bg-[#16181D] border border-outline-variant/40 text-on-surface-variant/40 cursor-not-allowed'
              }`}
            >
              {executing ? (
                <>
                  <span>Executing Promotion...</span>
                  <span className="material-symbols-outlined text-[16px] animate-spin">sync</span>
                </>
              ) : (
                <>
                  <span>Promote to Stage {nextStage}</span>
                  <span className="material-symbols-outlined text-[18px]">workspace_premium</span>
                </>
              )}
            </button>

            {!status.eligible && (
              <p className="text-[10px] text-on-surface-variant/75 text-center leading-relaxed font-mono-code uppercase">
                [GATE_LOCKED] Complete all outstanding stage targets to unlock promotion sequence.
              </p>
            )}
          </div>

          {/* Simulation Helper Panel (Offline Preview Mode Only) */}
          {isOffline && (
            <div className="mt-12 p-4 bg-[#16181D] border border-dashed border-outline-variant/60 text-left space-y-3">
              <span className="font-mono-code text-[9px] text-primary uppercase block font-bold">Preview Environment Controls</span>
              <p className="text-[11px] text-on-surface-variant">
                You are currently running in offline preview mode because no backend server JWT was found. Toggle the status below to test the celebratory promotion screens:
              </p>
              <div className="flex gap-2">
                <button
                  onClick={() => setSimulateEligible(prev => !prev)}
                  className="bg-primary/10 border border-primary/40 text-primary hover:bg-primary/20 text-[10px] font-mono-code uppercase px-3 py-1.5 transition-colors"
                >
                  {simulateEligible ? "Simulate Ineligible Gate" : "Simulate Eligible Gate"}
                </button>
              </div>
            </div>
          )}
        </main>
      )}

      {viewMode === "celebration" && promotionResult && (
        <main className="fixed inset-0 bg-[#0D0F12] flex items-center justify-center p-6 z-50 animate-fade-in select-none">
          <div className="max-w-md w-full border border-outline-variant bg-[#16181D] p-12 text-center space-y-8 shadow-2xl relative overflow-hidden">
            {/* Visual background lights */}
            <div className="absolute top-0 left-1/2 -translate-x-1/2 w-64 h-64 bg-primary/15 rounded-full filter blur-[60px] pointer-events-none"></div>
            
            <div className="space-y-4 relative z-10">
              <div className="w-16 h-16 rounded-full border border-primary bg-primary/5 flex items-center justify-center mx-auto text-primary animate-bounce">
                <span className="material-symbols-outlined text-[32px]">workspace_premium</span>
              </div>
              
              <div className="space-y-1">
                <span className="font-mono-code text-[10px] text-primary uppercase tracking-widest block">FORMATION REGISTRY UPDATE</span>
                <h1 className="font-display text-[32px] font-extrabold text-on-surface uppercase tracking-tight leading-none">
                  Stage Promoted
                </h1>
              </div>

              <div className="py-4">
                <span className="font-display text-[72px] font-extrabold text-primary leading-none tracking-tight block">
                  {promotionResult.toStage}
                </span>
                <span className="font-label-caps text-label-caps text-on-surface-variant block uppercase tracking-widest mt-2">
                  NEW CLASSIFICATION RATING
                </span>
              </div>
            </div>

            <div className="h-px bg-outline-variant/30 w-full relative z-10"></div>

            <div className="grid grid-cols-2 gap-4 text-left relative z-10">
              <div className="border border-outline-variant/30 p-3 bg-surface-container-lowest">
                <span className="font-mono-code text-[9px] text-on-surface-variant uppercase block mb-0.5">Pre XP Level</span>
                <span className="font-mono-code text-xs text-on-surface font-bold">
                  {promotionResult.xpAtPromotion} XP
                </span>
              </div>
              <div className="border border-outline-variant/30 p-3 bg-surface-container-lowest">
                <span className="font-mono-code text-[9px] text-on-surface-variant uppercase block mb-0.5">Build Score</span>
                <span className="font-mono-code text-xs text-on-surface font-bold">
                  {promotionResult.buildScoreAtPromotion} pts
                </span>
              </div>
            </div>

            <div className="pt-4 relative z-10">
              <button 
                onClick={() => {
                  // Reload page state or navigate
                  window.location.reload();
                  navigate('/dashboard');
                }}
                className="w-full bg-[#3B82F6] hover:bg-[#2563EB] text-white py-4 font-label-caps text-label-caps uppercase tracking-widest transition-colors rounded-none"
              >
                Launch Overview Dashboard
              </button>
            </div>
          </div>
        </main>
      )}
    </div>
  );
}
