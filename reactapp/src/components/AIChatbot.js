import React, { useState, useEffect, useRef } from 'react';
import { chatbotAPI } from '../services/api';

function AIChatbot({ user }) {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([
    {
      id: 'welcome',
      sender: 'ai',
      text: `👋 Hello ${user?.name || user?.username || 'there'}! I am your **CarePortal AI Healthcare Assistant**.\n\nHow can I help you today? You can search for specialists, check doctor available slots, view your appointments, check prescriptions, or ask for guided booking assistance.`,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      quickChips: [
        '🔍 Find Doctors',
        '📅 Book Appointment',
        '📋 My Appointments',
        '⏳ My Waitlist',
        '💊 My Prescriptions',
        '🏥 Departments'
      ]
    }
  ]);
  const [inputValue, setInputValue] = useState('');
  const [loading, setLoading] = useState(false);
  const [conversationContext, setConversationContext] = useState({});
  const [conversationState, setConversationState] = useState('NONE');

  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
      setTimeout(() => inputRef.current?.focus(), 150);
    }
  }, [isOpen, messages]);

  if (!user) {
    return null; // Only show for authenticated users
  }

  const handleSendMessage = async (customText = null, payloadContext = null) => {
    const textToSend = (customText !== null ? customText : inputValue).trim();
    if (!textToSend && !payloadContext) return;

    const userMsg = {
      id: 'user-' + Date.now(),
      sender: 'user',
      text: textToSend,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    };

    setMessages((prev) => [...prev, userMsg]);
    if (customText === null) {
      setInputValue('');
    }
    setLoading(true);

    try {
      const mergedContext = { ...conversationContext, ...(payloadContext?.context || {}) };
      const req = {
        message: textToSend,
        conversationState: payloadContext?.conversationState || conversationState,
        selectedDoctorId: payloadContext?.selectedDoctorId || mergedContext.doctorId,
        selectedDate: payloadContext?.selectedDate || mergedContext.date,
        selectedTime: payloadContext?.selectedTime || mergedContext.time,
        reason: payloadContext?.reason || mergedContext.reason,
        appointmentIdToCancel: payloadContext?.appointmentIdToCancel,
        context: mergedContext
      };

      const res = await chatbotAPI.sendMessage(req);

      // Update state and context from response
      if (res.conversationState) {
        setConversationState(res.conversationState);
      }
      if (res.context) {
        setConversationContext((prev) => ({ ...prev, ...res.context }));
      } else if (res.conversationState === 'NONE') {
        setConversationContext({});
      }

      const aiMsg = {
        id: 'ai-' + Date.now(),
        sender: 'ai',
        text: res.message,
        replyType: res.replyType,
        doctors: res.doctors,
        availableSlots: res.availableSlots,
        doctorId: res.doctorId,
        doctorName: res.doctorName,
        appointmentDate: res.appointmentDate,
        appointmentTime: res.appointmentTime,
        reason: res.reason,
        appointments: res.appointments,
        prescriptions: res.prescriptions,
        departments: res.departments,
        waitlistEntries: res.waitlistEntries,
        waitlistEntry: res.waitlistEntry,
        nextAction: res.nextAction,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };

      setMessages((prev) => [...prev, aiMsg]);
    } catch (err) {
      console.error('Chatbot API error:', err);
      const errMsg = err.response?.data?.message || err.message || 'Error processing your message. Please try again.';
      setMessages((prev) => [
        ...prev,
        {
          id: 'ai-err-' + Date.now(),
          sender: 'ai',
          text: `⚠️ ${errMsg}`,
          isError: true,
          timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleClearChat = () => {
    setMessages([
      {
        id: 'welcome-' + Date.now(),
        sender: 'ai',
        text: `Conversation cleared. How can I help you?`,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        quickChips: [
          '🔍 Find Doctors',
          '📅 Book Appointment',
          '📋 My Appointments',
          '⏳ My Waitlist',
          '💊 My Prescriptions',
          '🏥 Departments'
        ]
      }
    ]);
    setConversationContext({});
    setConversationState('NONE');
  };

  const handleSelectDoctor = (doctor) => {
    handleSendMessage(`I want to book an appointment with Dr. ${doctor.fullName}`, {
      selectedDoctorId: doctor.id,
      context: { doctorId: doctor.id, doctorName: doctor.fullName }
    });
  };

  const handleSelectSlot = (slot) => {
    handleSendMessage(`I choose slot ${slot}`, {
      selectedTime: slot,
      conversationState: 'AWAITING_SLOT',
      context: { time: slot }
    });
  };

  const handleConfirmBooking = (confirm = true) => {
    if (confirm) {
      handleSendMessage('Yes, confirm and schedule this appointment.', {
        conversationState: 'AWAITING_CONFIRMATION'
      });
    } else {
      handleSendMessage('No, cancel this booking request.', {
        conversationState: 'AWAITING_CONFIRMATION'
      });
    }
  };

  const handleConfirmWaitlistJoin = (confirm = true) => {
    if (confirm) {
      handleSendMessage('Yes', {
        conversationState: 'AWAITING_WAITLIST_CONFIRMATION'
      });
    } else {
      handleSendMessage('No', {
        conversationState: 'AWAITING_WAITLIST_CONFIRMATION'
      });
    }
  };

  const handleCancelAppointment = (apptId) => {
    handleSendMessage(`Cancel appointment #${apptId}`, {
      appointmentIdToCancel: apptId
    });
  };

  const handleConfirmWaitlistSlot = () => {
    handleSendMessage('Confirm the available slot');
  };

  const handleCancelWaitlist = (id) => {
    handleSendMessage(`Cancel my waitlist #${id}`);
  };

  return (
    <div className="ai-chatbot-root">
      {/* FLOATING TRIGGER BUTTON */}
      {!isOpen && (
        <button
          className="ai-chatbot-trigger-btn"
          onClick={() => setIsOpen(true)}
          title="CarePortal AI Assistant"
          aria-label="Open AI Assistant"
        >
          <div className="ai-chatbot-pulse-ring" />
          <span className="ai-chatbot-trigger-icon">🤖</span>
          <span className="ai-chatbot-trigger-label">CarePortal AI</span>
        </button>
      )}

      {/* CHAT WINDOW */}
      {isOpen && (
        <div className="ai-chatbot-window">
          {/* HEADER */}
          <div className="ai-chatbot-header">
            <div className="ai-chatbot-header-info">
              <div className="ai-chatbot-avatar">🤖</div>
              <div>
                <h4 className="ai-chatbot-title">CarePortal Assistant</h4>
                <span className="ai-chatbot-status">
                  <span className="ai-status-dot" /> Online • AI Healthcare Assistant
                </span>
              </div>
            </div>
            <div className="ai-chatbot-header-actions">
              <button
                className="ai-chatbot-header-btn"
                onClick={handleClearChat}
                title="Clear conversation"
              >
                🗑️
              </button>
              <button
                className="ai-chatbot-header-btn"
                onClick={() => setIsOpen(false)}
                title="Close chat"
              >
                ✕
              </button>
            </div>
          </div>

          {/* MESSAGES BODY */}
          <div className="ai-chatbot-body">
            {messages.map((m) => (
              <div
                key={m.id}
                className={`ai-chat-bubble-row ${m.sender === 'user' ? 'user-row' : 'ai-row'}`}
              >
                {m.sender === 'ai' && <div className="ai-bubble-avatar">🤖</div>}

                <div className={`ai-chat-bubble ${m.sender === 'user' ? 'user-bubble' : 'ai-bubble'}`}>
                  {/* TEXT CONTENT */}
                  <div className="ai-bubble-text" style={{ whiteSpace: 'pre-wrap' }}>
                    {m.text}
                  </div>

                  {/* QUICK REPLY CHIPS */}
                  {m.quickChips && m.quickChips.length > 0 && (
                    <div className="ai-quick-chips">
                      {m.quickChips.map((chip, idx) => (
                        <button
                          key={idx}
                          className="ai-chip-btn"
                          onClick={() => handleSendMessage(chip.replace(/^[^\w]+/, ''))}
                        >
                          {chip}
                        </button>
                      ))}
                    </div>
                  )}

                  {/* DOCTOR CARDS */}
                  {m.doctors && m.doctors.length > 0 && (
                    <div className="ai-doctor-cards-grid">
                      {m.doctors.slice(0, 4).map((doc) => (
                        <div key={doc.id} className="ai-doctor-card">
                          <div className="ai-doctor-header">
                            <strong className="ai-doctor-name">Dr. {doc.fullName}</strong>
                            <span className="ai-doctor-badge">{doc.specialization}</span>
                          </div>
                          <div className="ai-doctor-details">
                            <div>🏥 {doc.departmentName} • {doc.qualification}</div>
                            <div>🕒 Available: {doc.availableFrom} - {doc.availableTo}</div>
                            <div className="ai-doctor-fee">Fee: ${doc.consultationFee?.toFixed(2)}</div>
                          </div>
                          <button
                            className="btn btn-sm btn-primary ai-doc-select-btn"
                            onClick={() => handleSelectDoctor(doc)}
                          >
                            📅 Select & Book
                          </button>
                        </div>
                      ))}
                    </div>
                  )}

                  {/* CLICKABLE AVAILABLE SLOTS */}
                  {m.availableSlots && m.availableSlots.length > 0 && (
                    <div className="ai-slots-container">
                      <div className="ai-slots-title">
                        🕒 Available Slots on {m.appointmentDate} (Click to select):
                      </div>
                      <div className="ai-slots-grid">
                        {m.availableSlots.map((slot) => (
                          <button
                            key={slot}
                            className="ai-slot-chip"
                            onClick={() => handleSelectSlot(slot)}
                          >
                            {slot}
                          </button>
                        ))}
                      </div>
                    </div>
                  )}

                  {/* WAITLIST OFFER PROMPT (JOIN WAITLIST YES / NO) */}
                  {m.replyType === 'WAITLIST_OFFER_PROMPT' && (
                    <div className="ai-confirmation-card" style={{ marginTop: '0.65rem' }}>
                      <div className="ai-confirmation-actions" style={{ display: 'flex', gap: '0.5rem' }}>
                        <button
                          className="btn btn-sm btn-primary"
                          onClick={() => handleConfirmWaitlistJoin(true)}
                          style={{ fontWeight: 600 }}
                        >
                          📋 Yes, Join Waitlist
                        </button>
                        <button
                          className="btn btn-sm btn-secondary"
                          onClick={() => handleConfirmWaitlistJoin(false)}
                        >
                          ✕ No Thanks
                        </button>
                      </div>
                    </div>
                  )}

                  {/* CONFIRMATION PROMPT ACTION CARD */}
                  {m.replyType === 'CONFIRMATION_PROMPT' && (
                    <div className="ai-confirmation-card">
                      <div className="ai-confirmation-actions">
                        <button
                          className="btn btn-sm btn-success ai-confirm-btn"
                          onClick={() => handleConfirmBooking(true)}
                        >
                          ✓ Confirm & Schedule
                        </button>
                        <button
                          className="btn btn-sm btn-secondary ai-cancel-btn"
                          onClick={() => handleConfirmBooking(false)}
                        >
                          ✕ Cancel
                        </button>
                      </div>
                    </div>
                  )}

                  {/* APPOINTMENTS LIST */}
                  {m.appointments && m.appointments.length > 0 && m.replyType === 'APPOINTMENT_LIST' && (
                    <div className="ai-appts-list">
                      {m.appointments.map((a) => (
                        <div key={a.id} className="ai-appt-card">
                          <div className="ai-appt-header">
                            <strong>Appt #{a.id} • Dr. {a.doctorName}</strong>
                            <span className={`badge badge-${a.status?.toLowerCase()}`}>{a.status}</span>
                          </div>
                          <div className="ai-appt-sub">
                            📅 {a.appointmentDate} at {a.appointmentTime} ({a.departmentName})
                          </div>
                          {a.status !== 'CANCELLED' && a.status !== 'COMPLETED' && a.status !== 'EXPIRED' && a.status !== 'NO_SHOW' && (
                            <button
                              className="btn btn-sm btn-danger ai-cancel-appt-btn"
                              onClick={() => handleCancelAppointment(a.id)}
                            >
                              Cancel Appt #{a.id}
                            </button>
                          )}
                        </div>
                      ))}
                    </div>
                  )}

                  {/* WAITLIST LIST */}
                  {m.waitlistEntries && m.waitlistEntries.length > 0 && (
                    <div className="ai-appts-list" style={{ marginTop: '0.65rem' }}>
                      {m.waitlistEntries.map((w) => {
                        const isNotified = w.status === 'NOTIFIED' || w.offerActive;
                        return (
                          <div
                            key={w.id}
                            className="ai-appt-card"
                            style={isNotified ? { borderLeft: '4px solid #f59e0b', backgroundColor: '#fffbeb' } : {}}
                          >
                            <div className="ai-appt-header">
                              <strong>Waitlist #{w.id} • Dr. {w.doctorName}</strong>
                              <span className={`badge badge-${w.status?.toLowerCase()}`}>
                                {w.status === 'NOTIFIED' ? '🔔 OFFER READY' : w.status}
                              </span>
                            </div>
                            <div className="ai-appt-sub">
                              📅 {w.appointmentDate} {w.preferredTime ? `• Pref: ${w.preferredTime.substring(0, 5)}` : '• Any time'}
                              {w.status === 'WAITING' && ` • Position: #${w.queuePosition || 1}`}
                              {isNotified && w.offeredTime && ` • Offered: ${w.offeredTime.substring(0, 5)}`}
                            </div>
                            <div style={{ display: 'flex', gap: '0.4rem', marginTop: '0.4rem' }}>
                              {isNotified && (
                                <button
                                  className="btn btn-sm btn-success"
                                  onClick={handleConfirmWaitlistSlot}
                                  style={{ padding: '0.2rem 0.55rem', fontSize: '0.75rem', fontWeight: 600 }}
                                >
                                  ✅ Confirm Slot
                                </button>
                              )}
                              {(w.status === 'WAITING' || isNotified) && (
                                <button
                                  className="btn btn-sm btn-secondary"
                                  onClick={() => handleCancelWaitlist(w.id)}
                                  style={{ padding: '0.2rem 0.55rem', fontSize: '0.75rem' }}
                                >
                                  Leave Waitlist
                                </button>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}

                  {/* TIMESTAMP */}
                  <span className="ai-bubble-time">{m.timestamp}</span>
                </div>
              </div>
            ))}

            {/* TYPING INDICATOR */}
            {loading && (
              <div className="ai-chat-bubble-row ai-row">
                <div className="ai-bubble-avatar">🤖</div>
                <div className="ai-chat-bubble ai-bubble ai-typing-bubble">
                  <div className="ai-typing-dots">
                    <span />
                    <span />
                    <span />
                  </div>
                </div>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* INPUT BAR */}
          <form
            className="ai-chatbot-input-bar"
            onSubmit={(e) => {
              e.preventDefault();
              handleSendMessage();
            }}
          >
            <input
              ref={inputRef}
              type="text"
              className="ai-chatbot-input"
              placeholder="Ask CarePortal AI or book a doctor..."
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
              disabled={loading}
            />
            <button
              type="submit"
              className="ai-chatbot-send-btn"
              disabled={loading || !inputValue.trim()}
              title="Send message"
            >
              ➤
            </button>
          </form>
        </div>
      )}
    </div>
  );
}

export default AIChatbot;
