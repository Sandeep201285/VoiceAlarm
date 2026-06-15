const express = require('express');
const admin = require('firebase-admin');
require('dotenv').config();

const app = express();
app.use(express.json());

// Initialize Firebase Admin SDK
// Reads credentials from GCP environment naturally when deployed on Cloud Run,
// otherwise falls back to local Service Account file configured via environment variable.
if (process.env.FIREBASE_CONFIG) {
  admin.initializeApp();
} else {
  try {
    admin.initializeApp({
      credential: admin.credential.applicationDefault()
    });
  } catch (error) {
    console.log("Running in offline mock dev mode because no Firebase credentials were provided.");
  }
}

const db = admin.apps.length > 0 ? admin.firestore() : null;
const messaging = admin.apps.length > 0 ? admin.messaging() : null;

// ─── ENDPOINT: Register User ─────────────────────────────────────────
app.post('/users/register', async (req, res) => {
  const { uid, name, phone, fcmToken } = req.body;
  if (!uid || !name || !phone) {
    return res.status(400).json({ error: "Missing required registration parameters." });
  }

  try {
    if (db) {
      await db.collection('users').doc(uid).set({
        name,
        phone,
        fcmToken,
        updatedAt: admin.firestore.FieldValue.serverTimestamp()
      }, { merge: true });
    }
    console.log(`User registered: ${name} (${phone})`);
    res.status(200).json({ message: "User registered successfully." });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Failed to register user to Firestore." });
  }
});

// ─── ENDPOINT: Create Family Group ───────────────────────────────────
app.post('/groups', async (req, res) => {
  const { id, name, ownerId } = req.body;
  if (!id || !name || !ownerId) {
    return res.status(400).json({ error: "Missing required parameters to create group." });
  }

  try {
    if (db) {
      await db.collection('groups').doc(id).set({
        name,
        ownerId,
        createdAt: admin.firestore.FieldValue.serverTimestamp()
      });
      // Add owner as admin
      await db.collection('groups').doc(id).collection('members').doc(ownerId).set({
        role: "OWNER",
        status: "ACCEPTED"
      });
    }
    console.log(`Group created: ${name} by ${ownerId}`);
    res.status(200).json({ id, name, ownerId });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Failed to create group." });
  }
});

// ─── ENDPOINT: List Groups for User ──────────────────────────────────
app.get('/groups', async (req, res) => {
  const { userId } = req.query;
  if (!userId) {
    return res.status(400).json({ error: "Missing userId." });
  }

  try {
    const groups = [];
    if (db) {
      // In production, query groups where user is a member
      const memberSnapshot = await db.collectionGroup('members').where(admin.firestore.FieldPath.documentId(), '==', userId).get();
      for (const doc of memberSnapshot.docs) {
        const groupRef = doc.ref.parent.parent;
        const groupDoc = await groupRef.get();
        if (groupDoc.exists) {
          groups.push({ id: groupDoc.id, ...groupDoc.data() });
        }
      }
    }
    res.status(200).json(groups);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Failed to retrieve groups." });
  }
});

// ─── ENDPOINT: Send Invitation (SMS or FCM push) ─────────────────────
app.post('/groups/invite', async (req, res) => {
  const { groupId, phone, name } = req.body;
  if (!groupId || !phone || !name) {
    return res.status(400).json({ error: "Missing invite details." });
  }

  try {
    if (db) {
      // Create pending member record
      const tempId = `temp_${Date.now()}`;
      await db.collection('groups').doc(groupId).collection('members').doc(tempId).set({
        name,
        phone,
        role: "MEMBER",
        status: "PENDING"
      });

      // Find user token by phone to send push notification
      const userQuery = await db.collection('users').where('phone', '==', phone).limit(1).get();
      if (!userQuery.empty && messaging) {
        const targetUser = userQuery.docs[0];
        const fcmToken = targetUser.data().fcmToken;
        if (fcmToken) {
          await messaging.send({
            token: fcmToken,
            notification: {
              title: "Care Group Invitation",
              body: `You are invited to join the care group to receive voice alerts.`
            },
            data: {
              groupId,
              action: "INVITATION_PENDING"
            }
          });
        }
      }
    }
    console.log(`Invite dispatched to phone: ${phone}`);
    res.status(200).json({ status: "SENT" });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Failed to deliver invite." });
  }
});

// ─── ENDPOINT: Synchronize Alarms & Reminders ─────────────────────────
app.post('/reminders/sync', async (req, res) => {
  const { userId } = req.query;
  const localReminders = req.body; // list of ReminderDto
  if (!userId) {
    return res.status(400).json({ error: "Missing userId query parameter." });
  }

  try {
    const syncedReminders = [];
    if (db) {
      const batch = db.batch();
      for (const rem of localReminders) {
        const docRef = db.collection('reminders').doc(String(rem.id));
        batch.set(docRef, {
          ...rem,
          syncTime: admin.firestore.FieldValue.serverTimestamp()
        }, { merge: true });
        
        syncedReminders.push(rem);

        // If reminder is for a remote member, trigger FCM push immediately
        if (rem.recipientId && rem.recipientId !== userId && messaging) {
          const recipientDoc = await db.collection('users').doc(rem.recipientId).get();
          if (recipientDoc.exists) {
            const fcmToken = recipientDoc.data().fcmToken;
            if (fcmToken) {
              await messaging.send({
                token: fcmToken,
                data: {
                  reminderId: String(rem.id),
                  title: rem.title,
                  message: rem.message,
                  triggerTime: String(rem.triggerTime),
                  action: "NEW_REMINDER"
                }
              });
            }
          }
        }
      }
      await batch.commit();
    } else {
      // Mock echo for local dev
      syncedReminders.push(...localReminders);
    }
    res.status(200).json(syncedReminders);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Synchronization pipeline failure." });
  }
});

// ─── ENDPOINT: Acknowledge alarm trigger ──────────────────────────────
app.post('/reminders/:id/acknowledge', async (req, res) => {
  const reminderId = req.params.id;
  const { acknowledgedAt } = req.query;

  try {
    if (db) {
      await db.collection('reminders').doc(reminderId).update({
        status: "ACKNOWLEDGED",
        acknowledgedAt: parseInt(acknowledgedAt) || Date.now()
      });
    }
    console.log(`Alarm acknowledged: ${reminderId}`);
    res.status(200).json({ message: "Acknowledged." });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: "Failed to acknowledge." });
  }
});

// Launch server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
  console.log(`Voice Alarm Production Backend active on port ${PORT}`);
});
