import { state, setEditor, sourceEditor, setEditorContent, editor as cmEditor, exportBtn, exportDropdown, reportContent, sourceBtn, testList, runViewBtn, debugCustomBtn, cloneBtn, runCustomBtn, saveCustomBtn, globalsBtn, getCustomTests, getCustomGlobals, saveCustomTest, saveCustomGlobals, getSelectedEnvName } from './core.js';
import { escapeHtml } from './utils.js';
import { getSelectedEnvVars } from './env-manager.js';
import { setReadOnly } from './editor.js';
import { marked } from 'marked';
import hljs from 'highlight.js';

export function fetchTests() {
    fetch('/api/tests')
        .then(response => response.json())
        .then(tests => {
            renderTestList(tests);
        });
}

function renderTestList(tests) {
    if (!testList) return;
    testList.innerHTML = '';
    
    // Add server tests
    tests.forEach(testName => {
        const li = document.createElement('li');
        
        const main = document.createElement('div');
        main.className = 'test-item-main';
        
        const nameSpan = document.createElement('span');
        nameSpan.className = 'test-name';
        nameSpan.textContent = testName;
        main.appendChild(nameSpan);
        
        const tag = document.createElement('span');
        tag.className = 'server-tag';
        tag.textContent = 'Server';
        main.appendChild(tag);
        
        li.appendChild(main);
        li.setAttribute('data-name', testName);
        li.addEventListener('click', () => selectTest(testName, li, false));
        testList.appendChild(li);
    });
    
    // Add custom tests
    const customTests = getCustomTests();
    Object.keys(customTests).forEach(testName => {
        const li = document.createElement('li');
        li.setAttribute('data-name', testName);
        li.classList.add('custom-test-item');
        
        const main = document.createElement('div');
        main.className = 'test-item-main';
        
        const nameSpan = document.createElement('span');
        nameSpan.className = 'test-name';
        nameSpan.textContent = testName;
        main.appendChild(nameSpan);
        
        const tag = document.createElement('span');
        tag.className = 'custom-tag';
        tag.textContent = 'Custom';
        main.appendChild(tag);
        
        li.appendChild(main);
        
        const deleteBtn = document.createElement('span');
        deleteBtn.className = 'delete-test-btn';
        deleteBtn.innerHTML = '&times;';
        deleteBtn.onclick = (e) => {
            e.stopPropagation();
            if (confirm(`Delete custom test "${testName}"?`)) {
                deleteCustomTest(testName);
            }
        };
        li.appendChild(deleteBtn);
        
        li.addEventListener('click', () => selectTest(testName, li, true));
        testList.appendChild(li);
    });
}

function deleteCustomTest(name) {
    const tests = getCustomTests();
    delete tests[name];
    localStorage.setItem('custom_http_tests', JSON.stringify(tests));
    
    const allGlobals = getCustomGlobals();
    delete allGlobals[name];
    localStorage.setItem('custom_http_globals', JSON.stringify(allGlobals));
    
    fetchTests();
}

export async function selectTest(name, element, isCustom) {
    console.log(`Selecting test: ${name} (isCustom: ${isCustom})`);
    
    const dashboardContent = document.getElementById('dashboard-content');
    if (dashboardContent) dashboardContent.style.display = 'none';
    if (reportContent) reportContent.style.display = 'block';

    state.currentTestName = name;
    state.isCustomTest = isCustom;
    
    document.querySelectorAll('#test-list li').forEach(li => li.classList.remove('active'));
    if (element) element.classList.add('active');
    
    if (isCustom) {
        const customTests = getCustomTests();
        const content = customTests[name] || '';
        state.lastSource = content;
        state.isEditing = true;
        setEditorContent(content);
        setReadOnly(false);
        if (sourceEditor) {
            sourceEditor.style.display = 'block';
            sourceEditor.parentElement.classList.add('show');
        }
        if (runViewBtn) runViewBtn.style.display = 'none';
        if (runCustomBtn) runCustomBtn.style.display = 'inline-block';
        if (debugCustomBtn) debugCustomBtn.style.display = 'inline-block';
        if (saveCustomBtn) saveCustomBtn.style.display = 'inline-block';
        if (globalsBtn) globalsBtn.style.display = 'inline-block';
        if (cloneBtn) {
            cloneBtn.style.display = 'inline-block';
            cloneBtn.textContent = 'Cancel';
        }
        if (sourceBtn) sourceBtn.style.display = 'none';
        
        reportContent.innerHTML = `<p>Editing custom test: <strong>${name}</strong></p>`;
    } else {
        state.isEditing = false;
        if (sourceEditor) {
            sourceEditor.style.display = 'block';
            sourceEditor.parentElement.classList.add('show');
        }
        setReadOnly(true);
        if (runViewBtn) runViewBtn.style.display = 'inline-block';
        if (debugCustomBtn) debugCustomBtn.style.display = 'inline-block';
        if (runCustomBtn) runCustomBtn.style.display = 'none';
        if (saveCustomBtn) saveCustomBtn.style.display = 'none';
        if (globalsBtn) globalsBtn.style.display = 'none';
        if (cloneBtn) {
            cloneBtn.style.display = 'inline-block';
            cloneBtn.textContent = 'Clone';
        }
        if (window.innerWidth < 1024 && sourceBtn) sourceBtn.style.display = 'inline-block';
        
        try {
            console.log(`Fetching source for: ${name}`);
            const response = await fetch(`/api/test/${encodeURIComponent(name)}`);
            if (response.ok) {
                state.lastSource = await response.text();
                setEditorContent(state.lastSource);
            } else {
                console.error(`Failed to fetch source: ${response.status}`);
            }
        } catch (err) {
            console.error('Error fetching source:', err);
        }
        reportContent.innerHTML = `<p>Selected test: <strong>${name}</strong>. Click Run to execute.</p>`;
    }
}

