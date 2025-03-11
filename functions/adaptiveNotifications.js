import { defineFunction } from "@genkit-ai/core";
import { generateText } from "@genkit-ai/gemini";
import admin from "firebase-admin";

admin.initializeApp();

export const adaptiveNotification = defineFunction({
  name: "adaptiveNotification",
  args: { userId: "string", usageData: "array" },
  handler: async ({ userId, usageData }) => {
    let prompt = `Given the energy usage data: ${usageData.join(", ")} kWh, when is the best time to use appliances?`;
    let bestTime = await generateText(prompt);

    const message = {
      notification: {
        title: "Energy Tip",
        body: `Optimal time to use appliances: ${bestTime}`
      },
      token: userId
    };

    await admin.messaging().send(message);
    return `Notification sent: ${bestTime}`;
  }
});
