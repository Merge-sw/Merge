import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';

export default function PeerReviewScreen() {
  const navigate = useNavigate();
  
  // Screen views: "loading" | "peer_review_panel" | "empty" | "error"
  const [viewMode, setViewMode] = useState("loading");
  const [errorMsg, setErrorMsg] = useState("");
  
  // Data states
  const [pendingSubmissions, setPendingSubmissions] = useState([]);
  const [selectedSubmission, setSelectedSubmission] = useState(null);
  
  // Form input states
  const [score, setScore] = useState(70);
  const [comments, setComments] = useState("");
  const [submitting, setSubmitting] = useState(false);

  // Load reviews on mount
  useEffect(() => {
    fetchPendingReviews();
  }, []);

  const fetchPendingReviews = async () => {
    const token = localStorage.getItem('merge_jwt');
    if (!token) {
      loadMockReviews("Running in offline preview mode.");
      return;
    }

    try {
      setViewMode("loading");
      const res = await fetch('/api/v1/peer-reviews/pending', {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (res.status === 200) {
        const data = await res.json();
        setPendingSubmissions(data);
        if (data.length > 0) {
          setSelectedSubmission(data[0]);
          setViewMode("peer_review_panel");
        } else {
          setViewMode("empty");
        }
      } else {
        throw new Error(`API returned status: ${res.status}`);
      }
    } catch (e) {
      console.warn("Failed to fetch pending reviews. Using mock data.", e);
      loadMockReviews();
    }
  };

  const loadMockReviews = (warning = "") => {
    const mockPending = [
      {
        id: 201,
        studentInitials: "MW",
        conceptName: "Conditional Statements",
        stageName: "CADET",
        submittedAt: new Date(Date.now() - 3600000 * 2).toISOString(),
        code: `function checkCircuitState(failures, threshold) {
  // MW implementation
  if (failures > threshold) {
    return 'OPEN';
  } else {
    return 'CLOSED';
  }
}`,
        architectureDocument: `# Closed State Design
The implementation utilizes a standard conditional branch checking failures against threshold. 
No state machine object instantiated to minimize memory footprint.`
      },
      {
        id: 202,
        studentInitials: "ER",
        conceptName: "Loops and Iteration",
        stageName: "CADET",
        submittedAt: new Date(Date.now() - 3600000 * 5).toISOString(),
        code: `function findHealthyNode(servers) {
  // ER implementation
  let i = 0;
  while(i < servers.length) {
    if (servers[i].status === 'ONLINE') {
      return servers[i].id;
    }
    i++;
  }
  return null;
}`,
        architectureDocument: `# Search Strategy
Uses a linear scan while loop. Base conditions verify bounds before indexing.`
      }
    ];

    setPendingSubmissions(mockPending);
    setSelectedSubmission(mockPending[0]);
    setViewMode("peer_review_panel");
    if (warning) {
      console.log(warning);
    }
  };

  const handleSelectSubmission = (sub) => {
    setSelectedSubmission(sub);
    setScore(70);
    setComments("");
  };

  const handleSubmitReview = async (e) => {
    e.preventDefault();
    if (comments.trim().length < 10) {
      alert("Please provide constructive comments (minimum 10 characters).");
      return;
    }

    setSubmitting(true);
    const token = localStorage.getItem('merge_jwt');
    const payload = {
      submissionId: selectedSubmission.id,
      score,
      comments
    };

    if (!token) {
      // Simulate submission in preview mode
      setTimeout(() => {
        setSubmitting(false);
        alert("Peer review submitted successfully in preview mode!");
        removeLocalSubmission(selectedSubmission.id);
      }, 1200);
      return;
    }

    try {
      const res = await fetch('/api/v1/peer-reviews', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(payload)
      });

      if (res.status === 200) {
        alert("Peer review submitted successfully!");
        removeLocalSubmission(selectedSubmission.id);
      } else {
        const errorText = await res.text();
        alert(`Failed to submit review: ${errorText}`);
      }
    } catch (e) {
      console.error(e);
      alert("Network exception occurred submitting peer review.");
    } finally {
      setSubmitting(false);
    }
  };

  const removeLocalSubmission = (subId) => {
    const updatedList = pendingSubmissions.filter(s => s.id !== subId);
    setPendingSubmissions(updatedList);
    
    if (updatedList.length > 0) {
      setSelectedSubmission(updatedList[0]);
      setViewMode("peer_review_panel");
    } else {
      setViewMode("empty");
    }
  };

  if (viewMode === "loading") {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center text-on-surface select-none">
        <div className="animate-spin text-primary text-[32px] material-symbols-outlined mb-4">sync</div>
        <p className="font-mono-code text-xs uppercase tracking-widest text-on-surface-variant">Accessing Peer Submissions registry...</p>
      </div>
    );
  }

  if (viewMode === "empty") {
    return (
      <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6 text-on-surface select-none animate-fade-in">
        <div className="max-w-md bg-[#16181D] border border-outline-variant p-8 text-center space-y-6">
          <span className="material-symbols-outlined text-[36px] text-primary">reviews</span>
          <h2 className="font-headline-md text-on-surface uppercase">Reviews Complete</h2>
          <p className="text-xs text-on-surface-variant leading-relaxed">
            There are currently no outstanding peer reviews waiting in your queue. Return to your dashboard to continue.
          </p>
          <button 
            onClick={() => navigate('/dashboard')}
            className="w-full bg-[#3B82F6] hover:bg-[#2563EB] text-white py-3.5 text-label-caps uppercase tracking-wider transition-colors rounded-none"
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
            PEER ASSESSMENT WORKSPACE
          </span>
        </div>
      </header>

      {/* Main Workspace Layout */}
      <main className="flex-1 grid grid-cols-12 overflow-hidden h-[calc(100vh-48px)]">
        {/* Sidebar list: Pending Reviews */}
        <section className="col-span-3 border-r border-outline-variant flex flex-col overflow-y-auto bg-surface-container-low text-left">
          <div className="p-4 border-b border-outline-variant bg-surface-container">
            <span className="font-label-caps text-[10px] text-on-surface-variant uppercase tracking-wider">Awaiting Peer Review</span>
          </div>
          
          <div className="divide-y divide-outline-variant/30 functional-border border-x-0 border-b-0">
            {pendingSubmissions.map(sub => (
              <div 
                key={sub.id} 
                onClick={() => handleSelectSubmission(sub)}
                className={`p-4 flex flex-col gap-1 cursor-pointer transition-colors ${
                  selectedSubmission && selectedSubmission.id === sub.id 
                    ? 'bg-surface-container-highest border-l-2 border-primary' 
                    : 'hover:bg-surface-container-high'
                }`}
              >
                <div className="flex justify-between items-start">
                  <span className="font-body-md font-bold text-on-surface">Submission #{sub.id}</span>
                  <span className="px-1.5 py-0.2 border border-primary/30 text-primary font-mono-code text-[9px] bg-primary/5">
                    {sub.stageName}
                  </span>
                </div>
                <span className="text-[11px] text-on-surface-variant truncate font-semibold block">{sub.conceptName}</span>
                <span className="font-mono-code text-[10px] text-on-surface-variant uppercase mt-1">
                  Submitted: {new Date(sub.submittedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </span>
              </div>
            ))}
          </div>
        </section>

        {/* Center: Selected submission details and reviewer form */}
        {selectedSubmission && (
          <section className="col-span-9 grid grid-cols-12 overflow-hidden">
            {/* Left Col: Peer code and document */}
            <div className="col-span-7 flex flex-col border-r border-outline-variant bg-surface-container-lowest overflow-y-auto">
              <div className="flex justify-between items-center border-b border-outline-variant px-4 py-2 bg-surface-container text-left">
                <span className="font-mono-code text-[11px] text-on-surface-variant uppercase">Peer Submission Code Excerpt</span>
                <span className="font-mono-code text-[11px] text-primary uppercase">Read-Only</span>
              </div>
              
              <textarea
                readOnly
                value={selectedSubmission.code}
                className="w-full flex-grow-0 min-h-64 bg-[#09090B] font-mono-code text-xs text-[#a78bfa] p-4 outline-none border-b border-outline-variant/40 resize-none leading-relaxed select-all"
              />

              <div className="flex justify-between items-center border-b border-outline-variant px-4 py-2 bg-surface-container text-left">
                <span className="font-mono-code text-[11px] text-on-surface-variant uppercase">Architecture Document</span>
              </div>
              
              <div className="flex-1 p-6 text-left overflow-y-auto bg-surface-container-lowest font-body-md text-body-md leading-relaxed text-on-surface-variant prose prose-invert whitespace-pre-line">
                {selectedSubmission.architectureDocument}
              </div>
            </div>

            {/* Right Col: Grading Form */}
            <div className="col-span-5 flex flex-col bg-surface-container overflow-y-auto p-6 text-left">
              <div className="border-b border-outline-variant/30 pb-4 mb-6">
                <span className="font-mono-code text-[10px] text-primary uppercase block">Gate Evaluation</span>
                <h3 className="font-headline-md text-on-surface uppercase">Constructive Review</h3>
              </div>

              <form onSubmit={handleSubmitReview} className="space-y-6 flex-1 flex flex-col justify-between">
                <div className="space-y-5">
                  {/* Score Slider */}
                  <div className="space-y-2">
                    <div className="flex justify-between items-center text-xs">
                      <span className="font-label-caps text-label-caps text-on-surface-variant uppercase">Quality Score</span>
                      <span className="font-mono-code text-primary font-bold text-sm">{score}/100</span>
                    </div>
                    <input 
                      type="range" 
                      min="10" 
                      max="100" 
                      value={score} 
                      onChange={e => setScore(parseInt(e.target.value))}
                      className="w-full h-1 bg-surface-container-highest rounded-lg appearance-none cursor-pointer accent-primary"
                    />
                    <div className="flex justify-between font-mono-code text-[9px] text-on-surface-variant uppercase">
                      <span>Rework Needed</span>
                      <span>Passed</span>
                      <span>Distinction</span>
                    </div>
                  </div>

                  {/* Feedback area */}
                  <div className="space-y-2">
                    <label className="block font-label-caps text-label-caps text-on-surface-variant uppercase" htmlFor="review_comments">
                      Actionable Review Comments
                    </label>
                    <textarea
                      required
                      id="review_comments"
                      value={comments}
                      onChange={e => setComments(e.target.value)}
                      placeholder="Comment on variable naming consistency, design pattern selection, and structural performance deficits..."
                      rows={8}
                      className="w-full bg-[#09090B] border border-outline-variant p-4 font-mono-code text-xs focus:border-primary outline-none transition-colors rounded-none resize-none"
                    />
                    <span className="text-[10px] text-on-surface-variant leading-relaxed block uppercase font-mono-code">
                      * Reviews are periodically audited by Senior engineers. Actionable metrics enforced.
                    </span>
                  </div>
                </div>

                <div className="pt-8">
                  <button
                    type="submit"
                    disabled={submitting}
                    className="w-full bg-[#3B82F6] hover:bg-[#2563EB] disabled:opacity-50 disabled:cursor-wait text-white py-4 font-label-caps text-label-caps uppercase tracking-widest transition-colors rounded-none border-none flex items-center justify-center gap-2"
                  >
                    {submitting ? (
                      <>
                        <span>Submitting Grade...</span>
                        <span className="material-symbols-outlined text-[16px] animate-spin">sync</span>
                      </>
                    ) : (
                      <>
                        <span>Submit Peer Review</span>
                        <span className="material-symbols-outlined text-[18px]">publish</span>
                      </>
                    )}
                  </button>
                </div>
              </form>
            </div>
          </section>
        )}
      </main>
    </div>
  );
}
