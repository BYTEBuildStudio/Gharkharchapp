/**
 * GharKharch - Local Database & Application State Engine
 * Satisfies local SQLite/Room data model translations using web localStorage.
 */

class StateStore {
    constructor() {
        this.STORAGE_KEY = 'gharkharch_app_state';
        this.init();
    }

    init() {
        const saved = localStorage.getItem(this.STORAGE_KEY);
        if (saved) {
            try {
                const parsed = JSON.parse(saved);
                this.expenses = parsed.expenses || [];
                this.categories = parsed.categories || this.getDefaultCategories();
                this.budgets = parsed.budgets || this.getDefaultBudgets();
                this.activeUser = parsed.activeUser || null;
                this.googleAccessToken = parsed.googleAccessToken || '';
                this.syncLogs = parsed.syncLogs || [];
                this.syncStatus = parsed.syncStatus || 'Idle';
                this.theme = parsed.theme || 'dark';
            } catch (e) {
                this.loadDefaults();
            }
        } else {
            this.loadDefaults();
        }
    }

    loadDefaults() {
        this.expenses = [];
        this.categories = this.getDefaultCategories();
        this.budgets = this.getDefaultBudgets();
        this.activeUser = null;
        this.googleAccessToken = '';
        this.syncLogs = [];
        this.syncStatus = 'Idle';
        this.theme = 'dark';
        this.save();
    }

    save() {
        const payload = {
            expenses: this.expenses,
            categories: this.categories,
            budgets: this.budgets,
            activeUser: this.activeUser,
            googleAccessToken: this.googleAccessToken,
            syncLogs: this.syncLogs,
            syncStatus: this.syncStatus,
            theme: this.theme
        };
        localStorage.setItem(this.STORAGE_KEY, JSON.stringify(payload));
    }

    getDefaultCategories() {
        return [
            { name: "Groceries", icon: "shopping_basket", color: "#4CAF50" },
            { name: "Vegetables/Fruits", icon: "eco", color: "#8BC34A" },
            { name: "Milk/Dairy", icon: "opacity", color: "#03A9F4" },
            { name: "Electricity Bill", icon: "flash_on", color: "#FFC107" },
            { name: "Water Bill", icon: "water", color: "#2196F3" },
            { name: "Gas Cylinder (LPG)", icon: "local_fire_department", color: "#FF5722" },
            { name: "Rent", icon: "home", color: "#9C27B0" },
            { name: "Transport/Fuel", icon: "directions_car", color: "#3F51B5" },
            { name: "Mobile/Internet Recharge", icon: "phone_android", color: "#E91E63" },
            { name: "Education/School Fees", icon: "school", color: "#009688" },
            { name: "Medical/Pharmacy", icon: "medical_services", color: "#F44336" },
            { name: "Household Help (maid/cook)", icon: "person", color: "#795548" },
            { name: "Festivals/Pooja", icon: "brightness_high", color: "#FF9800" },
            { name: "EMI/Loan", icon: "account_balance", color: "#607D8B" },
            { name: "Entertainment", icon: "movie", color: "#673AB7" },
            { name: "Eating Out", icon: "restaurant", color: "#D32F2F" },
            { name: "Shopping", icon: "local_mall", color: "#00BCD4" },
            { name: "Maintenance (society/repairs)", icon: "construction", color: "#78909C" },
            { name: "Insurance", icon: "shield", color: "#4CAF50" },
            { name: "Other", icon: "category", color: "#9E9E9E" }
        ];
    }

    getDefaultBudgets() {
        return [
            { category: "Groceries", limit: 8000 },
            { category: "Milk/Dairy", limit: 2000 },
            { category: "Vegetables/Fruits", limit: 3000 },
            { category: "Electricity Bill", limit: 3500 },
            { category: "Transport/Fuel", limit: 5000 },
            { category: "Eating Out", limit: 4000 }
        ];
    }

    addExpense(amount, category, note, dateString, paymentMethod) {
        const id = 'exp_' + Math.random().toString(36).substr(2, 9);
        const dateMillis = dateString ? new Date(dateString).getTime() : Date.now();
        const expense = {
            id,
            amount: parseFloat(amount),
            category,
            note: note ? note.trim() : '',
            dateMillis,
            paymentMethod: paymentMethod || 'UPI'
        };
        this.expenses.push(expense);
        this.save();
        this.logTerminal(`Expense added: ₹${amount} for ${category} (${expense.paymentMethod}). Triggering transaction auto-sync...`, 'Success');
        return expense;
    }

    updateExpense(id, amount, category, note, dateString, paymentMethod) {
        const index = this.expenses.findIndex(e => e.id === id);
        if (index !== -1) {
            const dateMillis = dateString ? new Date(dateString).getTime() : Date.now();
            this.expenses[index] = {
                ...this.expenses[index],
                amount: parseFloat(amount),
                category,
                note: note ? note.trim() : '',
                dateMillis,
                paymentMethod: paymentMethod || 'UPI'
            };
            this.save();
            this.logTerminal(`Expense ID ${id} updated: ₹${amount} for ${category}. Outbound sync queued...`, 'Idle');
            return true;
        }
        return false;
    }

    deleteExpense(id) {
        const filtered = this.expenses.filter(e => e.id !== id);
        if (filtered.length !== this.expenses.length) {
            this.expenses = filtered;
            this.save();
            this.logTerminal(`Expense ID ${id} deleted locally. Outbound sync queued...`, 'Idle');
            return true;
        }
        return false;
    }

