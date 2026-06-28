const testList = document.getElementById('test-list');
const reportContent = document.getElementById('report-content');
const sourceContent = document.getElementById('source-content');
const exportBtn = document.getElementById('export-btn');
const exportDropdown = document.getElementById('export-dropdown');
const exportOptions = document.getElementById('export-options');
const exportPdfBtn = document.getElementById('export-pdf-btn');
const exportWordBtn = document.getElementById('export-word-btn');
const sourceBtn = document.getElementById('source-btn');
const cloneBtn = document.getElementById('clone-btn');
const runViewBtn = document.getElementById('run-view-btn');
const saveCustomBtn = document.getElementById('save-custom-btn');
const runCustomBtn = document.getElementById('run-custom-btn');
const globalsBtn = document.getElementById('globals-btn');
const addTestBtn = document.getElementById('add-test-btn');
const importBtnSidebar = document.getElementById('import-btn-sidebar');
const importOptions = document.getElementById('import-options');
const importFileBtn = document.getElementById('import-file-btn');
const importUrlBtn = document.getElementById('import-url-btn');
const importPasteBtn = document.getElementById('import-paste-btn');
const importFileInput = document.getElementById('import-file-input');
const sourceEditor = document.getElementById('source-editor');
let cmEditor = null;
const { EditorView, EditorState, basicSetup, http, javascript, oneDark, Compartment, keymap, indentWithTab, gutter, GutterMarker, RangeSet } = CodeMirror6;

class RunMarker extends GutterMarker {
    constructor(requestLine, lineIndex) {
        super();
        this.requestLine = requestLine;
        this.lineIndex = lineIndex;
    }
    toDOM() {
        const span = document.createElement("span");
        span.className = "play-button";
        span.textContent = "▶";
        span.title = "Run this request";
        span.onclick = (e) => {
            e.stopPropagation();
            runSingleRequest(this.requestLine, this.lineIndex);
        };
        return span;
    }
}

const runGutter = gutter({
    class: "cm-run-gutter",
    renderEmptyElements: false,
    markers(view) {
        let markers = [];
        const requestLineRegex = /^(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD|TRACE) /;
        for (let {from, to} of view.visibleRanges) {
            for (let pos = from; pos <= to;) {
                let line = view.state.doc.lineAt(pos);
                if (requestLineRegex.test(line.text.trim())) {
                    markers.push(new RunMarker(line.text.trim(), line.number - 1).range(line.from));
                }
                pos = line.to + 1;
            }
        }
        return RangeSet.of(markers);
    }
});
const languageConf = new Compartment();
const themeConf = new Compartment();

function initEditor(content = '') {
    if (cmEditor) {
        cmEditor.destroy();
    }
    const isDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    
    cmEditor = new EditorView({
        state: EditorState.create({
            doc: content,
            extensions: [
                basicSetup,
                keymap.of([indentWithTab]),
                languageConf.of(http()),
                themeConf.of(isDarkMode ? oneDark : []),
                runGutter,
            ]
        }),
        parent: sourceEditor
    });
}

function setEditorContent(content) {
    if (!cmEditor) {
        initEditor(content);
    } else {
        cmEditor.dispatch({
            changes: { from: 0, to: cmEditor.state.doc.length, insert: content }
        });
    }
}

function updateEditorTheme() {
    if (!cmEditor) return;
    const isDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    cmEditor.dispatch({
        effects: themeConf.reconfigure(isDarkMode ? oneDark : [])
    });
}

const sourceCode = document.getElementById('source-code');
const sourceGutter = document.getElementById('source-gutter');

// Globals Modal Elements
const globalsModal = document.getElementById('globals-modal');
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

let currentTestName = '';
let isCustomTest = false;
let isViewingSource = false;
let isEditing = false;
let lastMarkdown = '';
let lastSource = '';
let unsavedGlobals = null;

// Local Storage Helpers
const STORAGE_KEY = 'custom_http_tests';
const GLOBALS_KEY = 'custom_http_globals';
const ENVS_KEY = 'custom_http_envs';
const SELECTED_ENV_KEY = 'selected_env_name';

function getCustomTests() {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? JSON.parse(stored) : {};
}

function getCustomGlobals() {
    const stored = localStorage.getItem(GLOBALS_KEY);
    return stored ? JSON.parse(stored) : {};
}

function getEnvironments() {
    const stored = localStorage.getItem(ENVS_KEY);
    return stored ? JSON.parse(stored) : {};
}

function saveEnvironments(envs) {
    localStorage.setItem(ENVS_KEY, JSON.stringify(envs));
}

function getSelectedEnvName() {
    return localStorage.getItem(SELECTED_ENV_KEY) || '';
}

function setSelectedEnvName(name) {
    localStorage.setItem(SELECTED_ENV_KEY, name);
}

function saveCustomTest(name, content) {
    const tests = getCustomTests();
    tests[name] = content;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tests));
}

function saveCustomGlobals(name, globals) {
    const allGlobals = getCustomGlobals();
    allGlobals[name] = globals;
    localStorage.setItem(GLOBALS_KEY, JSON.stringify(allGlobals));
}

function deleteCustomTest(name) {
    const tests = getCustomTests();
    delete tests[name];
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tests));
    
    const allGlobals = getCustomGlobals();
    delete allGlobals[name];
    localStorage.setItem(GLOBALS_KEY, JSON.stringify(allGlobals));
    
    fetchTests();
}

// Configure marked to use highlight.js
if (typeof hljs !== 'undefined') {
    marked.setOptions({
        highlight: function(code, lang) {
            try {
                const language = hljs.getLanguage(lang) ? lang : 'http';
                return hljs.highlight(code, { language }).value;
            } catch (e) {
                console.error('Highlight error:', e);
                return code;
            }
        },
        langPrefix: 'hljs language-'
    });
} else {
    console.error('highlight.js not loaded!');
}

async function fetchTests() {
    try {
        const response = await fetch('/api/tests');
        const serverTests = await response.json();
        const customTests = getCustomTests();
        
        testList.innerHTML = '';
        
        // Render Server Tests
        serverTests.forEach(test => {
            renderTestItem(test, false);
        });

        // Render Custom Tests
        Object.keys(customTests).forEach(test => {
            renderTestItem(test, true);
        });

    } catch (error) {
        testList.innerHTML = '<li style="color: red">Error loading tests</li>';
        console.error('Error:', error);
    }
}

function renderTestItem(name, isCustom) {
    const li = document.createElement('li');
    li.setAttribute('data-name', name);
    if (currentTestName === name && isCustom === isCustomTest) li.classList.add('active');
    
    const nameSpan = document.createElement('span');
    nameSpan.className = 'test-name';
    if (isCustom) {
        const tag = document.createElement('span');
        tag.className = 'custom-tag';
        tag.textContent = 'Custom';
        nameSpan.appendChild(tag);
    } else {
        const tag = document.createElement('span');
        tag.className = 'server-tag';
        tag.textContent = 'HTTP';
        nameSpan.appendChild(tag);
    }
    nameSpan.appendChild(document.createTextNode(name));
    li.onclick = () => selectTest(name, li, isCustom);
    
    li.appendChild(nameSpan);

    if (isCustom) {
        const dropdown = document.createElement('div');
        dropdown.className = 'dropdown';
        dropdown.innerHTML = `
            <button class="dropbtn">⋮</button>
            <div class="dropdown-content">
                <a href="#" onclick="deleteCustomTest('${name}')">Delete</a>
                <a href="#" onclick="renameCustomTest('${name}')">Rename</a>
                <a href="#" onclick="showGlobalsModal('${name}')">Globals</a>
            </div>
        `;
        const dropbtn = dropdown.querySelector('.dropbtn');
        dropbtn.onclick = (e) => {
            e.stopPropagation();
            document.querySelectorAll('.dropdown-content').forEach(d => {
                if (d !== dropdown.querySelector('.dropdown-content')) d.classList.remove('show');
            });
            dropdown.querySelector('.dropdown-content').classList.toggle('show');
        };
        li.appendChild(dropdown);
    }

    testList.appendChild(li);
}

function renameCustomTest(oldName) {
    const newName = prompt('Enter new name for the test:', oldName);
    if (newName && newName !== oldName) {
        const tests = getCustomTests();
        if (tests[newName]) {
            alert('A test with this name already exists.');
            return;
        }
        tests[newName] = tests[oldName];
        delete tests[oldName];
        localStorage.setItem(STORAGE_KEY, JSON.stringify(tests));
        
        const allGlobals = getCustomGlobals();
        if (allGlobals[oldName]) {
            allGlobals[newName] = allGlobals[oldName];
            delete allGlobals[oldName];
            localStorage.setItem(GLOBALS_KEY, JSON.stringify(allGlobals));
        }

        if (currentTestName === oldName) currentTestName = newName;
        fetchTests();
    }
}

