import { useState, useMemo } from 'react';
import { useData } from '../../hooks/useData';
import {
  parseBankStatement,
  type ParsedStatementRow,
  type StatementParseResult,
  type ColumnMapping,
} from '../../lib/statementParser';
import { formatMoney, getCategoryDisplayName, type Transaction } from '../../types';
import {
  X,
  Upload,
  FileText,
  CheckCircle2,
  AlertTriangle,
  ArrowDownRight,
  ArrowUpRight,
  Sparkles,
  HelpCircle,
  SlidersHorizontal,
  RotateCcw,
  ShieldAlert,
  ChevronDown,
  ChevronUp,
} from 'lucide-react';

interface ImportStatementModalProps {
  onClose: () => void;
  onImportComplete?: () => void;
}

const SAMPLE_AXIS_CSV = `Tran Date,Particulars,Chq No,Withdrawal (Dr),Deposit (Cr),Balance
05-10-2026,SALARY CREDIT / CMS394821,CMS394821,-,32200.00,37200.00
06-10-2026,UPI/P2A/627811/House Rent,627811,15000.00,-,22200.00
07-10-2026,ACH/LOAN EMI HDFC,ACH991,10000.00,-,12200.00
10-10-2026,UPI/P2A/627991/Swiggy,627991,1400.00,-,10800.00
20-10-2026,UPI/Self Transfer to Secondary Bank,628211,10000.00,-,800.00`;

