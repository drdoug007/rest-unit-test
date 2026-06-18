const testList = document.getElementById('test-list');
const reportContent = document.getElementById('report-content');
const sourceContent = document.getElementById('source-content');
const exportBtn = document.getElementById('export-btn');
const sourceBtn = document.getElementById('source-btn');
const cloneBtn = document.getElementById('clone-btn');
const runViewBtn = document.getElementById('run-view-btn');
const saveCustomBtn = document.getElementById('save-custom-btn');
const runCustomBtn = document.getElementById('run-custom-btn');
const addTestBtn = document.getElementById('add-test-btn');
const importOpenApiBtn = document.getElementById('import-openapi-btn');
const importOptions = document.getElementById('import-options');
const importFileBtn = document.getElementById('import-file-btn');
const importUrlBtn = document.getElementById('import-url-btn');
const importPasteBtn = document.getElementById('import-paste-btn');
const openapiFileInput = document.getElementById('openapi-file-input');
const sourceEditor = document.getElementById('source-editor');
let currentTestName = '';
let isCustomTest = false;
let isViewingSource = false;
let isEditing = false;
let lastMarkdown = '';
let lastSource = '';

// Local Storage Helpers
const STORAGE_KEY = 'custom_http_tests';

function getCustomTests() {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? JSON.parse(stored) : {};
}

function saveCustomTest(name, content) {
    const tests = getCustomTests();
    tests[name] = content;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tests));
}

