export const testList = document.getElementById('test-list');
export const reportContent = document.getElementById('report-content');
export const exportBtn = document.getElementById('export-btn');
export const exportDropdown = document.getElementById('export-dropdown');
export const exportOptions = document.getElementById('export-options');
export const exportPdfBtn = document.getElementById('export-pdf-btn');
export const exportWordBtn = document.getElementById('export-word-btn');
export const sourceBtn = document.getElementById('source-btn');
export const cloneBtn = document.getElementById('clone-btn');
export const runViewBtn = document.getElementById('run-view-btn');
export const debugViewBtn = document.getElementById('debug-view-btn');
export const saveCustomBtn = document.getElementById('save-custom-btn');
export const runCustomBtn = document.getElementById('run-custom-btn');
export const debugCustomBtn = document.getElementById('debug-custom-btn');
export const globalsBtn = document.getElementById('globals-btn');
export const addTestBtn = document.getElementById('add-test-btn');
export const importBtnSidebar = document.getElementById('import-btn-sidebar');
export const importOptions = document.getElementById('import-options');
export const importFileBtn = document.getElementById('import-file-btn');
export const importUrlBtn = document.getElementById('import-url-btn');
export const importPasteBtn = document.getElementById('import-paste-btn');
export const importFileInput = document.getElementById('import-file-input');
export const sourceEditor = document.getElementById('source-editor');

// Modals
export const envModal = document.getElementById('env-modal');
export const globalsModal = document.getElementById('globals-modal');

export let state = {
    currentTestName: '',
    isCustomTest: false,
    isViewingSource: false,
    isEditing: false,
    lastMarkdown: '',
    lastSource: '',
    unsavedGlobals: null
};

// Local Storage Helpers
export const STORAGE_KEY = 'custom_http_tests';
export const GLOBALS_KEY = 'custom_http_globals';
export const ENVS_KEY = 'custom_http_envs';
export const SELECTED_ENV_KEY = 'selected_env_name';

// CodeMirror editor instance
export let editor = null;
export function setEditor(e) { editor = e; window.editor = e; }

export function setEditorContent(content) {
    if (editor) {
        editor.dispatch({
            changes: { from: 0, to: editor.state.doc.length, insert: content }
        });
    }
}

export function getCustomTests() {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? JSON.parse(stored) : {};
}

export function getCustomGlobals() {
    const stored = localStorage.getItem(GLOBALS_KEY);
    return stored ? JSON.parse(stored) : {};
}

export function getEnvironments() {
    const stored = localStorage.getItem(ENVS_KEY);
    return stored ? JSON.parse(stored) : {};
}

export function saveEnvironments(envs) {
    localStorage.setItem(ENVS_KEY, JSON.stringify(envs));
}

export function getSelectedEnvName() {
    return localStorage.getItem(SELECTED_ENV_KEY) || '';
}

export function setSelectedEnvName(name) {
    localStorage.setItem(SELECTED_ENV_KEY, name);
}

export function saveCustomTest(name, content) {
    const tests = getCustomTests();
    tests[name] = content;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tests));
}

export function saveCustomGlobals(name, globals) {
    const allGlobals = getCustomGlobals();
    allGlobals[name] = globals;
    localStorage.setItem(GLOBALS_KEY, JSON.stringify(allGlobals));
}

export function escapeHtml(text) {
    if (typeof text !== 'string') return text;
    return text.replace(/&/g, '&amp;')
               .replace(/</g, '&lt;')
               .replace(/>/g, '&gt;')
               .replace(/"/g, '&quot;')
               .replace(/'/g, '&#39;');
}

export function showAssertionToast(message) {
    let toast = document.querySelector('.assertion-toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.className = 'assertion-toast';
        document.body.appendChild(toast);
    }
    toast.textContent = message;
    toast.classList.add('show');
    setTimeout(() => {
        toast.classList.remove('show');
    }, 3000);
}
