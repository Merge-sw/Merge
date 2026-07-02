import React from 'react';

export default function MergeLogo({ className = "font-display font-extrabold uppercase tracking-widest text-[32px]" }) {
  return (
    <span className={className}>
      <span 
        className="bg-gradient-to-r from-[#1E40AF] via-[#3B82F6] to-[#60A5FA] bg-clip-text text-transparent"
        style={{ WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}
      >
        M
      </span>
      ERGE
    </span>
  );
}
