import { 
    ENVS_KEY, SELECTED_ENV_KEY, 
    getEnvironments, saveEnvironments, getSelectedEnvName, setSelectedEnvName,
    state, envModal
} from './core.js';
import { escapeHtml } from './utils.js';

export function initEnvironments() {
    renderEnvSelector();
    setupEnvEventListeners();
}

const envSelect = document.getElementById('env-select');
const manageEnvsBtn = document.getElementById('manage-envs-btn');
const addEnvQuickBtn = document.getElementById('add-env-quick-btn');
const envList = document.getElementById('env-list');
const envEditPanel = document.getElementById('env-edit-panel');
const envTableView = document.getElementById('env-table-view');
const envJsonView = document.getElementById('env-json-view');
const envJsonTextarea = document.getElementById('env-json-textarea');
const toggleEnvFormatBtn = document.getElementById('toggle-env-format-btn');
const applyEnvJsonBtn = document.getElementById('apply-env-json-btn');
const envNameInput = document.getElementById('env-name-input');
const envTbody = document.getElementById('env-vars-tbody');
const addEnvVarBtn = document.getElementById('add-env-var-btn');
const saveEnvsBtn = document.getElementById('save-envs-btn');
const closeEnvModal = document.querySelector('.close-env-modal');
const addNewEnvBtn = document.getElementById('add-new-env-btn');

let currentEnvName = '';

function setupEnvEventListeners() {
    if (manageEnvsBtn) manageEnvsBtn.onclick = () => showEnvModal();
    if (addEnvQuickBtn) addEnvQuickBtn.onclick = () => {
        showEnvModal();
        addNewEnv();
    };
    if (addNewEnvBtn) addNewEnvBtn.onclick = () => addNewEnv();
    if (addEnvVarBtn) addEnvVarBtn.onclick = () => addEnvVarRow();
    if (closeEnvModal) closeEnvModal.onclick = () => envModal.style.display = 'none';
    
    if (toggleEnvFormatBtn) toggleEnvFormatBtn.onclick = () => {
        if (envTableView.style.display === 'none') {
            envTableView.style.display = 'block';
            envJsonView.style.display = 'none';
            toggleEnvFormatBtn.textContent = 'Switch to JSON';
        } else {
            envTableView.style.display = 'none';
            envJsonView.style.display = 'flex';
            toggleEnvFormatBtn.textContent = 'Switch to Table';
            
            const envs = {};
            envTbody.querySelectorAll('tr').forEach(tr => {
                const keyInput = tr.querySelector('.env-key');
                const valueInput = tr.querySelector('.env-value');
                if (keyInput && valueInput) {
                    const key = keyInput.value.trim();
                    const value = valueInput.value.trim();
                    if (key) envs[key] = value;
                }
            });
            envJsonTextarea.value = JSON.stringify(envs, null, 2);
        }
    };

    if (applyEnvJsonBtn) applyEnvJsonBtn.onclick = async () => {
        try {
            const envs = JSON.parse(envJsonTextarea.value);
            envTbody.innerHTML = '';
            for (const [key, value] of Object.entries(envs)) {
                await addEnvVarRow(key, value);
            }
            if (Object.keys(envs).length === 0) await addEnvVarRow('', '');
            envTableView.style.display = 'block';
            envJsonView.style.display = 'none';
            toggleEnvFormatBtn.textContent = 'Switch to JSON';
        } catch (e) {
            alert("Invalid JSON: " + e.message);
        }
    };

    if (saveEnvsBtn) saveEnvsBtn.onclick = () => saveAllEnvs();
    
    if (envNameInput) envNameInput.oninput = (e) => {
        const newName = e.target.value.trim();
        if (newName && currentEnvName && newName !== currentEnvName) {
            const envs = getEnvironments();
            if (!envs[newName]) {
                const activeLi = envList.querySelector('li.active');
                if (activeLi) activeLi.textContent = newName;
                envs[newName] = envs[currentEnvName];
                delete envs[currentEnvName];
                if (getSelectedEnvName() === currentEnvName) setSelectedEnvName(newName);
                currentEnvName = newName;
                saveEnvironments(envs);
                renderEnvSelector();
            }
        }
    };

    const selector = document.getElementById('env-select');
    if (selector) selector.onchange = (e) => setSelectedEnvName(e.target.value);
}

function showEnvModal() {
    renderEnvList();
    envModal.style.display = 'block';
    envEditPanel.style.setProperty('display', 'none', 'important');
    currentEnvName = '';
}

function renderEnvList() {
    const envs = getEnvironments();
    envList.innerHTML = '';
    Object.keys(envs).forEach(name => {
        const li = document.createElement('li');
        li.textContent = name;
        li.onclick = () => editEnvironment(name);
        envList.appendChild(li);
    });
}

