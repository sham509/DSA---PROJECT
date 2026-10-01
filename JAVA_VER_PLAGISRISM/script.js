import {
  computeOverallSimilarity,
  getVerdict,
} from './algorithms.js';

const sourceText = document.querySelector('#sourceText');
const targetText = document.querySelector('#targetText');
const resultMetrics = document.querySelector('#resultMetrics');
const overallScore = document.querySelector('#overallScore');
const verdictLabel = document.querySelector('#verdictLabel');
const scoreBar = document.querySelector('#scoreBar');
const sourceMeta = document.querySelector('#sourceMeta');
const targetMeta = document.querySelector('#targetMeta');

const samples = {
  source: `Artificial intelligence is reshaping how modern businesses evaluate customer behavior and process data. By automating repetitive tasks and uncovering hidden patterns, organizations can improve decision-making quality, reduce operational costs, and deliver faster insight into customer needs.`,
  target: `Artificial intelligence is transforming how companies understand consumer behavior and manage information. With automation and pattern discovery, businesses can make stronger decisions, cut unnecessary costs, and gain quicker perspectives on customer needs and preferences.`,
};

function formatPercent(value) {
  return `${Math.round(value * 100)}%`;
}

function setMetaTarget(element, label, fileName) {
  if (!element) return;
  if (!fileName) {
    element.textContent = `${label}: no file loaded`;
    return;
  }

  element.textContent = `${label}: ${fileName}`;
}

async function extractTextFromFile(file) {
  const fileName = file.name.toLowerCase();

  if (fileName.endsWith('.txt') || file.type.startsWith('text/')) {
    return file.text();
  }

  if (fileName.endsWith('.pdf')) {
    if (!window.pdfjsLib) {
      throw new Error('PDF reader is unavailable.');
    }

    const { pdfjsLib } = window;
    pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js';

    const pdf = await pdfjsLib.getDocument({ data: await file.arrayBuffer() }).promise;
    const pages = [];

    for (let pageNumber = 1; pageNumber <= pdf.numPages; pageNumber += 1) {
      const page = await pdf.getPage(pageNumber);
      const textContent = await page.getTextContent();
      const pageText = textContent.items.map((item) => item.str).join(' ');
      pages.push(pageText);
    }

    return pages.join('\n\n');
  }

  if (fileName.endsWith('.docx') || fileName.endsWith('.doc')) {
    if (!window.mammoth) {
      throw new Error('Word parser is unavailable.');
    }

    const result = await window.mammoth.extractRawText({ arrayBuffer: await file.arrayBuffer() });
    return result.value;
  }

  return file.text();
}

async function handleFileUpload(event, side) {
  const file = event.target.files?.[0];
  if (!file) return;

  try {
    const extractedText = await extractTextFromFile(file);
    const textarea = side === 'source' ? sourceText : targetText;
    textarea.value = extractedText;
    setMetaTarget(side === 'source' ? sourceMeta : targetMeta, side === 'source' ? 'Source document' : 'Comparison document', file.name);
    analyzeText();
  } catch (error) {
    const textarea = side === 'source' ? sourceText : targetText;
    textarea.value = `Unable to extract text from ${file.name}. Please paste content manually instead.\n\nError: ${error.message}`;
    setMetaTarget(side === 'source' ? sourceMeta : targetMeta, side === 'source' ? 'Source document' : 'Comparison document', `${file.name} (read failed)`);
  }
}

function renderMetrics(metrics) {
  const entries = [
    ['Jaccard', metrics.jaccard],
    ['Cosine', metrics.cosine],
    ['Dice', metrics.dice],
    ['Levenshtein', metrics.levenshtein],
    ['LCS', metrics.lcs],
    ['Rabin-Karp', metrics.rabinKarp],
  ];

  resultMetrics.innerHTML = entries
    .map(
      ([label, value]) => `
        <div class="metric-item">
          <div class="metric-label-row">
            <span>${label}</span>
            <strong>${formatPercent(value)}</strong>
          </div>
          <div class="metric-bar"><span style="width:${formatPercent(value)}"></span></div>
        </div>
      `,
    )
    .join('');
}