// Close dropdowns when clicking outside
function showGlobalsModal(testName) {
    currentTestName = testName;
    globalsTestName.textContent = testName || 'Unsaved Test';
    const globals = testName ? (getCustomGlobals()[testName] || {}) : (unsavedGlobals || {});
    
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

addGlobalRowBtn.onclick = () => addGlobalRow();

toggleGlobalsFormatBtn.onclick = () => {
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

applyGlobalsJsonBtn.onclick = () => {
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

saveGlobalsBtn.onclick = () => {
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
    if (currentTestName) {
        saveCustomGlobals(currentTestName, globals);
    } else {
        unsavedGlobals = globals;
    }
    globalsModal.style.display = 'none';
};

globalsBtn.onclick = () => showGlobalsModal(currentTestName);

closeModal.onclick = () => {
    globalsModal.style.display = 'none';
};

window.onclick = function(event) {
    if (event.target == globalsModal) {
        globalsModal.style.display = 'none';
    }
    if (!event.target.matches('.dropbtn')) {
        document.querySelectorAll('.dropdown-content').forEach(d => d.classList.remove('show'));
    }
};

function highlightHttpSource(codeElement) {
    if (!codeElement) return;
    const isSourcePanel = codeElement.id === 'source-code';
    if (isSourcePanel) {
        sourceGutter.innerHTML = '';
    }

    try {
        // Determine language from classes or default to http
        let language = 'http';
        for (const cls of codeElement.classList) {
            if (cls.startsWith('language-')) {
                language = cls.replace('language-', '');
                break;
            }
        }

        const rawCode = codeElement.textContent;
        
        if (language === 'http') {
            // First, highlight the entire block as HTTP
            const highlightedHttp = hljs.highlight(rawCode, { language: 'http' }).value;
            
            // Now, find and highlight nested JavaScript blocks
            const regex = /(&gt;|&lt;)\s+{%([\s\S]*?)%}/g;
            const finalHtml = highlightedHttp.replace(regex, (match, prefix, content) => {
                // Decode entities to get raw JS for highlighting
                const decodedContent = content.replace(/&amp;/g, '&')
                                             .replace(/&lt;/g, '<')
                                             .replace(/&gt;/g, '>')
                                             .replace(/&quot;/g, '"')
                                             .replace(/&#39;/g, "'");
                const highlightedJs = hljs.highlight(decodedContent, { language: 'javascript' }).value;
                return `<span class="hljs-meta">${prefix} {%</span>${highlightedJs}<span class="hljs-meta">%}</span>`;
            });
            
            // Final adjustment for HTTP specific elements
            let customHighlighted = finalHtml;
            customHighlighted = customHighlighted.replace(/^### (.*)$/gm, '<span class="hljs-section">### $1</span>');
            customHighlighted = customHighlighted.replace(/^\/\/ (.*)$/gm, '<span class="hljs-comment">// $1</span>');
            customHighlighted = customHighlighted.replace(/\{\{(.*?)\}\}/g, '<span class="hljs-variable">{{$1}}</span>');
            
            if (isSourcePanel) {
                // Split into lines for gutter alignment
                const lines = customHighlighted.split('\n');
                const rawLines = rawCode.split('\n');
                const gutterLines = [];
                const processedLines = [];

                const requestLineRegex = /^(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD|TRACE) (.*)$/;
                const scriptMarkerRegex = /<span class="hljs-meta">(&gt;|&lt;)\s+{%<\/span>/;

                lines.forEach((line, index) => {
                    let markerHtml = '';
                    let processedLine = line;

                    if (scriptMarkerRegex.test(line)) {
                        markerHtml = '<span class="js-logo" title="JavaScript">JS</span>';
                    }

                    const requestMatch = line.match(requestLineRegex);
                    if (requestMatch) {
                        const [full, method, url] = requestMatch;
                        const rawLine = rawLines[index].trim();
                        markerHtml = `<span class="play-button" title="Run this test" data-request-line="${escapeHtml(rawLine)}" data-line-index="${index}">▶</span>`;
                        processedLine = `<span class="hljs-keyword">${method}</span> <span class="hljs-title">${url}</span>`;
                    }

                    gutterLines.push(`<div class="gutter-line">${markerHtml}</div>`);
                    processedLines.push(processedLine);
                });

                sourceGutter.innerHTML = gutterLines.join('');
                codeElement.innerHTML = processedLines.join('\n');

                sourceGutter.querySelectorAll('.play-button').forEach(btn => {
                    btn.onclick = (e) => {
                        e.stopPropagation();
                        const rawLine = btn.getAttribute('data-request-line');
                        const lineIndex = parseInt(btn.getAttribute('data-line-index'));
                        runSingleRequest(rawLine, lineIndex);
                    };
                });
            } else {
                const requestLineRegex = /^(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD|TRACE) (.*)$/gm;
                customHighlighted = customHighlighted.replace(requestLineRegex, (match, method, url) => {
                    return `<span class="hljs-keyword">${method}</span> <span class="hljs-title">${url}</span>`;
                });
                codeElement.innerHTML = customHighlighted;
            }
        } else if (language === 'json') {
            const raw = codeElement.textContent;
            try {
                const highlighted = hljs.highlight(raw, { language: 'json' }).value;
                codeElement.innerHTML = highlighted;
                
                // Add click listeners to keys for Visual Assertion Builder
                codeElement.querySelectorAll('.hljs-attr').forEach(el => {
                    el.style.cursor = 'pointer';
                });
            } catch (err) {
                console.warn('JSON highlight failed, falling back to highlightElement', err);
                hljs.highlightElement(codeElement);
            }
        } else {
            // Non-HTTP language (like json, xml, sql)
            // If it's XML, it might have escaped entities that need to be decoded for highlight.js
            // or we can let hljs handle it. If textContent already gives decoded text, we're good.
            if (language === 'xml' || language === 'html') {
                const raw = codeElement.textContent;
                // Try XML highlighting, but fallback to auto if it fails or isn't loaded
                try {
                    // Check if xml language is loaded
                    const hasXml = hljs.getLanguage('xml');
                    if (hasXml) {
                        const highlighted = hljs.highlight(raw, { language: 'xml' }).value;
                        codeElement.innerHTML = highlighted;
                    } else {
                        // Fallback to auto-detection which often handles XML well
                        const result = hljs.highlightAuto(raw, ['xml', 'html', 'json', 'sql']);
                        codeElement.innerHTML = result.value;
                    }
                    
                    // Add click listeners to newly created elements for Visual Assertion Builder
                    // We don't actually need to add listeners here because there's a global onclick handler
                    // but we can ensure they have the pointer cursor if CSS doesn't catch them.
                    codeElement.querySelectorAll('.hljs-name, .hljs-attr').forEach(el => {
                        el.style.cursor = 'pointer';
                    });
                } catch (err) {
                    console.warn('XML highlight failed, falling back to highlightElement', err);
                    hljs.highlightElement(codeElement);
                }
            } else {
                hljs.highlightElement(codeElement);
            }
        }

        codeElement.classList.add('hljs');
        codeElement.setAttribute('data-highlighted', 'yes');
    } catch (e) {
        console.error('Error in highlightHttpSource:', e);
        if (typeof hljs !== 'undefined') {
            hljs.highlightElement(codeElement);
        }
    }
}

async function runSingleRequest(requestLine, lineIndex) {
    // Determine the source to use: editor content if editing, otherwise lastSource
    const currentSource = isEditing && cmEditor ? cmEditor.state.doc.toString() : lastSource;
    if (!currentSource) return;
    
    // Parse the full source to find the request block starting with this line
    const lines = currentSource.split('\n');
    let requestContent = "";
    let found = false;
    const normalizedRequestLine = requestLine.trim();
    
    // Use lineIndex if available to uniquely identify the request
    if (lineIndex !== undefined && lines[lineIndex] && lines[lineIndex].trim() === normalizedRequestLine) {
        let i = lineIndex;
        found = true;
        // Backtrack to find pre-scripts and comments before the request
        let start = i;
        while (start > 0) {
            const prevLine = lines[start - 1].trim();
            if (prevLine.startsWith('###')) {
                start--;
                break;
            }
            // Include pre-scripts, comments, and empty lines
            if (prevLine.startsWith('< {%') || prevLine.startsWith('//') || prevLine === "") {
                start--;
            } else if (prevLine.endsWith('%}')) {
                // If it's the end of a block, we need to find the start
                let blockStart = start - 1;
                while (blockStart > 0 && !lines[blockStart].trim().startsWith('< {%') && !lines[blockStart].trim().startsWith('> {%')) {
                    blockStart--;
                }
                if (lines[blockStart].trim().startsWith('< {%')) {
                    start = blockStart;
                } else {
                    break;
                }
            } else {
                break;
            }
        }
        
        // Extract from start until next ### or end
        requestContent = lines.slice(start, i + 1).join('\n');
        for (let j = i + 1; j < lines.length; j++) {
            if (lines[j].startsWith('###')) break;
            requestContent += '\n' + lines[j];
        }
    } else {
        // Fallback to original search logic
        for (let i = 0; i < lines.length; i++) {
            if (lines[i].trim() === normalizedRequestLine) {
                found = true;
                // Backtrack to find pre-scripts and comments before the request
                let start = i;
                while (start > 0) {
                    const prevLine = lines[start - 1].trim();
                    if (prevLine.startsWith('###')) {
                        start--;
                        break;
                    }
                    // Include pre-scripts, comments, and empty lines
                    if (prevLine.startsWith('< {%') || prevLine.startsWith('//') || prevLine === "") {
                        start--;
                    } else if (prevLine.endsWith('%}')) {
                        // If it's the end of a block, we need to find the start
                        let blockStart = start - 1;
                        while (blockStart > 0 && !lines[blockStart].trim().startsWith('< {%') && !lines[blockStart].trim().startsWith('> {%')) {
                            blockStart--;
                        }
                        if (lines[blockStart].trim().startsWith('< {%')) {
                            start = blockStart;
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                
                // Extract from start until next ### or end
                requestContent = lines.slice(start, i + 1).join('\n');
                for (let j = i + 1; j < lines.length; j++) {
                    if (lines[j].startsWith('###')) break;
                    requestContent += '\n' + lines[j];
                }
                break;
            }
        }
    }

    if (!found) {
        // Try fuzzy match if exact match fails due to whitespace
        for (let i = 0; i < lines.length; i++) {
            if (lines[i].includes(normalizedRequestLine)) {
                found = true;
                let start = i;
                while (start > 0) {
                    const prevLine = lines[start - 1].trim();
                    if (prevLine.startsWith('###')) {
                        start--;
                        break;
                    }
                    if (prevLine.startsWith('< {%') || prevLine.startsWith('//') || prevLine === "") {
                        start--;
                    } else if (prevLine.endsWith('%}')) {
                        let blockStart = start - 1;
                        while (blockStart > 0 && !lines[blockStart].trim().startsWith('< {%') && !lines[blockStart].trim().startsWith('> {%')) {
                            blockStart--;
                        }
                        if (lines[blockStart].trim().startsWith('< {%')) {
                            start = blockStart;
                        } else {
                            break;
                        }
                    } else {
                        break;
                    }
                }
                requestContent = lines.slice(start, i + 1).join('\n');
                for (let j = i + 1; j < lines.length; j++) {
                    if (lines[j].startsWith('###')) break;
                    requestContent += '\n' + lines[j];
                }
                break;
            }
        }
    }

    if (!found) {
        console.log("Could not find request in source: " + requestLine);
        alert("Could not find request in source: " + requestLine);
        return;
    }

    const testName = "Single Request: " + requestLine.split(' ')[0];
    const globals = isCustomTest ? (getCustomGlobals()[currentTestName] || {}) : {};
    const envVars = getSelectedEnvVars();
    const mergedGlobals = { ...envVars, ...globals };

    reportContent.innerHTML = `<p class="loading">Running single request...</p>`;
    
    try {
        const response = await fetch('/api/runtest/custom', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json; charset=UTF-8' },
            body: JSON.stringify({
                name: testName,
                content: requestContent,
                globals: mergedGlobals
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Server returned ${response.status}: ${errorText}`);
        }

        const markdown = await response.text();
        lastMarkdown = markdown;
        reportContent.innerHTML = marked.parse(markdown);
        
        // Highlight report
        setTimeout(() => {
            reportContent.querySelectorAll('pre code').forEach((block) => {
                highlightHttpSource(block);
            });
        }, 0);
        
        exportBtn.style.display = 'block';
        exportDropdown.style.display = 'inline-block';
        if (window.innerWidth < 1024) {
            sourceBtn.style.display = 'block';
        } else {
            sourceBtn.style.display = 'none';
        }
    } catch (error) {
        reportContent.innerHTML = `<p style="color: red">Error running request: ${error.message}</p>`;
    }
}

async function selectTest(testName, element, isCustom = false) {
    currentTestName = testName;
    isCustomTest = isCustom;
    isViewingSource = false;
    sourceBtn.textContent = 'View Source';
    // Update UI state
    document.querySelectorAll('#test-list li').forEach(li => li.classList.remove('active'));
    element.classList.add('active');
    
    reportContent.innerHTML = `<p>Test <strong>${testName}</strong> selected. Click Run to execute.</p>`;
    sourceGutter.innerHTML = '';
    sourceCode.innerHTML = '<span class="loading">Fetching source...</span>';
    exportBtn.style.display = 'none';
    exportDropdown.style.display = 'none';
    sourceBtn.style.display = 'none';
    globalsBtn.style.display = 'none';
    cloneBtn.style.display = 'none';
    runViewBtn.style.display = 'none';

    try {
        let sourceResponse;
        if (isCustom) {
            const content = getCustomTests()[testName];
            sourceResponse = { text: async () => content };
        } else {
            sourceResponse = await fetch(`/api/test/${testName}`);
        }
        
        lastSource = await sourceResponse.text();
        sourceCode.textContent = lastSource;
        
        if (isCustom) {
            isEditing = true;
            setEditorContent(lastSource);
            sourceContent.style.display = 'none';
            sourceEditor.style.display = 'block';
            runCustomBtn.style.display = 'inline-block';
            saveCustomBtn.style.display = 'inline-block';
            globalsBtn.style.display = 'inline-block';
            runViewBtn.style.display = 'none';
            cloneBtn.style.display = 'inline-block';
            cloneBtn.textContent = 'Cancel';
            sourceBtn.style.display = 'none';
        } else {
            // Use setTimeout to ensure DOM is updated before highlighting
            setTimeout(() => {
                if (typeof hljs === 'undefined') {
                    console.error('highlight.js not available for highlighting');
                    return;
                }
                highlightHttpSource(sourceCode);
            }, 0);
            
            if (window.innerWidth < 1024) {
                sourceBtn.style.display = 'block';
            } else {
                sourceBtn.style.display = 'none';
            }
            globalsBtn.style.display = 'none';
            cloneBtn.style.display = 'inline-block';
            runViewBtn.style.display = 'inline-block';
            isEditing = false;
            sourceEditor.style.display = 'none';
            sourceContent.style.display = 'block';
            runCustomBtn.style.display = 'none';
            saveCustomBtn.style.display = 'none';
            cloneBtn.textContent = 'Clone';
        }
    } catch (error) {
        sourceGutter.innerHTML = '';
        sourceCode.innerHTML = `<span style="color: red">Error fetching source: ${error.message}</span>`;
    }
}

async function runTest(testName, element, isCustom = false) {
    currentTestName = testName;
    isCustomTest = isCustom;
    isViewingSource = false;
    sourceBtn.textContent = 'View Source';
    // Update UI state
    document.querySelectorAll('#test-list li').forEach(li => li.classList.remove('active'));
    element.classList.add('active');
    
    reportContent.innerHTML = `<p class="loading">Running test ${testName}...</p>`;
    sourceGutter.innerHTML = '';
    sourceCode.innerHTML = '<span class="loading">Fetching source...</span>';
    exportBtn.style.display = 'none';
    exportDropdown.style.display = 'none';
    sourceBtn.style.display = 'none';
    globalsBtn.style.display = 'none';

    try {
        let testResponse, sourceResponse;
        const envVars = getSelectedEnvVars();
        if (isCustom) {
            const content = getCustomTests()[testName];
            const globals = getCustomGlobals()[testName] || {};
            const mergedGlobals = { ...envVars, ...globals };
            testResponse = await fetch('/api/runtest/custom', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json; charset=UTF-8' },
                body: JSON.stringify({
                    name: testName,
                    content: content,
                    globals: mergedGlobals
                })
            });
            sourceResponse = { text: async () => content };
        } else {
            // Server tests - we pass envVars via a custom run endpoint or we need to update the API
            // For now, let's use the custom run endpoint for server tests too if env is selected,
            // or better, if there's an environment, always use the custom runner to pass variables.
            if (Object.keys(envVars).length > 0) {
                const sourceRes = await fetch(`/api/test/${testName}`);
                const content = await sourceRes.text();
                testResponse = await fetch('/api/runtest/custom', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json; charset=UTF-8' },
                    body: JSON.stringify({
                        name: testName,
                        content: content,
                        globals: envVars
                    })
                });
                sourceResponse = { text: async () => content };
            } else {
                testResponse = await fetch(`/api/runtest/${testName}`);
                sourceResponse = await fetch(`/api/test/${testName}`);
            }
        }
        
        if (!testResponse.ok) {
            const errorText = await testResponse.text();
            throw new Error(`Server returned ${testResponse.status}: ${errorText}`);
        }
        
        lastMarkdown = await testResponse.text();
        lastSource = await sourceResponse.text();

        reportContent.innerHTML = marked.parse(lastMarkdown);
        sourceCode.textContent = lastSource;
        
        // Use setTimeout to ensure DOM is updated before highlighting
        setTimeout(() => {
            if (typeof hljs === 'undefined') {
                console.error('highlight.js not available for highlighting');
                return;
            }
            
            highlightHttpSource(sourceCode);
            
            // Also highlight any code blocks in the report
            reportContent.querySelectorAll('pre code').forEach((block) => {
                highlightHttpSource(block);
            });
        }, 0);
        
        exportBtn.style.display = 'block';
        exportDropdown.style.display = 'inline-block';
        if (window.innerWidth < 1024) {
            sourceBtn.style.display = 'block';
        } else {
            sourceBtn.style.display = 'none';
        }
        globalsBtn.style.display = isCustom ? 'inline-block' : 'none';
        cloneBtn.style.display = 'inline-block';
        runViewBtn.style.display = 'inline-block';
        isEditing = false;
        sourceEditor.style.display = 'none';
        sourceContent.style.display = 'block';
        runCustomBtn.style.display = 'none';
        saveCustomBtn.style.display = 'none';
        cloneBtn.textContent = isCustomTest ? 'Edit' : 'Clone';
    } catch (error) {
        reportContent.innerHTML = `<p style="color: red">Error running test: ${error.message}</p>`;
    }
}

function escapeHtml(text) {
    return text.replace(/&/g, '&amp;')
               .replace(/</g, '&lt;')
               .replace(/>/g, '&gt;')
               .replace(/"/g, '&quot;')
               .replace(/'/g, '&#39;');
}

// Visual Assertion Builder Logic
function showAssertionToast(message) {
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

function getJsonPath(element) {
    let path = [];
    let current = element;
    
    // We expect element to be a .hljs-attr (JSON key)
    const key = current.textContent.replace(/["':]/g, '').trim();
    path.push(key);
    
    // Move up to find the containing object/array and its parent key
    // This is tricky with hljs highlighted HTML because it's mostly flat
    // But we can try to traverse the text content or previous siblings
    
    // Better approach: Find the full text of the code block and use a real JSON parser 
    // to find the path, but that's hard from just a clicked DOM node.
    
    // Let's try to traverse the DOM siblings to find the nesting level.
    // In hljs, JSON looks like: <span class="hljs-punctuation">{</span> ... <span class="hljs-attr">"key"</span> ...
    
    // Alternative: since we know the structure of the JSON being displayed, 
    // we can use the indentation or punctuation to guess the path.
    
    return path.join('.');
}

function generateAssertion(element) {
    const codeBlock = element.closest('pre code');
    if (!codeBlock) return;
    
    // Find the test name from the nearest preceding comment <!-- TEST_NAME: ... -->
    let testName = null;
    let currentElement = codeBlock.parentElement; // the <pre>
    while (currentElement) {
        // Look back through previous siblings
        let sibling = currentElement.previousSibling;
        while (sibling) {
            if (sibling.nodeType === Node.COMMENT_NODE) {
                const match = sibling.nodeValue.match(/TEST_NAME:\s*(.*)/);
                if (match) {
                    testName = match[1].trim();
                    break;
                }
            }
            sibling = sibling.previousSibling;
        }
        if (testName) break;
        currentElement = currentElement.parentElement;
    }

    const isJson = codeBlock.classList.contains('language-json');
    const isXml = codeBlock.classList.contains('language-xml');
    
    if (!isJson && !isXml) return;
    
    let assertion = "";
    const clickedText = element.textContent.replace(/["':]/g, '').trim();
    
    if (isJson) {
        // More robust JSON path generation by parsing the raw text
        const rawJson = codeBlock.textContent;
        let data;
        try {
            data = JSON.parse(rawJson);
        } catch (e) {
            console.error("Failed to parse JSON for assertion generation", e);
            return;
        }

        // Try to find the path to the clicked key
        // We'll use the clicked value to disambiguate matches in arrays
        function findPath(obj, targetKey, targetValue, currentPath = "response.body") {
            if (obj && typeof obj === 'object') {
                if (Array.isArray(obj)) {
                    for (let i = 0; i < obj.length; i++) {
                        const path = findPath(obj[i], targetKey, targetValue, `${currentPath}[${i}]`);
                        if (path) return path;
                    }
                } else {
                    if (Object.prototype.hasOwnProperty.call(obj, targetKey)) {
                        const val = obj[targetKey];
                        // If values match (loosely), we found our path
                        if (String(val) === String(targetValue)) {
                            return `${currentPath}.${targetKey}`;
                        }
                    }
                    for (const key in obj) {
                        const path = findPath(obj[key], targetKey, targetValue, `${currentPath}.${key}`);
                        if (path) return path;
                    }
                }
            }
            return null;
        }

        // Find the value for the assertion
        let clickedValue = "";
        let nextSibling = element.nextSibling;
        while (nextSibling) {
            const text = nextSibling.textContent.trim();
            if (text && text !== ":" && text !== ",") {
                clickedValue = text.replace(/["']/g, '');
                break;
            }
            nextSibling = nextSibling.nextSibling;
        }

        const fullPath = findPath(data, clickedText, clickedValue) || `response.body.${clickedText}`;
        
        // Generate the test/assertion snippet
        // We'll use \n as a placeholder for indentation which will be handled during insertion
        assertion = `client.test("Check ${clickedText}", () => {\n    client.assert(${fullPath} === ${isNaN(clickedValue) || clickedValue === "" ? `"${clickedValue}"` : clickedValue}, "Expected ${clickedText} to be ${clickedValue}");\n});`;
    } else if (isXml) {
        // Find the value for the assertion (content inside tags)
        let clickedValue = "";
        
        // In hljs highlighted XML, the value is often a text node between the opening tag's ">" and the next "<"
        // Find the closing bracket of the current tag
        let closingBracket = null;
        let curr = element;
        while (curr) {
            if (curr.textContent === ">") {
                closingBracket = curr;
                break;
            }
            curr = curr.nextSibling;
        }

        if (closingBracket) {
            // Collect all text nodes until the next tag starts
            let next = closingBracket.nextSibling;
            
            // If the closing bracket is the last child of a span.hljs-tag, the value starts at the next sibling of that span
            if (!next && closingBracket.parentElement && closingBracket.parentElement.classList.contains('hljs-tag')) {
                next = closingBracket.parentElement.nextSibling;
            }

            while (next) {
                if (next.nodeType === Node.TEXT_NODE) {
                    clickedValue += next.textContent;
                } else if (next.nodeType === Node.ELEMENT_NODE) {
                    // Check if this element is an opening tag of another element or a closing tag
                    if (next.classList.contains('hljs-tag')) {
                        // If it's a closing tag of the CURRENT element, we should probably stop.
                        // But wait, highlight.js might split the closing tag into siblings too.
                        // A simple way is to check if it contains "</"
                        if (next.textContent.includes('</')) {
                            break;
                        }
                        // If it's an opening tag of a CHILD element, we want to capture its content too?
                        // No, usually we want the text content of the parent element.
                        // If we are clicking a parent tag, response.body.getElementsByTagName(tag)[i].textContent
                        // will return the concatenated text of all children anyway.
                        // So we should continue until we hit the CLOSING tag of the clicked element.
                        // BUT, the current implementation of findXmlPath doesn't know about depth.
                        // For simplicity, let's just capture until the next tag starts.
                        break; 
                    }
                    if (next.classList.contains('hljs-meta')) {
                        break;
                    }
                    clickedValue += next.textContent;
                }
                next = next.nextSibling;
            }
            clickedValue = clickedValue.trim();
        }

        // For XML, use a smarter path detection
        const rawXml = codeBlock.textContent;
        const parser = new DOMParser();
        const xmlDoc = parser.parseFromString(rawXml, "text/xml");
        
        // Disambiguate by calculating the occurrence index of the clicked element in the report
        function findXmlPath(targetTag, targetValue, targetElement) {
            const allElementsOfSameTag = codeBlock.querySelectorAll('.hljs-name');
            let occurrenceIndex = 0;
            let found = false;
            
            for (let i = 0; i < allElementsOfSameTag.length; i++) {
                const el = allElementsOfSameTag[i];
                if (el.textContent.trim() === targetTag && el.parentElement.classList.contains('hljs-tag')) {
                    // Check if it's an opening tag (not preceded by </)
                    let isOpening = true;
                    let prev = el.previousSibling;
                    while (prev) {
                        if (prev.textContent === '</') {
                            isOpening = false;
                            break;
                        }
                        if (prev.textContent === '<') {
                            isOpening = true;
                            break;
                        }
                        prev = prev.previousSibling;
                    }

                    if (isOpening) {
                        if (el === targetElement) {
                            found = true;
                            break;
                        }
                        occurrenceIndex++;
                    }
                }
            }
            
            return `response.body.getElementsByTagName("${targetTag}")[${occurrenceIndex}]`;
        }

        const fullPath = findXmlPath(clickedText, clickedValue, element);
        
        // Escape clickedValue for the assertion string
        const escapedValue = clickedValue.replace(/"/g, '\\"');
        assertion = `client.test("Check ${clickedText}", () => {\n    client.assert(${fullPath}.textContent === "${escapedValue}", "Expected ${clickedText} to be ${escapedValue}");\n});`;
    }
    
    if (assertion && cmEditor) {
        const doc = cmEditor.state.doc;
        const fullContent = doc.toString();
        
        // Target index for search and insertion
        let targetInsertPos = doc.length;
        let existingPostScriptRange = null;
        let indent = "    "; // Default 4 spaces

        if (testName) {
            // Find the request block in the source
            // Escape testName for regex
            const escapedName = testName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
            const requestRegex = new RegExp(`^###\\s*${escapedName}\\s*$`, 'm');
            const match = fullContent.match(requestRegex);
            
            if (match) {
                const requestStartIdx = match.index;
                // Find where this request block ends (either next ### or end of file)
                const nextRequestMatch = fullContent.slice(requestStartIdx + 1).match(/^###/m);
                const requestEndIdx = nextRequestMatch ? requestStartIdx + 1 + nextRequestMatch.index : doc.length;
                
                const requestBlock = fullContent.slice(requestStartIdx, requestEndIdx);
                const postScriptMatch = requestBlock.match(/> {%([\s\S]*?)%}/);
                
                if (postScriptMatch) {
                    const relativeStart = postScriptMatch.index;
                    const relativeEnd = requestBlock.indexOf('%}', relativeStart);
                    existingPostScriptRange = {
                        from: requestStartIdx + relativeEnd,
                        to: requestStartIdx + relativeEnd
                    };

                    // Detect indentation from the last line in the script block
                    const scriptContent = postScriptMatch[1];
                    const lines = scriptContent.split('\n').filter(l => l.trim().length > 0);
                    if (lines.length > 0) {
                        const lastLine = lines[lines.length - 1];
                        const indentMatch = lastLine.match(/^(\s+)/);
                        if (indentMatch) {
                            indent = indentMatch[1];
                        }
                    }
                } else {
                    targetInsertPos = requestEndIdx;
                }
            }
        } else {
            // Fallback to original behavior: first post-script or end of file
            const postScriptMatch = fullContent.match(/> {%([\s\S]*?)%}/);
            if (postScriptMatch) {
                const endIdx = fullContent.indexOf('%}', postScriptMatch.index);
                existingPostScriptRange = { from: endIdx, to: endIdx };

                // Detect indentation
                const scriptContent = postScriptMatch[1];
                const lines = scriptContent.split('\n').filter(l => l.trim().length > 0);
                if (lines.length > 0) {
                    const lastLine = lines[lines.length - 1];
                    const indentMatch = lastLine.match(/^(\s+)/);
                    if (indentMatch) {
                        indent = indentMatch[1];
                    }
                }
            }
        }

        // Apply indentation to the assertion block
        const indentedAssertion = assertion.split('\n').join('\n' + indent);
        
        if (existingPostScriptRange) {
            // Append to existing post-script
            cmEditor.dispatch({
                changes: { from: existingPostScriptRange.from, to: existingPostScriptRange.to, insert: `\n${indent}${indentedAssertion}\n` }
            });
        } else {
            // Create new post-script
            // Ensure we add it on a new line and not directly after content if it's missing one
            const prefix = targetInsertPos > 0 && !fullContent[targetInsertPos - 1].match(/\n/) ? '\n\n' : '\n\n';
            cmEditor.dispatch({
                changes: { from: targetInsertPos, to: targetInsertPos, insert: `${prefix}> {%\n${indent}${indentedAssertion}\n%}` }
            });
        }
        
        showAssertionToast(`Assertion for "${clickedText}" added!`);
    }
}

reportContent.onclick = (e) => {
    const target = e.target;
    if (target.classList.contains('hljs-attr') || (target.classList.contains('hljs-name') && target.parentElement.classList.contains('hljs-tag'))) {
        generateAssertion(target);
    }
};

sourceBtn.onclick = async () => {
    if (isViewingSource) {
        reportContent.innerHTML = marked.parse(lastMarkdown);
        sourceBtn.textContent = 'View Source';
        exportBtn.style.display = 'block';
        exportDropdown.style.display = 'inline-block';
        isViewingSource = false;
    } else {
        reportContent.innerHTML = `<pre><code class="language-http">${escapeHtml(lastSource)}</code></pre>`;
        setTimeout(() => {
            if (typeof hljs !== 'undefined') {
                const code = reportContent.querySelector('code');
                if (code) {
                    highlightHttpSource(code);
                }
            }
        }, 0);
        sourceBtn.textContent = 'View Report';
        exportBtn.style.display = 'none';
        exportDropdown.style.display = 'none';
        isViewingSource = true;
    }
};

exportBtn.onclick = (e) => {
    e.stopPropagation();
    exportOptions.style.display = exportOptions.style.display === 'block' ? 'none' : 'block';
};

exportPdfBtn.onclick = async () => {
    exportOptions.style.display = 'none';
    const element = document.getElementById('report-content');
    
    // Force light mode for PDF export
    const isDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    if (isDarkMode) {
        document.documentElement.classList.add('light-mode');
        // Switch highlight.js theme to light for the export
        const hljsStyle = document.getElementById('hljs-style');
        if (hljsStyle) {
            hljsStyle.href = 'lib/highlight/styles/github.min.css';
        }
    }

    const opt = {
        margin:       10,
        filename:     `test-report-${currentTestName}.pdf`,
        image:        { type: 'jpeg', quality: 0.98 },
        html2canvas:  { scale: 2, useCORS: true },
        jsPDF:        { unit: 'mm', format: 'a4', orientation: 'portrait' }
    };
    
    try {
        // Add page-break class to all h2 elements except the first one for PDF export
        const h2s = element.querySelectorAll('h2');
        h2s.forEach((h2, index) => {
            if (index > 0) {
                h2.classList.add('pdf-page-break');
            }
        });

        await html2pdf().set(opt).from(element).save();

        // Remove the temporary class
        h2s.forEach(h2 => h2.classList.remove('pdf-page-break'));
    } finally {
        // Revert to system theme
        if (isDarkMode) {
            document.documentElement.classList.remove('light-mode');
            updateHighlightTheme();
        }
    }
};

exportWordBtn.onclick = () => {
    exportOptions.style.display = 'none';
    const element = document.getElementById('report-content');
    
    // Create a clones of the element to modify for export
    const clone = element.cloneNode(true);
    
    // Ensure styles are preserved in the DOCX (best effort)
    const htmlContent = `
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <style>
                body { font-family: sans-serif; }
                table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }
                th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                th { background-color: #f2f2f2; }
                pre { background-color: #f8f8f8; padding: 10px; border: 1px solid #ddd; white-space: pre-wrap; word-wrap: break-word; }
                code { font-family: monospace; }
                h1, h2, h3 { color: #007bff; }
                .pdf-page-break { page-break-before: always; }
            </style>
        </head>
        <body>
            ${clone.innerHTML}
        </body>
        </html>
    `;

    const converted = htmlDocx.asBlob(htmlContent);
    const url = URL.createObjectURL(converted);
    const a = document.createElement('a');
    a.href = url;
    a.download = `test-report-${currentTestName}.docx`;
    document.body.appendChild(a);
    a.click();
    setTimeout(() => {
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
    }, 0);
};

cloneBtn.onclick = () => {
    if (!isEditing) {
        isEditing = true;
        setEditorContent(lastSource);
        sourceContent.style.display = 'none';
        sourceEditor.style.display = 'block';
        runCustomBtn.style.display = 'inline-block';
        saveCustomBtn.style.display = 'inline-block';
        globalsBtn.style.display = 'inline-block';
        runViewBtn.style.display = 'none';
        cloneBtn.textContent = 'Cancel';
    } else {
        if (isCustomTest) {
            // If it's a custom test, "Cancel" should reload the original source from storage
            // or just stay in edit mode but revert changes. 
            // Given the requirement "always show the editor", we don't go back to sourceContent.
            const content = getCustomTests()[currentTestName] || lastSource;
            setEditorContent(content);
        } else {
            isEditing = false;
            sourceContent.style.display = 'block';
            sourceEditor.style.display = 'none';
            runCustomBtn.style.display = 'none';
            saveCustomBtn.style.display = 'none';
            globalsBtn.style.display = 'none';
            runViewBtn.style.display = 'inline-block';
            cloneBtn.textContent = 'Clone';
        }
    }
};

runViewBtn.onclick = () => {
    const activeLi = document.querySelector('#test-list li.active');
    if (activeLi) {
        runTest(currentTestName, activeLi, isCustomTest);
    }
};

addTestBtn.onclick = () => {
    currentTestName = '';
    unsavedGlobals = null;
    isCustomTest = true;
    isEditing = true;
    lastSource = '### New Test\nGET https://api.example.com\n';
    
    // UI updates
    setEditorContent(lastSource);
    sourceContent.style.display = 'none';
    sourceEditor.style.display = 'block';
    runCustomBtn.style.display = 'inline-block';
    saveCustomBtn.style.display = 'inline-block';
    globalsBtn.style.display = 'inline-block';
    runViewBtn.style.display = 'none';
    cloneBtn.style.display = 'inline-block';
    cloneBtn.textContent = 'Cancel';
    
    // Clear active state in sidebar
    document.querySelectorAll('#test-list li').forEach(li => li.classList.remove('active'));
    
    // Clear report
    reportContent.innerHTML = '<p>Write your test and click Save or Run.</p>';
};

importBtnSidebar.onclick = (e) => {
    e.stopPropagation();
    const rect = importBtnSidebar.getBoundingClientRect();
    importOptions.style.top = (rect.bottom + window.scrollY) + 'px';
    importOptions.style.left = (rect.left + window.scrollX) + 'px';
    importOptions.style.display = importOptions.style.display === 'block' ? 'none' : 'block';
};

document.addEventListener('click', () => {
    importOptions.style.display = 'none';
    exportOptions.style.display = 'none';
});

importFileBtn.onclick = () => {
    importFileInput.click();
};

importUrlBtn.onclick = async () => {
    const url = prompt('Enter Spec URL (OpenAPI, Postman, or Insomnia):');
    if (!url) return;
    try {
        const response = await fetch(`/api/fetch-external?url=${encodeURIComponent(url)}`);
        if (!response.ok) throw new Error('Failed to fetch URL');
        const content = await response.text();
        handleImportedContent(content, url);
    } catch (err) {
        alert('Error fetching spec: ' + err.message);
    }
};

importPasteBtn.onclick = () => {
    const content = prompt('Paste Spec (OpenAPI JSON/YAML, Postman JSON, or Insomnia JSON):');
    if (!content) return;
    handleImportedContent(content, 'Pasted Content');
};

importFileInput.onchange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
        handleImportedContent(event.target.result, file.name);
    };
    reader.readAsText(file);
    importFileInput.value = '';
};

function handleImportedContent(content, sourceName) {
    try {
        let data;
        try {
            data = JSON.parse(content);
        } catch (e) {
            data = jsyaml.load(content);
        }
        
        let httpContent = "";
        let type = "";

        if (data.openapi || data.swagger) {
            httpContent = convertOpenApiToHttp(data);
            type = "OpenAPI";
        } else if (data.info && data.info._postman_id || data.info && data.info.schema && data.info.schema.includes('postman')) {
            httpContent = convertPostmanToHttp(data);
            type = "Postman";
        } else if (data._type === 'export' && data.__export_format === 4) {
            httpContent = convertInsomniaToHttp(data);
            type = "Insomnia";
        } else {
            throw new Error('Unrecognized format. Please provide an OpenAPI, Postman, or Insomnia spec.');
        }
        
        currentTestName = '';
        unsavedGlobals = null;
        isCustomTest = true;
        isEditing = true;
        lastSource = httpContent;
        
        setEditorContent(lastSource);
        sourceContent.style.display = 'none';
        sourceEditor.style.display = 'block';
        runCustomBtn.style.display = 'inline-block';
        saveCustomBtn.style.display = 'inline-block';
        globalsBtn.style.display = 'inline-block';
        runViewBtn.style.display = 'none';
        cloneBtn.style.display = 'inline-block';
        cloneBtn.textContent = 'Cancel';
        
        document.querySelectorAll('#test-list li').forEach(li => li.classList.remove('active'));
        reportContent.innerHTML = `<p>Imported ${type} from ${sourceName}. Review and Save or Run.</p>`;
    } catch (err) {
        alert('Error parsing content: ' + err.message);
    }
}

function generateExampleFromSchema(schema, spec) {
    if (!schema) return null;

    // Handle $ref
    if (schema.$ref) {
        const refPath = schema.$ref.split('/');
        let refObj = spec;
        for (let i = 1; i < refPath.length; i++) {
            refObj = refObj[refPath[i]];
            if (!refObj) break;
        }
        return generateExampleFromSchema(refObj, spec);
    }

    if (schema.example !== undefined) return schema.example;
    if (schema.default !== undefined) return schema.default;

    const type = schema.type;

    if (type === 'object') {
        const obj = {};
        if (schema.properties) {
            for (const [propName, propSchema] of Object.entries(schema.properties)) {
                obj[propName] = generateExampleFromSchema(propSchema, spec);
            }
        }
        return obj;
    } else if (type === 'array') {
        return [generateExampleFromSchema(schema.items, spec)];
    } else if (type === 'string') {
        if (schema.format === 'date-time') return new Date().toISOString();
        if (schema.format === 'date') return new Date().toISOString().split('T')[0];
        if (schema.enum) return schema.enum[0];
        return "string";
    } else if (type === 'number' || type === 'integer') {
        return 0;
    } else if (type === 'boolean') {
        return true;
    }

    return null;
}

function convertOpenApiToHttp(spec) {
    let http = `### ${spec.info?.title || 'OpenAPI Import'}\n`;
    if (spec.info?.description) {
        http += `// ${spec.info.description.replace(/\n/g, '\n// ')}\n`;
    }
    http += '\n';

    const baseUrl = spec.servers?.[0]?.url || '{{baseUrl}}';
    
    for (const [path, methods] of Object.entries(spec.paths || {})) {
        for (const [method, operation] of Object.entries(methods)) {
            if (['get', 'post', 'put', 'delete', 'patch', 'options', 'head'].includes(method.toLowerCase())) {
                http += `### ${operation.summary || operation.operationId || (method.toUpperCase() + ' ' + path)}\n`;
                if (operation.description) {
                    http += `// ${operation.description.replace(/\n/g, '\n// ')}\n`;
                }
                
                let fullPath = path;
                // Basic path parameter replacement if examples exist
                if (operation.parameters) {
                    operation.parameters.filter(p => p.in === 'path').forEach(p => {
                        const example = p.example || (p.schema && p.schema.example) || `{{${p.name}}}`;
                        fullPath = fullPath.replace(`{${p.name}}`, example);
                    });
                }

                // Request Body example
                let contentType = 'application/json';
                if (operation.requestBody) {
                    const content = operation.requestBody.content;
                    const mediaTypes = Object.keys(content || {});
                    if (mediaTypes.length > 0) {
                        // Prefer JSON if available, otherwise take the first one
                        contentType = mediaTypes.includes('application/json') ? 'application/json' : mediaTypes[0];
                    }
                }

                // Accept header based on responses
                let acceptHeader = 'application/json';
                const successStatus = Object.keys(operation.responses || {}).find(s => s.startsWith('2')) || '200';
                const successResponse = operation.responses?.[successStatus];
                if (successResponse?.content) {
                    const mediaTypes = Object.keys(successResponse.content);
                    if (mediaTypes.length > 0) {
                        acceptHeader = mediaTypes.includes('application/json') ? 'application/json' : mediaTypes[0];
                    }
                }

                http += `${method.toUpperCase()} ${baseUrl}${fullPath}\n`;
                
                // Add Authorization header if security is defined
                const security = operation.security || spec.security;
                if (security && security.length > 0) {
                    // Just take the first one for now
                    const requirement = security[0];
                    const schemeName = Object.keys(requirement)[0];
                    const scheme = spec.components?.securitySchemes?.[schemeName];
                    
                    if (scheme) {
                        if (scheme.type === 'http') {
                            if (scheme.scheme === 'basic') {
                                http += 'Authorization: Basic {{username}} {{password}}\n';
                            } else if (scheme.scheme === 'bearer') {
                                http += 'Authorization: Bearer {{auth_token}}\n';
                            } else if (scheme.scheme === 'digest') {
                                http += 'Authorization: Digest {{username}} {{password}}\n';
                            }
                        } else if (scheme.type === 'apiKey' && scheme.in === 'header') {
                            http += `${scheme.name}: {{${scheme.name}}}\n`;
                        }
                    }
                }

                if (method.toLowerCase() !== 'delete') {
                    if (method.toLowerCase() !== 'get') {
                        http += `Content-Type: ${contentType}\n`;
                    }
                    http += `Accept: ${acceptHeader}\n`;
                }
                http += '\n';

                // Request Body example
                if (operation.requestBody && method.toLowerCase() !== 'get') {
                    const content = operation.requestBody.content;
                    const jsonContent = content?.['application/json'];
                    if (jsonContent?.example) {
                        http += JSON.stringify(jsonContent.example, null, 2) + '\n';
                    } else if (jsonContent?.schema) {
                        const example = generateExampleFromSchema(jsonContent.schema, spec);
                        http += JSON.stringify(example, null, 2) + '\n';
                    } else if (content && contentType !== 'application/json') {
                        // For non-JSON content types, if there's an example, use it
                        const otherContent = content[contentType];
                        if (otherContent?.example) {
                            http += (typeof otherContent.example === 'object' ? JSON.stringify(otherContent.example, null, 2) : otherContent.example) + '\n';
                        }
                    }
                    http += '\n';
                }

                // Response assertions
                http += '> {%\n';
                http += '  // Basic assertions\n';
                http += '  client.test("Request executed successfully", function () {\n'
                http += '    client.assert(response.status === 200, "Response status is ${response.status}");\n';
                http += '  });\n\n';


                // Response Body
                http += '  if (response.body) {\n';
                http += '    markdowner.heading(3, "Response Message");\n';
                http += '    if (response.contentType.mimeType.includes("xml") || response.contentType.mimeType.includes("html")) {\n';
                http += '      markdowner.codeBlock("xml", response.body.xml || response.body);\n';
                http += '    } else {\n';
                http += '      markdowner.codeBlock("json", JSON.stringify(response.body, null, 2));\n';
                http += '    }\n';
                http += '  }\n';

                http += '%}\n\n';
            }
        }
    }
    return http;
}

function convertPostmanToHttp(data) {
    let http = `### ${data.info?.name || 'Postman Import'}\n`;
    if (data.info?.description) {
        let desc = typeof data.info.description === 'string' ? data.info.description : data.info.description.content || "";
        http += `// ${desc.replace(/\n/g, '\n// ')}\n`;
    }
    http += '\n';

    function processItems(items) {
        for (const item of items) {
            if (item.item) {
                // It's a folder
                http += `// Folder: ${item.name}\n`;
                processItems(item.item);
                continue;
            }

            const request = item.request;
            if (!request) continue;

            http += `### ${item.name || 'Request'}\n`;
            if (request.description) {
                let desc = typeof request.description === 'string' ? request.description : request.description.content || "";
                http += `// ${desc.replace(/\n/g, '\n// ')}\n`;
            }

            const method = request.method || 'GET';
            let url = "";
            if (typeof request.url === 'string') {
                url = request.url;
            } else if (request.url && request.url.raw) {
                url = request.url.raw;
            }

            // Postman uses {{variable}} which is compatible with our format
            http += `${method} ${url}\n`;

            if (request.header) {
                request.header.forEach(h => {
                    if (!h.disabled) {
                        http += `${h.key}: ${h.value}\n`;
                    }
                });
            }

            if (request.body && request.body.mode === 'raw') {
                http += '\n';
                http += request.body.raw + '\n';
            } else if (request.body && request.body.mode === 'urlencoded') {
                http += 'Content-Type: application/x-www-form-urlencoded\n\n';
                const params = request.body.urlencoded.map(p => `${p.key}=${p.value}`).join('&');
                http += params + '\n';
            } else if (request.body && request.body.mode === 'formdata') {
                http += '// Form-data body is not fully supported in conversion yet\n';
            }

            http += '\n';

            // Scripts (tests)
            if (item.event) {
                item.event.forEach(ev => {
                    if (ev.listen === 'test' && ev.script && ev.script.exec) {
                        http += '> {%\n';
                        const lines = Array.isArray(ev.script.exec) ? ev.script.exec : [ev.script.exec];
                        lines.forEach(line => {
                            // Basic mapping of postman specific snippets to our format
                            // We now provide a 'pm' shim in the backend, so we only map things that are easy 
                            // or common to avoid relying solely on the shim.
                            let mappedLine = line
                                .replace(/pm\.expect\(([^)]+)\)\.to\.eql\(([^)]+)\)/g, 'client.assert($1 == $2, "Expected " + $2 + " but got " + $1)')
                                .replace(/pm\.expect\(([^)]+)\)\.to\.have\.status\((\d+)\)/g, 'client.assert(response.status === $2, "Expected status $2 but got " + response.status)')
                                .replace(/pm\.response\.json\(\)/g, 'response.body')
                                .replace(/pm\.environment\.set\(/g, 'client.global.set(')
                                .replace(/pm\.globals\.set\(/g, 'client.global.set(')
                                .replace(/pm\.expect\(([^)]+)\)\.to\.be\.true/g, 'client.assert($1 === true, "Expected true but got " + $1)')
                                .replace(/pm\.expect\(([^)]+)\)\.to\.be\.false/g, 'client.assert($1 === false, "Expected false but got " + $1)')
                                .replace(/pm\.test\(/g, 'client.test(')
                                .replace(/pm\.response\.to\.have\.status\((\d+)\)/g, 'client.assert(response.status === $1, "Expected status $1 but got " + response.status)')
                                .replace(/pm\.response\.to\.be\.success/g, 'client.assert(response.status >= 200 && response.status < 300, "Expected success status but got " + response.status)');
                            http += `    ${mappedLine}\n`;
                        });
                        http += '%}\n\n';
                    } else if (ev.listen === 'prerequest' && ev.script && ev.script.exec) {
                         http += '< {%\n';
                         const lines = Array.isArray(ev.script.exec) ? ev.script.exec : [ev.script.exec];
                         lines.forEach(line => {
                             let mappedLine = line
                                 .replace(/pm\.environment\.set\(/g, 'client.global.set(')
                                 .replace(/pm\.globals\.set\(/g, 'client.global.set(');
                             http += `    ${mappedLine}\n`;
                         });
                         http += '%}\n\n';
                    }
                });
            }
        }
    }

    if (data.item) {
        processItems(data.item);
    }

    return http;
}

function convertInsomniaToHttp(data) {
    let http = `### Insomnia Export\n\n`;
    
    const resources = data.resources || [];
    const requests = resources.filter(r => r._type === 'request');
    const environments = resources.filter(r => r._type === 'environment');
    
    // We can add environment variables as comments or at the top
    if (environments.length > 0) {
        http += `// Environments found:\n`;
        environments.forEach(env => {
            http += `// ${env.name}: ${JSON.stringify(env.data)}\n`;
        });
        http += '\n';
    }

    requests.forEach(req => {
        http += `### ${req.name || 'Request'}\n`;
        if (req.description) {
            http += `// ${req.description.replace(/\n/g, '\n// ')}\n`;
        }

        const method = req.method || 'GET';
        // Insomnia uses {{ _.variable }} or {{ variable }}
        let url = req.url.replace(/\{\{\s*_\./g, '{{').replace(/\{\{\s*/g, '{{').replace(/\s*\}\}/g, '}}');
        
        http += `${method} ${url}\n`;

        if (req.headers) {
            req.headers.forEach(h => {
                http += `${h.name}: ${h.value}\n`;
            });
        }

        if (req.authentication) {
            const auth = req.authentication;
            if (auth.type === 'basic') {
                http += `Authorization: Basic ${auth.username} ${auth.password}\n`;
            } else if (auth.type === 'bearer') {
                http += `Authorization: Bearer ${auth.token}\n`;
            }
        }

        if (req.body && req.body.text) {
            http += '\n';
            http += req.body.text + '\n';
        } else if (req.body && req.body.params) {
             http += 'Content-Type: application/x-www-form-urlencoded\n\n';
             const params = req.body.params.map(p => `${p.name}=${p.value}`).join('&');
             http += params + '\n';
        }

        http += '\n';
        
        // Insomnia doesn't have standard script support in export format as easily as postman 
        // but some versions might have them. If they exist in resources as 'script' we could link them.
    });

    return http;
}

saveCustomBtn.onclick = () => {
    const editedCode = cmEditor ? cmEditor.state.doc.toString() : '';
    if (!isCustomTest || currentTestName === '') {
        const saveName = prompt('Enter a name for this custom test:', currentTestName ? currentTestName + ' (Clone)' : 'New Test');
        if (saveName) {
            saveCustomTest(saveName, editedCode);
            if (unsavedGlobals) {
                saveCustomGlobals(saveName, unsavedGlobals);
                unsavedGlobals = null;
            }
            currentTestName = saveName;
            isCustomTest = true;
            lastSource = editedCode;
            sourceCode.textContent = lastSource;
            fetchTests();
            // In custom tests, we stay in edit mode
            isEditing = true;
            cloneBtn.textContent = 'Cancel';
            sourceBtn.style.display = 'none';
        }
    } else {
        saveCustomTest(currentTestName, editedCode);
        lastSource = editedCode;
        sourceCode.textContent = lastSource;
        // In custom tests, we stay in edit mode
        isEditing = true;
        cloneBtn.textContent = 'Cancel';
        // Only hide sourceBtn if we are on desktop where source is always visible
        if (window.innerWidth >= 1024) {
            sourceBtn.style.display = 'none';
        }
    }
};

runCustomBtn.onclick = async () => {
    const editedCode = cmEditor ? cmEditor.state.doc.toString() : '';
    const globals = isCustomTest ? (currentTestName ? (getCustomGlobals()[currentTestName] || {}) : (unsavedGlobals || {})) : {};
    const envVars = getSelectedEnvVars();
    const mergedGlobals = { ...envVars, ...globals };
    
    reportContent.innerHTML = `<p class="loading">Running custom test...</p>`;
    
    try {
        const response = await fetch('/api/runtest/custom', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json; charset=UTF-8'
            },
            body: JSON.stringify({
                name: currentTestName || 'Custom Test',
                content: editedCode,
                globals: mergedGlobals
            })
        });

        if (response.status === 404 && isCustomTest) {
            // This could happen if we just clicked "Run" on a new, unsaved test
        } else if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        // If it was a cloned/new test (not yet saved as custom) or an update to an existing custom test
        if (!isCustomTest || currentTestName === '') {
            const saveName = prompt('Enter a name for this custom test:', currentTestName ? currentTestName + ' (Clone)' : 'New Test');
            if (saveName) {
                saveCustomTest(saveName, editedCode);
                if (unsavedGlobals) {
                    saveCustomGlobals(saveName, unsavedGlobals);
                    unsavedGlobals = null;
                }
                currentTestName = saveName;
                isCustomTest = true;
                fetchTests();
            }
        } else {
            // Update existing custom test
            saveCustomTest(currentTestName, editedCode);
        }

        lastMarkdown = await response.text();
        lastSource = editedCode;

        reportContent.innerHTML = marked.parse(lastMarkdown);
        sourceCode.textContent = lastSource;
        
        // Ensure buttons are visible after run
        exportBtn.style.display = 'block';
        exportDropdown.style.display = 'inline-block';
        
        // Use setTimeout to ensure DOM is updated before highlighting
        setTimeout(() => {
            if (typeof hljs !== 'undefined') {
                reportContent.querySelectorAll('pre code').forEach((block) => {
                    highlightHttpSource(block);
                });
            }
        }, 0);
        
        // In custom tests, we stay in edit mode
        isEditing = true;
        cloneBtn.textContent = 'Cancel';
        // Only hide sourceBtn if we are on desktop where source is always visible
        if (window.innerWidth >= 1024) {
            sourceBtn.style.display = 'none';
        }
        
    } catch (error) {
        reportContent.innerHTML = `<p style="color: red">Error running custom test: ${error.message}</p>`;
    }
};

// Handle Light/Dark Mode for Highlight.js and CodeMirror
function updateHighlightTheme() {
    const hljsStyle = document.getElementById('hljs-style');
    if (hljsStyle) {
        if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
            hljsStyle.href = 'lib/highlight/styles/github-dark.min.css';
        } else {
            hljsStyle.href = 'lib/highlight/styles/github.min.css';
        }
    }
    updateEditorTheme();
}

// Listen for theme changes
if (window.matchMedia) {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', updateHighlightTheme);
}
updateHighlightTheme();

// Environment Modal Elements
const envModal = document.getElementById('env-modal');
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
const envVarsTbody = document.getElementById('env-vars-tbody');
const addEnvBtn = document.getElementById('add-env-btn');
const addEnvVarBtn = document.getElementById('add-env-var-btn');
const saveEnvsBtn = document.getElementById('save-envs-btn');
const deleteEnvBtn = document.getElementById('delete-env-btn');
const closeEnvModal = document.querySelector('.close-env-modal');

let currentEnvName = '';
let tempEnvs = {};

function initEnvironments() {
    const envs = getEnvironments();
    const selected = getSelectedEnvName();
    
    envSelect.innerHTML = '<option value="">No Environment</option>';
    Object.keys(envs).sort().forEach(name => {
        const option = document.createElement('option');
        option.value = name;
        option.textContent = name;
        if (name === selected) option.selected = true;
        envSelect.appendChild(option);
    });
}

envSelect.onchange = () => {
    setSelectedEnvName(envSelect.value);
};

manageEnvsBtn.onclick = () => {
    tempEnvs = JSON.parse(JSON.stringify(getEnvironments()));
    currentEnvName = ''; // Reset when opening
    renderEnvList();
    envEditPanel.style.display = 'flex';
    envModal.style.display = 'block';
};

addEnvQuickBtn.onclick = () => {
    tempEnvs = JSON.parse(JSON.stringify(getEnvironments()));
    currentEnvName = ''; // Reset when opening
    renderEnvList();
    envEditPanel.style.display = 'none';
    envModal.style.display = 'block';
    addEnvBtn.onclick(); // Trigger the add new environment logic
};

function renderEnvList() {
    envList.innerHTML = '';
    const sortedNames = Object.keys(tempEnvs).sort();
    if (sortedNames.length > 0 && !currentEnvName) {
        // Don't auto-select here, let the user click or manageEnvsBtn handles it
    }
    sortedNames.forEach(name => {
        const li = document.createElement('li');
        li.textContent = name;
        if (name === currentEnvName) li.classList.add('active');
        li.onclick = async () => {
            if (currentEnvName) {
                await saveCurrentEnvToTemp();
            }
            editEnvironment(name);
        };
        envList.appendChild(li);
    });
}

function editEnvironment(name) {
    currentEnvName = name;
    renderEnvList();
    envEditPanel.style.display = 'flex';
    envTableView.style.display = 'block';
    envJsonView.style.display = 'none';
    toggleEnvFormatBtn.textContent = 'Switch to JSON';
    envNameInput.value = name;
    renderEnvVars();
}

const SENSITIVE_VAR_NAMES = ['password', 'secret', 'apikey', 'dbpassword', 'token', 'auth'];

function isSensitiveVar(name) {
    if (!name) return false;
    const lower = name.toLowerCase();
    return SENSITIVE_VAR_NAMES.some(s => lower.includes(s));
}

function renderEnvVars() {
    envVarsTbody.innerHTML = '';
    const vars = tempEnvs[currentEnvName] || {};
    Object.entries(vars).forEach(([key, value]) => {
        addEnvVarRow(key, value);
    });
}

function addEnvVarRow(key = '', value = '') {
    const tr = document.createElement('tr');
    const displayValue = (isSensitiveVar(key) && value.startsWith('{enc}')) ? '********' : value;
    const inputType = isSensitiveVar(key) ? 'password' : 'text';
    
    // Sanitize values to prevent HTML injection (e.g. login form in environment value)
    const sanitizedKey = escapeHtml(key);
    const sanitizedValue = escapeHtml(displayValue + '');
    
    tr.innerHTML = `
        <td><input type="text" class="env-var-key" value="${sanitizedKey}" placeholder="Key"></td>
        <td><input type="${inputType}" class="env-var-value" value="${sanitizedValue}" placeholder="Value"></td>
        <td><button class="btn-delete-row">&times;</button></td>
    `;
    
    const keyInput = tr.querySelector('.env-var-key');
    const valueInput = tr.querySelector('.env-var-value');
    
    keyInput.oninput = () => {
        if (isSensitiveVar(keyInput.value)) {
            valueInput.type = 'password';
        } else {
            valueInput.type = 'text';
        }
    };
    
    tr.querySelector('.btn-delete-row').onclick = () => tr.remove();
    envVarsTbody.appendChild(tr);
}

addEnvBtn.onclick = async () => {
    if (currentEnvName) {
        await saveCurrentEnvToTemp();
    }
    const name = 'New Environment';
    let newName = name;
    let counter = 1;
    while (tempEnvs[newName]) {
        newName = `${name} ${counter++}`;
    }
    tempEnvs[newName] = {};
    editEnvironment(newName);
};

addEnvVarBtn.onclick = () => addEnvVarRow();

saveEnvsBtn.onclick = async () => {
    if (currentEnvName) {
        await saveCurrentEnvToTemp();
    }
    
    const wasEmpty = Object.keys(getEnvironments()).length === 0;
    
    saveEnvironments(tempEnvs);
    
    const currentEnvs = getEnvironments();
    const envNames = Object.keys(currentEnvs);
    const selectedEnvName = getSelectedEnvName();
    
    if (wasEmpty && envNames.length > 0) {
        setSelectedEnvName(envNames.sort()[0]);
    } else if (selectedEnvName && !currentEnvs[selectedEnvName]) {
        // Selected env was deleted or renamed
        setSelectedEnvName('');
    }
    
    initEnvironments();
    envModal.style.display = 'none';
};

envNameInput.oninput = () => {
    const newName = envNameInput.value.trim();
    if (newName && newName !== currentEnvName) {
        if (tempEnvs[newName]) {
            // Already exists, maybe show error? For now just don't allow duplicate if it's not the current one
            return;
        }
        const vars = tempEnvs[currentEnvName];
        delete tempEnvs[currentEnvName];
        tempEnvs[newName] = vars;
        
        // If the renamed environment was the one selected in the header, update the selection
        if (getSelectedEnvName() === currentEnvName) {
            setSelectedEnvName(newName);
        }
        
        currentEnvName = newName;
        // renderEnvList(); // Don't re-render here as it loses focus on the input
        // Instead, just update the active item in the list
        const activeItem = envList.querySelector('li.active');
        if (activeItem) {
            activeItem.textContent = newName;
            activeItem.onclick = async () => {
                if (currentEnvName) {
                    await saveCurrentEnvToTemp();
                }
                editEnvironment(newName);
            };
        }
    }
};

toggleEnvFormatBtn.onclick = () => {
    if (envTableView.style.display === 'none') {
        envTableView.style.display = 'block';
        envJsonView.style.display = 'none';
        toggleEnvFormatBtn.textContent = 'Switch to JSON';
        renderEnvVars();
    } else {
        envTableView.style.display = 'none';
        envJsonView.style.display = 'flex';
        toggleEnvFormatBtn.textContent = 'Switch to Table';
        
        // Prepare JSON from current table view
        const vars = {};
        envVarsTbody.querySelectorAll('tr').forEach(tr => {
            const keyInput = tr.querySelector('.env-var-key');
            const valueInput = tr.querySelector('.env-var-value');
            if (keyInput && valueInput) {
                const key = keyInput.value.trim();
                let value = valueInput.value.trim();
                if (key) {
                    if (isSensitiveVar(key) && value === '********') {
                        value = (tempEnvs[currentEnvName] && tempEnvs[currentEnvName][key]) || '';
                    }
                    if (value && value.toString().startsWith('<!DOCTYPE html>')) {
                         // Corrupted value found, reset it to empty or try to keep old one if available
                         value = (tempEnvs[currentEnvName] && tempEnvs[currentEnvName][key]) || '';
                         if (value && value.toString().startsWith('<!DOCTYPE html>')) {
                             value = '';
                         }
                    }
                    vars[key] = value;
                }
            }
        });
        envJsonTextarea.value = JSON.stringify(vars, null, 2);
    }
};

applyEnvJsonBtn.onclick = async () => {
    try {
        const vars = JSON.parse(envJsonTextarea.value);
        envVarsTbody.innerHTML = '';
        for (const [key, value] of Object.entries(vars)) {
            // If it's sensitive and not encrypted yet, encrypt it
            let valToSet = value;
            if (isSensitiveVar(key) && !value.toString().startsWith('{enc}')) {
                try {
                    const response = await fetch('/api/crypto/encrypt', {
                        method: 'POST',
                        headers: { 'Content-Type': 'text/plain' },
                        body: value.toString()
                    });
                    if (response.ok) {
                        const encrypted = await response.text();
                        if (encrypted.startsWith('{enc}')) {
                            valToSet = encrypted;
                        } else {
                            throw new Error("Unexpected encryption response");
                        }
                    } else if (response.status === 401) {
                        alert("Session expired. Please reload the page and login again.");
                        return;
                    }
                } catch (e) {
                    console.error("Encryption failed", e);
                }
            }
            addEnvVarRow(key, valToSet);
        }
        
        // Switch back to table view
        envTableView.style.display = 'block';
        envJsonView.style.display = 'none';
        toggleEnvFormatBtn.textContent = 'Switch to JSON';
        
        // To be safe, let's call saveCurrentEnvToTemp to sync everything
        await saveCurrentEnvToTemp();
        renderEnvVars(); 

    } catch (e) {
        alert("Invalid JSON: " + e.message);
    }
};

async function saveCurrentEnvToTemp() {
    if (!currentEnvName) return;
    const nameFromInput = envNameInput.value.trim();
    if (!nameFromInput) return;
    
    const vars = {};
    
    if (envJsonView && envJsonView.style.display !== 'none') {
        try {
            const jsonVars = JSON.parse(envJsonTextarea.value);
            for (const [key, value] of Object.entries(jsonVars)) {
            if (isSensitiveVar(key) && !value.toString().startsWith('{enc}')) {
                const response = await fetch('/api/crypto/encrypt', {
                    method: 'POST',
                    headers: { 'Content-Type': 'text/plain' },
                    body: value.toString()
                });
                if (response.ok) {
                    const encrypted = await response.text();
                    if (encrypted.startsWith('{enc}')) {
                        vars[key] = encrypted;
                    } else {
                        vars[key] = value;
                    }
                } else if (response.status === 401) {
                    alert("Session expired. Please reload the page and login again.");
                    return;
                } else {
                    vars[key] = value;
                }
            } else {
                    vars[key] = value;
                }
            }
        } catch (e) {
            console.error("Failed to parse JSON on save", e);
        }
    } else {
        const rows = Array.from(envVarsTbody.querySelectorAll('tr'));
        for (const tr of rows) {
            const keyInput = tr.querySelector('.env-var-key');
            const valueInput = tr.querySelector('.env-var-value');
            if (keyInput && valueInput) {
                const key = keyInput.value.trim();
                let value = valueInput.value.trim();
                if (key) {
                    if (isSensitiveVar(key) && value !== '********') {
                        try {
                            const response = await fetch('/api/crypto/encrypt', {
                                method: 'POST',
                                headers: { 'Content-Type': 'text/plain' },
                                body: value
                            });
                            if (response.ok) {
                                const encrypted = await response.text();
                                if (encrypted.startsWith('{enc}')) {
                                    value = encrypted;
                                    valueInput.value = '********';
                                }
                            } else if (response.status === 401) {
                                alert("Session expired. Please reload the page and login again.");
                                return;
                            }
                        } catch (e) {
                            console.error("Encryption failed", e);
                        }
                    } else if (isSensitiveVar(key) && value === '********') {
                        value = (tempEnvs[currentEnvName] && tempEnvs[currentEnvName][key]) || '';
                    }
                    if (value && value.toString().startsWith('<!DOCTYPE html>')) {
                         // Corrupted value found
                         value = '';
                    }
                    vars[key] = value;
                }
            }
        }
    }

    if (nameFromInput !== currentEnvName) {
        delete tempEnvs[currentEnvName];
        tempEnvs[nameFromInput] = vars;
        currentEnvName = nameFromInput;
    } else {
        tempEnvs[currentEnvName] = vars;
    }
}

deleteEnvBtn.onclick = () => {
    if (confirm(`Are you sure you want to delete the environment "${currentEnvName}"?`)) {
        delete tempEnvs[currentEnvName];
        currentEnvName = '';
        envEditPanel.style.display = 'none';
        renderEnvList();
    }
};

closeEnvModal.onclick = () => {
    envModal.style.display = 'none';
};

window.onclick = (event) => {
    if (event.target == envModal) envModal.style.display = 'none';
    if (event.target == globalsModal) globalsModal.style.display = 'none';
};

function getSelectedEnvVars() {
    const envName = envSelect.value;
    if (!envName) return {};
    const envs = getEnvironments();
    return envs[envName] || {};
}

initEnvironments();
fetchTests();
