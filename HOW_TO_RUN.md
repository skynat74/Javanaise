# How to run JAVANAISE

You need **3 programs running at the same time** (in parallel):

1. Coordinator  
2. IRC client 1  
3. IRC client 2  

Do not stop one when you start the next.

---

## Before the first time

1. **Build → Build Project**
2. On the **Irc** run config: **Allow multiple instances**  
   (Run → Edit Configurations → Irc → Modify options → Allow multiple instances)  
   so you can start IRC twice.

---

## Step 1 — Coordinator

1. Open `SOURCES/src/jvn/JvnCoordImpl.java`
2. Click the green ▶ next to `main`
3. Wait until the console shows:  
   `Coordinator ready as JvnCoord on port 1099`
4. **Leave it running**

---

## Step 2 — IRC 1

1. Open `SOURCES/src/irc/Irc.java`
2. Click the green ▶ next to `main`
3. A chat window opens
4. Type a message → click **write**
5. **Leave it running**

---

## Step 3 — IRC 2 (same time as IRC 1)

1. Stay in `Irc.java` (or open it again)
2. Click the green ▶ next to `main` **again**
3. A **second** chat window opens
4. Click **read** → you should see IRC 1’s message
5. Type something else → **write**
6. In IRC 1 → **read** → you should see the new message

---

## Checklist

| Running? | Process |
|----------|---------|
| ☐ | Coordinator (green ▶ on `JvnCoordImpl`) |
| ☐ | IRC 1 (green ▶ on `Irc`) |
| ☐ | IRC 2 (green ▶ on `Irc` again) |

All three = **parallel**.

---

## If the second IRC won’t start

Run → Edit Configurations → select **Irc** → enable **Allow multiple instances**.
