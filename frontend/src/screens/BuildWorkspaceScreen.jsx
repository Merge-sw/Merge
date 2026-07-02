import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';

export default function BuildWorkspaceScreen() {
  const navigate = useNavigate();
  
  // State for build data
  const [loading, setLoading] = useState(true);
  const [generating, setGenerating] = useState(false);
  const [errorMsg, setErrorMsg] = useState("");
  const [buildData, setBuildData] = useState(null);
  
  // Active editor tab: "code" | "tests" | "architecture"
  const [activeTab, setActiveTab] = useState("code");
  
  // Editor content states
  const [code, setCode] = useState("");
  const [testSuite, setTestSuite] = useState("");
  const [architecture, setArchitecture] = useState("");
  
  // Modals & submission state
  const [submitting, setSubmitting] = useState(false);
  const [showExitModal, setShowExitModal] = useState(false);
  const [generationPollCount, setGenerationPollCount] = useState(0);

  // Load active build
  useEffect(() => {
    fetchActiveBuild();
  }, []);

  const fetchActiveBuild = async () => {
    const token = localStorage.getItem('merge_jwt');
    if (!token) {
      loadMockBuild("Authentication token not found. Running in offline preview mode.");
      return;
    }

    try {
      setLoading(true);
      const res = await fetch('/api/v1/builds/current', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (res.status === 200) {
        const data = await res.json();
        setBuildData(data);
        initializeEditorDefaults(data.stageName);
        setGenerating(false);
        setLoading(false);
      } else if (res.status === 202) {
        // Build PRD is generating
        setGenerating(true);
        setLoading(false);
        // Start polling every 4 seconds
        pollGeneration();
      } else if (res.status === 403) {
        setErrorMsg("Build is currently locked. Complete all Stage Drills and reach the XP threshold to unlock.");
        setLoading(false);
      } else {
        throw new Error(`API error: ${res.status}`);
      }
    } catch (e) {
      console.warn("Failed to fetch active build. Falling back to mock data.", e);
      loadMockBuild();
    }
  };

  const pollGeneration = () => {
    const timer = setTimeout(async () => {
      const token = localStorage.getItem('merge_jwt');
      if (!token) return;

      try {
        setGenerationPollCount(prev => prev + 1);
        const res = await fetch('/api/v1/builds/current', {
          headers: {
            'Authorization': `Bearer ${token}`
          }
        });

        if (res.status === 200) {
          const data = await res.json();
          setBuildData(data);
          initializeEditorDefaults(data.stageName);
          setGenerating(false);
        } else if (res.status === 202) {
          pollGeneration(); // Continue polling
        } else {
          setGenerating(false);
          setErrorMsg("Failed to generate Build PRD. Please contact system admin.");
        }
      } catch (e) {
        setGenerating(false);
        loadMockBuild("Server went offline during generation. Running in offline preview mode.");
      }
    }, 4000);
  };

  const loadMockBuild = (warning = "") => {
    // Generate context appropriate fallback data for visual styling demonstration
    const mockData = {
      buildId: 99,
      stageName: "CADET",
      unlockedAt: new Date(Date.now() - 3600000 * 24).toISOString(),
      prd: `# CloudStream Resilient Load Balancer (FACT)

## 1. System Overview
In high-concurrency systems, traffic must be balanced across multiple running server nodes. If a server goes offline, it should be removed from the pool dynamically to avoid routing requests to a dead service.

Your task is to build a round-robin load balancer that performs active ping-based health checks on its target servers.

## 2. Requirements
* Create a \`LoadBalancer\` class.
* Implement \`selectServer()\` returning the next active server in round-robin sequence.
* Implement \`pingCheck()\` which iterates all registered servers. If a server fails its ping 3 times consecutively, transition its status to \`OFFLINE\`.
* Servers are initialized as \`ONLINE\`.

## 3. Architecture Constraints
* Memory limit: 256MB
* CPU limit: 2.0s
* Network access disabled. Code execution isolated.`,
      requirements: [
        "Implement a dynamic list of registered servers",
        "Select next server using round-robin routing logic",
        "Conduct ping health checks to detect offline nodes",
        "Transition server state dynamically to OFFLINE after 3 failures"
      ],
      constraints: [
        "Memory limit: 256MB",
        "CPU limit: 2 seconds",
        "Network-disabled environment (Judge0 constraint)",
        "Zero external dependencies"
      ],
      sfiaCompetencies: [
        "Software Design & Development (SFIA LEVEL 3 - METC)",
        "Testing & Debugging (SFIA LEVEL 3 - TEST)"
      ]
    };
    
    setBuildData(mockData);
    initializeEditorDefaults("CADET");
    setGenerating(false);
    setLoading(false);
    if (warning) {
      console.log(warning);
    }
  };

  const initializeEditorDefaults = (stageName) => {
    setCode(
`/**
 * CLOUDSTREAM SYSTEMS RESILIENT LOAD BALANCER
 * Stage: ${stageName}
 */
class LoadBalancer {
  constructor() {
    this.servers = []; // { id: string, status: 'ONLINE'|'OFFLINE', failCount: number }
    this.currentIndex = 0;
  }

  registerServer(serverId) {
    this.servers.push({
      id: serverId,
      status: 'ONLINE',
      failCount: 0
    });
  }

  selectServer() {
    const activeServers = this.servers.filter(s => s.status === 'ONLINE');
    if (activeServers.length === 0) return null;
    
    const server = activeServers[this.currentIndex % activeServers.length];
    this.currentIndex++;
    return server.id;
  }

  pingCheck(pingService) {
    for (let server of this.servers) {
      const isHealthy = pingService.ping(server.id);
      if (isHealthy) {
        server.status = 'ONLINE';
        server.failCount = 0;
      } else {
        server.failCount++;
        if (server.failCount >= 3) {
          server.status = 'OFFLINE';
        }
      }
    }
  }
}`
    );

    setTestSuite(
`// Write TDD test cases targeting happy-path and failure edge cases
describe("LoadBalancer Round Robin", () => {
  it("should balance requests evenly among registered servers", () => {
    const lb = new LoadBalancer();
    lb.registerServer("srv-1");
    lb.registerServer("srv-2");
    
    expect(lb.selectServer()).toBe("srv-1");
    expect(lb.selectServer()).toBe("srv-2");
    expect(lb.selectServer()).toBe("srv-1");
  });
});`
    );

    setArchitecture(
`# Architecture Specification
Provide a description of your implementation:
1. Component layout & round robin index pointer logic
2. Exception wrapping strategy during server down intervals
3. Health check frequency and threshold constraints`
    );
  };

  const handleExitTrigger = () => {
    // Designer Brief: Friction placed at exit. Real friction to quit.
    if (code.trim().length > 100 || testSuite.trim().length > 100) {
      setShowExitModal(true);
    } else {
      navigate('/dashboard');
    }
  };

  const confirmDiscardAndExit = () => {
    setShowExitModal(false);
    navigate('/dashboard');
  };

  const handleSubmitBuild = async () => {
    if (code.trim().length === 0 || testSuite.trim().length === 0 || architecture.trim().length === 0) {
      alert("All submissions require implementation code, TDD test suite, and architecture document.");
      return;
    }

    setSubmitting(true);
    const token = localStorage.getItem('merge_jwt');
    const idempotencyKey = `build-sub-${buildData.buildId}-${Date.now()}`;

    const payload = {
      code,
      testSuite,
      architectureDocument: architecture,
      idempotencyKey
    };

    if (!token) {
      // Simulate submission in preview mode
      setTimeout(() => {
        setSubmitting(false);
        // Navigates to mocked results screen or displays preview warning
        alert("Build submitted successfully in offline preview mode! Connecting to mock gate review...");
        navigate('/dashboard');
      }, 1500);
      return;
    }

    try {
      const res = await fetch(`/api/v1/builds/${buildData.buildId}/submit`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(payload)
      });

      if (res.status === 200) {
        const result = await res.json();
        // Capture submission details in local storage for details screen representation
        localStorage.setItem('last_build_submission_result', JSON.stringify(result));
        
        if (result.comprehensionCheckId) {
          navigate(`/build-comprehension/${result.comprehensionCheckId}`);
        } else {
          // If no timed check was triggered (e.g. gates 1-3 failed)
          navigate(`/dashboard`);
        }
      } else {
        const errText = await res.text();
        alert(`Submission failed: ${errText}`);
      }
    } catch(e) {
      console.error(e);
      alert("Network exception occurred during build verification.");
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center text-on-surface select-none">
        <div className="animate-spin text-primary text-[32px] material-symbols-outlined mb-4">sync</div>
        <p className="font-mono-code text-xs uppercase tracking-widest text-on-surface-variant">Accessing Stage Sandbox...</p>
      </div>
    );
  }

  if (generating) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6 text-on-surface select-none animate-fade-in">
        <div className="w-full max-w-md bg-[#16181D] border border-outline-variant p-8 flex flex-col space-y-6">
          <div className="flex justify-between items-center border-b border-outline-variant/30 pb-3">
            <span className="text-primary font-bold animate-pulse font-mono-code text-[11px] uppercase">BUILD_PRD_GENERATION ACTIVE</span>
            <span className="text-on-surface-variant font-mono-code text-[10px]">JOB_ID: #AI-03</span>
          </div>

          <div className="space-y-4 text-left">
            <h1 className="font-headline-md text-on-surface uppercase">Orchestrating Personalized PRD</h1>
            <p className="text-xs text-on-surface-variant leading-relaxed">
              Gemini is analyzing your Drill performance history (weak concepts, hint usage patterns, naming issues) to construct a challenge matching your formation gaps.
            </p>
            
            <div className="font-mono-code text-[10px] text-primary/80 space-y-1.5 pt-2 max-h-36 overflow-y-auto">
              <div>&gt; Querying pgvector history profile...</div>
              {generationPollCount >= 1 && <div>&gt; Identifying logic deficits: [Exceptions, Data Mapping]</div>}
              {generationPollCount >= 2 && <div>&gt; Formulating real-world failure vector mapping...</div>}
              {generationPollCount >= 3 && <div>&gt; Establishing SFIA competency rubrics...</div>}
              <div className="animate-pulse text-white font-bold">&gt; Compiling specs (poll attempt: {generationPollCount})...</div>
            </div>
          </div>

          <div className="flex gap-3 pt-2">
            <button 
              onClick={fetchActiveBuild}
              className="flex-1 bg-surface-container-high border border-outline-variant hover:bg-surface-container-highest text-on-surface text-label-caps uppercase py-3 transition-colors"
            >
              Check Status
            </button>
            <button 
              onClick={() => navigate('/dashboard')}
              className="flex-1 border border-transparent text-on-surface-variant hover:text-on-surface text-label-caps uppercase py-3 transition-colors text-[10px]"
            >
              Exit to Dashboard
            </button>
          </div>
        </div>
      </div>
    );
  }

  if (errorMsg) {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6 text-on-surface select-none">
        <div className="max-w-md bg-[#16181D] border border-outline border-error/50 p-8 text-center space-y-6">
          <span className="material-symbols-outlined text-[36px] text-error">lock</span>
          <h2 className="font-headline-md text-on-surface uppercase">Access Restrained</h2>
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
    <div className="min-h-screen flex flex-col bg-background text-on-surface relative select-text font-body-md text-body-md">
      {/* Workspace Header */}
      <header className="h-[48px] bg-surface border-b border-outline-variant flex justify-between items-center px-margin-md z-40">
        <div className="flex items-center gap-4">
          <button 
            onClick={handleExitTrigger}
            className="flex items-center gap-1.5 hover:text-primary transition-colors text-on-surface-variant font-label-caps text-label-caps"
          >
            <span className="material-symbols-outlined text-[16px]">arrow_back</span>
            <span>Abort Session</span>
          </button>
        </div>
        <div className="flex items-center gap-4">
          <span className="px-2 py-0.5 border border-primary/45 bg-primary/5 text-primary font-mono-code text-[10px] uppercase">
            STAGE: {buildData.stageName}
          </span>
          <button 
            onClick={handleExitTrigger} 
            className="hover:text-primary transition-colors font-label-caps text-label-caps uppercase text-on-surface-variant text-[10px]"
          >
            Dashboard
          </button>
        </div>
      </header>

      {/* Main Workspace Workspace */}
      <main className="flex-1 grid grid-cols-12 overflow-hidden h-[calc(100vh-48px)]">
        {/* Left Panel: Specifications */}
        <section className="col-span-5 border-r border-outline-variant p-6 flex flex-col gap-6 text-left overflow-y-auto bg-surface-container-lowest">
          <div>
            <span className="font-mono-code text-[10px] text-primary uppercase tracking-widest block mb-1">
              BUILD STAGE PROJECT / #{buildData.buildId}
            </span>
            <h2 className="font-headline-lg text-headline-lg text-on-surface uppercase">Stage Capstone</h2>
          </div>

          <div className="border-t border-outline-variant/30 pt-4 prose prose-invert max-w-none text-xs text-on-surface-variant leading-relaxed space-y-4">
            {/* PRD Text */}
            <div className="whitespace-pre-line font-body-md text-body-md">
              {buildData.prd}
            </div>
          </div>

          {/* Quick Checklists */}
          <div className="border-t border-outline-variant/30 pt-4 space-y-4">
            <div>
              <span className="font-label-caps text-label-caps text-on-surface block mb-2 uppercase">Key Target Requirements</span>
              <ul className="space-y-1.5 text-xs text-on-surface-variant pl-4 list-disc">
                {buildData.requirements.map((req, idx) => (
                  <li key={idx}>{req}</li>
                ))}
              </ul>
            </div>
            
            <div>
              <span className="font-label-caps text-label-caps text-on-surface block mb-2 uppercase">Architectural Constraints</span>
              <ul className="space-y-1.5 text-xs text-on-surface-variant pl-4 list-disc">
                {buildData.constraints.map((c, idx) => (
                  <li key={idx} className="font-mono-code text-[10px]">{c}</li>
                ))}
              </ul>
            </div>

            <div>
              <span className="font-label-caps text-label-caps text-on-surface block mb-2 uppercase">SFIA Competency Alignments</span>
              <div className="flex flex-wrap gap-2">
                {buildData.sfiaCompetencies.map((sfia, idx) => (
                  <span key={idx} className="px-2 py-1 bg-surface-container border border-outline-variant text-[10px] font-mono-code text-primary uppercase">
                    {sfia}
                  </span>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Right Panel: Editors */}
        <section className="col-span-7 flex flex-col overflow-hidden relative bg-[#09090B]">
          {/* Editor Header / Tab Switcher */}
          <div className="flex justify-between items-center border-b border-outline-variant px-4 py-2 bg-surface-container">
            <div className="flex gap-4">
              <button 
                onClick={() => setActiveTab("code")}
                className={`font-label-caps text-[10px] uppercase py-1 border-b-2 transition-all ${
                  activeTab === "code" 
                    ? "border-primary text-primary" 
                    : "border-transparent text-on-surface-variant hover:text-on-surface"
                }`}
              >
                Implementation Code
              </button>
              <button 
                onClick={() => setActiveTab("tests")}
                className={`font-label-caps text-[10px] uppercase py-1 border-b-2 transition-all ${
                  activeTab === "tests" 
                    ? "border-primary text-primary" 
                    : "border-transparent text-on-surface-variant hover:text-on-surface"
                }`}
              >
                TDD Test Suite
              </button>
              <button 
                onClick={() => setActiveTab("architecture")}
                className={`font-label-caps text-[10px] uppercase py-1 border-b-2 transition-all ${
                  activeTab === "architecture" 
                    ? "border-primary text-primary" 
                    : "border-transparent text-on-surface-variant hover:text-on-surface"
                }`}
              >
                Architecture Specification
              </button>
            </div>
            <span className="font-mono-code text-[10px] text-primary uppercase">[EDITING_MODE]</span>
          </div>

          {/* Code Area */}
          <div className="flex-1 flex flex-col relative overflow-hidden">
            {activeTab === "code" && (
              <textarea
                value={code}
                onChange={e => setCode(e.target.value)}
                className="w-full flex-1 bg-[#09090B] font-mono-code text-xs text-[#a78bfa] p-6 outline-none border-none resize-none leading-relaxed select-all"
                spellCheck="false"
                placeholder="// Write your component implementation code here..."
              />
            )}
            
            {activeTab === "tests" && (
              <textarea
                value={testSuite}
                onChange={e => setTestSuite(e.target.value)}
                className="w-full flex-1 bg-[#09090B] font-mono-code text-xs text-primary p-6 outline-none border-none resize-none leading-relaxed select-all"
                spellCheck="false"
                placeholder="// Write your unit testing suite code here..."
              />
            )}
            
            {activeTab === "architecture" && (
              <textarea
                value={architecture}
                onChange={e => setArchitecture(e.target.value)}
                className="w-full flex-1 bg-[#09090B] font-mono-code text-xs text-on-surface p-6 outline-none border-none resize-none leading-relaxed select-all"
                spellCheck="false"
                placeholder="# Architecture Document\nDescribe your structure and design choices..."
              />
            )}
          </div>

          {/* Bottom Bar Controls */}
          <div className="border-t border-outline-variant p-4 bg-surface-container flex justify-between items-center">
            <span className="text-[11px] font-mono-code text-on-surface-variant flex items-center gap-1">
              <span className="w-1.5 h-1.5 bg-green-500 animate-pulse"></span>
              <span>SANDBOX_ACTIVE // JWT AUTHENTICATED</span>
            </span>
            <button 
              disabled={submitting}
              onClick={handleSubmitBuild}
              className="bg-[#3B82F6] hover:bg-[#2563EB] disabled:opacity-50 disabled:cursor-wait text-white px-8 py-3.5 font-label-caps text-label-caps uppercase tracking-wider transition-colors flex items-center gap-2"
            >
              {submitting ? (
                <>
                  <span>Executing Gates...</span>
                  <span className="material-symbols-outlined text-[16px] animate-spin">sync</span>
                </>
              ) : (
                <>
                  <span>Submit Project For Evaluation</span>
                  <span className="material-symbols-outlined text-[16px]">send</span>
                </>
              )}
            </button>
          </div>
        </section>
      </main>

      {/* Exit confirmation dialog modal (Designer Brief compliance) */}
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
                className="w-full border border-outline-variant hover:bg-surface-container-high text-on-surface-variant py-2.5 font-label-caps text-[10px] uppercase tracking-wider rounded-none"
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
