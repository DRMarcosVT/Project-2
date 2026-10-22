// Focused rendering regression; run with: node test/WebControlsTest.cjs
// Uses DOM stubs, so this does not replace a manual browser smoke test.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const html = fs.readFileSync(path.join(__dirname, '../web/index.html'), 'utf8');
const source = html.slice(html.indexOf('  function renderTable(s)'),
    html.indexOf('  // ---------------------------------------------------------------- shoe'));
function element() {
    const classes = new Set();
    return {
        children: [], textContent: '',
        get firstChild() { return this.children[0]; },
        appendChild(child) { this.children.push(child); },
        removeChild() { this.children.shift(); },
        addEventListener() {},
        set className(value) { classes.clear(); value.split(' ').forEach(c => classes.add(c)); },
        classList: {
            add(name) {
                if (!name || /\s/.test(name)) throw new Error('Invalid class token');
                classes.add(name);
            },
            contains(name) { return classes.has(name); }
        }
    };
}
const nodes = new Map();
const context = {
    $: id => {
        if (!nodes.has(id)) nodes.set(id, element());
        return nodes.get(id);
    },
    clear(node) { while (node.firstChild) node.removeChild(node.firstChild); },
    el: (tag, attrs, text) => Object.assign(element(), { textContent: text }),
    renderHand() {}, money: n => '$' + Math.abs(n), move() {},
    document: {}, setTimeout() {}
};
vm.createContext(context);
vm.runInContext(source, context);
const playing = {
    phase: 'PLAYER_TURN', player: {}, dealer: { holeHidden: true, total: 4 },
    bet: 10, balance: 90, round: 1, wins: 0, losses: 0, message: ''
};
const buttons = () => nodes.get('controls').children.map(n => n.textContent);
for (const [outcome, net, resultClass] of [
    ['PUSH', 0, null], ['WIN', 10, 'win'], ['LOSS', -10, 'loss']
]) {
    context.renderTable(playing);
    assert.deepEqual(buttons(), ['Hit', 'Stand']);
    context.renderTable({ ...playing, phase: 'ROUND_OVER', outcome, net });
    assert.deepEqual(buttons(), ['Play another round', 'Leave the table']);
    const msg = nodes.get('message');
    if (resultClass) assert.ok(msg.classList.contains(resultClass));
    else {
        assert.equal(msg.textContent, 'Push. Bet returned.');
        assert.ok(!msg.classList.contains('win') && !msg.classList.contains('loss'));
    }
}
console.log('PASS: Push, win, and loss replace Hit/Stand with next-round controls.');
