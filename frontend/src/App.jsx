import React from 'react';
import { HashRouter as Router, Routes, Route, Navigate } from 'react-router-dom';

// Import Modular screens
import RegisterScreen from './screens/RegisterScreen';
import LoginScreen from './screens/LoginScreen';
import GitHubConnectScreen from './screens/GitHubConnectScreen';
import GeminiTokenSetupScreen from './screens/GeminiTokenSetupScreen';
import ScoutIntroScreen from './screens/ScoutIntroScreen';
import ScoutLayer1Screen from './screens/ScoutLayer1Screen';
import ScoutLayer2Screen from './screens/ScoutLayer2Screen';
import ScoutLayer3Screen from './screens/ScoutLayer3Screen';
import ScoutCompleteScreen from './screens/ScoutCompleteScreen';
import CadetWorkspaceScreen from './screens/CadetWorkspaceScreen';
import BuildWorkspaceScreen from './screens/BuildWorkspaceScreen';
import BuildComprehensionScreen from './screens/BuildComprehensionScreen';
import StagePromotionScreen from './screens/StagePromotionScreen';
import EngineeringIdentityScreen from './screens/EngineeringIdentityScreen';
import SessionStartScreen from './screens/SessionStartScreen';
import DrillResultScreen from './screens/DrillResultScreen';
import PeerReviewScreen from './screens/PeerReviewScreen';
import DashboardScreen from './screens/DashboardScreen';

export default function App() {
  // If the JWT token exists in storage, default landing route shifts to the main next task workspace
  const initialRoute = localStorage.getItem('merge_jwt') ? "/workspace" : "/register";

  return (
    <Router>
      <Routes>
        <Route path="/register" element={<RegisterScreen />} />
        <Route path="/login" element={<LoginScreen />} />
        <Route path="/connect/github" element={<GitHubConnectScreen />} />
        <Route path="/setup/gemini" element={<GeminiTokenSetupScreen />} />
        <Route path="/scout" element={<ScoutIntroScreen />} />
        <Route path="/scout/layer-1" element={<ScoutLayer1Screen />} />
        <Route path="/scout/layer-2" element={<ScoutLayer2Screen />} />
        <Route path="/scout/layer-3" element={<ScoutLayer3Screen />} />
        <Route path="/scout/complete" element={<ScoutCompleteScreen />} />
        <Route path="/workspace" element={<CadetWorkspaceScreen />} />
        <Route path="/build-workspace" element={<BuildWorkspaceScreen />} />
        <Route path="/build-comprehension/:id" element={<BuildComprehensionScreen />} />
        <Route path="/promote" element={<StagePromotionScreen />} />
        <Route path="/identity" element={<EngineeringIdentityScreen />} />
        <Route path="/session/start" element={<SessionStartScreen />} />
        <Route path="/drill-result/:id" element={<DrillResultScreen />} />
        <Route path="/peer-review" element={<PeerReviewScreen />} />
        <Route path="/dashboard" element={<DashboardScreen />} />
        <Route path="*" element={<Navigate to={initialRoute} replace />} />
      </Routes>
    </Router>
  );
}







