import { state, editor as cmEditor, reportContent, sourceCode, sourceEditor, sourceContent, runCustomBtn, saveCustomBtn, globalsBtn, runViewBtn, cloneBtn, sourceBtn, testList, addTestBtn, setEditorContent, exportBtn, exportDropdown, saveCustomTest, saveCustomGlobals, getCustomTests, getCustomGlobals } from './core.js';
import { fetchTests, runTest, highlightHttpSource } from './test-runner.js';
import { getSelectedEnvVars } from './env-manager.js';
import { updateEditorTheme } from './editor.js';
import { marked } from 'marked';
import hljs from 'highlight.js';

export function setupInit() {
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

    if (window.matchMedia) {
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', updateHighlightTheme);
    }
    updateHighlightTheme();

    window.onclick = (event) => {
        const envModal = document.getElementById('env-modal');
        const globalsModal = document.getElementById('globals-modal');
        if (event.target == envModal) envModal.style.display = 'none';
        if (event.target == globalsModal) globalsModal.style.display = 'none';
    };

    // Source panel and custom test actions
    if (addTestBtn) addTestBtn.onclick = () => {
        try {
            state.currentTestName = '';
            state.unsavedGlobals = null;
            state.isCustomTest = true;
            state.isEditing = true;
            state.lastSource = '### New Test\nGET http://localhost:8080/api/cardealer\n\n> {%\n    client.test("Request executed successfully", function() {\n        client.assert(response.status === 200, "Response status is not 200");\n    });\n%}\n';
            setEditorContent(state.lastSource);
            
            const se = document.getElementById('source-editor');
            const sc = document.getElementById('source-content');
            if (sc) sc.style.display = 'none';
            if (se) {
                se.style.display = 'block';
                se.parentElement.classList.add('show');
            }
            
            if (runCustomBtn) runCustomBtn.style.display = 'inline-block';
            if (saveCustomBtn) saveCustomBtn.style.display = 'inline-block';
            if (globalsBtn) globalsBtn.style.display = 'inline-block';
            if (runViewBtn) runViewBtn.style.display = 'none';
            if (cloneBtn) {
                cloneBtn.style.display = 'inline-block';
                cloneBtn.textContent = 'Cancel';
            }
            if (sourceBtn) sourceBtn.style.display = 'none';
            
            document.querySelectorAll('#test-list li').forEach(li => li.classList.remove('active'));
            reportContent.innerHTML = '<p>Create your new test. Review and Save or Run.</p>';
        } catch (err) {
            console.error('Error in addTestBtn.onclick:', err);
        }
    };

    if (cloneBtn) cloneBtn.onclick = () => {
        if (state.isCustomTest) {
            if (state.isEditing) {
                if (state.currentTestName === '') {
                    state.isEditing = false;
                    state.isCustomTest = false;
                    sourceEditor.style.display = 'none';
                    sourceContent.style.display = 'block';
                    runCustomBtn.style.display = 'none';
                    saveCustomBtn.style.display = 'none';
                    globalsBtn.style.display = 'none';
                    runViewBtn.style.display = 'none';
                    cloneBtn.style.display = 'none';
                    cloneBtn.textContent = 'Clone';
                    reportContent.innerHTML = '<p>Test creation cancelled.</p>';
                } else {
                    const originalContent = getCustomTests()[state.currentTestName];
                    setEditorContent(originalContent);
                    state.unsavedGlobals = null;
                }
            } else {
                state.isEditing = true;
                setEditorContent(state.lastSource);
                sourceContent.style.display = 'none';
                sourceEditor.style.display = 'block';
                runCustomBtn.style.display = 'inline-block';
                saveCustomBtn.style.display = 'inline-block';
                globalsBtn.style.display = 'inline-block';
                runViewBtn.style.display = 'none';
                cloneBtn.textContent = 'Cancel';
                sourceBtn.style.display = 'none';
            }
        } else {
            state.isEditing = true;
            state.isCustomTest = true;
            state.currentTestName = '';
            state.unsavedGlobals = null;
            setEditorContent(state.lastSource);
            sourceContent.style.display = 'none';
            sourceEditor.style.display = 'block';
            runCustomBtn.style.display = 'inline-block';
            saveCustomBtn.style.display = 'inline-block';
            globalsBtn.style.display = 'inline-block';
            runViewBtn.style.display = 'none';
            cloneBtn.textContent = 'Cancel';
            sourceBtn.style.display = 'none';
            document.querySelectorAll('#test-list li').forEach(li => li.classList.remove('active'));
        }
    };

    if (runViewBtn) runViewBtn.onclick = () => {
        const activeLi = testList.querySelector('li.active');
        if (activeLi) {
            const testName = activeLi.getAttribute('data-name');
            const isCustom = activeLi.querySelector('.custom-tag') !== null;
            runTest(testName, activeLi, isCustom);
        }
    };

    if (runCustomBtn) runCustomBtn.onclick = async () => {
        const editedCode = cmEditor ? cmEditor.state.doc.toString() : '';
        const envVars = getSelectedEnvVars();
        const globals = (state.currentTestName ? (getCustomGlobals()[state.currentTestName] || {}) : (state.unsavedGlobals || {}));
        const mergedGlobals = { ...envVars, ...globals };
        
        reportContent.innerHTML = '<p class="loading">Running custom test...</p>';
        try {
            const response = await fetch('/api/runtest/custom', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json; charset=UTF-8' },
                body: JSON.stringify({
                    name: state.currentTestName || 'Unsaved Custom Test',
                    content: editedCode,
                    globals: mergedGlobals
                })
            });
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            
            if (!state.isCustomTest || state.currentTestName === '') {
                const saveName = prompt('Enter a name for this custom test:', state.currentTestName ? state.currentTestName + ' (Clone)' : 'New Test');
                if (saveName) {
                    saveCustomTest(saveName, editedCode);
                    if (state.unsavedGlobals) {
                        saveCustomGlobals(saveName, state.unsavedGlobals);
                        state.unsavedGlobals = null;
                    }
                    state.currentTestName = saveName;
                    state.isCustomTest = true;
                    fetchTests();
                }
            } else {
                saveCustomTest(state.currentTestName, editedCode);
            }
            state.lastMarkdown = await response.text();
            state.lastSource = editedCode;
            reportContent.innerHTML = marked.parse(state.lastMarkdown);
            sourceCode.textContent = state.lastSource;
            exportBtn.style.display = 'block';
            exportDropdown.style.display = 'inline-block';
            setTimeout(() => {
                if (typeof hljs !== 'undefined') {
                    reportContent.querySelectorAll('pre code').forEach((block) => {
                        highlightHttpSource(block);
                    });
                }
            }, 0);
            state.isEditing = true;
            cloneBtn.textContent = 'Cancel';
            if (window.innerWidth >= 1024) sourceBtn.style.display = 'none';
        } catch (error) {
            reportContent.innerHTML = `<p style="color: red">Error running custom test: ${error.message}</p>`;
        }
    };

    if (saveCustomBtn) saveCustomBtn.onclick = () => {
        const editedCode = cmEditor ? cmEditor.state.doc.toString() : '';
        if (!state.isCustomTest || state.currentTestName === '') {
            const saveName = prompt('Enter a name for this custom test:', state.currentTestName ? state.currentTestName + ' (Clone)' : 'New Test');
            if (saveName) {
                saveCustomTest(saveName, editedCode);
                if (state.unsavedGlobals) {
                    saveCustomGlobals(saveName, state.unsavedGlobals);
                    state.unsavedGlobals = null;
                }
                state.currentTestName = saveName;
                state.isCustomTest = true;
                state.lastSource = editedCode;
                sourceCode.textContent = state.lastSource;
                fetchTests();
                state.isEditing = true;
                cloneBtn.textContent = 'Cancel';
                sourceBtn.style.display = 'none';
            }
        } else {
            saveCustomTest(state.currentTestName, editedCode);
            state.lastSource = editedCode;
            sourceCode.textContent = state.lastSource;
            state.isEditing = true;
            cloneBtn.textContent = 'Cancel';
            if (window.innerWidth >= 1024) sourceBtn.style.display = 'none';
            alert('Test saved successfully.');
        }
    };
    
    if (sourceBtn) sourceBtn.onclick = () => {
        sourceContent.parentElement.classList.toggle('show');
    };
}