function addNewEnv() {
    const envs = getEnvironments();
    let name = 'New Environment';
    let i = 1;
    while (envs[name]) {
        name = `New Environment ${i++}`;
    }
    envs[name] = {};
    saveEnvironments(envs);
    renderEnvList();
    editEnvironment(name);
}

async function editEnvironment(name) {
    if (currentEnvName && currentEnvName !== name) saveCurrentEnvToTemp();
    currentEnvName = name;
    envNameInput.value = name;
    
    envList.querySelectorAll('li').forEach(li => {
        li.classList.toggle('active', li.textContent === name);
    });
    
    const envs = getEnvironments();
    const vars = envs[name] || {};
    envTbody.innerHTML = '';
    for (const [key, value] of Object.entries(vars)) {
        await addEnvVarRow(key, value);
    }
    if (Object.keys(vars).length === 0) await addEnvVarRow('', '');
    
    envTableView.style.display = 'block';
    envJsonView.style.display = 'none';
    toggleEnvFormatBtn.textContent = 'Switch to JSON';
    envEditPanel.style.setProperty('display', 'flex', 'important');
}

async function addEnvVarRow(key = '', value = '') {
    const tr = document.createElement('tr');
    const isSensitive = ['password', 'apikey', 'dbpassword'].includes(key.toLowerCase());
    const displayValue = isSensitive && value.startsWith('{enc}') ? '********' : value;
    
    tr.innerHTML = `
        <td style="padding: 8px;"><input type="text" class="env-key" value="${escapeHtml(key)}" placeholder="Key"></td>
        <td style="padding: 8px;"><input type="${isSensitive ? 'password' : 'text'}" class="env-value" value="${escapeHtml(displayValue)}" placeholder="Value"></td>
        <td style="padding: 8px; vertical-align: middle;"><button class="btn-delete-row" title="Delete Variable">&times;</button></td>
    `;
    
    const keyInput = tr.querySelector('.env-key');
    const valueInput = tr.querySelector('.env-value');
    
    keyInput.oninput = () => {
        const sensitive = ['password', 'apikey', 'dbpassword'].includes(keyInput.value.toLowerCase());
        valueInput.type = sensitive ? 'password' : 'text';
    };

    tr.querySelector('.btn-delete-row').onclick = () => {
        tr.remove();
        if (envTbody.children.length === 0) addEnvVarRow();
    };
    envTbody.appendChild(tr);
}

function saveCurrentEnvToTemp() {
    if (!currentEnvName) return;
    const envs = getEnvironments();
    const vars = {};
    
    if (envJsonView.style.display !== 'none') {
        try {
            Object.assign(vars, JSON.parse(envJsonTextarea.value));
        } catch (e) {}
    } else {
        envTbody.querySelectorAll('tr').forEach(tr => {
            const key = tr.querySelector('.env-key').value.trim();
            let value = tr.querySelector('.env-value').value.trim();
            if (key) vars[key] = value;
        });
    }
    envs[currentEnvName] = vars;
    saveEnvironments(envs);
}

async function saveAllEnvs() {
    saveCurrentEnvToTemp();
    const envs = getEnvironments();
    
    for (const [envName, vars] of Object.entries(envs)) {
        for (const [key, value] of Object.entries(vars)) {
            if (['password', 'apikey', 'dbpassword'].includes(key.toLowerCase()) && value && !value.startsWith('{enc}') && !value.includes('********')) {
                try {
                    const resp = await fetch('/api/crypto/encrypt', {
                        method: 'POST',
                        body: value
                    });
                    if (resp.status === 401) {
                        alert('Session expired. Please login again to encrypt sensitive variables.');
                        return;
                    }
                    const encrypted = await resp.text();
                    if (encrypted.startsWith('{enc}')) vars[key] = encrypted;
                } catch (e) {
                    console.error('Encryption failed', e);
                }
            }
        }
    }
    
    saveEnvironments(envs);
    renderEnvSelector();
    alert('All environments saved successfully.');
}

export function renderEnvSelector() {
    const selector = document.getElementById('env-select');
    if (!selector) return;
    
    const envs = getEnvironments();
    const selected = getSelectedEnvName();
    
    selector.innerHTML = '<option value="">No Environment</option>';
    Object.keys(envs).forEach(name => {
        const option = document.createElement('option');
        option.value = name;
        option.textContent = name;
        if (name === selected) option.selected = true;
        selector.appendChild(option);
    });
}

export function getSelectedEnvVars() {
    const selected = getSelectedEnvName();
    if (!selected) return {};
    const envs = getEnvironments();
    return envs[selected] || {};
}