function deleteCustomTest(name) {
    const tests = getCustomTests();
    delete tests[name];
    localStorage.setItem(STORAGE_KEY, JSON.stringify(tests));
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
    }
    nameSpan.appendChild(document.createTextNode(name));
    nameSpan.onclick = () => selectTest(name, li, isCustom);
    
    li.appendChild(nameSpan);

    if (isCustom) {
        const dropdown = document.createElement('div');
        dropdown.className = 'dropdown';
        dropdown.innerHTML = `
            <button class="dropbtn">⋮</button>
            <div class="dropdown-content">
                <a href="#" onclick="deleteCustomTest('${name}')">Delete</a>
                <a href="#" onclick="renameCustomTest('${name}')">Rename</a>
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
        if (currentTestName === oldName) currentTestName = newName;
        fetchTests();
    }
}

// Close dropdowns when clicking outside
window.onclick = function(event) {
    if (!event.target.matches('.dropbtn')) {
        document.querySelectorAll('.dropdown-content').forEach(d => d.classList.remove('show'));
    }
};

async function selectTest(testName, element, isCustom = false) {
    currentTestName = testName;
    isCustomTest = isCustom;
    isViewingSource = false;
    sourceBtn.textContent = 'View Source';
    // Update UI state
    document.querySelectorAll('#test-list li').forEach(li => li.classList.remove('active'));
    element.classList.add('active');
    
    reportContent.innerHTML = `<p>Test <strong>${testName}</strong> selected. Click Run to execute.</p>`;
    sourceContent.innerHTML = `<p class="loading">Fetching source...</p>`;
    exportBtn.style.display = 'none';
    sourceBtn.style.display = 'none';
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
        sourceContent.innerHTML = `<pre><code class="language-http">${escapeHtml(lastSource)}</code></pre>`;
        
        // Use setTimeout to ensure DOM is updated before highlighting
        setTimeout(() => {
            if (typeof hljs === 'undefined') {
                console.error('highlight.js not available for highlighting');
                return;
            }
            // Ensure highlighting is applied to the source panel
            const sourceCode = sourceContent.querySelector('code');
            if (sourceCode) {
                try {
                    hljs.highlightElement(sourceCode);
                } catch (e) { console.error('Error highlighting source panel:', e); }
            }
        }, 0);
        
        sourceBtn.style.display = 'block';
        cloneBtn.style.display = 'inline-block';
        runViewBtn.style.display = 'inline-block';
        isEditing = false;
        sourceEditor.style.display = 'none';
        sourceContent.style.display = 'block';
        runCustomBtn.style.display = 'none';
        saveCustomBtn.style.display = 'none';
        cloneBtn.textContent = isCustomTest ? 'Edit' : 'Clone';
    } catch (error) {
        sourceContent.innerHTML = `<p style="color: red">Error fetching source: ${error.message}</p>`;
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
    sourceContent.innerHTML = `<p class="loading">Fetching source...</p>`;
    exportBtn.style.display = 'none';
    sourceBtn.style.display = 'none';

    try {
        let testResponse, sourceResponse;
        if (isCustom) {
            const content = getCustomTests()[testName];
            testResponse = await fetch('/api/runtest/custom', {
                method: 'POST',
                headers: { 'Content-Type': 'text/plain; charset=UTF-8' },
                body: content
            });
            sourceResponse = { text: async () => content };
        } else {
            testResponse = await fetch(`/api/runtest/${testName}`);
            sourceResponse = await fetch(`/api/test/${testName}`);
        }
        
        lastMarkdown = await testResponse.text();
        lastSource = await sourceResponse.text();

        reportContent.innerHTML = marked.parse(lastMarkdown);
        sourceContent.innerHTML = `<pre><code class="language-http">${escapeHtml(lastSource)}</code></pre>`;
        
        // TODO: Fix highlighting not being applied correctly.
        // Even though hljs.highlightElement is called, the UI does not show the expected colors.
        // Investigating if it's a CSS conflict or a timing issue with the library loading.
    
        // Use setTimeout to ensure DOM is updated before highlighting
        setTimeout(() => {
            if (typeof hljs === 'undefined') {
                console.error('highlight.js not available for highlighting');
                return;
            }
            // Ensure highlighting is applied to the source panel
            const sourceCode = sourceContent.querySelector('code');
            if (sourceCode) {
                try {
                    hljs.highlightElement(sourceCode);
                } catch (e) { console.error('Error highlighting source panel:', e); }
            }
            
            // Also highlight any code blocks in the report
            reportContent.querySelectorAll('pre code').forEach((block) => {
                try {
                    hljs.highlightElement(block);
                } catch (e) { console.error('Error highlighting report block:', e); }
            });
        }, 0);
        
        exportBtn.style.display = 'block';
        sourceBtn.style.display = 'block';
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
    return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

sourceBtn.onclick = async () => {
    if (isViewingSource) {
        reportContent.innerHTML = marked.parse(lastMarkdown);
        sourceBtn.textContent = 'View Source';
        exportBtn.style.display = 'block';
        isViewingSource = false;
    } else {
        reportContent.innerHTML = `<pre><code class="language-http">${escapeHtml(lastSource)}</code></pre>`;
        setTimeout(() => {
            if (typeof hljs !== 'undefined') {
                const reportCode = reportContent.querySelector('code');
                if (reportCode) {
                    try {
                        hljs.highlightElement(reportCode);
                    } catch (e) { console.error('Error highlighting toggled source:', e); }
                }
            }
        }, 0);
        sourceBtn.textContent = 'View Report';
        exportBtn.style.display = 'none';
        isViewingSource = true;
    }
};

exportBtn.onclick = () => {
    const element = document.getElementById('report-content');
    const opt = {
        margin:       10,
        filename:     `test-report-${currentTestName}.pdf`,
        image:        { type: 'jpeg', quality: 0.98 },
        html2canvas:  { scale: 2 },
        jsPDF:        { unit: 'mm', format: 'a4', orientation: 'portrait' }
    };
    html2pdf().set(opt).from(element).save();
};

cloneBtn.onclick = () => {
    if (!isEditing) {
        isEditing = true;
        sourceEditor.value = lastSource;
        sourceContent.style.display = 'none';
        sourceEditor.style.display = 'block';
        runCustomBtn.style.display = 'inline-block';
        saveCustomBtn.style.display = 'inline-block';
        runViewBtn.style.display = 'none';
        cloneBtn.textContent = 'Cancel';
    } else {
        isEditing = false;
        sourceContent.style.display = 'block';
        sourceEditor.style.display = 'none';
        runCustomBtn.style.display = 'none';
        saveCustomBtn.style.display = 'none';
        runViewBtn.style.display = 'inline-block';
        cloneBtn.textContent = isCustomTest ? 'Edit' : 'Clone';
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
    isCustomTest = true;
    isEditing = true;
    lastSource = '### New Test\nGET https://api.example.com\n';
    
    // UI updates
    sourceEditor.value = lastSource;
    sourceContent.style.display = 'none';
    sourceEditor.style.display = 'block';
    runCustomBtn.style.display = 'inline-block';
    saveCustomBtn.style.display = 'inline-block';
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
        isCustomTest = true;
        isEditing = true;
        lastSource = httpContent;
        
        sourceEditor.value = lastSource;
        sourceContent.style.display = 'none';
        sourceEditor.style.display = 'block';
        runCustomBtn.style.display = 'inline-block';
        saveCustomBtn.style.display = 'inline-block';
        runViewBtn.style.display = 'none';
        cloneBtn.style.display = 'inline-block';
        cloneBtn.textContent = 'Cancel';
        
        document.querySelectorAll('#test-list li').forEach(li => li.classList.remove('active'));
        reportContent.innerHTML = `<p>Imported from ${sourceName}. Review and Save or Run.</p>`;
    } catch (err) {
        alert('Error parsing OpenAPI spec: ' + err.message);
    }
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

                http += `${method.toUpperCase()} ${baseUrl}${fullPath}\n`;
                http += 'Content-Type: application/json\n';
                http += '\n';

                // Request Body example
                if (operation.requestBody) {
                    const content = operation.requestBody.content;
                    const jsonContent = content?.['application/json'];
                    if (jsonContent?.example) {
                        http += JSON.stringify(jsonContent.example, null, 2) + '\n';
                    } else if (jsonContent?.schema) {
                        // Could generate from schema, but let's keep it simple for now
                        http += '{\n  "TODO": "Add request body"\n}\n';
                    }
                    http += '\n';
                }

                // Response assertions
                http += '> {%\n';
                http += '  // Basic assertions\n';
                const successStatus = Object.keys(operation.responses || {}).find(s => s.startsWith('2')) || '200';
                http += `  client.assert(response.status === ${successStatus}, "Response status is ${successStatus}");\n`;
                http += '  client.assert(response.contentType.mimeType === "application/json", "Expected JSON content type");\n';
                http += '%}\n\n';
            }
        }
    }
    return http;
}

saveCustomBtn.onclick = () => {
    const editedCode = sourceEditor.value;
    if (!isCustomTest || currentTestName === '') {
        const saveName = prompt('Enter a name for this custom test:', currentTestName ? currentTestName + ' (Clone)' : 'New Test');
        if (saveName) {
            saveCustomTest(saveName, editedCode);
            currentTestName = saveName;
            isCustomTest = true;
            lastSource = editedCode;
            sourceContent.innerHTML = `<pre><code class="language-http">${escapeHtml(lastSource)}</code></pre>`;
            fetchTests();
            // Exit edit mode after save
            isEditing = false;
            sourceContent.style.display = 'block';
            sourceEditor.style.display = 'none';
            runCustomBtn.style.display = 'none';
            saveCustomBtn.style.display = 'none';
            cloneBtn.textContent = isCustomTest ? 'Edit' : 'Clone';
            // Re-highlight
            setTimeout(() => {
                if (typeof hljs !== 'undefined') {
                    const sourceCode = sourceContent.querySelector('code');
                    if (sourceCode) hljs.highlightElement(sourceCode);
                }
            }, 0);
        }
    } else {
        saveCustomTest(currentTestName, editedCode);
        lastSource = editedCode;
        sourceContent.innerHTML = `<pre><code class="language-http">${escapeHtml(lastSource)}</code></pre>`;
        // Exit edit mode after save
        isEditing = false;
        sourceContent.style.display = 'block';
        sourceEditor.style.display = 'none';
        runCustomBtn.style.display = 'none';
        saveCustomBtn.style.display = 'none';
        cloneBtn.textContent = isCustomTest ? 'Edit' : 'Clone';
        // Re-highlight
        setTimeout(() => {
            if (typeof hljs !== 'undefined') {
                const sourceCode = sourceContent.querySelector('code');
                if (sourceCode) hljs.highlightElement(sourceCode);
            }
        }, 0);
    }
};

runCustomBtn.onclick = async () => {
    const editedCode = sourceEditor.value;
    reportContent.innerHTML = `<p class="loading">Running custom test...</p>`;
    
    try {
        const response = await fetch('/api/runtest/custom', {
            method: 'POST',
            headers: {
                'Content-Type': 'text/plain; charset=UTF-8'
            },
            body: editedCode
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
                currentTestName = saveName;
                isCustomTest = true;
                fetchTests();
            }
        } else {
            // Update existing custom test
            saveCustomTest(currentTestName, editedCode);
            // No need to fetchTests, but update lastSource to reflect the changes in the source panel
        }

        lastMarkdown = await response.text();
        lastSource = editedCode;

        reportContent.innerHTML = marked.parse(lastMarkdown);
        sourceContent.innerHTML = `<pre><code class="language-http">${escapeHtml(lastSource)}</code></pre>`;
        
        // Highlight
        setTimeout(() => {
            if (typeof hljs !== 'undefined') {
                const sourceCode = sourceContent.querySelector('code');
                if (sourceCode) hljs.highlightElement(sourceCode);
                reportContent.querySelectorAll('pre code').forEach((block) => {
                    hljs.highlightElement(block);
                });
            }
        }, 0);

        // Exit edit mode
        isEditing = false;
        sourceContent.style.display = 'block';
        sourceEditor.style.display = 'none';
        runCustomBtn.style.display = 'none';
        saveCustomBtn.style.display = 'none';
        cloneBtn.textContent = isCustomTest ? 'Edit' : 'Clone';
        
    } catch (error) {
        reportContent.innerHTML = `<p style="color: red">Error running custom test: ${error.message}</p>`;
    }
};

fetchTests();
