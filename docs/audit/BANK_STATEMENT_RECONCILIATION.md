# WealthOS — Axis Bank Statement Import & Reconciliation Guide

> [!CAUTION]
> **Real Money At Risk**: On October 5, 2026, ₹32,200 was credited to your Axis Bank account. Your current available balance is approximately ₹800. There is a **₹31,400 difference** that must be accounted for with 100% financial accuracy before making any other adjustments.
> **DO NOT** create dummy transactions in WealthOS to force your balance to ₹800. Follow this reconciliation protocol to track every single rupee.

---

## 1. The Core Reconciliation Formula

Every bank statement obeys the fundamental double-entry identity:

$$\text{Expected Closing Balance} = \text{Opening Balance} + \sum \text{Credits (Income)} - \sum \text{Debits (Expenses/Transfers)}$$

$$\text{Reconciliation Discrepancy} = \text{Expected Closing Balance} - \text{Actual Bank Closing Balance}$$

- If **Discrepancy = ₹0.00**: Your records are mathematically 100% verified.
- If **Discrepancy $\neq$ ₹0.00**: An unrecorded transaction, bank charge, or duplicate entry exists.

---

## 2. Axis Bank Statement Extraction Guide

### 2.1 Via Axis Mobile App (Recommended)
1. Open **Axis Mobile** app and authenticate securely via Biometric / MPIN.
2. Navigate to **Accounts** $\rightarrow$ Select your primary salary savings account.
3. Tap **View Statement** $\rightarrow$ Choose custom date range: **01-Oct-2026 to 22-Oct-2026**.
4. Tap **Download** and select **CSV format** (or PDF if CSV is unavailable).
5. The statement will be saved to your device's `Downloads` folder.