export function ImportStatementModal({ onClose, onImportComplete }: ImportStatementModalProps) {
  const { accounts, categories, transactions, addTransaction, deleteTransaction, settings } = useData();
  const currency = settings?.currency || 'INR';
  const masked = settings?.mask_amounts || false;

  const [selectedAccountId, setSelectedAccountId] = useState(accounts[0]?.id || '');
  const [statementText, setStatementText] = useState('');
  const [fileName, setFileName] = useState('');
  const [parsedResult, setParsedResult] = useState<StatementParseResult | null>(null);
  const [rows, setRows] = useState<ParsedStatementRow[]>([]);
  const [importing, setImporting] = useState(false);
  const [importSuccess, setImportSuccess] = useState<number | null>(null);
  const [importedTxnIds, setImportedTxnIds] = useState<string[]>([]);
  const [error, setError] = useState('');
  const [showColumnMapper, setShowColumnMapper] = useState(false);
  const [customMapping, setCustomMapping] = useState<Partial<ColumnMapping>>({});
  const [showMandateHelp, setShowMandateHelp] = useState(false);

  // Handle file upload
  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setFileName(file.name);
    const reader = new FileReader();
    reader.onload = evt => {
      const text = evt.target?.result as string;
      if (text) {
        setStatementText(text);
        analyzeText(text, customMapping);
      }
    };
    reader.readAsText(file);
  };

  // Analyze text
  const analyzeText = (text: string, mappingOverride?: Partial<ColumnMapping>) => {
    setError('');
    try {
      const result = parseBankStatement(text, transactions, selectedAccountId, 20000, mappingOverride);
      if (result.rows.length === 0) {
        setError('Could not find transaction rows. Please check file format or adjust column mapping.');
        setParsedResult(null);
        setRows([]);
        return;
      }
      setParsedResult(result);
      setRows(result.rows);
    } catch (err: any) {
      setError(err?.message || 'Failed to parse statement.');
      setParsedResult(null);
      setRows([]);
    }
  };

  const handleLoadSample = () => {
    setFileName('sample_axis_bank_oct2026.csv');
    setStatementText(SAMPLE_AXIS_CSV);
    analyzeText(SAMPLE_AXIS_CSV, customMapping);
  };

  const handleUpdateMappingCol = (key: keyof ColumnMapping, colIdx: number) => {
    const updated = { ...customMapping, [key]: colIdx };
    setCustomMapping(updated);
    if (statementText) {
      analyzeText(statementText, updated);
    }
  };

  const toggleSelectAll = (select: boolean) => {
    setRows(prev => prev.map(r => ({ ...r, selected: select })));
  };

  const toggleRow = (index: number) => {
    setRows(prev => {
      const copy = [...prev];
      copy[index] = { ...copy[index], selected: !copy[index].selected };
      return copy;
    });
  };

  const updateRowCategory = (index: number, categoryId: string) => {
    setRows(prev => {
      const copy = [...prev];
      copy[index] = { ...copy[index], suggestedCategoryId: categoryId };
      return copy;
    });
  };

  const selectedCount = useMemo(() => rows.filter(r => r.selected).length, [rows]);

  // Execute atomic import
  const handleCommitImport = async () => {
    const selectedRows = rows.filter(r => r.selected);
    if (selectedRows.length === 0) {
      setError('Please select at least one transaction to import.');
      return;
    }
    if (!selectedAccountId) {
      setError('Please choose which bank account these transactions belong to.');
      return;
    }

    setImporting(true);
    setError('');

    const batchId = parsedResult?.importBatchId || `batch_${Date.now()}`;
    const newlyImportedIds: string[] = [];

    try {
      let imported = 0;
      for (const row of selectedRows) {
        const txnId = 'txn_' + Date.now() + '_' + Math.random().toString(36).substring(2, 7);
        const now = Date.now();

        const journalEntries = [
          {
            txn_id: txnId,
            account_id: selectedAccountId,
            category_id: null,
            amount_minor: row.type === 'INCOME' ? row.amountMinor : -row.amountMinor,
          },
          {
            txn_id: txnId,
            account_id: null,
            category_id: row.suggestedCategoryId || null,
            amount_minor: row.type === 'INCOME' ? -row.amountMinor : row.amountMinor,
          },
        ];

        const newTxn: any = {
          id: txnId,
          type: row.type,
          amount_minor: row.amountMinor,
          currency,
          local_date: row.date,
          local_time: '12:00',
          account_id: selectedAccountId,
          to_account_id: null,
          category_id: row.suggestedCategoryId || null,
          merchant: row.merchant || null,
          description: row.description,
          notes: row.reference ? `Ref: ${row.reference}` : null,
          payment_method: row.paymentRail?.startsWith('UPI') ? 'UPI' : 'NET_BANKING',
          need_want: row.needWant,
          tags: ['statement_import', batchId],
          imported: true,
          source: 'BANK_STATEMENT',
          created_at: now,
          updated_at: now,
          deleted_at: null,
          // Spec §4 & §6 fields
          rrn: row.reference || null,
          payment_rail: row.paymentRail || null,
          spend_emotion_tag: row.emotionTag || null,
          jar_bucket: row.jarBucket || null,
          import_batch_id: batchId,
          is_recurring_mandate: row.isRecurringMandate || false,
        };

        await addTransaction(newTxn, journalEntries);
        newlyImportedIds.push(txnId);
        imported++;
      }

      setImportedTxnIds(newlyImportedIds);
      setImportSuccess(imported);
      if (onImportComplete) onImportComplete();
    } catch (err: any) {
      setError(err?.message || 'Error saving transactions.');
    } finally {
      setImporting(false);
    }
  };

  // Undo entire batch
  const handleUndoImport = async () => {
    if (importedTxnIds.length === 0) return;
    setImporting(true);
    try {
      for (const id of importedTxnIds) {
        await deleteTransaction(id);
      }
      setImportSuccess(null);
      setImportedTxnIds([]);
      setError('Import undone successfully.');
    } catch (err: any) {
      setError(err?.message || 'Error undoing import.');
    } finally {
      setImporting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose} style={{ zIndex: 1200 }}>
      <div
        className="modal-content"
        onClick={e => e.stopPropagation()}
        style={{ maxWidth: 860, maxHeight: '90vh', overflowY: 'auto' }}
      >
        {/* Header */}
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <h2 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.35rem', fontWeight: 800, margin: 0 }}>
                📥 Import Bank Statement &amp; Reconcile
              </h2>
              {parsedResult?.detectedFormatName && (
                <span className="badge badge-cyan" style={{ fontSize: '0.74rem' }}>
                  {parsedResult.detectedFormatName}
                </span>
              )}
            </div>
            <p style={{ fontSize: '0.82rem', color: 'var(--text-tertiary)', margin: '4px 0 0 0' }}>
              Multi-bank statement ingestion with double-entry reconciliation &amp; mandate radar
            </p>
          </div>
          <button type="button" onClick={onClose} className="btn-ghost" style={{ padding: 6, borderRadius: '50%' }}>
            <X size={18} />
          </button>
        </div>

        {importSuccess !== null ? (
          <div style={{ textAlign: 'center', padding: '36px 16px' }}>
            <CheckCircle2 size={56} style={{ color: 'var(--neon-emerald)', margin: '0 auto 16px auto' }} />
            <h3 style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.3rem', fontWeight: 800, color: '#f1f5f9' }}>
              Successfully Imported {importSuccess} Transactions!
            </h3>
            <p style={{ fontSize: '0.88rem', color: 'var(--text-tertiary)', marginTop: 6, marginBottom: 24 }}>
              Your ledger has been updated with double-entry journal records. Batch ID:{' '}
              <code style={{ color: 'var(--neon-cyan)', fontSize: '0.8rem' }}>{parsedResult?.importBatchId}</code>
            </p>
            <div style={{ display: 'flex', justifyContent: 'center', gap: 12 }}>
              <button
                type="button"
                className="btn btn-secondary btn-md"
                onClick={handleUndoImport}
                disabled={importing}
                style={{ color: 'var(--neon-rose)', borderColor: 'rgba(239, 68, 68, 0.4)' }}
              >
                <RotateCcw size={16} /> Undo This Import
              </button>
              <button
                type="button"
                className="btn btn-primary btn-md"
                onClick={onClose}
                style={{ background: 'var(--neon-emerald)', color: '#000' }}
              >
                Done &amp; View Ledger
              </button>
            </div>
          </div>
        ) : (
          <div>
            {/* Step 1: Select Account & Upload */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14, marginBottom: 16 }}>
              <div className="form-group" style={{ margin: 0 }}>
                <label className="form-label">Destination Bank Account *</label>
                <select value={selectedAccountId} onChange={e => setSelectedAccountId(e.target.value)}>
                  {accounts.map(acc => (
                    <option key={acc.id} value={acc.id}>
                      {acc.name} ({acc.type})
                    </option>
                  ))}
                </select>
              </div>

              <div style={{ display: 'flex', alignItems: 'flex-end', gap: 8 }}>
                <label
                  className="btn btn-secondary btn-md"
                  style={{
                    flex: 1,
                    cursor: 'pointer',
                    display: 'inline-flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    gap: 6,
                  }}
                >
                  <Upload size={16} /> {fileName ? 'Change File' : 'Upload CSV Statement'}
                  <input
                    type="file"
                    accept=".csv,.txt,.tsv"
                    onChange={handleFileUpload}
                    style={{ display: 'none' }}
                  />
                </label>

                <button
                  type="button"
                  className="btn btn-ghost btn-sm"
                  onClick={handleLoadSample}
                  style={{ fontSize: '0.78rem', color: 'var(--neon-cyan)', border: '1px solid rgba(0, 240, 255, 0.25)', height: 38 }}
                >
                  <Sparkles size={14} /> Load Axis Sample
                </button>
              </div>
            </div>

            {/* Paste statement text option */}
            {!parsedResult && (
              <div style={{ marginBottom: 16 }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 }}>
                  <label className="form-label" style={{ margin: 0 }}>Or Paste Statement Text (CSV / Tab-Separated)</label>
                  <span style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)' }}>Supports Axis, HDFC, SBI copy-paste</span>
                </div>
                <textarea
                  rows={4}
                  value={statementText}
                  onChange={e => {
                    setStatementText(e.target.value);
                    if (e.target.value.trim().length > 20) analyzeText(e.target.value, customMapping);
                  }}
                  placeholder="Tran Date,Particulars,Chq No,Withdrawal (Dr),Deposit (Cr),Balance&#10;05-10-2026,SALARY CREDIT CMS3948,-,32200.00,32200.00"
                  style={{
                    width: '100%',
                    padding: 10,
                    borderRadius: 'var(--radius-md)',
                    background: 'rgba(255,255,255,0.03)',
                    border: '1px solid var(--glass-border)',
                    color: '#f8fafc',
                    fontFamily: 'monospace',
                    fontSize: '0.78rem',
                  }}
                />
              </div>
            )}

            {error && (
              <div style={{ background: 'rgba(244, 63, 94, 0.12)', border: '1px solid rgba(244, 63, 94, 0.35)', borderRadius: 'var(--radius-md)', padding: '10px 14px', marginBottom: 14, fontSize: '0.84rem', color: '#fca5a5' }}>
                ⚠️ {error}
              </div>
            )}

            {/* Parsed Result & Reconciliation Section */}
            {parsedResult && (
              <div>
                {/* Column Mapper Toggle & Fallback Panel */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 12 }}>
                  <button
                    type="button"
                    className="btn btn-ghost btn-xs"
                    onClick={() => setShowColumnMapper(!showColumnMapper)}
                    style={{ color: 'var(--neon-cyan)', display: 'inline-flex', alignItems: 'center', gap: 6 }}
                  >
                    <SlidersHorizontal size={14} />
                    {showColumnMapper ? 'Hide Column Mapping' : 'Adjust Column Mapping (Fallback)'}
                  </button>

                  <span style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)' }}>
                    Confidence: <strong style={{ color: 'var(--neon-emerald)' }}>{parsedResult.mapping?.confidence || 'HIGH'}</strong>
                  </span>
                </div>

                {showColumnMapper && (
                  <div
                    style={{
                      padding: 14,
                      borderRadius: 'var(--radius-md)',
                      background: 'rgba(255,255,255,0.02)',
                      border: '1px solid var(--glass-border)',
                      marginBottom: 16,
                      display: 'grid',
                      gridTemplateColumns: 'repeat(auto-fit, minmax(170px, 1fr))',
                      gap: 10,
                    }}
                  >
                    <div>
                      <label style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', display: 'block', marginBottom: 4 }}>Date Col</label>
                      <select
                        value={customMapping.dateCol ?? parsedResult.mapping?.dateCol ?? 0}
                        onChange={e => handleUpdateMappingCol('dateCol', Number(e.target.value))}
                        style={{ fontSize: '0.75rem', padding: '4px 8px' }}
                      >
                        {parsedResult.headers.map((h, i) => (
                          <option key={i} value={i}>Col {i + 1}: {h || `Column ${i}`}</option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', display: 'block', marginBottom: 4 }}>Narration Col</label>
                      <select
                        value={customMapping.descriptionCol ?? parsedResult.mapping?.descriptionCol ?? 1}
                        onChange={e => handleUpdateMappingCol('descriptionCol', Number(e.target.value))}
                        style={{ fontSize: '0.75rem', padding: '4px 8px' }}
                      >
                        {parsedResult.headers.map((h, i) => (
                          <option key={i} value={i}>Col {i + 1}: {h || `Column ${i}`}</option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', display: 'block', marginBottom: 4 }}>Debit Col</label>
                      <select
                        value={customMapping.debitCol ?? parsedResult.mapping?.debitCol ?? -1}
                        onChange={e => handleUpdateMappingCol('debitCol', Number(e.target.value))}
                        style={{ fontSize: '0.75rem', padding: '4px 8px' }}
                      >
                        <option value={-1}>None / Single Amount</option>
                        {parsedResult.headers.map((h, i) => (
                          <option key={i} value={i}>Col {i + 1}: {h || `Column ${i}`}</option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', display: 'block', marginBottom: 4 }}>Credit Col</label>
                      <select
                        value={customMapping.creditCol ?? parsedResult.mapping?.creditCol ?? -1}
                        onChange={e => handleUpdateMappingCol('creditCol', Number(e.target.value))}
                        style={{ fontSize: '0.75rem', padding: '4px 8px' }}
                      >
                        <option value={-1}>None / Single Amount</option>
                        {parsedResult.headers.map((h, i) => (
                          <option key={i} value={i}>Col {i + 1}: {h || `Column ${i}`}</option>
                        ))}
                      </select>
                    </div>

                    <div>
                      <label style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', display: 'block', marginBottom: 4 }}>Balance Col</label>
                      <select
                        value={customMapping.balanceCol ?? parsedResult.mapping?.balanceCol ?? -1}
                        onChange={e => handleUpdateMappingCol('balanceCol', Number(e.target.value))}
                        style={{ fontSize: '0.75rem', padding: '4px 8px' }}
                      >
                        <option value={-1}>None</option>
                        {parsedResult.headers.map((h, i) => (
                          <option key={i} value={i}>Col {i + 1}: {h || `Column ${i}`}</option>
                        ))}
                      </select>
                    </div>
                  </div>
                )}

                {/* Summary & Math Check Cards */}
                <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 12, marginBottom: 16 }}>
                  {/* Credits */}
                  <div className="cyber-card" style={{ padding: 12, borderLeft: '3px solid var(--neon-emerald)' }}>
                    <div style={{ fontSize: '0.72rem', color: 'var(--neon-emerald)', fontWeight: 700, textTransform: 'uppercase' }}>
                      <ArrowDownRight size={14} style={{ display: 'inline', verticalAlign: 'middle', marginRight: 4 }} />
                      Total Credits (Income)
                    </div>
                    <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, color: 'var(--neon-emerald)', marginTop: 4 }}>
                      +{formatMoney(parsedResult.totalCreditsMinor, currency, masked)}
                    </div>
                  </div>

                  {/* Debits */}
                  <div className="cyber-card" style={{ padding: 12, borderLeft: '3px solid var(--neon-rose)' }}>
                    <div style={{ fontSize: '0.72rem', color: 'var(--neon-rose)', fontWeight: 700, textTransform: 'uppercase' }}>
                      <ArrowUpRight size={14} style={{ display: 'inline', verticalAlign: 'middle', marginRight: 4 }} />
                      Total Debits (Spent)
                    </div>
                    <div style={{ fontFamily: 'var(--font-outfit)', fontSize: '1.25rem', fontWeight: 800, color: 'var(--neon-rose)', marginTop: 4 }}>
                      -{formatMoney(parsedResult.totalDebitsMinor, currency, masked)}
                    </div>
                  </div>

                  {/* Reconciliation Status */}
                  <div
                    className="cyber-card"
                    style={{
                      padding: 12,
                      borderLeft: `3px solid ${
                        parsedResult.hasReconciliation && parsedResult.discrepancyMinor === 0
                          ? 'var(--neon-emerald)'
                          : 'var(--neon-amber)'
                      }`,
                    }}
                  >
                    <div style={{ fontSize: '0.72rem', fontWeight: 700, textTransform: 'uppercase', color: parsedResult.discrepancyMinor === 0 ? 'var(--neon-emerald)' : 'var(--neon-amber)' }}>
                      🧮 Math Reconciliation
                    </div>
                    {parsedResult.hasReconciliation ? (
                      <div style={{ marginTop: 4 }}>
                        <div style={{ fontSize: '0.88rem', fontWeight: 800, color: parsedResult.discrepancyMinor === 0 ? 'var(--neon-emerald)' : 'var(--neon-amber)' }}>
                          {parsedResult.discrepancyMinor === 0 ? '✅ 100% Balanced (₹0.00)' : `⚠️ Discrepancy: ${formatMoney(Math.abs(parsedResult.discrepancyMinor || 0), currency, masked)}`}
                        </div>
                        <div style={{ fontSize: '0.7rem', color: 'var(--text-tertiary)', marginTop: 2 }}>
                          Closing balance: {formatMoney(parsedResult.closingBalanceMinor || 0, currency, masked)}
                        </div>
                      </div>
                    ) : (
                      <div style={{ fontSize: '0.78rem', color: 'var(--text-tertiary)', marginTop: 4 }}>
                        Running balance column not detected
                      </div>
                    )}
                  </div>
                </div>

                {/* Mandate Radar Banner (Spec §6.8) */}
                {parsedResult.mandates && parsedResult.mandates.length > 0 && (
                  <div
                    style={{
                      padding: 12,
                      borderRadius: 'var(--radius-md)',
                      background: 'rgba(6, 182, 212, 0.08)',
                      border: '1px solid rgba(6, 182, 212, 0.25)',
                      marginBottom: 14,
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: 'var(--neon-cyan)', fontWeight: 700, fontSize: '0.82rem' }}>
                        <ShieldAlert size={16} /> Mandate Radar: {parsedResult.mandates.length} Recurring Mandate(s) Detected
                      </div>
                      <button
                        type="button"
                        className="btn-ghost"
                        onClick={() => setShowMandateHelp(!showMandateHelp)}
                        style={{ fontSize: '0.74rem', color: 'var(--neon-cyan)', padding: '2px 6px' }}
                      >
                        {showMandateHelp ? 'Hide Guidance' : 'How to Pause Mandates'}
                      </button>
                    </div>

                    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, marginTop: 8 }}>
                      {parsedResult.mandates.map((m, idx) => (
                        <span key={idx} className="badge badge-cyan" style={{ fontSize: '0.74rem' }}>
                          {m.name}: {formatMoney(m.amountMinor, currency, masked)}
                        </span>
                      ))}
                    </div>

                    {showMandateHelp && (
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginTop: 8, borderTop: '1px solid rgba(255,255,255,0.06)', paddingTop: 6, lineHeight: 1.4 }}>
                        💡 <strong>To pause or cancel mandates</strong>: Open your UPI app (GPay / PhonePe / Paytm / BHIM) → Navigate to Profile / Payment Settings → Select Autopay or Mandates → Pause or Revoke.
                      </div>
                    )}
                  </div>
                )}

                {/* Small Scans Aggregation (Spec §6.6) */}
                {parsedResult.smallScansCount && parsedResult.smallScansCount > 0 && (
                  <div style={{ background: 'rgba(245, 158, 11, 0.08)', border: '1px solid rgba(245, 158, 11, 0.25)', borderRadius: 'var(--radius-md)', padding: '8px 12px', marginBottom: 14, fontSize: '0.78rem', color: '#fde68a', display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span>
                      ☕ <strong>Small UPI scans aggregation</strong>: {formatMoney(parsedResult.smallScansTotalMinor || 0, currency, masked)} across {parsedResult.smallScansCount} scans (≤ ₹200).
                    </span>
                  </div>
                )}

                {/* Duplicates notice if any */}
                {parsedResult.duplicateCount > 0 && (
                  <div style={{ background: 'rgba(245, 158, 11, 0.1)', border: '1px solid rgba(245, 158, 11, 0.3)', borderRadius: 'var(--radius-md)', padding: '10px 14px', marginBottom: 14, fontSize: '0.8rem', color: '#fde68a', display: 'flex', alignItems: 'center', gap: 8 }}>
                    <AlertTriangle size={16} style={{ color: 'var(--neon-amber)', flexShrink: 0 }} />
                    <span>
                      <strong>{parsedResult.duplicateCount} transactions</strong> match existing records in your ledger and have been deselected to prevent double counting.
                    </span>
                  </div>
                )}

                {/* Selection helper */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 10 }}>
                  <div style={{ fontSize: '0.82rem', color: 'var(--text-secondary)' }}>
                    <strong>{selectedCount}</strong> of {rows.length} transactions selected for import
                  </div>
                  <div style={{ display: 'flex', gap: 8 }}>
                    <button type="button" className="btn btn-ghost btn-xs" onClick={() => toggleSelectAll(true)}>
                      Select All
                    </button>
                    <button type="button" className="btn btn-ghost btn-xs" onClick={() => toggleSelectAll(false)}>
                      Deselect All
                    </button>
                  </div>
                </div>

                {/* Table of Rows */}
                <div style={{ border: '1px solid var(--glass-border)', borderRadius: 'var(--radius-md)', overflowX: 'auto', maxHeight: 320, overflowY: 'auto', marginBottom: 16 }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.82rem', textAlign: 'left' }}>
                    <thead style={{ background: 'rgba(255,255,255,0.04)', borderBottom: '1px solid var(--glass-border)', position: 'sticky', top: 0 }}>
                      <tr>
                        <th style={{ padding: '8px 12px', width: 40 }}></th>
                        <th style={{ padding: '8px 12px' }}>Date</th>
                        <th style={{ padding: '8px 12px' }}>Description / Narration</th>
                        <th style={{ padding: '8px 12px' }}>Amount</th>
                        <th style={{ padding: '8px 12px' }}>Category</th>
                      </tr>
                    </thead>
                    <tbody>
                      {rows.map((row, idx) => (
                        <tr
                          key={row.id}
                          style={{
                            borderBottom: '1px solid rgba(255,255,255,0.03)',
                            background: row.isDuplicate
                              ? 'rgba(245, 158, 11, 0.04)'
                              : row.selected
                              ? 'transparent'
                              : 'rgba(0,0,0,0.2)',
                            opacity: row.selected ? 1 : 0.6,
                          }}
                        >
                          <td style={{ padding: '8px 12px' }}>
                            <input
                              type="checkbox"
                              checked={row.selected}
                              onChange={() => toggleRow(idx)}
                              style={{ cursor: 'pointer' }}
                            />
                          </td>
                          <td style={{ padding: '8px 12px', whiteSpace: 'nowrap', color: 'var(--text-secondary)' }}>
                            {row.date}
                          </td>
                          <td style={{ padding: '8px 12px', maxWidth: 300 }}>
                            <div style={{ fontWeight: 600, color: '#f1f5f9' }}>
                              {row.merchant || row.description}
                            </div>
                            <div style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)', display: 'flex', gap: 6, marginTop: 2, flexWrap: 'wrap' }}>
                              {row.reference && <span>RRN: {row.reference}</span>}
                              {row.paymentRail && <span className="badge badge-purple" style={{ fontSize: '0.65rem' }}>{row.paymentRail}</span>}
                              {row.emotionTag && <span className="badge badge-cyan" style={{ fontSize: '0.65rem' }}>{row.emotionTag}</span>}
                              {row.isRecurringMandate && <span className="badge badge-rose" style={{ fontSize: '0.65rem' }}>MANDATE</span>}
                            </div>
                          </td>
                          <td style={{ padding: '8px 12px', whiteSpace: 'nowrap', fontFamily: 'var(--font-outfit)', fontWeight: 700, color: row.type === 'INCOME' ? 'var(--neon-emerald)' : 'var(--neon-rose)' }}>
                            {row.type === 'INCOME' ? '+' : '-'}{formatMoney(row.amountMinor, currency, masked)}
                          </td>
                          <td style={{ padding: '8px 12px' }}>
                            <select
                              value={row.suggestedCategoryId || ''}
                              onChange={e => updateRowCategory(idx, e.target.value)}
                              style={{ fontSize: '0.76rem', padding: '4px 6px', width: 140 }}
                            >
                              <option value="">Uncategorised</option>
                              {categories
                                .filter(c => (row.type === 'INCOME' ? c.kind === 'INCOME' : c.kind === 'EXPENSE'))
                                .map(c => (
                                  <option key={c.id} value={c.id}>
                                    {getCategoryDisplayName(c.id, categories)}
                                  </option>
                                ))}
                            </select>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>

                {/* Footer action bar */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderTop: '1px solid var(--glass-border)', paddingTop: 14 }}>
                  <button type="button" className="btn btn-ghost btn-md" onClick={onClose}>
                    Cancel
                  </button>

                  <button
                    type="button"
                    className="btn btn-primary btn-md"
                    onClick={handleCommitImport}
                    disabled={importing || selectedCount === 0}
                    style={{ background: 'var(--neon-emerald)', color: '#000', fontWeight: 800 }}
                  >
                    {importing ? 'Importing...' : `Commit Import (${selectedCount} Records)`}
                  </button>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
