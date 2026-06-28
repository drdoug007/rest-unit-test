import { state, sourceEditor, runCustomBtn, saveCustomBtn, globalsBtn, runViewBtn, cloneBtn, sourceBtn, reportContent, saveCustomTest, saveCustomGlobals, getCustomTests, editor as cmEditor } from './core.js';
import { fetchTests } from './test-runner.js';
import { initEditor } from './editor.js';
import * as jsyaml from 'js-yaml';

const importFileBtn = document.getElementById('import-file-btn');
const importUrlBtn = document.getElementById('import-url-btn');
const importPasteBtn = document.getElementById('import-paste-btn');
const importFileInput = document.getElementById('import-file-input');

export function setupImporters() {
    if (importFileBtn) importFileBtn.onclick = () => importFileInput.click();
    if (importUrlBtn) importUrlBtn.onclick = async () => {
        const url = prompt('Enter OpenAPI specification URL:');
        if (url) {
            try {
                const response = await fetch(url);
                const content = await response.text();
                handleImportedContent(content, url);
            } catch (err) {
                alert('Error fetching URL: ' + err.message);
            }
        }
    };
    if (importPasteBtn) importPasteBtn.onclick = () => {
        const content = prompt('Paste OpenAPI (YAML/JSON), Postman, or Insomnia content here:');
        if (content) {
            handleImportedContent(content, 'clipboard');
        }
    };

    if (importFileInput) importFileInput.onchange = (e) => {
        const file = e.target.files[0];
        if (!file) return;
        const reader = new FileReader();
        reader.onload = (event) => handleImportedContent(event.target.result, file.name);
        reader.readAsText(file);
        importFileInput.value = '';
    };
}

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
        
        state.currentTestName = '';
        state.unsavedGlobals = null;
        state.isCustomTest = true;
        state.isEditing = true;
        state.lastSource = httpContent;
        
        setEditorContent(state.lastSource);
        sourceEditor.style.display = 'block';
        runCustomBtn.style.display = 'inline-block';
        saveCustomBtn.style.display = 'inline-block';
        globalsBtn.style.display = 'inline-block';
        runViewBtn.style.display = 'none';
        cloneBtn.style.display = 'inline-block';
        cloneBtn.textContent = 'Cancel';
        sourceBtn.style.display = 'none';
        
        document.querySelectorAll('#test-list li').forEach(li => li.classList.remove('active'));
        reportContent.innerHTML = `<p>Imported ${type} from ${sourceName}. Review and Save or Run.</p>`;
    } catch (err) {
        alert('Error parsing content: ' + err.message);
    }
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

function generateExampleFromSchema(schema, spec) {
    if (!schema) return null;
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
    } else if (type === 'number' || type === 'integer') return 0;
    else if (type === 'boolean') return true;
    return null;
}

function convertOpenApiToHttp(spec) {
    let httpStr = `### ${spec.info?.title || 'OpenAPI Import'}\n`;
    if (spec.info?.description) httpStr += `// ${spec.info.description.replace(/\n/g, '\n// ')}\n`;
    httpStr += '\n';
    const baseUrl = spec.servers?.[0]?.url || '{{baseUrl}}';
    for (const [path, methods] of Object.entries(spec.paths || {})) {
        for (const [method, operation] of Object.entries(methods)) {
            if (['get', 'post', 'put', 'delete', 'patch', 'options', 'head'].includes(method.toLowerCase())) {
                httpStr += `### ${operation.summary || operation.operationId || (method.toUpperCase() + ' ' + path)}\n`;
                if (operation.description) httpStr += `// ${operation.description.replace(/\n/g, '\n// ')}\n`;
                let fullPath = path;
                if (operation.parameters) {
                    operation.parameters.filter(p => p.in === 'path').forEach(p => {
                        const val = p.example || p.default || `{${p.name}}`;
                        fullPath = fullPath.replace(`{${p.name}}`, val);
                    });
                }
                const url = fullPath.startsWith('http') ? fullPath : `${baseUrl}${fullPath}`;
                httpStr += `${method.toUpperCase()} ${url}\n`;
                if (operation.parameters) {
                    operation.parameters.filter(p => p.in === 'header').forEach(p => {
                        const val = p.example || p.default || 'value';
                        httpStr += `${p.name}: ${val}\n`;
                    });
                }
                if (operation.responses) {
                    const firstSuccess = Object.keys(operation.responses).find(r => r.startsWith('2'));
                    if (firstSuccess && operation.responses[firstSuccess].content) {
                        const contentTypes = Object.keys(operation.responses[firstSuccess].content);
                        if (contentTypes.length > 0) httpStr += `Accept: ${contentTypes[0]}\n`;
                    }
                }
                if (spec.components?.securitySchemes) {
                    const security = operation.security || spec.security;
                    if (security) {
                        security.forEach(s => {
                            const schemeName = Object.keys(s)[0];
                            const scheme = spec.components.securitySchemes[schemeName];
                            if (scheme) {
                                if (scheme.type === 'http' && scheme.scheme === 'basic') httpStr += `Authorization: Basic username password\n`;
                                else if (scheme.type === 'http' && scheme.scheme === 'bearer') httpStr += `Authorization: Bearer {{token}}\n`;
                                else if (scheme.type === 'apiKey' && scheme.in === 'header') httpStr += `${scheme.name}: {{apiKey}}\n`;
                            }
                        });
                    }
                }
                if (method.toLowerCase() !== 'get' && method.toLowerCase() !== 'delete' && operation.requestBody?.content) {
                    const contentTypes = Object.keys(operation.requestBody.content);
                    if (contentTypes.length > 0) {
                        const contentType = contentTypes[0];
                        httpStr += `Content-Type: ${contentType}\n`;
                        const schema = operation.requestBody.content[contentType].schema;
                        const example = generateExampleFromSchema(schema, spec);
                        if (example) {
                            httpStr += '\n';
                            httpStr += contentType.includes('json') ? JSON.stringify(example, null, 2) : example;
                            httpStr += '\n';
                        }
                    }
                }
                httpStr += `\n> {%\n    client.test("Request executed successfully", function() {\n        client.assert(response.status === 200, "Response status is not 200");\n    });\n%}\n\n`;
            }
        }
    }
    return httpStr;
}

