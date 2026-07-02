import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import MergeLogo from '../components/MergeLogo';

export default function LoginScreen() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errorMsg, setErrorMsg] = useState("");

  const handleSubmit = (e) => {
    e.preventDefault();
    if (email === "") {
      setErrorMsg("Please enter your email address.");
      return;
    }
    if (password.length < 6) {
      setErrorMsg("Password must be at least 6 characters.");
      return;
    }
    setErrorMsg("");

    localStorage.setItem('merge_jwt', 'mock-jwt-token');
    const existingStudent = localStorage.getItem('merge_student');
    if (!existingStudent) {
      localStorage.setItem('merge_student', JSON.stringify({
        fullName: "David Park",
        email: email,
        phone: "+2348011223344",
        uniEmail: "david@unilag.edu.ng",
        total_xp: 1250,
        current_stage: "SCOUT"
      }));
    }

    navigate('/connect/github');
  };

  return (
    <div className="min-h-screen flex items-center justify-center font-body-md text-body-md p-margin-md bg-[#10131a] text-[#e1e2ec]">
      <main className="w-full max-w-[480px] flex flex-col items-center">
        <header className="w-full mb-10 text-center">
          <MergeLogo className="font-display font-extrabold uppercase tracking-widest text-[36px]" />
        </header>

        <div className="w-full bg-[#16181D] border border-outline-variant p-8 md:p-12">
          <form className="space-y-6" onSubmit={handleSubmit}>
            <div className="space-y-2">
              <label className="block font-label-caps text-label-caps text-on-surface-variant uppercase" htmlFor="login_email">Email Address / ID</label>
              <div className="relative">
                <input 
                  className="w-full bg-[#09090B] border border-outline-variant text-on-surface font-mono-code text-mono-code p-3 focus:border-primary transition-colors duration-150 rounded-none outline-none" 
                  id="login_email" 
                  type="email" 
                  value={email}
                  onChange={e => setEmail(e.target.value)}
                  required
                />
              </div>
            </div>

            <div className="space-y-2">
              <label className="block font-label-caps text-label-caps text-on-surface-variant uppercase" htmlFor="login_password">Password</label>
              <div className="relative">
                <input 
                  className="w-full bg-[#09090B] border border-outline-variant text-on-surface font-mono-code text-mono-code p-3 focus:border-primary transition-colors duration-150 rounded-none outline-none" 
                  id="login_password" 
                  type="password" 
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  required
                />
              </div>
              {errorMsg && (
                <div className="flex items-center space-x-2 pt-1">
                  <span className="text-error font-mono-code text-[11px]">{errorMsg}</span>
                </div>
              )}
            </div>

            <div className="pt-4">
              <button className="w-full bg-[#3B82F6] hover:bg-[#2563EB] text-white font-headline-md text-headline-md py-4 transition-colors duration-150 rounded-none uppercase tracking-wide" type="submit">
                Log In
              </button>
            </div>
          </form>
        </div>

        <footer className="mt-8 text-center">
          <p className="font-body-md text-body-md text-on-surface-variant">
            Don't have an account?{" "}
            <button className="text-primary hover:underline transition-all duration-150 font-semibold bg-transparent border-none p-0 cursor-pointer" onClick={() => navigate('/register')}>
              Create Account
            </button>
          </p>
        </footer>
      </main>
    </div>
  );
}
