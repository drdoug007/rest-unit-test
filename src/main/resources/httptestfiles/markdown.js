export class Markdown {

    generateMarkdownTable(columns, data) {
        // 1. Create the Header Row: | id | name | email |
        const header = `| ${columns.join(' | ')} |`;

        // 2. Create the Separator Row: | --- | --- | --- |
        const separator = `| ${columns.map(() => '---').join(' | ')} |`;

        // 3. Create the Data Rows
        const rows = data.map(row => {
            const values = columns.map(col => {
                const val = row[col];
                // Convert null/undefined to empty string for clean markdown
                return (val !== null && val !== undefined) ? val : '';
            });
            return `| ${values.join(' | ')} |`;
        });

        // Join everything with newlines
        return [header, separator, ...rows].join('\n');
    }

    heading(level, text) {
        client.markdown("#".repeat(level) + " " + text + "\n");
    }

    description(text) {
        client.markdown(text + "\n");
    }

    codeBlock(language, code) {
        client.markdown("```" + language + "\n" + code + "\n```\n");
    }

    table(columns, data) {
        client.markdown(this.generateMarkdownTable(columns, data) + "\n");
    }

}

export const markdowner = new Markdown();