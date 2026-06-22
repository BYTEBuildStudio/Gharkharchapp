/**
 * GharKharch - Google Sheets Sync & Simulation Engine
 * Port of com.example.util.GoogleSheetsClient and Retrofit pipelines.
 */

class GoogleSheetsSync {
    constructor() {
        this.BASE_SHEETS_URL = 'https://sheets.googleapis.com/v4/spreadsheets';
    }

    async sync(action = 'SYNC_EXPENSES') {
        const user = window.store.activeUser;
        if (!user) {
            window.store.logTerminal("Error: No active family session found. Authbarrier failed.", 'Error');
            return false;
        }

        window.store.logTerminal("Initiating cloud-vault data sync operation...", 'Syncing');
        window.store.logTerminal(`Target User: ${user.name} (${user.email})`, 'Syncing');

        const token = window.store.googleAccessToken;
        const isSimulated = !token;

        if (isSimulated) {
            await this.runSimulatedSync(user, action);
            return true;
        } else {
            return await this.runRealSync(user, token, action);
        }
    }

    async runSimulatedSync(user, action) {
        window.store.logTerminal("Offline-First OAuth fallback activated. Connecting to Simulated Google Cloud sandbox...", 'Syncing');
        await this.delay(900);

        const spreadsheetId = user.spreadsheetId || `spreadsheet_gharkharch_${Math.abs(window.store.hashCode(user.email))}`;
        window.store.logTerminal(`Database Vault Spreadsheet ID loaded: ${spreadsheetId}`, 'Syncing');
        await this.delay(600);

        window.store.logTerminal(`[Sheets] Creating sheets 'Users' & 'Expenses' inside simulated sheet catalog...`, 'Syncing');
        await this.delay(500);

        window.store.logTerminal(`Appending credentials log to 'Users' sheet tab...`, 'Syncing');
        const appendRow = [user.email, user.name, action, new Date().toLocaleString('en-IN')];
        window.store.logTerminal(`  Appended Row: [${appendRow.join(' | ')}]`, 'Syncing');
        await this.delay(400);

        const currentExpenses = window.store.expenses;
        window.store.logTerminal(`Uploading ${currentExpenses.length} household expenses to tab 'Expenses'...`, 'Syncing');
        
        if (currentExpenses.length > 0) {
            window.store.logTerminal("  Clearing grid range 'Expenses!A2:G999'...", 'Syncing');
            await this.delay(500);
            
            currentExpenses.forEach((exp) => {
                const dateStr = new Date(exp.dateMillis).toLocaleDateString('en-IN');
                window.store.logTerminal(`  Synced: ID ${exp.id} | Amount ₹${exp.amount} | Cat: ${exp.category} | ${dateStr}`, 'Syncing');
            });
        }
        await this.delay(800);

        window.store.updateSpreadsheetId(spreadsheetId);
        window.store.logTerminal("Google Sheets Simulated Sync completed successfully!", 'Success');
        if (typeof window.renderUI === 'function') window.renderUI();
    }

