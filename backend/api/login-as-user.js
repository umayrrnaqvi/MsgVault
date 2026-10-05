const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert({
      project_id: process.env.FIREBASE_PROJECT_ID,
      client_email: process.env.FIREBASE_CLIENT_EMAIL,
      private_key: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, "\n"),
    }),
  });
}

const ADMIN_EMAIL = "umairnaqvi902@gmail.com";

module.exports = async (req, res) => {
  if (req.method !== "POST") {
    return res.status(405).json({ error: "Method not allowed" });
  }

  const authHeader = req.headers.authorization;
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return res.status(401).json({ error: "Unauthorized" });
  }

  try {
    const token = authHeader.split("Bearer ")[1];
    const decoded = await admin.auth().verifyIdToken(token);

    if (decoded.email !== ADMIN_EMAIL) {
      return res.status(403).json({ error: "Admin access only" });
    }

    const { targetUid } = req.body;
    if (!targetUid) {
      return res.status(400).json({ error: "targetUid is required" });
    }

    const customToken = await admin.auth().createCustomToken(targetUid);
    const userRecord = await admin.auth().getUser(targetUid);

    return res.status(200).json({
      token: customToken,
      email: userRecord.email || "Unknown",
    });
  } catch (error) {
    return res.status(500).json({ error: error.message });
  }
};
