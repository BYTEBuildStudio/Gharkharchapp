/**
 * GharKharch - Main Applications Controller & View Coordinator
 * Powers custom state router pipelines, forms controller, custom SVG charts generator,
 * live terminal renderer, and Indian Rupee formatter.
 */

class GharKharchApp {
    constructor() {
        this.store = window.store;
        this.sheets = window.sheetsSync;
        this.init();
    }

    init() {
        this.grabDOMElements();
        this.registerEvents();
        this.populateCategorySelectElements();
        this.checkAuthStatus();
        this.startRealTimeClock();
    }

    grabDOMElements() {
        // Screens
        this.authRoot = document.getElementById('auth-screen-root');
        this.appRoot = document.getElementById('app-screen-root');
        
        // Navigation targets
        this.navItems = document.querySelectorAll('.nav-item, .navigator-mobile-btn');
        this.views = document.querySelectorAll('.page-view');
        this.topTitle = document.getElementById('top-title');

        // Authentication Inputs
        this.tabLogin = document.getElementById('tab-login');
        this.tabSignup = document.getElementById('tab-signup');
        this.nameGroup = document.getElementById('name-group');
        this.authForm = document.getElementById('auth-form');
        this.authSubmitBtn = document.getElementById('auth-submit-btn');
        this.submitBtnText = document.getElementById('submit-btn-text');
        this.authDemoBtn = document.getElementById('auth-demo-btn');
        this.authNameInput = document.getElementById('auth-name');
        this.authEmailInput = document.getElementById('auth-email');
        this.authPasswordInput = document.getElementById('auth-password');

        // Navigation Footer Elements
        this.userAvatar = document.getElementById('user-avatar');
        this.userDisplayName = document.getElementById('user-display-name');
        this.userDisplayEmail = document.getElementById('user-display-email');
        this.clockField = document.getElementById('clock-timestamp');
        this.themeToggleBtn = document.getElementById('theme-toggle-btn');

        // Onboarding card
        this.onboardingCard = document.getElementById('onboarding-card');
        this.onboardDemoBtn = document.getElementById('onboard-demo-btn');
        this.onboardAddBtn = document.getElementById('onboard-add-btn');

        // Metrics Counters
        this.metricToday = document.getElementById('metric-today-val');
        this.metricWeek = document.getElementById('metric-week-val');
        this.metricMonth = document.getElementById('metric-month-val');
        this.metricMomIndicator = document.getElementById('metric-mom-indicator');

        // SVG Charts container
        this.donutSvg = document.getElementById('donut-chart-svg');
        this.chartTotalOutlay = document.getElementById('chart-total-outlay');
        this.donutLegend = document.getElementById('donut-legend');
        this.barChartBars = document.getElementById('bar-chart-bars');
        this.topRanksRoot = document.getElementById('top-ranks-root');

        // Reports View Filters
        this.filterSearch = document.getElementById('filter-search');
        this.filterCategory = document.getElementById('filter-category');
        this.filterPayment = document.getElementById('filter-payment');
        this.expensesScroller = document.getElementById('expenses-scroller-root');

        // Settings inputs
        this.settingsUser = document.getElementById('settings-user-name');
        this.settingsEmail = document.getElementById('settings-user-email');
        this.settingsSpreadsheet = document.getElementById('settings-spreadsheet-id');
        this.syncNowBtn = document.getElementById('sync-now-btn');
        this.logoutBtn = document.getElementById('logout-btn');
        this.customOAuthToken = document.getElementById('custom-oauth-token');
        this.seedDataBtn = document.getElementById('seed-data-btn');
        this.wipeDataBtn = document.getElementById('wipe-data-btn');

        // Console Log terminal block
        this.terminalScroller = document.getElementById('terminal-scroller');
        this.terminalBeacon = document.getElementById('terminal-beacon');

        // Add Spend Modal Controls
        this.globalFabBtn = document.getElementById('global-fab-btn');
        this.spendModal = document.getElementById('spend-modal');
        this.closeSpendModal = document.getElementById('close-spend-modal');
        this.btnCancelSpend = document.getElementById('cancel-spend-modal');
        this.spendForm = document.getElementById('spend-form');
        this.spendAmount = document.getElementById('spend-amount');
        this.spendCategory = document.getElementById('spend-category');
        this.spendDateInput = document.getElementById('spend-date');
        this.spendPayment = document.getElementById('spend-payment');
        this.spendNote = document.getElementById('spend-note');
        this.editIdField = document.getElementById('edit-id-field');
    }