export async function runTest(name, element, isCustom, debug = false) {
    const envVars = getSelectedEnvVars();
    const globals = isCustom ? (getCustomGlobals()[name] || {}) : {};
    const selectedEnvName = getSelectedEnvName();
    const mergedGlobals = { ...envVars, ...globals };
    if (selectedEnvName) {
        mergedGlobals['__ENV_NAME__'] = selectedEnvName;
    }
    
    reportContent.innerHTML = `<p class="loading">${debug ? 'Debugging' : 'Running'} test...</p>`;
    try {
        const response = await fetch(`/api/runtest/${encodeURIComponent(name)}${debug ? '?debug=true' : ''}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(mergedGlobals)
        });
        state.lastMarkdown = await response.text();
        reportContent.innerHTML = marked.parse(state.lastMarkdown);
        exportBtn.style.display = 'block';
        exportDropdown.style.display = 'inline-block';
        
        setTimeout(() => {
            reportContent.querySelectorAll('pre code').forEach((block) => {
                highlightHttpSource(block);
            });
        }, 0);
    } catch (err) {
        reportContent.innerHTML = `<p style="color: red">Error: ${err.message}</p>`;
    }
}

export async function runSingleRequest(requestLine, lineIndex, debug = false) {
    const source = state.isEditing ? cmEditor.state.doc.toString() : state.lastSource;
    const envVars = getSelectedEnvVars();
    const globals = state.isCustomTest && state.currentTestName ? (getCustomGlobals()[state.currentTestName] || {}) : (state.unsavedGlobals || {});
    const selectedEnvName = getSelectedEnvName();
    const mergedGlobals = { ...envVars, ...globals };
    if (selectedEnvName) {
        mergedGlobals['__ENV_NAME__'] = selectedEnvName;
    }
    
    reportContent.innerHTML = `<p class="loading">${debug ? 'Debugging' : 'Running'} request...</p>`;
    try {
        const response = await fetch(`/api/runtest/single${debug ? '?debug=true' : ''}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json; charset=UTF-8' },
            body: JSON.stringify({
                name: state.currentTestName || 'Single Request',
                content: source,
                requestLine: requestLine,
                lineIndex: lineIndex,
                globals: mergedGlobals
            })
        });
        state.lastMarkdown = await response.text();
        reportContent.innerHTML = marked.parse(state.lastMarkdown);
        exportBtn.style.display = 'block';
        exportDropdown.style.display = 'inline-block';
        
        setTimeout(() => {
            reportContent.querySelectorAll('pre code').forEach((block) => {
                highlightHttpSource(block);
            });
        }, 0);
    } catch (err) {
        reportContent.innerHTML = `<p style="color: red">Error: ${err.message}</p>`;
    }
}

export function highlightHttpSource(element) {
    const isHttp = element.classList.contains('language-http') || (!element.classList.contains('language-json') && !element.classList.contains('language-xml') && !element.classList.contains('language-html') && !element.classList.contains('language-javascript') && !element.classList.contains('language-sql'));
    
    if (isHttp) {
        let content = element.textContent;
        // Basic HTTP highlighting logic
        content = content.replace(/^(GET|POST|PUT|DELETE|PATCH|OPTIONS|HEAD|TRACE) /gm, '<span class="hljs-keyword">$1</span> ');
        content = content.replace(/^(HTTP\/1\.[01]) (\d{3})/gm, '<span class="hljs-keyword">$1</span> <span class="hljs-number">$2</span>');
        content = content.replace(/^([A-Za-z0-9-]+): /gm, '<span class="hljs-attribute">$1</span>: ');
        content = content.replace(/\{\{([^}]+)\}\}/g, '<span class="hljs-variable">{{$1}}</span>');
        content = content.replace(/^(#.*|\/\/.*)$/gm, '<span class="hljs-comment">$1</span>');
        element.innerHTML = content;
    } else {
        hljs.highlightElement(element);
    }
}

