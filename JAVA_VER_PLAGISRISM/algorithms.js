export function normalizeText(input = "") {
  return input
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

export function tokenize(input = "") {
  const normalized = normalizeText(input);
  return normalized ? normalized.split(" ").filter(Boolean) : [];
}

export function createFrequencyMap(tokens = []) {
  const map = new Map();

  tokens.forEach((token) => {
    map.set(token, (map.get(token) || 0) + 1);
  });

  return map;
}

export function hashWord(word = "") {
  let hash = 0;
  for (const letter of word) {
    hash = (hash * 31 + letter.charCodeAt(0)) >>> 0;
  }
  return hash;
}

export function rollingHashWindows(tokens = [], windowSize = 3) {
  if (!tokens.length || windowSize <= 0) return [];

  const values = tokens.map((token) => hashWord(token));
  const base = 911382629;
  const mod = 1_000_000_007;

  if (values.length < windowSize) {
    return [values.reduce((accumulator, value) => (accumulator * base + value) % mod, 0)];
  }

  let hashValue = 0;
  for (let index = 0; index < windowSize; index += 1) {
    hashValue = (hashValue * base + values[index]) % mod;
  }

  const hashes = [hashValue];
  const power = Math.pow(base, windowSize - 1) % mod;

  for (let index = windowSize; index < values.length; index += 1) {
    const outgoing = values[index - windowSize];
    const incoming = values[index];
    hashValue = ((hashValue - outgoing * power) % mod + mod) % mod;
    hashValue = (hashValue * base + incoming) % mod;
    hashes.push(hashValue);
  }

  return hashes;
}

export function rabinKarpSimilarity(textA = "", textB = "") {
  const tokensA = tokenize(textA);
  const tokensB = tokenize(textB);

  if (!tokensA.length && !tokensB.length) return 1;
  if (!tokensA.length || !tokensB.length) return 0;

  const windowSize = Math.min(3, Math.max(1, Math.min(tokensA.length, tokensB.length)));
  const hashesA = rollingHashWindows(tokensA, windowSize);
  const hashesB = rollingHashWindows(tokensB, windowSize);

  if (!hashesA.length || !hashesB.length) return 0;

  const setA = new Set(hashesA);
  const setB = new Set(hashesB);
  const intersection = [...setA].filter((value) => setB.has(value)).length;
  const union = new Set([...setA, ...setB]).size;

  if (!union) return 0;
  return intersection / union;
}

export function jaccardSimilarity(textA = "", textB = "") {
  const setA = new Set(tokenize(textA));
  const setB = new Set(tokenize(textB));

  if (!setA.size && !setB.size) return 1;
  if (!setA.size || !setB.size) return 0;

  const intersection = [...setA].filter((word) => setB.has(word)).length;
  const union = new Set([...setA, ...setB]).size;

  return intersection / union;
}

export function diceCoefficient(textA = "", textB = "") {
  const tokensA = tokenize(textA);
  const tokensB = tokenize(textB);

  if (!tokensA.length && !tokensB.length) return 1;
  if (!tokensA.length || !tokensB.length) return 0;

  const setA = new Set(tokensA);
  const setB = new Set(tokensB);
  const intersection = [...setA].filter((token) => setB.has(token)).length;

  return (2 * intersection) / (setA.size + setB.size);
}

export function cosineSimilarity(textA = "", textB = "") {
  const tokensA = tokenize(textA);
  const tokensB = tokenize(textB);

  if (!tokensA.length && !tokensB.length) return 1;
  if (!tokensA.length || !tokensB.length) return 0;

  const freqA = createFrequencyMap(tokensA);
  const freqB = createFrequencyMap(tokensB);

  const terms = new Set([...freqA.keys(), ...freqB.keys()]);
  let dotProduct = 0;
  let magnitudeA = 0;
  let magnitudeB = 0;

  terms.forEach((term) => {
    const a = freqA.get(term) || 0;
    const b = freqB.get(term) || 0;
    dotProduct += a * b;
    magnitudeA += a * a;
    magnitudeB += b * b;
  });

  if (!magnitudeA || !magnitudeB) return 0;
  return dotProduct / (Math.sqrt(magnitudeA) * Math.sqrt(magnitudeB));
}

export function levenshteinDistance(textA = "", textB = "") {
  const source = normalizeText(textA);
  const target = normalizeText(textB);

  if (!source.length && !target.length) return 0;
  if (!source.length) return target.length;
  if (!target.length) return source.length;

  const matrix = Array.from({ length: source.length + 1 }, () => Array(target.length + 1).fill(0));

  for (let i = 0; i <= source.length; i += 1) matrix[i][0] = i;
  for (let j = 0; j <= target.length; j += 1) matrix[0][j] = j;

  for (let i = 1; i <= source.length; i += 1) {
    for (let j = 1; j <= target.length; j += 1) {
      const substitutionCost = source[i - 1] === target[j - 1] ? 0 : 1;
      matrix[i][j] = Math.min(
        matrix[i - 1][j] + 1,
        matrix[i][j - 1] + 1,
        matrix[i - 1][j - 1] + substitutionCost,
      );
    }
  }

  return matrix[source.length][target.length];
}

export function levenshteinSimilarity(textA = "", textB = "") {
  const source = normalizeText(textA);
  const target = normalizeText(textB);
  const maxLength = Math.max(source.length, target.length, 1);
  const distance = levenshteinDistance(source, target);
  return 1 - distance / maxLength;
}

export function longestCommonSubsequenceSimilarity(textA = "", textB = "") {
  const source = normalizeText(textA).split(" ");
  const target = normalizeText(textB).split(" ");

  if (!source.length && !target.length) return 1;
  if (!source.length || !target.length) return 0;

  const matrix = Array.from({ length: source.length + 1 }, () => Array(target.length + 1).fill(0));

  for (let i = 1; i <= source.length; i += 1) {
    for (let j = 1; j <= target.length; j += 1) {
      if (source[i - 1] === target[j - 1]) {
        matrix[i][j] = matrix[i - 1][j - 1] + 1;
      } else {
        matrix[i][j] = Math.max(matrix[i - 1][j], matrix[i][j - 1]);
      }
    }
  }

  const lcsLength = matrix[source.length][target.length];
  const totalWords = Math.max(source.length, target.length, 1);

  return lcsLength / totalWords;
}

export function computeOverallSimilarity(textA = "", textB = "") {
  const metrics = {
    jaccard: jaccardSimilarity(textA, textB),
    cosine: cosineSimilarity(textA, textB),
    dice: diceCoefficient(textA, textB),
    levenshtein: levenshteinSimilarity(textA, textB),
    lcs: longestCommonSubsequenceSimilarity(textA, textB),
    rabinKarp: rabinKarpSimilarity(textA, textB),
  };

  const average = Object.values(metrics).reduce((sum, value) => sum + value, 0) / Object.values(metrics).length;

  return {
    metrics,
    average,
  };
}

export function getVerdict(score = 0) {
  if (score >= 0.8) return { label: "High similarity risk", tone: "danger" };
  if (score >= 0.5) return { label: "Moderate similarity risk", tone: "warning" };
  if (score >= 0.2) return { label: "Low similarity risk", tone: "neutral" };
  return { label: "Distinct content", tone: "safe" };
}