### 2.2 Via Axis Internet Banking (Desktop)
1. Log in to [retail.axisbank.co.in](https://retail.axisbank.co.in).
2. Go to **Accounts** $\rightarrow$ **Operative Accounts** $\rightarrow$ **View Statement**.
3. Set Date Range: `01/10/2026` to `22/10/2026`.
4. Select **Format**: **CSV / Excel** (preferred for automated parser) or **PDF**.
5. Click **Download**.

---

## 3. Privacy & Redaction Checklist

> [!IMPORTANT]
> **Protect Your Banking Credentials**: WealthOS never asks for and should never receive your banking passwords, MPIN, OTP, or card CVVs.

Before parsing or storing statements:
- [x] **Keep Intact**:
  - Transaction Date (`DD-MM-YYYY`)
  - Description / Narration (contains merchant name, UPI ID, or NEFT/IMPS UTR)
  - Debit Amount (₹)
  - Credit Amount (₹)
  - Running Account Balance (₹)
  - Reference / Cheque / UTR number
- [ ] **Redact / Remove**:
  - Full 15-digit Axis Bank Account Number (keep only last 4 digits, e.g., `...4821`)
  - Customer ID / CIF
  - Home address, branch IFSC, and personal phone number
  - Debit Card 16-digit numbers or expiry dates

---

## 4. Axis Bank Reconciliation Worksheet Template

Copy and populate this structured table with your actual transactions between Oct 1 and Oct 22, 2026:

| # | Date | Narration / Description | UTR / Ref No. | Debit (₹) | Credit (₹) | Running Balance (₹) | Classification | WealthOS Category |
|---|---|---|---|---|---|---|---|---|
| **0** | **01-Oct-2026** | **Opening Balance** | — | — | — | **₹[______]** | Baseline | Opening Balance |
| 1 | 05-Oct-2026 | SALARY CREDIT / EMPLOYER / CMS... | CMS3948... | — | ₹32,200.00 | ₹[______] | Income | `💰 Salary` |
| 2 | 05-Oct-2026 | UPI / House Rent / ... | 62781... | ₹[______] | — | ₹[______] | Need | `🏠 Housing & Rent` |
| 3 | 06-Oct-2026 | ACH / Loan EMI / ... | ACH9912... | ₹[______] | — | ₹[______] | Debt | `💳 Loan EMI / Repayment` |
| 4 | 07-Oct-2026 | UPI / Swiggy / Zomato / ... | 62799... | ₹[______] | — | ₹[______] | Want | `🍔 Food & Dining` |
| 5 | 08-Oct-2026 | ATM CASH WDL / AXIS ATM ... | ATM0192... | ₹[______] | — | ₹[______] | Cash Need | `💵 Cash Withdrawal` |
| 6 | 10-Oct-2026 | UPI / Electricity / BESCOM / TNEB | 62804... | ₹[______] | — | ₹[______] | Need | `⚡ Electricity & Bills` |
| 7 | 12-Oct-2026 | UPI / Self Transfer / Other Bank | 62821... | ₹[______] | — | ₹[______] | Transfer | `🔄 Internal Transfer` |
| 8 | 15-Oct-2026 | POS / Supermarket / Groceries | POS4821... | ₹[______] | — | ₹[______] | Need | `🛒 Groceries` |
| 9 | 18-Oct-2026 | UPI / Fuel / HPCL / IOCL | 62881... | ₹[______] | — | ₹[______] | Need | `⛽ Fuel & Commute` |
| 10| 22-Oct-2026 | **Closing Balance** | — | — | — | **₹800.00** | Target | Verified Bank Balance |

---

## 5. Identifying Where the ₹31,400 Went

When categorizing debits from your statement, use these 5 buckets to pinpoint the outflow:

### Bucket 1: Fixed Commitments (Non-Negotiable Needs)
- **Rent**: Direct UPI / IMPS to landlord.
- **EMIs / Loans**: Home loan, personal loan, vehicle EMI, or gold loan.
- **Utilities**: Electricity, water, Wi-Fi / broadband, mobile recharge.
- **Chit Funds**: Monthly installment or chit pool payment.

### Bucket 2: Variable Necessities
- **Groceries**: Supermarkets, local provision stores, dairy, vegetables.
- **Fuel / Transportation**: Petrol pumps, metro card top-up, auto/cab rides.
- **Medical / Pharmacy**: Apollo, MedPlus, doctor consultations.

### Bucket 3: Own-Account Transfers (NOT Expenses!)
> [!WARNING]
> Transfers between your own accounts (e.g., Axis Bank to Google Pay balance, Paytm wallet, Zerodha, or a secondary bank like SBI/HDFC) are **Transfers**, not expenses.
- Check if any debits were transfers to your own accounts or investment platforms.
- If ₹10,000 was moved to an investment or secondary account, that ₹10,000 is still yours—it just moved accounts!

### Bucket 4: Discretionary Outflows (Wants)
- Food delivery (Zomato, Swiggy, Blinkit, Zepto, Instamart).
- Weekend dining, coffee, social outings.
- Online shopping (Amazon, Flipkart, Myntra).
- Subscriptions and entertainment.

### Bucket 5: Cash Withdrawals
- If you withdrew cash from an ATM, record the amount as a transfer to a **Cash Wallet** account.
- Note down what the physical cash was spent on.

---

## 6. How to Ingest into WealthOS Web

Once your statement is reviewed:
1. Open WealthOS at [wealthos-dw9.pages.dev](https://wealthos-dw9.pages.dev) or locally at `http://localhost:5173`.
2. Go to **Accounts** $\rightarrow$ Select your **Axis Bank** account.
3. Click **Add Transaction** or use **Import Statement** to record each verified debit.
4. If a debit was an inter-account transfer, select type **Transfer** and choose the destination account. The system will create both balanced journal legs automatically (verified by fix F-04).
5. Check your dashboard: your Axis Bank account balance in WealthOS will exactly equal **₹800**, and your dashboard **Monthly Outflow** and **50/30/20** breakdown will show you the exact breakdown of the ₹31,400!
