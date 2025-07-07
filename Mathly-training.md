# Mathly Training Documentation

> **Mathly** is a modular math AI assistant screen, built in Kotlin for Android, that provides voice-driven math help. This guide covers how to train the AI model powering Mathly, what data to use, how to collect and prepare it, and how to integrate the trained model or API with your Kotlin app.

## 1. Overview

- **Mathly is a screen, not a standalone app.**
- Training is done outside the app (using Python and open datasets), then the model or API is integrated into your Kotlin code.
- Mathly should solve:
    - Regular calculations (arithmetic, percentages, etc.)
    - Unit-based calculations (BMI, age, area, conversions)
    - Some advanced math (roots, exponents, simple algebra)


## 2. Data Collection

### A. Use Large, High-Quality Datasets

| Dataset | Description | Download Link |
| :-- | :-- | :-- |
| Big-Math | 250k+ filtered math Q\&A, all levels | [Big-Math RL-Verified](https://huggingface.co/datasets/SynthLabsAI/Big-Math-RL-Verified) |
| MATH | 12k+ step-by-step competition problems | [MATH GitHub](https://github.com/hendrycks/math) |
| DeepMind Math | School-level arithmetic and word problems | [DeepMind Math](https://huggingface.co/datasets/deepmind/math_dataset) |

**Tip:** For BMI, age, area, and conversions, generate synthetic data (see below).

### B. Generate Synthetic Data

For calculators (BMI, age, area, etc.), create thousands of Q\&A pairs programmatically:

```python
# Example: Generate BMI questions
import random

def generate_bmi_data(n=10000):
    data = []
    for _ in range(n):
        weight = random.randint(40, 120)
        height = round(random.uniform(1.4, 2.0), 2)
        bmi = round(weight / (height * height), 2)
        q = f"What is the BMI for {weight}kg and {height}m?"
        data.append({"problem": q, "answer": str(bmi)})
    return data
```


## 3. Data Preparation

- **Format:** All data should be in a simple JSON or CSV format:

```json
{
  "problem": "What is 15 plus 7?",
  "answer": "22"
}
```

- **Clean:** Remove duplicates, fix errors, ensure units and answers are correct.
- **Combine:** Merge open datasets and your synthetic data into one large file.


## 4. Model Training

### A. Choose a Model

- For cloud/API: Use OpenAI’s GPT-3.5/4, Gemini, or a fine-tuned open-source LLM (Llama, Mistral, etc.).
- For on-device: Use a small model (DistilBERT, TinyLlama, or similar) converted to ONNX or TensorFlow Lite.


### B. Fine-Tune (Python Example)

1. **Install dependencies:**

```bash
pip install datasets transformers
```

2. **Load data and model:**

```python
from datasets import load_dataset, Dataset
from transformers import AutoModelForCausalLM, AutoTokenizer, Trainer, TrainingArguments

# Load or create your dataset
dataset = Dataset.from_json("combined_mathly_data.json")

# Choose a model (GPT-2 for demo; use Llama/Mistral for better results)
model_name = "gpt2"
tokenizer = AutoTokenizer.from_pretrained(model_name)
model = AutoModelForCausalLM.from_pretrained(model_name)

def preprocess(example):
    prompt = example["problem"]
    target = example["answer"]
    return tokenizer(f"Q: {prompt}\nA: {target}", truncation=True)

tokenized = dataset.map(preprocess)
```

3. **Train:**

```python
training_args = TrainingArguments(
    output_dir="./mathly-model",
    per_device_train_batch_size=4,
    num_train_epochs=2,
    logging_steps=100,
    save_steps=500
)
trainer = Trainer(
    model=model,
    args=training_args,
    train_dataset=tokenized,
)
trainer.train()
```

4. **Export:**
    - For cloud: Deploy to an API endpoint.
    - For on-device: Convert to ONNX or TFLite (see [TensorFlow Lite Guide](https://www.tensorflow.org/lite/convert)).

## 5. Integration with Kotlin (App Side)

- **Cloud API:**
    - Call your deployed model or OpenAI/Gemini using Retrofit or HttpURLConnection.
    - Example (using OpenAI Assistants API)[^1]:

```kotlin
// Pseudocode for making an API call to your math assistant
val request = JSONObject()
request.put("question", userQuestion)
// Use Retrofit/OkHttp to POST this to your API and get the answer
```

- **On-device:**
    - Load the ONNX/TFLite model using [ONNX Runtime](https://onnxruntime.ai/docs/build/android/) or [TensorFlow Lite](https://www.tensorflow.org/lite/guide/android).
    - Pass user’s math question as input, get answer as output.


## 6. User-Driven Learning

- **Log user math queries and corrections** (with consent) in local storage (Room/SQLite).
- **Periodically retrain** your model with anonymized, validated user data for continuous improvement.
- **Only store math-related queries** (filter using regex or simple classifiers).


## 7. Prompt Engineering (API/LLM)

- Use clear, math-focused prompts for best results.
- Example prompt for OpenAI API[^1]:

```
You are a personal math tutor. Answer questions briefly, in a sentence or less.
```

- For calculators, add instructions:

```
If the question is about BMI, age, area, or unit conversion, compute and return only the numeric result.
```


## 8. Monitoring \& Continuous Improvement

- **Monitor queries and answers** to identify gaps or errors.
- **Export logs** (CSV/JSON) for analysis and future training[^6].
- **Iterate**: Add more data and retrain as needed.


## 9. Summary Table

| Step | What to Do | Tools/Libraries |
| :-- | :-- | :-- |
| Collect Data | Download open datasets, generate synthetic Q\&A | HuggingFace, Python |
| Prepare Data | Clean, merge, format as JSON/CSV | Python, pandas |
| Train Model | Fine-tune LLM on math data | transformers, datasets |
| Export Model | For API or on-device use | ONNX, TFLite, Flask, FastAPI |
| Integrate in App | Retrofit/OkHttp for API, or ONNX/TFLite for on-device | Kotlin, Android |
| User Learning | Log queries, retrain with new math data | Room, SQLite, Python |

## 10. References \& Further Reading

- [OpenAI Assistants API Overview][^1]
- [Big-Math RL-Verified Dataset](https://huggingface.co/datasets/SynthLabsAI/Big-Math-RL-Verified)
- [MATH Dataset GitHub](https://github.com/hendrycks/math)
- [DeepMind Math Dataset](https://huggingface.co/datasets/deepmind/math_dataset)
- [ONNX Runtime for Android](https://onnxruntime.ai/docs/build/android/)
- [TensorFlow Lite for Android](https://www.tensorflow.org/lite/guide/android)

**In summary:**
Train Mathly’s backend using large, open math datasets plus your own synthetic calculator data. Fine-tune a language model (or use OpenAI/Gemini), then connect it to your Kotlin screen via API or on-device inference. Collect user math queries for continual improvement, always respecting privacy.

---
[^1]: https://cookbook.openai.com/examples/assistants_api_overview_python

<div style="text-align: center">⁂</div>

[^1]: https://cookbook.openai.com/examples/assistants_api_overview_python

[^2]: https://document360.com/blog/eddy-ai-assistant-prompt-templates/

[^3]: https://docs.praison.ai/docs/features/mathagent

[^4]: https://www.lindy.ai/templates/ai-math-assistant

[^5]: https://idratherbewriting.com/ai/prompt-engineering-populating-documentation-templates.html

[^6]: https://www.mintlify.com/docs/guides/assistant

[^7]: https://www.goodnotes.com/blog/how-its-made-ai-math-assistance

[^8]: https://texta.ai/ai-writing-assistant/other/mathematician

[^9]: https://ckeditor.com/docs/ckeditor5/latest/features/ai-assistant/ai-assistant-overview.html

[^10]: https://math-gpt.ai

