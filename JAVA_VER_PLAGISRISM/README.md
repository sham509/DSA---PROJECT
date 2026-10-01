# Plagiarism Detection Studio

A polished, interactive plagiarism detection prototype inspired by premium award-show landing pages. It compares two text samples using multiple similarity algorithms and displays a clear risk verdict.

## Included algorithms

- Jaccard similarity
- Cosine similarity
- Dice coefficient
- Levenshtein similarity
- Longest common subsequence similarity

## Run locally

Open the folder in a browser or serve it locally:

```bash
cd "c:\KNOX\KLH\2nd Year\TRIMESTER-1\DSA-III\PROJECT FILES\Plagiarism_Dectection"
python -m http.server 8000
```

Then visit:

```text
http://localhost:8000
```

## Files

- `index.html` – page structure
- `styles.css` – styling and motion
- `algorithms.js` – plagiarism detection logic
- `script.js` – interactive UI and result rendering
