import { state, editor as cmEditor, showAssertionToast } from './core.js';

export function setupVisualAssertionBuilder() {
    document.addEventListener('click', (e) => {
        if (e.target.classList.contains('hljs-attr') || e.target.classList.contains('hljs-name')) {
            const container = e.target.closest('pre');
            if (!container) return;
            
            const codeBlock = container.querySelector('code');
            if (!codeBlock) return;

            const isJson = codeBlock.classList.contains('language-json');
            const isXml = codeBlock.classList.contains('language-xml') || codeBlock.classList.contains('language-html');
            
            if (isJson || isXml) generateAssertion(e.target);
        }
    });
}

function findPath(obj, targetKey, targetValue, currentPath = "response.body") {
    if (typeof obj !== 'object' || obj === null) return null;

    if (Array.isArray(obj)) {
        for (let i = 0; i < obj.length; i++) {
            const result = findPath(obj[i], targetKey, targetValue, `${currentPath}[${i}]`);
            if (result) return result;
        }
    } else {
        for (const key in obj) {
            if (key === targetKey) {
                let val = obj[key];
                if (typeof val === 'object' && val !== null) val = JSON.stringify(val);
                if (String(val) === String(targetValue)) {
                    return `${currentPath}.${key}`;
                }
            }
            const result = findPath(obj[key], targetKey, targetValue, `${currentPath}.${key}`);
            if (result) return result;
        }
    }
    return null;
}

function findXmlPath(codeBlock, clickedTag, clickedValue) {
    const allTags = Array.from(codeBlock.querySelectorAll('.hljs-tag .hljs-name'));
    const sameNameTags = allTags.filter(t => t.textContent === clickedTag);
    
    let occurrenceIndex = 0;
    let count = 0;
    for (let i = 0; i < sameNameTags.length; i++) {
        const tag = sameNameTags[i];
        let val = "";
        let next = tag.closest('.hljs-tag').nextSibling;
        while (next) {
            if (next.nodeType === Node.TEXT_NODE) val += next.textContent;
            else if (next.classList && !next.classList.contains('hljs-tag')) val += next.textContent;
            else break;
            next = next.nextSibling;
        }
        if (val.trim() === clickedValue.trim()) {
            occurrenceIndex = count;
            break;
        }
        count++;
    }
    return `response.body.getElementsByTagName("${clickedTag}")[${occurrenceIndex}].textContent`;
}

function generateAssertion(element) {
    const codeBlock = element.closest('pre code');
    if (!codeBlock) return;
    
    let testName = null;
    let currentElement = codeBlock.parentElement; 
    while (currentElement) {
        let sibling = currentElement.previousSibling;
        while (sibling) {
            if (sibling.nodeType === Node.COMMENT_NODE) {
                const match = sibling.nodeValue.match(/TEST_NAME:\s*(.*)/);
                if (match) { testName = match[1].trim(); break; }
            }
            sibling = sibling.previousSibling;
        }
        if (testName) break;
        currentElement = currentElement.parentElement;
    }

    const isJson = codeBlock.classList.contains('language-json');
    const isXml = codeBlock.classList.contains('language-xml') || codeBlock.classList.contains('language-html');
    const clickedText = element.textContent.replace(/["':]/g, '').trim();
    
    let clickedValue = "";
    if (isJson) {
        let next = element.nextSibling;
        while (next && next.textContent.trim() === ":") next = next.nextSibling;
        if (next) clickedValue = next.textContent.replace(/["',]/g, '').trim();
    } else if (isXml) {
        let next = element.closest('.hljs-tag').nextSibling;
        while (next) {
            if (next.nodeType === Node.TEXT_NODE) clickedValue += next.textContent;
            else if (next.classList && !next.classList.contains('hljs-tag')) clickedValue += next.textContent;
            else break;
            next = next.nextSibling;
        }
        clickedValue = clickedValue.trim();
    }

    let assertionPath = "";
    if (isJson) {
        try {
            const fullJson = JSON.parse(codeBlock.textContent);
            assertionPath = findPath(fullJson, clickedText, clickedValue) || `response.body.${clickedText}`;
        } catch (e) {
            assertionPath = `response.body.${clickedText}`;
        }
    } else if (isXml) {
        assertionPath = findXmlPath(codeBlock, element.textContent, clickedValue);
    }

    if (!assertionPath) return;

    let displayValue = clickedValue;
    if (isNaN(displayValue) && displayValue !== "true" && displayValue !== "false" && displayValue !== "null") {
        displayValue = `"${displayValue.replace(/"/g, '\\"')}"`;
    }

    let assertion = `client.test("Check ${clickedText}", () => {\n    client.assert(${assertionPath} === ${displayValue}, "Expected ${clickedText} to be ${displayValue}");\n});`;

    if (cmEditor) {
        const doc = cmEditor.state.doc.toString();
        const fullContent = doc;
        let targetInsertPos = doc.length;
        let existingPostScriptRange = null;
        let indent = "    ";

        if (testName) {
            const escapedName = testName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
            const requestRegex = new RegExp(`^###\\s*${escapedName}\\s*$`, 'm');
            const match = fullContent.match(requestRegex);
            
            if (match) {
                const requestStartIdx = match.index;
                const nextRequestMatch = fullContent.slice(requestStartIdx + 1).match(/^###/m);
                const requestEndIdx = nextRequestMatch ? requestStartIdx + 1 + nextRequestMatch.index : doc.length;
                const requestBlock = fullContent.slice(requestStartIdx, requestEndIdx);
                const postScriptMatch = requestBlock.match(/> {%([\s\S]*?)%}/);
                
                if (postScriptMatch) {
                    const relativeStart = postScriptMatch.index;
                    const relativeEnd = requestBlock.indexOf('%}', relativeStart);
                    existingPostScriptRange = { from: requestStartIdx + relativeEnd, to: requestStartIdx + relativeEnd };
                    const scriptContent = postScriptMatch[1];
                    const lines = scriptContent.split('\n').filter(l => l.trim().length > 0);
                    if (lines.length > 0) {
                        const lastLine = lines[lines.length - 1];
                        const indentMatch = lastLine.match(/^(\s+)/);
                        if (indentMatch) indent = indentMatch[1];
                    }
                } else {
                    targetInsertPos = requestEndIdx;
                }
            }
        } else {
            const postScriptMatch = fullContent.match(/> {%([\s\S]*?)%}/);
            if (postScriptMatch) {
                const endIdx = fullContent.indexOf('%}', postScriptMatch.index);
                existingPostScriptRange = { from: endIdx, to: endIdx };
                const scriptContent = postScriptMatch[1];
                const lines = scriptContent.split('\n').filter(l => l.trim().length > 0);
                if (lines.length > 0) {
                    const lastLine = lines[lines.length - 1];
                    const indentMatch = lastLine.match(/^(\s+)/);
                    if (indentMatch) indent = indentMatch[1];
                }
            }
        }

        const indentedAssertion = assertion.split('\n').join('\n' + indent);
        
        if (existingPostScriptRange) {
            cmEditor.dispatch({
                changes: { from: existingPostScriptRange.from, to: existingPostScriptRange.to, insert: `\n${indent}${indentedAssertion}\n` }
            });
        } else {
            const prefix = targetInsertPos > 0 && !fullContent[targetInsertPos - 1].match(/\n/) ? '\n\n' : '\n\n';
            cmEditor.dispatch({
                changes: { from: targetInsertPos, to: targetInsertPos, insert: `${prefix}> {%\n${indent}${indentedAssertion}\n%}` }
            });
        }
        
        showAssertionToast(`Assertion for "${clickedText}" added!`);
    }
}
