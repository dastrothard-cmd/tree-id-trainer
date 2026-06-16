const OPENAI_SPEECH_URL = "https://api.openai.com/v1/audio/speech";

function sameOrigin(req) {
  const origin = req.headers.origin;
  const host = req.headers.host;
  if (!origin || !host) return true;
  try {
    return new URL(origin).host === host;
  } catch {
    return false;
  }
}

module.exports = async function handler(req, res) {
  res.setHeader("Cache-Control", "no-store");

  if (req.method !== "POST") {
    return res.status(405).json({ error: "POST requests only." });
  }
  if (!sameOrigin(req)) {
    return res.status(403).json({ error: "Request origin not allowed." });
  }
  if (!process.env.OPENAI_API_KEY) {
    return res.status(503).json({ error: "The online voice is not connected." });
  }

  const text = String(req.body?.text || "").trim().slice(0, 320);
  if (!text) {
    return res.status(400).json({ error: "Speech text is required." });
  }

  try {
    const response = await fetch(OPENAI_SPEECH_URL, {
      method: "POST",
      headers: {
        "Authorization": `Bearer ${process.env.OPENAI_API_KEY}`,
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        model: "gpt-4o-mini-tts",
        voice: "marin",
        input: text,
        instructions:
          "Use a consistent natural Australian English accent for the entire utterance. " +
          "Do not drift into an American, Canadian, British or other accent. " +
          "Speak at a calm educational pace and pronounce botanical Latin names carefully and distinctly.",
        response_format: "mp3"
      })
    });

    if (!response.ok) {
      let message = `Online voice request failed (${response.status}).`;
      try {
        const body = await response.json();
        if (body?.error?.message) message = body.error.message;
      } catch {}
      return res.status(response.status).json({ error: message });
    }

    const audio = Buffer.from(await response.arrayBuffer());
    res.setHeader("Content-Type", "audio/mpeg");
    res.setHeader("Content-Length", String(audio.length));
    return res.status(200).send(audio);
  } catch (error) {
    return res.status(500).json({
      error: error?.message || "The online voice request failed."
    });
  }
};
