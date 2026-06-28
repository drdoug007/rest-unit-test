const globalsTestName = document.getElementById('globals-test-name');
const globalsTbody = document.getElementById('globals-tbody');
const addGlobalRowBtn = document.getElementById('add-global-row-btn');
const saveGlobalsBtn = document.getElementById('save-globals-btn');
const toggleGlobalsFormatBtn = document.getElementById('toggle-globals-format-btn');
const applyGlobalsJsonBtn = document.getElementById('apply-globals-json-btn');
const globalsTableView = document.getElementById('globals-table-view');
const globalsJsonView = document.getElementById('globals-json-view');
const globalsJsonTextarea = document.getElementById('globals-json-textarea');
const closeModal = document.querySelector('.close-modal');

import { state, getCustomGlobals, saveCustomGlobals, globalsModal, globalsBtn, escapeHtml } from './core.js';

export function setupGlobalVars() {
    if (globalsBtn) globalsBtn.onclick = () => showGlobalsModal(state.currentTestName);
    if (addGlobalRowBtn) addGlobalRowBtn.onclick = () => addGlobalRow();

    if (toggleGlobalsFormatBtn) toggleGlobalsFormatBtn.onclick = () => {
        if (globalsTableView.style.display === 'none') {
            globalsTableView.style.display = 'block';
            globalsJsonView.style.display = 'none';
            toggleGlobalsFormatBtn.textContent = 'Switch to JSON';
        } else {
            globalsTableView.style.display = 'none';
            globalsJsonView.style.display = 'flex';
            toggleGlobalsFormatBtn.textContent = 'Switch to Table';
            
            const globals = {};
            globalsTbody.querySelectorAll('tr').forEach(tr => {
                const keyInput = tr.querySelector('.global-key');
                const valueInput = tr.querySelector('.global-value');
                if (keyInput && valueInput) {
                    const key = keyInput.value.trim();
                    const value = valueInput.value.trim();
                    if (key) {
                        globals[key] = value;
                    }
                }
            });
            globalsJsonTextarea.value = JSON.stringify(globals, null, 2);
        }
    };

    if (applyGlobalsJsonBtn) applyGlobalsJsonBtn.onclick = () => {
        try {
            const globals = JSON.parse(globalsJsonTextarea.value);
            globalsTbody.innerHTML = '';
            Object.entries(globals).forEach(([key, value]) => {
                addGlobalRow(key, value);
            });
            
            if (Object.keys(globals).length === 0) {
                addGlobalRow('', '');
            }
            
            globalsTableView.style.display = 'block';
            globalsJsonView.style.display = 'none';
            toggleGlobalsFormatBtn.textContent = 'Switch to JSON';
        } catch (e) {
            alert("Invalid JSON: " + e.message);
        }
    };

    if (saveGlobalsBtn) saveGlobalsBtn.onclick = () => {
        const globals = {};
        if (globalsJsonView.style.display !== 'none') {
            try {
                const jsonGlobals = JSON.parse(globalsJsonTextarea.value);
                Object.assign(globals, jsonGlobals);
            } catch (e) {
                console.error("Failed to parse JSON on save", e);
            }
        } else {
            globalsTbody.querySelectorAll('tr').forEach(tr => {
                const key = tr.querySelector('.global-key').value.trim();
                const value = tr.querySelector('.global-value').value.trim();
                if (key) {
                    globals[key] = value;
                }
            });
        }
        if (state.currentTestName) {
            saveCustomGlobals(state.currentTestName, globals);
        } else {
            state.unsavedGlobals = globals;
        }
        globalsModal.style.display = 'none';
    };

    if (closeModal) closeModal.onclick = () => {
        globalsModal.style.display = 'none';
    };
}

export function showGlobalsModal(testName) {
    state.currentTestName = testName;
    globalsTestName.textContent = testName || 'Unsaved Test';
    const globals = testName ? (getCustomGlobals()[testName] || {}) : (state.unsavedGlobals || {});
    
    globalsTbody.innerHTML = '';
    Object.entries(globals).forEach(([key, value]) => {
        addGlobalRow(key, value);
    });
    
    if (Object.keys(globals).length === 0) {
        addGlobalRow('', '');
    }
    
    globalsTableView.style.display = 'block';
    globalsJsonView.style.display = 'none';
    toggleGlobalsFormatBtn.textContent = 'Switch to JSON';
    
    globalsModal.style.display = 'block';
}

function addGlobalRow(key = '', value = '') {
    const tr = document.createElement('tr');
    
    // Sanitize values to prevent HTML injection
    const sanitizedKey = escapeHtml(key);
    const sanitizedValue = escapeHtml(value + '');
    
    tr.innerHTML = `
        <td style="padding: 8px;"><input type="text" class="global-key" value="${sanitizedKey}" placeholder="Key"></td>
        <td style="padding: 8px;"><input type="text" class="global-value" value="${sanitizedValue}" placeholder="Value"></td>
        <td style="padding: 8px; vertical-align: middle;"><button class="btn-delete-row" title="Delete Variable">&times;</button></td>
    `;
    tr.querySelector('.btn-delete-row').onclick = () => {
        tr.remove();
        if (globalsTbody.children.length === 0) {
            addGlobalRow();
        }
    };
    globalsTbody.appendChild(tr);
}
