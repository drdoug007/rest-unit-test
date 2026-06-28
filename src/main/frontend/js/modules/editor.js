import { EditorView, basicSetup } from "codemirror";
import { EditorState, Compartment } from "@codemirror/state";
import { javascript } from "@codemirror/lang-javascript";
import { sql } from "@codemirror/lang-sql";
import { keymap } from "@codemirror/view";
import { indentWithTab } from "@codemirror/commands";
import { gutter, GutterMarker } from "@codemirror/view";
import { RangeSet } from "@codemirror/state";
import { oneDark } from "@codemirror/theme-one-dark";
import { state, sourceEditor, setEditor, editor as cmEditor } from './core.js';
import { runSingleRequest } from './test-runner.js';

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

export function initEditor(content = '') {
    const isDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    
    const view = new EditorView({
        state: EditorState.create({
            doc: content,
            extensions: [
                basicSetup,
                keymap.of([indentWithTab]),
                // We'll use javascript as default for now, but in a real scenario we'd use the http parser if available
                languageConf.of(javascript()), 
                themeConf.of(isDarkMode ? oneDark : []),
                runGutter,
            ]
        }),
        parent: sourceEditor
    });
    setEditor(view);
}

export function updateEditorTheme() {
    if (!cmEditor) return;
    const isDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    cmEditor.dispatch({
        effects: themeConf.reconfigure(isDarkMode ? oneDark : [])
    });
}