function updateSummary(score) {
  const verdict = getVerdict(score);
  verdictLabel.textContent = verdict.label;
  verdictLabel.dataset.tone = verdict.tone;
  overallScore.textContent = formatPercent(score);
  scoreBar.style.width = formatPercent(score);

  if (verdict.tone === 'danger') {
    scoreBar.style.background = 'linear-gradient(90deg, #ff6b6b 0%, #ff8e53 100%)';
  } else if (verdict.tone === 'warning') {
    scoreBar.style.background = 'linear-gradient(90deg, #ffc857 0%, #ff9f43 100%)';
  } else if (verdict.tone === 'neutral') {
    scoreBar.style.background = 'linear-gradient(90deg, #60a5fa 0%, #7dd3fc 100%)';
  } else {
    scoreBar.style.background = 'linear-gradient(90deg, #35d399 0%, #7efdd0 100%)';
  }
}

function analyzeText() {
  const docA = sourceText.value.trim();
  const docB = targetText.value.trim();

  if (!docA || !docB) {
    verdictLabel.textContent = 'Enter both texts';
    verdictLabel.dataset.tone = 'neutral';
    overallScore.textContent = '0%';
    scoreBar.style.width = '0%';
    resultMetrics.innerHTML = '<p class="empty-state">Add text or upload a document to view similarity metrics.</p>';
    return;
  }

  const { metrics, average } = computeOverallSimilarity(docA, docB);
  renderMetrics(metrics);
  updateSummary(average);
}

function setSample(kind) {
  const sample = samples[kind];
  if (kind === 'source') {
    sourceText.value = sample;
    setMetaTarget(sourceMeta, 'Source document', 'sample text');
  } else {
    targetText.value = sample;
    setMetaTarget(targetMeta, 'Comparison document', 'sample text');
  }

  if (sourceText.value && targetText.value) {
    analyzeText();
  }
}

function resetFields() {
  sourceText.value = '';
  targetText.value = '';
  verdictLabel.textContent = 'Awaiting analysis';
  verdictLabel.dataset.tone = 'neutral';
  overallScore.textContent = '0%';
  scoreBar.style.width = '0%';
  resultMetrics.innerHTML = '';
  setMetaTarget(sourceMeta, 'Source document', '');
  setMetaTarget(targetMeta, 'Comparison document', '');
  document.querySelector('[data-target="source"]').value = '';
  document.querySelector('[data-target="target"]').value = '';
}

const revealItems = document.querySelectorAll('.reveal');
const parallaxItems = document.querySelectorAll('[data-depth]');
const observer = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) {
        entry.target.classList.add('visible');
      }
    });
  },
  { threshold: 0.2 },
);

revealItems.forEach((item) => observer.observe(item));

const handleParallax = () => {
  const scrollY = window.scrollY;

  parallaxItems.forEach((item) => {
    const depth = Number(item.dataset.depth || 0);
    const rect = item.getBoundingClientRect();
    const offset = ((window.innerHeight - rect.top) / window.innerHeight) * 18 * depth;
    item.style.transform = `translate3d(0, ${Math.max(-12, Math.min(12, (scrollY * depth) / 10))}px, 0) rotate(${depth * 1.5}deg)`;
  });
};

window.addEventListener('scroll', handleParallax, { passive: true });
window.addEventListener('load', handleParallax);

document.querySelector('#compareBtn').addEventListener('click', analyzeText);
document.querySelector('#resetBtn').addEventListener('click', resetFields);
document.querySelector('[data-sample="source"]').addEventListener('click', () => setSample('source'));
document.querySelector('[data-sample="target"]').addEventListener('click', () => setSample('target'));
document.querySelector('[data-target="source"]').addEventListener('change', (event) => handleFileUpload(event, 'source'));
document.querySelector('[data-target="target"]').addEventListener('change', (event) => handleFileUpload(event, 'target'));

sourceText.value = samples.source;
targetText.value = samples.target;
setMetaTarget(sourceMeta, 'Source document', 'sample text');
setMetaTarget(targetMeta, 'Comparison document', 'sample text');
analyzeText();
