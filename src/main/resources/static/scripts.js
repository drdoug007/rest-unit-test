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
const importOpenApiBtn = document.getElementById('import-openapi-btn');
const importOptions = document.getElementById('import-options');
const importFileBtn = document.getElementById('import-file-btn');
const importUrlBtn = document.getElementById('import-url-btn');
const importPasteBtn = document.getElementById('import-paste-btn');
const openapiFileInput = document.getElementById('openapi-file-input');
const sourceEditor = document.getElementById('source-editor');
let cmEditor = null;
const { EditorView, EditorState, basicSetup, http, javascript, oneDark, Compartment, keymap, indentWithTab } = CodeMirror6;
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

function getCustomTests() {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? JSON.parse(stored) : {};
}

function getCustomGlobals() {
    const stored = localStorage.getItem(GLOBALS_KEY);
    return stored ? JSON.parse(stored) : {};
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
    
    globalsModal.style.display = 'block';
}

function addGlobalRow(key = '', value = '') {
    const tr = document.createElement('tr');
    tr.innerHTML = `
        <td style="padding: 8px;"><input type="text" class="global-key" value="${key}" placeholder="Key"></td>
        <td style="padding: 8px;"><input type="text" class="global-value" value="${value}" placeholder="Value"></td>
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

saveGlobalsBtn.onclick = () => {
    const globals = {};
    globalsTbody.querySelectorAll('tr').forEach(tr => {
        const key = tr.querySelector('.global-key').value.trim();
        const value = tr.querySelector('.global-value').value.trim();
        if (key) {
            globals[key] = value;
        }
    });
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
        // First, highlight the entire block as HTTP
        // We use textContent to get the raw source code
        const rawCode = codeElement.textContent;
        const highlightedHttp = hljs.highlight(rawCode, { language: 'http' }).value;
        
        // Now, find and highlight nested JavaScript blocks in the already highlighted HTTP HTML
        // The nested blocks in the highlighted HTML might have escaped characters
        // &gt; corresponds to > and &lt; corresponds to <
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
        
        // Final adjustment for HTTP specific elements if not caught by hljs-http
        let customHighlighted = finalHtml;

        // Highlight ### as section
        customHighlighted = customHighlighted.replace(/^### (.*)$/gm, '<span class="hljs-section">### $1</span>');
        // Highlight // comments
        customHighlighted = customHighlighted.replace(/^\/\/ (.*)$/gm, '<span class="hljs-comment">// $1</span>');
        // Highlight variables {{var}} with a specific class
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

                // Check for script marker
                if (scriptMarkerRegex.test(line)) {
                    markerHtml = '<span class="js-logo" title="JavaScript">JS</span>';
                }

                // Check for request line
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

            // Add event listeners for play buttons in gutter
            sourceGutter.querySelectorAll('.play-button').forEach(btn => {
                btn.onclick = (e) => {
                    e.stopPropagation();
                    const rawLine = btn.getAttribute('data-request-line');
                    const lineIndex = parseInt(btn.getAttribute('data-line-index'));
                    runSingleRequest(rawLine, lineIndex);
                };
            });
        } else {
            // Standard highlighting for non-source-panel code blocks (e.g. in reports)
            const requestLineRegex = /^(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD|TRACE) (.*)$/gm;
            customHighlighted = customHighlighted.replace(requestLineRegex, (match, method, url) => {
                return `<span class="hljs-keyword">${method}</span> <span class="hljs-title">${url}</span>`;
            });
            codeElement.innerHTML = customHighlighted;
        }

        codeElement.classList.add('hljs');
        codeElement.setAttribute('data-highlighted', 'yes');
    } catch (e) {
        console.error('Error in highlightHttpSource:', e);
        // Fallback to standard highlighting if our custom one fails
        if (typeof hljs !== 'undefined') {
            hljs.highlightElement(codeElement);
        }
    }
}

async function runSingleRequest(requestLine, lineIndex) {
    if (!lastSource) return;
    
    // Parse the full source to find the request block starting with this line
    const lines = lastSource.split('\n');
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

    reportContent.innerHTML = `<p class="loading">Running single request...</p>`;
    
    try {
        const response = await fetch('/api/runtest/custom', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json; charset=UTF-8' },
            body: JSON.stringify({
                name: testName,
                content: requestContent,
                globals: globals
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
                if (typeof hljs !== 'undefined') {
                    hljs.highlightElement(block);
                }
            });
        }, 0);
        
        exportBtn.style.display = 'block';
        exportDropdown.style.display = 'inline-block';
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
            
            sourceBtn.style.display = 'block';
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
        if (isCustom) {
            const content = getCustomTests()[testName];
            const globals = getCustomGlobals()[testName] || {};
            testResponse = await fetch('/api/runtest/custom', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json; charset=UTF-8' },
                body: JSON.stringify({
                    name: testName,
                    content: content,
                    globals: globals
                })
            });
            sourceResponse = { text: async () => content };
        } else {
            testResponse = await fetch(`/api/runtest/${testName}`);
            sourceResponse = await fetch(`/api/test/${testName}`);
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
                try {
                    hljs.highlightElement(block);
                } catch (e) { console.error('Error highlighting report block:', e); }
            });
        }, 0);
        
        exportBtn.style.display = 'block';
        exportDropdown.style.display = 'inline-block';
        sourceBtn.style.display = 'block';
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
                highlightHttpSource(reportContent.querySelector('code'));
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

importOpenApiBtn.onclick = (e) => {
    e.stopPropagation();
    const rect = importOpenApiBtn.getBoundingClientRect();
    importOptions.style.top = (rect.bottom + window.scrollY) + 'px';
    importOptions.style.left = (rect.left + window.scrollX) + 'px';
    importOptions.style.display = importOptions.style.display === 'block' ? 'none' : 'block';
};

document.addEventListener('click', () => {
    importOptions.style.display = 'none';
    exportOptions.style.display = 'none';
});

importFileBtn.onclick = () => {
    openapiFileInput.click();
};

importUrlBtn.onclick = async () => {
    const url = prompt('Enter OpenAPI Spec URL:');
    if (!url) return;
    try {
        const response = await fetch(`/api/fetch-external?url=${encodeURIComponent(url)}`);
        if (!response.ok) throw new Error('Failed to fetch URL');
        const content = await response.text();
        handleImportedSpec(content, url);
    } catch (err) {
        alert('Error fetching OpenAPI spec: ' + err.message);
    }
};

importPasteBtn.onclick = () => {
    const content = prompt('Paste OpenAPI Spec (JSON or YAML):');
    if (!content) return;
    handleImportedSpec(content, 'Pasted Spec');
};

openapiFileInput.onchange = (e) => {
    const file = e.target.files[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
        handleImportedSpec(event.target.result, file.name);
    };
    reader.readAsText(file);
    openapiFileInput.value = '';
};

function handleImportedSpec(content, sourceName) {
    try {
        let spec;
        try {
            spec = JSON.parse(content);
        } catch (e) {
            spec = jsyaml.load(content);
        }
        
        const httpContent = convertOpenApiToHttp(spec);
        
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
        reportContent.innerHTML = `<p>Imported from ${sourceName}. Review and Save or Run.</p>`;
    } catch (err) {
        alert('Error parsing OpenAPI spec: ' + err.message);
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
        sourceBtn.style.display = 'none';
    }
};

runCustomBtn.onclick = async () => {
    const editedCode = cmEditor ? cmEditor.state.doc.toString() : '';
    const globals = isCustomTest ? (currentTestName ? (getCustomGlobals()[currentTestName] || {}) : (unsavedGlobals || {})) : {};
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
                globals: globals
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
        
        // In custom tests, we stay in edit mode
        isEditing = true;
        cloneBtn.textContent = 'Cancel';
        sourceBtn.style.display = 'none';
        
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

fetchTests();