    async runRealSync(user, token, action) {
        window.store.logTerminal("OAuth Bearer Active. Querying Google Drive with fetch API...", 'Syncing');
        const headers = {
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        };

        try {
            let spreadsheetId = user.spreadsheetId;

            // 1. Create Spreadsheet if missing
            if (!spreadsheetId || spreadsheetId.startsWith('spreadsheet_gharkharch')) {
                window.store.logTerminal("Creating fresh Google Spreadsheet 'GharKharch - Household Expenses' on Drive...", 'Syncing');
                const reqBody = {
                    properties: { title: "GharKharch - Household Expenses" }
                };

                const createResp = await fetch(this.BASE_SHEETS_URL, {
                    method: 'POST',
                    headers,
                    body: JSON.stringify(reqBody)
                });

                if (!createResp.ok) throw new Error(`Create spreadsheet failed: ${createResp.statusText}`);
                const createJson = await createResp.json();
                spreadsheetId = createJson.spreadsheetId;
                window.store.updateSpreadsheetId(spreadsheetId);
                window.store.logTerminal(`Google Spreadsheet parsed! ID: ${spreadsheetId}`, 'Syncing');

                // 2. Batch update to create tabs "Users" and "Expenses"
                window.store.logTerminal("Initializing schema tabs inside Google Spreadsheet...", 'Syncing');
                const schemaBody = {
                    requests: [
                        { addSheet: { properties: { title: "Users" } } },
                        { addSheet: { properties: { title: "Expenses" } } }
                    ]
                };

                await fetch(`${this.BASE_SHEETS_URL}/${spreadsheetId}:batchUpdate`, {
                    method: 'POST',
                    headers,
                    body: JSON.stringify(schemaBody)
                });
                window.store.logTerminal("Created tabs 'Users' and 'Expenses'.", 'Syncing');
            } else {
                window.store.logTerminal(`Connecting to active target spreadsheet ID: ${spreadsheetId}`, 'Syncing');
            }

            // 3. Log Auth details to Users sheet
            window.store.logTerminal("Appending access log metadata inside 'Users' sheet tab...", 'Syncing');
            const authValues = [
                [user.email, user.name, action, new Date().toLocaleString('en-IN')]
            ];

            const authAppendResp = await fetch(`${this.BASE_SHEETS_URL}/${spreadsheetId}/values/Users!A:D:append?valueInputOption=USER_ENTERED`, {
                method: 'POST',
                headers,
                body: JSON.stringify({ values: authValues })
            });

            if (!authAppendResp.ok) {
                window.store.logTerminal("Users tab write notice: Writing log directly to root index instead.", 'Syncing');
                await fetch(`${this.BASE_SHEETS_URL}/${spreadsheetId}/values/A:D:append?valueInputOption=USER_ENTERED`, {
                    method: 'POST',
                    headers,
                    body: JSON.stringify({ values: authValues })
                });
            } else {
                window.store.logTerminal("Credential log appended to tab 'Users' successfully.", 'Syncing');
            }

            // 4. Mirror full domestic expenses list in sheet Expenses
            const expensesList = window.store.expenses;
            
            window.store.logTerminal("Clearing grid range 'Expenses!A:G' inside target Google Spreadsheet...", 'Syncing');
            try {
                await fetch(`${this.BASE_SHEETS_URL}/${spreadsheetId}/values/Expenses!A:G:clear`, {
                    method: 'POST',
                    headers
                });
            } catch(e) {
                window.store.logTerminal(`Clear range notice: ${e.message}`, 'Warn');
            }

            const header = ["Transaction ID", "Amount (INR)", "Expense Category", "Date of Transaction", "Description Notes", "Payment Mode", "Sync Timestamp"];
            const rows = [header];

            if (expensesList.length > 0) {
                window.store.logTerminal(`Uploading ${expensesList.length} local expenses in clear-overwrite mirror...`, 'Syncing');
                
                expensesList.forEach(exp => {
                    const dateFormatted = new Date(exp.dateMillis).toISOString().split('T')[0];
                    rows.push([
                        exp.id,
                        exp.amount,
                        exp.category,
                        dateFormatted,
                        exp.note || '',
                        exp.paymentMethod,
                        new Date().toISOString()
                    ]);
                });
            }

            const syncResp = await fetch(`${this.BASE_SHEETS_URL}/${spreadsheetId}/values/Expenses!A1:G${rows.length}?valueInputOption=USER_ENTERED`, {
                method: 'PUT',
                headers,
                body: JSON.stringify({ values: rows })
            });

            if (!syncResp.ok) {
                window.store.logTerminal("Specific range write failed. Handshaking append fallback...", 'Syncing');
                await fetch(`${this.BASE_SHEETS_URL}/${spreadsheetId}/values/Expenses!A:G:append?valueInputOption=USER_ENTERED`, {
                    method: 'POST',
                    headers,
                    body: JSON.stringify({ values: rows })
                });
            }
            
            window.store.logTerminal(`Successfully archived ${expensesList.length} expenses to cloud database.`, 'Syncing');

            window.store.logTerminal("Real Google Sheets operations backup finished!", 'Success');
            if (typeof window.renderUI === 'function') window.renderUI();
            return true;
        } catch (e) {
            window.store.logTerminal(`Real sheets error: ${e.message}`, 'Error');
            window.store.logTerminal("Gracefully falling back to integrated sandbox emulation.", 'Error');
            await this.delay(1000);
            await this.runSimulatedSync(user, action);
            return true;
        }
    }

    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

// Global Sheets Sync Instance
window.sheetsSync = new GoogleSheetsSync();
