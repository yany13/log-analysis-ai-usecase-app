# Chapter 9: Introduction to the Streamlit Log Analysis Toolkit
## Using AI to Accelerate Root Cause Analysis

---

## Why We Need Better Tools

In Chapters 4-8, you learned to manually analyze logs—tracing requests, reading error messages, correlating events across components. These skills are **essential** because they build your intuition about how systems fail.

But here's the truth about production troubleshooting:

- You have **hundreds of megabytes** of logs (not single failures)
- You have **multiple services** logging independently (Tomcat, PostgreSQL, OpenLDAP)
- You have **limited time** (users are waiting, revenue is dropping)
- You have **imperfect memory** about past incidents (when was the last deadlock?)

**Manual analysis doesn't scale.**

The Streamlit Log Analysis Toolkit addresses this by combining:
1. **Your expertise** (asking the right questions)
2. **AI capabilities** (semantic understanding of logs)
3. **Knowledge management** (remembering what you've learned)
4. **Automation** (generating runbooks and alerts)

---

## The Hybrid Intelligence Model

### Traditional Approach (Manual Analysis)
```
Logs → Human reads → Hypothesis → Check → Diagnosis
↑                                        ↓
└────────────────── Takes hours ───────┘
```

### AI-Assisted Approach (Hybrid)
```
Logs ──→ AI (semantic search)      ↓ Vectors found
     ├──→ AI (retrieves KB)         ↓ Similar incidents
     ├──→ AI (web search)           ↓ External resources
     └──→ AI (summarizes findings)  ↓
                                    ↓
                    Human validates & decides
                           ↓
                       Diagnosis (minutes)
```

**The AI isn't making the decision—you are. The AI is making you smarter.**

---

## Key Innovations in the Toolkit

### 1. Semantic Search Over Keyword Search

**Keyword Search** (traditional):
```
Search for: "timeout"
Results: 47 million hits with the word "timeout"
Problem: No understanding of context or meaning
```

**Semantic Search** (AI-powered):
```
Query: "Orders taking 30+ seconds to process"
Results: Past incidents where order-processing slowness occurred
Understanding: Meaning-based, contextual relevance
```

### 2. Knowledge Base Integration

The toolkit doesn't just analyze logs—it correlates them with your accumulated knowledge:

- **Runbooks**: "How to fix connection pool exhaustion"
- **Past Incidents**: "That one time the LDAP server went down last quarter"
- **Architecture Docs**: "How the order service interacts with inventory"
- **Configuration**: "Standard pool size for this service"

When you ask "why are orders slow?", the toolkit simultaneously:
- Searches current logs
- Finds similar past incidents
- Retrieves related runbooks
- Summarizes everything

### 3. Multi-LLM Support

Different tasks need different tools:

| Task | Best Tool | Reason |
|------|-----------|--------|
| Local experimentation | **Ollama** (free, local) | No API costs, instant feedback |
| Expert analysis | **Claude/GPT-4** (powerful) | Better reasoning, more reliable |
| Cost optimization | **Local + fallback** | Use local by default, fallback to API |

The toolkit supports all of these—you pick based on your needs.

---

## System Architecture

### High-Level Components

```
┌──────────────────────────────────┐
│    Streamlit Web Interface       │
│  (Browser-based, Tabs & Sidebars)│
└─────┬──────────────────────────┬─┘
      │                          │
      ▼                          ▼
┌──────────────────┐  ┌──────────────────┐
│   Log Analysis   │  │   Knowledge Base │
│   Components     │  │   Components     │
└──────┬───────────┘  └────────┬─────────┘
       │                       │
       ▼                       ▼
┌──────────────────────────────────┐
│    ChromaDB Vector Database      │
│  (Stores embeddings locally)     │
└──────────────────┬───────────────┘
                   │
       ┌───────────┼───────────┐
       ▼           ▼           ▼
    ┌──────┐   ┌────────┐   ┌──────┐
    │Ollama│   │OpenAI  │   │HF API│
    │(Local)   │(Cloud) │   │(Cloud)
    └──────┘   └────────┘   └──────┘
```

### Data Flow for a Query

1. **User uploads logs** → Stored in ChromaDB with embeddings
2. **User describes problem** → Converted to embedding by LLM
3. **Semantic search** → Find relevant log segments, past incidents, KB docs
4. **LLM analysis** → Generate RCA, suggest next steps
5. **PDF export** → Create incident report

---

## Installation and Setup

### Prerequisites

- **Python 3.9+**
- **Pip** (Python package manager)
- **Ollama** (optional but recommended for local analysis)
- **Git**

### Step 1: Clone and Install
```bash
cd D:\workspace\ai\code\log-analysis-streamlit-ai-toolkit
pip install -r requirements.txt
```

### Step 2A: Local Setup with Ollama (Recommended)
```bash
# Install Ollama from https://ollama.ai
# Start Ollama server
ollama serve

# In another terminal: Pull models
ollama pull gemma2:9b
ollama pull llama2
ollama pull nomic-embed-text

# Run Streamlit app
streamlit run app.py
```

### Step 2B: Docker Setup (Easiest)
```bash
cd docker
docker-compose up --build

# App available at http://localhost:8501
# Ollama available at http://localhost:11434
```

### Step 3: Open in Browser
```
http://localhost:8501
```

---

## User Interface Tour

### Sidebar Configuration

The sidebar lets you configure the toolkit:

**LLM Provider**
- Ollama (local, free, instant)
- OpenAI (cloud, powerful, costs $)
- Hugging Face (cloud, diverse models)

**Model Selection**
- Default models pre-configured
- Custom models via text field
- Model switching doesn't restart app

**Temperature**
- **0.0** (deterministic): Same input → same output, good for analysis
- **0.7** (balanced): Some creativity, good for brainstorming
- **1.0** (creative): Diverse outputs, good for runbook generation

**Embedding Engine**
- Ollama: `nomic-embed-text` (small, fast)
- OpenAI: `text-embedding-3-small` (powerful, costs $)
- Hugging Face: Various options

### Main Interface Tabs

#### Tab 1: Log Analysis
- **Upload logs** (drag/drop or select)
- **Pattern search** (regex)
- **Semantic search** (free text)
- **Run hybrid RCA** (expert analysis combining everything)

#### Tab 2: Knowledge Base
- **Upload runbooks** (.md, .txt, .pdf)
- **Auto-indexing** (instant, no manual categorization)
- **View indexed documents**
- **Update/delete entries**

#### Tab 3: Maintenance
- **View inventory** (all indexed logs and KB)
- **Delete specific entries**
- **Clear entire databases**
- **Export for backup**

---

## Working With Logs: Example Workflow

### Step 1: Upload Your ByteBite Logs

Start with fresh failure scenarios:

```bash
# In another terminal, trigger a failure
curl "http://localhost:8080/bytebite/api/menu?failureType=db_leak" &
for i in {1..15}; do
  curl "http://localhost:8080/bytebite/api/menu?failureType=db_leak" & 
done

# Wait a bit then capture logs
docker logs bytebite-tomcat > bytebite_failure.log 2>&1
```

In Streamlit toolkit:
1. Go to **Log Analysis** tab
2. Click **"Upload logs"**
3. Select `bytebite_failure.log`
4. Wait for ingestion (shows progress)

### Step 2: Explore with Pattern Search

Search for specific keywords:
- Pattern: `Connection`
- Result: 47 matches showing connection-related log lines

Search for specific errors:
- Pattern: `timeout|ERROR|FAIL`
- Result: Organized by match type

### Step 3: Run Semantic Search

Instead of keyword matching, ask natural questions:

**Query**: "Why are requests timing out?"
- AI analyzes logs semantically
- Finds: Connection pool exhaustion symptoms
- Finds: HikariCP timeout messages
- Suggests: Check connection management

### Step 4: Run Hybrid RCA

Provide context and ask for analysis:

```
Incident: Orders started timing out around 3:45 PM.
The system was fine until then.
What happened?
```

The toolkit will:
1. Search logs for that time window
2. Find relevant events
3. Check knowledge base for similar incidents
4. Search web for relevant patterns
5. Synthesize into root cause analysis

---

## Building Your Knowledge Base

### What to Include

Start with:
1. **Runbooks** from your organization
2. **Architecture documentation** (how services interact)
3. **Past incident reports** (what went wrong and how you fixed it)
4. **Configuration reference** (standard settings, limits)
5. **Troubleshooting guides** (common issues and solutions)

### Example: Adding a Runbook

**File**: `db-connection-pool-exhaustion.md`
```markdown
# DB Connection Pool Exhaustion Runbook

## Symptoms
- Requests timing out with "Connection not available"
- HikariCP logs show available connections = 0
- Response times suddenly increase

## Root Causes
1. Connection leak in code (forgot to close)
2. Long-running queries holding connections
3. Pool size too small for current load

## Diagnosis
1. Check HikariCP logs for available connection count
2. Find which endpoint is affected
3. Check code for try-with-resources

## Remediation
1. **Immediate**: Restart service (clears leaked connections)
2. **Short-term**: Increase pool size
3. **Long-term**: Fix the leak in code
```

Upload to **Knowledge Base** tab:
1. Click **"Upload Runbooks"**
2. Select file
3. Auto-indexed (no categorization needed)

Now when you ask about connection problems, the toolkit retrieves this runbook.

---

## LLM Provider Comparison

| Feature | Ollama | OpenAI | Hugging Face |
|---------|--------|--------|--------------|
| **Cost** | Free | $0.002/1K tokens | $0-varied |
| **Speed** | Local (fast) | Cloud (slower) | Cloud (slower) |
| **Setup** | Download app | API key | API key |
| **Best for** | Learning, local | Production, reliability | Experimentation |
| **Models** | Llama, Mistral, etc | GPT-3.5, GPT-4 | 1000s of models |

### Recommendation for This Book
**Use Ollama** for learning because:
- No API costs (important for lots of experimentation)
- Instant results (no waiting for cloud)
- Privacy (logs stay local)
- Perfect for practicing diagnostics

Switch to OpenAI for production incidents if:
- You need the most reliable analysis
- Cost is not a concern
- You want advanced features like function calling

---

## Next Steps

Now you're ready to:

1. **Chapter 10**: Deep dive into embeddings and semantic search
2. **Chapter 11**: Master the LLM prompting for expert analysis
3. **Chapter 12**: Build a comprehensive knowledge base
4. **Chapter 13**: End-to-end workflow combining ByteBite + Toolkit

---

## Key Insights

1. **AI augments, doesn't replace** - Your expertise makes AI better, not the reverse
2. **Local > Cloud for learning** - Ollama is your best friend while practicing
3. **Knowledge base is everything** - The more context you provide, the better the analysis
4. **Hybrid = the future** - Humans asking questions + AI analyzing data = fast diagnosis

---

**Next Chapter**: Chapter 10 – Vector Embeddings and Semantic Search