    registerEvents() {
        // Theme config
        this.themeToggleBtn.addEventListener('click', () => this.toggleTheme());

        // Nav switch routers
        this.navItems.forEach(item => {
            item.addEventListener('click', (e) => {
                e.preventDefault();
                const target = item.getAttribute('data-target');
                this.switchPageView(target);
            });
        });

        // Authentication tabs switch
        this.tabLogin.addEventListener('click', () => this.toggleAuthTab(false));
        this.tabSignup.addEventListener('click', () => this.toggleAuthTab(true));

        this.authForm.addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleAuthSubmit();
        });

        this.authDemoBtn.addEventListener('click', () => {
            this.store.logIn('familysteward@gharkharch.org');
            this.store.prepopulateSampleData();
            this.checkAuthStatus();
        });

        // Add spend Modal open and close
        this.globalFabBtn.addEventListener('click', () => this.openSpendFormModal());
        this.onboardAddBtn.addEventListener('click', () => this.openSpendFormModal());
        this.closeSpendModal.addEventListener('click', () => this.closeSpendFormModal());
        this.btnCancelSpend.addEventListener('click', () => this.closeSpendFormModal());
        
        this.spendForm.addEventListener('submit', (e) => {
            e.preventDefault();
            this.handleSpendSubmit();
        });

        // Filter hooks
        const filterTrigger = () => this.renderLedgerList();
        this.filterSearch.addEventListener('input', filterTrigger);
        this.filterCategory.addEventListener('change', filterTrigger);
        this.filterPayment.addEventListener('change', filterTrigger);

        // Core Actions settings triggers
        this.syncNowBtn.addEventListener('click', () => this.syncNowCloudActions());
        this.logoutBtn.addEventListener('click', () => {
            this.store.logOut();
            this.checkAuthStatus();
        });

        this.customOAuthToken.addEventListener('input', (e) => {
            this.store.setCustomToken(e.target.value);
            this.renderSettingsPanel();
            this.renderConsoleLogs();
        });

        this.seedDataBtn.addEventListener('click', () => {
            this.store.prepopulateSampleData();
            this.sheets.sync('PRESAMPLE_LOADED');
            this.renderUI();
            this.showToast('Prepopulated budget database!');
        });

        this.onboardDemoBtn.addEventListener('click', () => {
            this.store.prepopulateSampleData();
            this.sheets.sync('PRESAMPLE_LOADED');
            this.renderUI();
            this.showToast('Preset dataset loaded successfully!');
        });

        this.wipeDataBtn.addEventListener('click', () => {
            if (confirm("Are you sure you want to drop all household ledger transactions?")) {
                this.store.clearAllData();
                this.sheets.sync('TRUNCATE_DB');
                this.renderUI();
                this.showToast('Local database tables dropped');
            }
        });
    }

    startRealTimeClock() {
        const update = () => {
            const now = new Date();
            const dateFormatted = now.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
            const timeFormatted = now.toLocaleTimeString('en-IN', { hour12: false });
            this.clockField.textContent = `${dateFormatted} | ${timeFormatted}`;
        };
        update();
        setInterval(update, 1000);
    }

    toggleTheme() {
        const currentTheme = document.documentElement.getAttribute('data-theme');
        const nextTheme = currentTheme === 'light' ? 'dark' : 'light';
        document.documentElement.setAttribute('data-theme', nextTheme);
        this.store.theme = nextTheme;
        this.store.save();
        
        const themeIcon = this.themeToggleBtn.querySelector('.material-icons');
        themeIcon.textContent = nextTheme === 'light' ? 'dark_mode' : 'light_mode';
    }

    checkAuthStatus() {
        const user = this.store.activeUser;
        if (user) {
            this.authRoot.style.display = 'none';
            this.appRoot.style.display = 'flex';
            
            // Apply profile metadata layout fields
            this.userAvatar.textContent = user.name.charAt(0).toUpperCase();
            this.userDisplayName.textContent = user.name;
            this.userDisplayEmail.textContent = user.email;
            
            // If OAuth previously active, preset the credentials token input
            this.customOAuthToken.value = this.store.googleAccessToken;

            // Load initial visual components
            document.documentElement.setAttribute('data-theme', this.store.theme);
            const themeIcon = this.themeToggleBtn.querySelector('.material-icons');
            themeIcon.textContent = this.store.theme === 'light' ? 'dark_mode' : 'light_mode';

            this.renderUI();
        } else {
            this.authRoot.style.display = 'flex';
            this.appRoot.style.display = 'none';
        }
    }

    toggleAuthTab(isSignup) {
        if (isSignup) {
            this.tabLogin.classList.remove('active');
            this.tabSignup.classList.add('active');
            this.nameGroup.style.display = 'block';
            this.submitBtnText.textContent = "Register & Sync Dashboard";
        } else {
            this.tabSignup.classList.remove('active');
            this.tabLogin.classList.add('active');
            this.nameGroup.style.display = 'none';
            this.submitBtnText.textContent = "Authorize & Sync";
        }
    }

    handleAuthSubmit() {
        const email = this.authEmailInput.value;
        const name = this.authNameInput.value;
        const isSignup = this.tabSignup.classList.contains('active');

        if (!email) {
            alert('Family head email is required.');
            return;
        }

        if (isSignup) {
            if (!name) {
                alert('Household head Registry Name is required for registration.');
                return;
            }
            this.store.signUp(name, email);
        } else {
            this.store.logIn(email);
        }

        this.sheets.sync('ACCESS_CREDENTIALS');
        this.checkAuthStatus();
    }

    populateCategorySelectElements() {
        // Clear old items first
        let htmlOptionMarkup = '';
        this.store.categories.forEach(cat => {
            htmlOptionMarkup += `<option value="${cat.name}">${cat.name}</option>`;
        });
        
        this.spendCategory.innerHTML = htmlOptionMarkup;
        this.filterCategory.innerHTML = '<option value="">All Categories</option>' + htmlOptionMarkup;
    }

    switchPageView(targetViewId) {
        this.views.forEach(view => {
            if (view.id === targetViewId) {
                view.classList.add('active');
            } else {
                view.classList.remove('active');
            }
        });

        this.navItems.forEach(item => {
            if (item.getAttribute('data-target') === targetViewId) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        });

        // Dynamic header updates
        switch (targetViewId) {
            case 'dashboard-page':
                this.topTitle.textContent = "Household Dashboard";
                break;
            case 'reports-page':
                this.topTitle.textContent = "Family Ledger";
                break;
            case 'settings-page':
                this.topTitle.textContent = "Developer Configuration";
                break;
        }

        window.scrollTo(0, 0);
    }

    renderUI() {
        this.renderMetricsCounters();
        this.renderSpendDonutChart();
        this.renderTrendBarChart();
        this.renderTopSpentRanks();
        this.renderLedgerList();
        this.renderSettingsPanel();
        this.renderConsoleLogs();
        
        // Show/Hide Sandbox onboarding badge
        if (this.store.expenses.length === 0) {
            this.onboardingCard.style.display = 'block';
        } else {
            this.onboardingCard.style.display = 'none';
        }
    }

    renderMetricsCounters() {
        const todayStart = new Date().setHours(0,0,0,0);
        const weekStart = todayStart - (6 * 24 * 3600 * 1000);
        
        const dateObj = new Date();
        const currentMonthStart = new Date(dateObj.getFullYear(), dateObj.getMonth(), 1).getTime();
        
        const prevMonthCalStart = new Date(dateObj.getFullYear(), dateObj.getMonth() - 1, 1).getTime();
        const prevMonthCalEnd = currentMonthStart - 1;

        // Perform counter aggregations
        let spendToday = 0;
        let spendWeek = 0;
        let spendMonth = 0;
        let spendPrevMonth = 0;

        this.store.expenses.forEach(e => {
            if (e.dateMillis >= todayStart) spendToday += e.amount;
            if (e.dateMillis >= weekStart) spendWeek += e.amount;
            if (e.dateMillis >= currentMonthStart) spendMonth += e.amount;
            if (e.dateMillis >= prevMonthCalStart && e.dateMillis <= prevMonthCalEnd) spendPrevMonth += e.amount;
        });

        this.metricToday.textContent = this.formatCurrency(spendToday);
        this.metricWeek.textContent = this.formatCurrency(spendWeek);
        this.metricMonth.textContent = this.formatCurrency(spendMonth);

        // MoM indexing calculations
        if (spendPrevMonth > 0) {
            const diffPercent = ((spendMonth - spendPrevMonth) / spendPrevMonth) * 100;
            const isUp = spendMonth > spendPrevMonth;
            const sign = isUp ? '▲' : '▼';
            const cls = isUp ? 'up' : 'down';
            
            this.metricMomIndicator.className = `indicator-row ${cls}`;
            this.metricMomIndicator.innerHTML = `
                <i class="material-icons" style="font-size:12px;">${isUp ? 'trending_up' : 'trending_down'}</i>
                <span>${sign} ${Math.abs(diffPercent).toFixed(1)}% MoM (Prev Month: ${this.formatCurrency(spendPrevMonth)})</span>
            `;
        } else {
            this.metricMomIndicator.className = 'indicator-row-variant';
            this.metricMomIndicator.style.fontSize = '12px';
            this.metricMomIndicator.style.color = 'var(--on-surface-variant)';
            this.metricMomIndicator.innerHTML = `<span>Prev Month total: ${this.formatCurrency(0)}</span>`;
        }
    }

    renderSpendDonutChart() {
        const dateObj = new Date();
        const startOfMonth = new Date(dateObj.getFullYear(), dateObj.getMonth(), 1).getTime();
        
        // Group amounts
        const monthlyExp = this.store.expenses.filter(e => e.dateMillis >= startOfMonth);
        const mapCategoryTotal = {};
        let totalOutlay = 0;

        monthlyExp.forEach(e => {
            mapCategoryTotal[e.category] = (mapCategoryTotal[e.category] || 0) + e.amount;
            totalOutlay += e.amount;
        });

        this.chartTotalOutlay.textContent = this.formatCurrency(totalOutlay, true);

        // Clean SVG donut segments
        const existingSegments = this.donutSvg.querySelectorAll('.donut-segment');
        existingSegments.forEach(el => el.remove());

        if (totalOutlay === 0) {
            this.donutLegend.innerHTML = '<div class="no-records">No monthly transctions.</div>';
            return;
        }

        // Generate circular segments
        let accumPercent = 0;
        let legendHTML = '';

        Object.keys(mapCategoryTotal)
            .sort((a,b) => mapCategoryTotal[b] - mapCategoryTotal[a])
            .forEach(catName => {
                const amount = mapCategoryTotal[catName];
                const catObj = this.store.categories.find(c => c.name === catName) || { color: '#9E9E9E' };
                const pct = (amount / totalOutlay) * 100;

                // Create SVG Circle stroke properties representing coordinates
                const strokeDashOffset = 100 - accumPercent + 25; // Offset calibration inside 42 unit baseline circle
                const circleElement = document.createElementNS("http://www.w3.org/2000/svg", "circle");
                circleElement.setAttribute('class', 'donut-segment');
                circleElement.setAttribute('cx', '21');
                circleElement.setAttribute('cy', '21');
                circleElement.setAttribute('r', '15.91549430918954');
                circleElement.setAttribute('fill', 'transparent');
                circleElement.setAttribute('stroke', catObj.color);
                circleElement.setAttribute('stroke-width', '3.2');
                circleElement.setAttribute('stroke-dasharray', `${pct} ${100 - pct}`);
                circleElement.setAttribute('stroke-dashoffset', strokeDashOffset.toString());
                
                this.donutSvg.appendChild(circleElement);
                accumPercent += pct;

                // Form legend layout row
                legendHTML += `
                    <div class="legend-item">
                        <div style="display:flex; align-items:center;">
                            <span class="legend-color-dot" style="background-color:${catObj.color}"></span>
                            <span>${catName}</span>
                        </div>
                        <span style="color:var(--on-surface-variant)">${pct.toFixed(0)}%</span>
                    </div>
                `;
            });

        this.donutLegend.innerHTML = legendHTML;
    }

    renderTrendBarChart() {
        this.barChartBars.innerHTML = '';
        const dayLabels = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
        const listWeekValues = [];

        // Fetch aggregation for each of the last 7 days
        for (let i = 6; i >= 0; i--) {
            const date = new Date();
            date.setDate(date.getDate() - i);
            date.setHours(0,0,0,0);
            
            const startOfDay = date.getTime();
            const endOfDay = startOfDay + (24 * 3600 * 1000) - 1;
            
            const labelStr = dayLabels[date.getDay()];
            let dailyTotal = 0;

            this.store.expenses.forEach(e => {
                if (e.dateMillis >= startOfDay && e.dateMillis <= endOfDay) {
                    dailyTotal += e.amount;
                }
            });

            listWeekValues.push({ label: labelStr, amount: dailyTotal });
        }

        const maxAmount = Math.max(...listWeekValues.map(v => v.amount), 1000);

        listWeekValues.forEach(item => {
            const pct = (item.amount / maxAmount) * 100;
            const barHTML = `
                <div class="bar-column">
                    <div class="bar-pillar" style="height:${Math.max(pct, 5)}%">
                        <div class="bar-tooltip">₹${Math.round(item.amount)}</div>
                    </div>
                    <span class="bar-label">${item.label}</span>
                </div>
            `;
            this.barChartBars.insertAdjacentHTML('beforeend', barHTML);
        });
    }

    renderTopSpentRanks() {
        const dateObj = new Date();
        const startOfMonth = new Date(dateObj.getFullYear(), dateObj.getMonth(), 1).getTime();
        const monthlyExp = this.store.expenses.filter(e => e.dateMillis >= startOfMonth);

        const maps = {};
        monthlyExp.forEach(e => {
            maps[e.category] = (maps[e.category] || 0) + e.amount;
        });

        const ranks = Object.keys(maps)
            .map(k => ({ name: k, amount: maps[k] }))
            .sort((a,b) => b.amount - a.amount)
            .slice(0, 3); // Take top 3 spent

        if (ranks.length === 0) {
            this.topRanksRoot.innerHTML = '<div class="no-records" style="padding:16px;">No dynamic ledger events tracked this calendar month.</div>';
            return;
        }

        let ranksHTML = '';
        ranks.forEach(rank => {
            const catObj = this.store.categories.find(c => c.name === rank.name) || { icon: 'category', color: '#9E9E9E' };
            ranksHTML += `
                <div class="top-rank-item">
                    <div class="rank-left-cluster">
                        <div class="rank-item-icon-box" style="background-color:${catObj.color}22; color:${catObj.color}">
                            <i class="material-icons">${catObj.icon}</i>
                        </div>
                        <span class="rank-item-title">${rank.name}</span>
                    </div>
                    <span class="rank-item-value">${this.formatCurrency(rank.amount)}</span>
                </div>
            `;
        });

        this.topRanksRoot.innerHTML = ranksHTML;
    }

    renderLedgerList() {
        const query = this.filterSearch.value.trim().toLowerCase();
        const selCat = this.filterCategory.value;
        const selPay = this.filterPayment.value;

        // Apply robust searching filter
        const filtered = this.store.expenses.filter(e => {
            const matchQuery = !query || e.note.toLowerCase().includes(query) || e.category.toLowerCase().includes(query);
            const matchCategory = !selCat || e.category === selCat;
            const matchPayment = !selPay || e.paymentMethod === selPay;
            return matchQuery && matchCategory && matchPayment;
        });

        // Sort descending by transaction date
        filtered.sort((a,b) => b.dateMillis - a.dateMillis);

        if (filtered.length === 0) {
            this.expensesScroller.innerHTML = `
                <div class="no-records">
                    <i class="material-icons">search_off</i>
                    <p style="font-size:14px; font-weight:700;">No transactions matched your filtering criteria.</p>
                </div>
            `;
            return;
        }

        let scrollerHTML = '';
        filtered.forEach(exp => {
            const catObj = this.store.categories.find(c => c.name === exp.category) || { icon: 'category', color: '#9E9E9E' };
            const formattedDate = new Date(exp.dateMillis).toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
            
            scrollerHTML += `
                <div class="expense-row-item">
                    <div>
                        <span class="row-icon-span" style="background-color:${catObj.color}22; color:${catObj.color}">
                            <i class="material-icons" style="font-size:20px;">${catObj.icon}</i>
                        </span>
                    </div>
                    <div>
                        <div class="row-primary-txt">${exp.category}</div>
                        <div class="row-secondary-txt">${exp.note || 'No notes loaded'}</div>
                    </div>
                    <div>
                        <div class="row-primary-txt" style="font-family:var(--font-mono); font-size:12px;">${formattedDate}</div>
                    </div>
                    <div class="row-payment-cell">
                        <span style="font-size:11px; font-weight:700; background:var(--surface-variant); padding:4px 8px; border-radius:12px;">${exp.paymentMethod}</span>
                    </div>
                    <div>
                        <div class="row-value">-₹${exp.amount}</div>
                        <div class="row-actions">
                            <button class="btn-row-action" onclick="app.editExpenseAction('${exp.id}')" aria-label="Edit spend"><i class="material-icons" style="font-size:16px;">edit</i></button>
                            <button class="btn-row-action" style="color:var(--tertiary);" onclick="app.deleteExpenseAction('${exp.id}')" aria-label="Drop spend"><i class="material-icons" style="font-size:16px;">delete</i></button>
                        </div>
                    </div>
                </div>
            `;
        });

        this.expensesScroller.innerHTML = scrollerHTML;
    }

    renderSettingsPanel() {
        const user = this.store.activeUser;
        if (user) {
            this.settingsUser.textContent = user.name;
            this.settingsEmail.textContent = user.email;
            this.settingsSpreadsheet.textContent = user.spreadsheetId || 'Waiting first cloud sync...';
        }
    }

    renderConsoleLogs() {
        this.terminalScroller.innerHTML = '';
        const logs = [...this.store.syncLogs].reverse(); // Latest logs on top or bottom
        
        if (logs.length === 0) {
            this.terminalScroller.innerHTML = '<div class="terminal-line" style="color:gray;">Ready for backup operations... Trigger dynamic sync.</div>';
            return;
        }

        logs.forEach(msg => {
            let cls = '';
            if (msg.includes('Success')) cls = 'success-txt';
            else if (msg.includes('Error')) cls = 'error-txt';
            else if (msg.includes('Warn')) cls = 'warn-txt';
            
            this.terminalScroller.insertAdjacentHTML('beforeend', `<div class="terminal-line ${cls}">${msg}</div>`);
        });

        // Set live beacon pulse classes
        this.terminalBeacon.className = 'terminal-node-beac ' + this.store.syncStatus;
    }

    openSpendFormModal() {
        this.editIdField.value = '';
        this.spendForm.reset();
        this.spendDateInput.value = new Date().toISOString().split('T')[0]; // Auto select current date ISO form
        document.getElementById('spend-modal-title').textContent = "Log New Spending Transaction";
        this.spendModal.classList.add('active');
    }

    closeSpendFormModal() {
        this.spendModal.classList.remove('active');
    }

    handleSpendSubmit() {
        const id = this.editIdField.value;
        const amount = parseFloat(this.spendAmount.value);
        const category = this.spendCategory.value;
        const date = this.spendDateInput.value;
        const payment = this.spendPayment.value;
        const note = this.spendNote.value;

        if (isNaN(amount) || amount <= 0) {
            alert('Please output a valid INR amount.');
            return;
        }

        if (id) {
            this.store.updateExpense(id, amount, category, note, date, payment);
            this.sheets.sync('UPDATE_EXPENSE');
            this.showToast('Ledger record updated');
        } else {
            this.store.addExpense(amount, category, note, date, payment);
            this.sheets.sync('ADD_EXPENSE');
            this.showToast('Ghar Spend logged!');
        }

        this.closeSpendFormModal();
        this.renderUI();
    }

    editExpenseAction(id) {
        const exp = this.store.expenses.find(e => e.id === id);
        if (exp) {
            this.editIdField.value = exp.id;
            this.spendAmount.value = exp.amount;
            this.spendCategory.value = exp.category;
            this.spendDateInput.value = new Date(exp.dateMillis).toISOString().split('T')[0];
            this.spendPayment.value = exp.paymentMethod;
            this.spendNote.value = exp.note;

            document.getElementById('spend-modal-title').textContent = "Edit Spend Transaction";
            this.spendModal.classList.add('active');
        }
    }

    deleteExpenseAction(id) {
        if (confirm("Remove this spend transaction from your local database memory?")) {
            this.store.deleteExpense(id);
            this.sheets.sync('DELETE_EXPENSE');
            this.renderUI();
            this.showToast('Transaction log scrubbed');
        }
    }

    syncNowCloudActions() {
        this.sheets.sync('FORCE_SYNC_MANUAL');
        this.renderUI();
        this.showToast('Forced Google Sheets archive backup sync initiated.');
    }

    formatCurrency(amount, compact = false) {
        if (compact && amount >= 100000) {
            const lakhs = amount / 100000;
            return `₹${lakhs.toFixed(1)}L`;
        }
        return new Intl.NumberFormat('en-IN', {
            style: 'currency',
            currency: 'INR',
            maximumFractionDigits: 0
        }).format(amount);
    }

    showToast(msg) {
        const toast = document.getElementById('toast-notify-root');
        toast.textContent = msg;
        toast.style.opacity = 1;
        setTimeout(() => toast.style.opacity = 0, 3000);
    }
}

// Instantiate and coordinate App Controller
window.addEventListener('DOMContentLoaded', () => {
    window.app = new GharKharchApp();
});
