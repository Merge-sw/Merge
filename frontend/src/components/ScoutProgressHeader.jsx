import React from 'react';

export default function ScoutProgressHeader({ currentStep }) {
  return (
    <div className="w-full flex border border-outline-variant font-mono-code text-[11px] uppercase select-none mb-8 bg-surface-container-low">
      <div className={`flex-1 p-3 text-center border-r border-outline-variant ${currentStep === 1 ? "bg-primary text-on-primary font-bold" : "text-on-surface-variant opacity-60"}`}>
        01 / Background
      </div>
      <div className={`flex-1 p-3 text-center border-r border-outline-variant ${currentStep === 2 ? "bg-primary text-on-primary font-bold" : "text-on-surface-variant opacity-60"}`}>
        02 / Cognitive
      </div>
      <div className={`flex-1 p-3 text-center ${currentStep === 3 ? "bg-primary text-on-primary font-bold" : "text-on-surface-variant opacity-60"}`}>
        03 / Baseline
      </div>
    </div>
  );
}
