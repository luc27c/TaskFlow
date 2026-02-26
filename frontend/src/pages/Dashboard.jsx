import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { workflowApi, googleApi } from '../services/api';

function Dashboard() {
  const [workflows, setWorkflows] = useState([]);
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(true);
  const [gmailConnected, setGmailConnected] = useState(false);
  const [runningWorkflow, setRunningWorkflow] = useState(null);
  const [formData, setFormData] = useState({
    name: '',
    triggerType: 'SCHEDULE',
    actionType: 'EMAIL_RECAP',
    // Schedule options
    scheduleTime: '09:00',
    scheduleFrequency: 'daily', // daily, weekdays, weekly
    // EMAIL_RECAP config
    hoursBack: 18,
    // SEND_EMAIL config
    emailTo: '',
    emailSubject: '',
    emailBody: '',
  });
  const navigate = useNavigate();

  const user = JSON.parse(localStorage.getItem('user') || '{}');

  useEffect(() => {
    loadWorkflows();
    checkGmailStatus();

    // Listen for Gmail connection popup
    const handleMessage = (event) => {
      if (event.data === 'gmail-connected') {
        setGmailConnected(true);
      }
    };
    window.addEventListener('message', handleMessage);
    return () => window.removeEventListener('message', handleMessage);
  }, []);

  const handleAuthError = () => {
    // Token is invalid - clear it and redirect to login
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  const loadWorkflows = async () => {
    try {
      const data = await workflowApi.getAll();
      setWorkflows(data);
    } catch (err) {
      console.error('Failed to load workflows:', err);
      // If unauthorized, redirect to login
      if (err.message.includes('401') || err.message.includes('403') || err.message.includes('Failed')) {
        handleAuthError();
        return;
      }
    } finally {
      setLoading(false);
    }
  };

  const checkGmailStatus = async () => {
    try {
      const status = await googleApi.getStatus();
      setGmailConnected(status.connected);
    } catch (err) {
      console.error('Failed to check Gmail status:', err);
      // Don't redirect here - loadWorkflows will handle auth errors
    }
  };

  const handleConnectGmail = async () => {
    try {
      const { authUrl } = await googleApi.getAuthUrl();
      window.open(authUrl, 'gmail-auth', 'width=600,height=700');
    } catch (err) {
      console.error('Failed to get auth URL');
    }
  };

  const handleDisconnectGmail = async () => {
    try {
      await googleApi.disconnect();
      setGmailConnected(false);
    } catch (err) {
      console.error('Failed to disconnect Gmail');
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  // Convert form schedule settings to cron expression (6 fields: sec min hour day month dayOfWeek)
  const buildCronExpression = () => {
    const [hours, minutes] = formData.scheduleTime.split(':').map(Number);
    switch (formData.scheduleFrequency) {
      case 'weekdays':
        return `0 ${minutes} ${hours} * * MON-FRI`;
      case 'weekly':
        return `0 ${minutes} ${hours} * * MON`;
      case 'daily':
      default:
        return `0 ${minutes} ${hours} * * *`;
    }
  };

  // Build actionConfig JSON based on action type
  const buildActionConfig = () => {
    if (formData.actionType === 'EMAIL_RECAP') {
      return JSON.stringify({ hoursBack: formData.hoursBack });
    } else if (formData.actionType === 'SEND_EMAIL') {
      return JSON.stringify({
        to: formData.emailTo,
        subject: formData.emailSubject,
        body: formData.emailBody,
      });
    }
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const workflowData = {
        name: formData.name,
        triggerType: formData.triggerType,
        actionType: formData.actionType,
        cronExpression: formData.triggerType === 'SCHEDULE' ? buildCronExpression() : null,
        actionConfig: buildActionConfig(),
        active: true,
      };

      if (editingId) {
        await workflowApi.update(editingId, workflowData);
      } else {
        await workflowApi.create(workflowData);
      }
      setShowForm(false);
      setEditingId(null);
      resetFormData();
      loadWorkflows();
    } catch (err) {
      console.error('Failed to save workflow');
    }
  };

  const resetFormData = () => {
    setFormData({
      name: '',
      triggerType: 'SCHEDULE',
      actionType: 'EMAIL_RECAP',
      scheduleTime: '09:00',
      scheduleFrequency: 'daily',
      hoursBack: 18,
      emailTo: '',
      emailSubject: '',
      emailBody: '',
    });
  };

  // Parse cron expression back to time and frequency (handles both 5 and 6 field formats)
  const parseCronExpression = (cron) => {
    if (!cron) return { time: '09:00', frequency: 'daily' };
    const parts = cron.split(' ');

    // Handle 5-field (min hour day month dow) or 6-field (sec min hour day month dow) cron
    let minutes, hours, dayOfWeek;
    if (parts.length === 5) {
      minutes = parts[0].padStart(2, '0');
      hours = parts[1].padStart(2, '0');
      dayOfWeek = parts[4];
    } else if (parts.length >= 6) {
      minutes = parts[1].padStart(2, '0');
      hours = parts[2].padStart(2, '0');
      dayOfWeek = parts[5];
    } else {
      return { time: '09:00', frequency: 'daily' };
    }

    let frequency = 'daily';
    if (dayOfWeek === 'MON-FRI') frequency = 'weekdays';
    else if (dayOfWeek === 'MON') frequency = 'weekly';

    return { time: `${hours}:${minutes}`, frequency };
  };

  const handleEdit = (workflow) => {
    const { time, frequency } = parseCronExpression(workflow.cronExpression);
    let config = {};
    try {
      config = workflow.actionConfig ? JSON.parse(workflow.actionConfig) : {};
    } catch (e) {
      config = {};
    }

    setFormData({
      name: workflow.name,
      triggerType: workflow.triggerType,
      actionType: workflow.actionType,
      scheduleTime: time,
      scheduleFrequency: frequency,
      hoursBack: config.hoursBack || 18,
      emailTo: config.to || '',
      emailSubject: config.subject || '',
      emailBody: config.body || '',
    });
    setEditingId(workflow.id);
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (window.confirm('Are you sure you want to delete this workflow?')) {
      try {
        await workflowApi.delete(id);
        loadWorkflows();
      } catch (err) {
        console.error('Failed to delete workflow');
      }
    }
  };

  const handleRun = async (id) => {
    if (!gmailConnected) {
      alert('Please connect your Gmail account first');
      return;
    }
    setRunningWorkflow(id);
    try {
      await workflowApi.run(id);
      alert('Workflow executed successfully! Check your email.');
      loadWorkflows();
    } catch (err) {
      alert('Failed to run workflow: ' + err.message);
    } finally {
      setRunningWorkflow(null);
    }
  };

  return (
    <div className="min-h-screen bg-amber-50">
      {/* Navbar */}
      <nav className="bg-white border-b border-amber-100 shadow-sm">
        <div className="max-w-4xl mx-auto px-4 sm:px-6">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-teal-600 rounded-xl flex items-center justify-center shadow-sm">
                <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
              </div>
              <span className="text-xl font-bold text-stone-800">TaskFlow</span>
            </div>
            <div className="flex items-center gap-4">
              <div className="flex items-center gap-2">
                <div className="w-8 h-8 bg-teal-100 rounded-full flex items-center justify-center">
                  <span className="text-teal-700 text-sm font-semibold">
                    {user.firstName?.[0]}{user.lastName?.[0]}
                  </span>
                </div>
                <span className="text-stone-600 text-sm font-medium">{user.firstName}</span>
              </div>
              <button
                onClick={handleLogout}
                className="px-3 py-1.5 text-stone-500 hover:text-stone-700 hover:bg-stone-100 rounded-lg text-sm transition"
              >
                Log out
              </button>
            </div>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="max-w-4xl mx-auto px-4 sm:px-6 py-8">
        {/* Gmail Connection Card */}
        <div className={`mb-8 p-5 rounded-2xl border-2 ${gmailConnected ? 'bg-teal-50 border-teal-200' : 'bg-orange-50 border-orange-200'}`}>
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-4">
              <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${gmailConnected ? 'bg-teal-100' : 'bg-orange-100'}`}>
                <svg className={`w-6 h-6 ${gmailConnected ? 'text-teal-600' : 'text-orange-500'}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                </svg>
              </div>
              <div>
                <h3 className={`font-semibold ${gmailConnected ? 'text-teal-800' : 'text-orange-800'}`}>
                  {gmailConnected ? 'Gmail Connected' : 'Connect your Gmail'}
                </h3>
                <p className={`text-sm ${gmailConnected ? 'text-teal-600' : 'text-orange-600'}`}>
                  {gmailConnected
                    ? 'Ready to send email recaps!'
                    : 'Required for email recap workflows'}
                </p>
              </div>
            </div>
            {gmailConnected ? (
              <button
                onClick={handleDisconnectGmail}
                className="px-4 py-2 bg-white border border-teal-300 hover:bg-teal-50 text-teal-700 rounded-xl text-sm font-medium transition"
              >
                Disconnect
              </button>
            ) : (
              <button
                onClick={handleConnectGmail}
                className="px-5 py-2.5 bg-red-500 hover:bg-red-600 text-white text-sm font-semibold rounded-xl transition flex items-center gap-2 shadow-sm"
              >
                <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M24 5.457v13.909c0 .904-.732 1.636-1.636 1.636h-3.819V11.73L12 16.64l-6.545-4.91v9.273H1.636A1.636 1.636 0 0 1 0 19.366V5.457c0-2.023 2.309-3.178 3.927-1.964L5.455 4.64 12 9.548l6.545-4.91 1.528-1.145C21.69 2.28 24 3.434 24 5.457z"/>
                </svg>
                Connect Gmail
              </button>
            )}
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-4 mb-8">
          <div className="bg-white rounded-2xl p-5 border border-stone-100 shadow-sm">
            <p className="text-stone-400 text-sm font-medium">Total</p>
            <p className="text-3xl font-bold text-stone-800 mt-1">{workflows.length}</p>
          </div>
          <div className="bg-white rounded-2xl p-5 border border-stone-100 shadow-sm">
            <p className="text-stone-400 text-sm font-medium">Active</p>
            <p className="text-3xl font-bold text-teal-600 mt-1">{workflows.filter(w => w.active).length}</p>
          </div>
          <div className="bg-white rounded-2xl p-5 border border-stone-100 shadow-sm">
            <p className="text-stone-400 text-sm font-medium">Recaps</p>
            <p className="text-3xl font-bold text-stone-800 mt-1">{workflows.filter(w => w.actionType === 'EMAIL_RECAP').length}</p>
          </div>
        </div>

        {/* Header */}
        <div className="flex items-center justify-between mb-5">
          <div>
            <h1 className="text-2xl font-bold text-stone-800">My Workflows</h1>
            <p className="text-stone-500 mt-1">Automate your email digests</p>
          </div>
          <button
            onClick={() => { setShowForm(true); setEditingId(null); resetFormData(); }}
            className="px-5 py-2.5 bg-teal-600 hover:bg-teal-700 text-white font-semibold rounded-xl transition flex items-center gap-2 shadow-sm"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
            </svg>
            New Workflow
          </button>
        </div>

        {/* Workflow List */}
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <svg className="animate-spin h-8 w-8 text-teal-500" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
          </div>
        ) : workflows.length === 0 ? (
          <div className="bg-white rounded-2xl border border-stone-100 p-12 text-center shadow-sm">
            <div className="w-16 h-16 bg-amber-100 rounded-2xl flex items-center justify-center mx-auto mb-4">
              <svg className="w-8 h-8 text-amber-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
              </svg>
            </div>
            <h3 className="text-xl font-semibold text-stone-800 mb-2">No workflows yet</h3>
            <p className="text-stone-500 mb-6">Create your first workflow to start getting email summaries</p>
            <button
              onClick={() => setShowForm(true)}
              className="px-6 py-3 bg-teal-600 hover:bg-teal-700 text-white font-semibold rounded-xl transition shadow-sm"
            >
              Create your first workflow
            </button>
          </div>
        ) : (
          <div className="space-y-4">
            {workflows.map((workflow) => (
              <div key={workflow.id} className="bg-white rounded-2xl border border-stone-100 p-5 hover:shadow-md transition shadow-sm">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-4">
                    <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${workflow.active ? 'bg-teal-100' : 'bg-stone-100'}`}>
                      <svg className={`w-6 h-6 ${workflow.active ? 'text-teal-600' : 'text-stone-400'}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
                      </svg>
                    </div>
                    <div>
                      <h3 className="font-semibold text-stone-800 text-lg">{workflow.name}</h3>
                      <div className="flex items-center gap-2 mt-1 flex-wrap">
                        <span className="text-stone-500 text-sm">
                          {workflow.triggerType === 'SCHEDULE' ? (() => {
                            const { time, frequency } = parseCronExpression(workflow.cronExpression);
                            const freqLabels = { daily: 'Daily', weekdays: 'Weekdays', weekly: 'Weekly' };
                            return `${freqLabels[frequency] || 'Daily'} at ${time}`;
                          })() : 'Manual only'}
                        </span>
                        <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${workflow.active ? 'bg-teal-100 text-teal-700' : 'bg-stone-100 text-stone-500'}`}>
                          {workflow.active ? 'Active' : 'Paused'}
                        </span>
                        {workflow.lastRunAt && (
                          <span className="text-stone-400 text-sm">
                            Last run {new Date(workflow.lastRunAt).toLocaleDateString()}
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => handleRun(workflow.id)}
                      disabled={runningWorkflow === workflow.id}
                      className="px-4 py-2 bg-teal-600 hover:bg-teal-700 disabled:bg-teal-400 text-white text-sm font-semibold rounded-xl transition flex items-center gap-2 shadow-sm"
                    >
                      {runningWorkflow === workflow.id ? (
                        <>
                          <svg className="animate-spin h-4 w-4" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                          </svg>
                          Running...
                        </>
                      ) : (
                        <>
                          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
                          </svg>
                          Run
                        </>
                      )}
                    </button>
                    <button
                      onClick={() => handleEdit(workflow)}
                      className="p-2 text-stone-400 hover:text-teal-600 hover:bg-teal-50 rounded-xl transition"
                    >
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" />
                      </svg>
                    </button>
                    <button
                      onClick={() => handleDelete(workflow.id)}
                      className="p-2 text-stone-400 hover:text-red-500 hover:bg-red-50 rounded-xl transition"
                    >
                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                      </svg>
                    </button>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </main>

      {/* Modal */}
      {showForm && (
        <div className="fixed inset-0 bg-stone-900/50 flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl p-7 w-full max-w-md shadow-xl max-h-[90vh] overflow-y-auto">
            <h2 className="text-xl font-bold text-stone-800 mb-1">
              {editingId ? 'Edit Workflow' : 'New Workflow'}
            </h2>
            <p className="text-stone-500 text-sm mb-6">Set up your automated email recap</p>

            <form onSubmit={handleSubmit} className="space-y-5">
              <div>
                <label className="block text-stone-700 text-sm font-medium mb-1.5">Name</label>
                <input
                  type="text"
                  value={formData.name}
                  onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                  className="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-stone-800 placeholder-stone-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 focus:bg-white transition"
                  placeholder="Morning Email Digest"
                  required
                />
              </div>

              <div>
                <label className="block text-stone-700 text-sm font-medium mb-1.5">What should it do?</label>
                <select
                  value={formData.actionType}
                  onChange={(e) => setFormData({ ...formData, actionType: e.target.value })}
                  className="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-stone-800 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 focus:bg-white transition"
                >
                  <option value="EMAIL_RECAP">Send me an email recap</option>
                  <option value="SEND_EMAIL">Send a reminder email</option>
                </select>
              </div>

              {/* EMAIL_RECAP specific options */}
              {formData.actionType === 'EMAIL_RECAP' && (
                <div>
                  <label className="block text-stone-700 text-sm font-medium mb-1.5">Summarize emails from</label>
                  <select
                    value={formData.hoursBack}
                    onChange={(e) => setFormData({ ...formData, hoursBack: Number(e.target.value) })}
                    className="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-stone-800 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 focus:bg-white transition"
                  >
                    <option value={6}>Last 6 hours</option>
                    <option value={12}>Last 12 hours</option>
                    <option value={18}>Last 18 hours</option>
                    <option value={24}>Last 24 hours</option>
                    <option value={48}>Last 2 days</option>
                    <option value={72}>Last 3 days</option>
                  </select>
                </div>
              )}

              {/* SEND_EMAIL specific options */}
              {formData.actionType === 'SEND_EMAIL' && (
                <>
                  <div>
                    <label className="block text-stone-700 text-sm font-medium mb-1.5">Send To</label>
                    <input
                      type="email"
                      value={formData.emailTo}
                      onChange={(e) => setFormData({ ...formData, emailTo: e.target.value })}
                      className="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-stone-800 placeholder-stone-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 focus:bg-white transition"
                      placeholder="recipient@example.com"
                      required
                    />
                    <p className="text-stone-400 text-xs mt-1.5">Leave empty to send to yourself</p>
                  </div>
                  <div>
                    <label className="block text-stone-700 text-sm font-medium mb-1.5">Subject</label>
                    <input
                      type="text"
                      value={formData.emailSubject}
                      onChange={(e) => setFormData({ ...formData, emailSubject: e.target.value })}
                      className="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-stone-800 placeholder-stone-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 focus:bg-white transition"
                      placeholder="Daily Reminder"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-stone-700 text-sm font-medium mb-1.5">Message</label>
                    <textarea
                      value={formData.emailBody}
                      onChange={(e) => setFormData({ ...formData, emailBody: e.target.value })}
                      className="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-stone-800 placeholder-stone-400 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 focus:bg-white transition resize-none"
                      placeholder="Don't forget to..."
                      rows={4}
                      required
                    />
                  </div>
                </>
              )}

              <div>
                <label className="block text-stone-700 text-sm font-medium mb-1.5">When should it run?</label>
                <select
                  value={formData.triggerType}
                  onChange={(e) => setFormData({ ...formData, triggerType: e.target.value })}
                  className="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-stone-800 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 focus:bg-white transition"
                >
                  <option value="SCHEDULE">On a schedule</option>
                  <option value="MANUAL">Only when I click Run</option>
                </select>
              </div>

              {/* Schedule options */}
              {formData.triggerType === 'SCHEDULE' && (
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="block text-stone-700 text-sm font-medium mb-1.5">Time</label>
                    <input
                      type="time"
                      value={formData.scheduleTime}
                      onChange={(e) => setFormData({ ...formData, scheduleTime: e.target.value })}
                      className="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-stone-800 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 focus:bg-white transition"
                    />
                  </div>
                  <div>
                    <label className="block text-stone-700 text-sm font-medium mb-1.5">Repeat</label>
                    <select
                      value={formData.scheduleFrequency}
                      onChange={(e) => setFormData({ ...formData, scheduleFrequency: e.target.value })}
                      className="w-full px-4 py-2.5 bg-stone-50 border border-stone-200 rounded-xl text-stone-800 focus:outline-none focus:ring-2 focus:ring-teal-500 focus:border-teal-500 focus:bg-white transition"
                    >
                      <option value="daily">Every day</option>
                      <option value="weekdays">Weekdays only</option>
                      <option value="weekly">Once a week</option>
                    </select>
                  </div>
                </div>
              )}

              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowForm(false)}
                  className="flex-1 px-4 py-3 bg-stone-100 hover:bg-stone-200 text-stone-700 font-semibold rounded-xl transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="flex-1 px-4 py-3 bg-teal-600 hover:bg-teal-700 text-white font-semibold rounded-xl transition shadow-sm"
                >
                  {editingId ? 'Save changes' : 'Create workflow'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default Dashboard;
