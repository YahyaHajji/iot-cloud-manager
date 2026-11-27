import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { login } from "./auth";
import { dataMode, apiErrorMessage } from "./servicesLayer";

export default function Login() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const navigate = useNavigate();
  async function submit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      await login(username, password);
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setBusy(false);
    }
  }
  return (
    <main className="login-page">
      <form className="card login-card" onSubmit={submit}>
        <div className="brand-icon">IoT</div>
        <div className="eyebrow">ESPACE DE TRAVAIL</div>
        <h1>Connexion</h1>
        <p>Connectez-vous à IoT Cloud Manager.</p>
        <label className="field">
          Nom d’utilisateur
          <input
            autoComplete="username"
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
        </label>
        <label className="field">
          Mot de passe
          <input
            type="password"
            autoComplete="current-password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        {error && <div className="form-error">{error}</div>}
        <button className="button primary" disabled={busy}>
          {busy ? "Connexion…" : "Se connecter"}
        </button>
        {dataMode === "mock" && (
          <small>
            Mode démo : utilisez un nom d’utilisateur et un mot de passe de
            votre choix.
          </small>
        )}
      </form>
    </main>
  );
}