function convertPostmanToHttp(data) {
    let httpStr = `### ${data.info?.name || 'Postman Import'}\n`;
    if (data.info?.description) httpStr += `// ${data.info.description.replace(/\n/g, '\n// ')}\n`;
    httpStr += '\n';
    function processItems(items) {
        items.forEach(item => {
            if (item.item) processItems(item.item);
            else if (item.request) {
                const req = item.request;
                httpStr += `### ${item.name || 'Postman Request'}\n`;
                if (req.description) httpStr += `// ${req.description.replace(/\n/g, '\n// ')}\n`;
                const method = typeof req === 'string' ? 'GET' : (req.method || 'GET');
                const url = typeof req === 'string' ? req : (req.url?.raw || req.url || '');
                httpStr += `${method} ${url}\n`;
                if (req.header) {
                    req.header.forEach(h => {
                        if (!h.disabled) httpStr += `${h.key}: ${h.value}\n`;
                    });
                }
                if (req.body && req.body.raw) {
                    httpStr += '\n' + req.body.raw + '\n';
                }
                httpStr += '\n';
                if (item.event) {
                    item.event.forEach(ev => {
                        if (ev.listen === 'test' && ev.script && ev.script.exec) {
                            httpStr += '> {%\n';
                            const lines = Array.isArray(ev.script.exec) ? ev.script.exec : [ev.script.exec];
                            lines.forEach(line => {
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
                                httpStr += `    ${mappedLine}\n`;
                            });
                            httpStr += '%}\n\n';
                        } else if (ev.listen === 'prerequest' && ev.script && ev.script.exec) {
                             httpStr += '< {%\n';
                             const lines = Array.isArray(ev.script.exec) ? ev.script.exec : [ev.script.exec];
                             lines.forEach(line => {
                                 let mappedLine = line
                                     .replace(/pm\.environment\.set\(/g, 'client.global.set(')
                                     .replace(/pm\.globals\.set\(/g, 'client.global.set(');
                                 httpStr += `    ${mappedLine}\n`;
                             });
                             httpStr += '%}\n\n';
                        }
                    });
                }
            }
        });
    }
    if (data.item) processItems(data.item);
    return httpStr;
}

function convertInsomniaToHttp(data) {
    let httpStr = `### Insomnia Export\n\n`;
    const resources = data.resources || [];
    const requests = resources.filter(r => r._type === 'request');
    const environments = resources.filter(r => r._type === 'environment');
    if (environments.length > 0) {
        httpStr += `// Environments found:\n`;
        environments.forEach(env => {
            httpStr += `// ${env.name}: ${JSON.stringify(env.data)}\n`;
        });
        httpStr += '\n';
    }
    requests.forEach(req => {
        httpStr += `### ${req.name || 'Request'}\n`;
        if (req.description) httpStr += `// ${req.description.replace(/\n/g, '\n// ')}\n`;
        const method = req.method || 'GET';
        let url = req.url.replace(/\{\{\s*_\./g, '{{').replace(/\{\{\s*/g, '{{').replace(/\s*\}\}/g, '}}');
        httpStr += `${method} ${url}\n`;
        if (req.headers) req.headers.forEach(h => { httpStr += `${h.name}: ${h.value}\n`; });
        if (req.authentication) {
            const auth = req.authentication;
            if (auth.type === 'basic') httpStr += `Authorization: Basic ${auth.username} ${auth.password}\n`;
            else if (auth.type === 'bearer') httpStr += `Authorization: Bearer ${auth.token}\n`;
        }
        if (req.body && req.body.text) {
            httpStr += '\n' + req.body.text + '\n';
        } else if (req.body && req.body.params) {
             httpStr += 'Content-Type: application/x-www-form-urlencoded\n\n';
             const params = req.body.params.map(p => `${p.name}=${p.value}`).join('&');
             httpStr += params + '\n';
        }
        httpStr += '\n';
    });
    return httpStr;
}