    prepopulateSampleData() {
        const now = Date.now();
        const oneDay = 24 * 60 * 60 * 1000;
        
        // Formulate 15 realistic household transactions scattered across previous 6 days
        const samples = [
            { amount: 1200, category: "Groceries", note: "Monthly dry rations (Aashirvaad, Fortune)", dateOffset: 0, paymentMethod: "UPI" },
            { amount: 4800, category: "Rent", note: "House electricity and room maintenance advance", dateOffset: 5, paymentMethod: "NetBanking" },
            { amount: 350, category: "Vegetables/Fruits", note: "Weekly sabzi mandi purchase", dateOffset: 1, paymentMethod: "Cash" },
            { amount: 650, category: "Milk/Dairy", note: "Mother Dairy milk tokens backlog", dateOffset: 2, paymentMethod: "UPI" },
            { amount: 2200, category: "Electricity Bill", note: "BSES Yamuna May bill", dateOffset: 3, paymentMethod: "Card" },
            { amount: 1050, category: "Gas Cylinder (LPG)", note: "Indane Gas cylinder refill", dateOffset: 4, paymentMethod: "NetBanking" },
            { amount: 800, category: "Transport/Fuel", note: "Petrol shell pump refueling", dateOffset: 0, paymentMethod: "Card" },
            { amount: 599, category: "Mobile/Internet Recharge", note: "Jio Fiber monthly plan", dateOffset: 2, paymentMethod: "UPI" },
            { amount: 1500, category: "Medical/Pharmacy", note: "Grandparent regular diagnostics", dateOffset: 1, paymentMethod: "UPI" },
            { amount: 3000, category: "Household Help (maid/cook)", note: "Salary for domestic help", dateOffset: 5, paymentMethod: "Cash" },
            { amount: 850, category: "Shopping", note: "Pooja ghee and home essentials", dateOffset: 3, paymentMethod: "UPI" },
            { amount: 1200, category: "Eating Out", note: "Haldiram family dinner weekend", dateOffset: 1, paymentMethod: "UPI" },
            { amount: 450, category: "Entertainment", note: "Netflix premium monthly renewal", dateOffset: 4, paymentMethod: "Card" },
            { amount: 180, category: "Milk/Dairy", note: "Paneer & curd raw pack", dateOffset: 0, paymentMethod: "UPI" },
            { amount: 450, category: "Groceries", note: "Spices and flour emergency bag", dateOffset: 2, paymentMethod: "Cash" }
        ];

        this.expenses = samples.map((s, idx) => ({
            id: `exp_sample_${idx}_${Date.now().toString(36)}`,
            amount: s.amount,
            category: s.category,
            note: s.note,
            dateMillis: now - (s.dateOffset * oneDay),
            paymentMethod: s.paymentMethod
        }));

        this.save();
        this.logTerminal(`Prepopulated database with ${this.expenses.length} realistic Delhi/NCR domestic household expense presets.`, 'Success');
        return this.expenses;
    }

    clearAllData() {
        this.expenses = [];
        this.save();
        this.logTerminal("All household logs dropped from sandbox memory. Storage reset complete.", 'Idle');
    }

    signUp(name, email) {
        this.activeUser = {
            email: email.trim().toLowerCase(),
            name: name.trim(),
            isGoogleUser: false,
            spreadsheetId: '',
            dateJoinedMillis: Date.now()
        };
        const defaultHash = Math.abs(this.hashCode(email));
        this.activeUser.spreadsheetId = `spreadsheet_gharkharch_${defaultHash}`;
        this.save();
        this.logTerminal(`User sign-up: Created sandbox profile for ${this.activeUser.name} (${this.activeUser.email}).`, 'Success');
    }

    logIn(email) {
        // Simple automatic registration or login
        const emailClean = email.trim().toLowerCase();
        const fallbackName = emailClean.split('@')[0].replace(/^\w/, c => c.toUpperCase());
        this.activeUser = {
            email: emailClean,
            name: fallbackName,
            isGoogleUser: false,
            spreadsheetId: `spreadsheet_gharkharch_${Math.abs(this.hashCode(emailClean))}`,
            dateJoinedMillis: Date.now()
        };
        this.save();
        this.logTerminal(`Active family session restored for member: ${this.activeUser.name}`, 'Success');
    }

    logOut() {
        this.logTerminal(`Terminated active cloud sync session for: ${this.activeUser?.name || 'User'}`, 'Idle');
        this.activeUser = null;
        this.googleAccessToken = '';
        this.syncStatus = 'Idle';
        this.save();
    }

    setCustomToken(token) {
        this.googleAccessToken = token ? token.trim() : '';
        this.save();
        this.logTerminal(`Bearer OAuth token configured: ${token ? 'ACTIVE (Real Sheets Mode)' : 'EMPTY (Simulated Sandbox Enabled)'}`, 'Success');
    }

    updateSpreadsheetId(id) {
        if (this.activeUser) {
            this.activeUser.spreadsheetId = id.trim();
            this.save();
        }
    }

    logTerminal(message, status = 'Idle') {
        const time = new Date().toLocaleTimeString('en-IN', { hour12: false });
        const logStr = `[${time}] ${message}`;
        this.syncLogs.push(logStr);
        // Cap terminal history logs at last 100 for storage thrift
        if (this.syncLogs.length > 100) {
            this.syncLogs.shift();
        }
        this.syncStatus = status;
        this.save();
    }

    hashCode(str) {
        let hash = 0;
        for (let i = 0; i < str.length; i++) {
            const chr = str.charCodeAt(i);
            hash = ((hash << 5) - hash) + chr;
            hash |= 0;
        }
        return hash;
    }
}

// Global App State Instance
window.store = new StateStore();
console.log('GharKharch Database State Initialized.');
