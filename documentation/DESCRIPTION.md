# Project Requirements Documentation

## Features and Requirements

### 1. Registration Page

**Description:**
Allows users to create a new account, verify their email, enroll in two-factor authentication (2FA), and review the Privacy Policy and Terms of Use.

**Requirements:**
- Page has fields where users enter optional personal information, a valid email address, a formatted phone number, and a secure password that meets stringent criteria.
- System sends a verification email upon registration. Users must verify this email before proceeding.
- Prompts to enroll in 2FA using a phone number is provided to the user.  
- Page requires agreement to Privacy Policy (PP) and Terms of Use (TOU) before completing registration.

### 2. Login Page

**Description:**
Enables users to access their account using Google Sign-In or a previously registered email/password combination with 2FA support.

**Requirements:**
- Allows login via Google account sign-in.  
- Allows login with registered email and password.  
- Prompt for 2FA code after successful credential authentication if the user has a registered account.  
- Redirects to the Settings page upon successful login and authentication. 

### 3. Settings Page

**Description:**
Allows users to connect their energy data to the application. The data can be demo data provided by UtilityAPI. Or, the data can be uploaded from PG&E in zipped file format.   

**Requirements:** 
- Displays a button to connect sample energy data via UtilityAPI.  
- Displays a button to connect personalized energy data using a zip file of PG&E data. 
- Redirects to the Home Dashboard page upon successful integration of energy data.

### 4. Home Dashboard

**Description:**
Displays a textual and visual summary of the user's energy consumption, monthly trends, cost insights, and tips.

**Requirements:** 
- Shows the difference in energy usage and cost between the last two months.  
- Displays a reminder to check monthly bills, the trend of the average monthly cost over six months, and a helpful suggestion to reduce energy use.
- Shows a pie chart visualizing current usage in relation to the user's monthly energy budget (percentage spent along with the percentage and dollar amount remaining).

### 5. User Notifications

**Description:**
Delivers important, real-time updates and insights to users through both system-level Android notifications (visible outside of the application) and in-app snackbar banners.

**Requirements:**
- Displays notifications of real-time reminders, trends, and suggestions based on the latest user data outside the app using the Android notification tray.  
- Displays in-app banners for immediate, contextual insights that reflect recent user data.  
- Ensures relevant notifications are shown in the Android notification panel even after the app has been closed.  

### 6. Navigation Drawer

**Description:**
Provides users with quick access to various sections of the application after logging in.

**Requirements:**
- Displays the navigation drawer when the user taps the hamburger icon on the top-left corner.  
- Includes links to the following sections:  
  - Profile  
  - Settings  
  - Energy Monitor  
  - Home Dashboard  
  - Energy Analysis  
  - Household Comparison  
  - Logout  
- Ensures each item redirects the user to the corresponding page. 

### 7. Profile Page

**Description:**
Displays the user's personal information and provides options to update details, manage their account, and upload a profile photo.

**Requirements:**
- Displays user's name, email, and monthly energy budget.  
- Allows users to upload and save a profile photo.  
- Allows users to permanently delete their account by pressing on the appropriate button.   
- Allows users to edit and save changes to the fields with their personal information after pressing the correct button.

### 8. Energy Monitor Charts

**Description:**
Displays interactive bar charts that help users analyze their historical energy consumption or cost. The charts are organized by monthly billing periods over the past year.

**Requirements:**
- Shows a bar chart visualizing historical data segmented by billing month.  
- Allows users to toggle between energy usage and cost views.  
- Enables users to page through newer or older datasets (from the most recent six months to the six months prior) using pagination controls. 
- Shows helpful metrics below the charts, such as average consumption and expenditure across each six-month period.

### 9. Monthly Predictions

**Description:**
Generates forecasted monthly energy usage or cost using predictive analytics.

**Requirements:**
- Analyzes past energy usage and cost trends using linear regression.
- Displays the forecasted monthly value beneath the Energy Monitor chart.  
- Dynamically updates the prediction based on the selected view (cost or kWh).  

### 10. Energy Analysis

**Description:**
Provides users with AI-generated insights and breakdowns of their energy data.

**Requirements:**
- Allows users to select a billing period from a dropdown menu.
- Generates and displays analytical content based on user data, including:  
  - General summary  
  - Cost breakdown  
  - Usage patterns  
  - Rate analysis  
  - Efficiency tips  
- Allows users to compare two different billing periods using a comparison option. 

### 11. User Household Comparison

**Description:**
Generates an analytical comparison of the user's energy consumption relative to similar households.

**Requirements:**
- Allows users to input their household profile using the following fields: number of bedrooms, number of occupants, and zip code.
- Displays a textual comparison that describes whether or not the energy usage of the user's household for the current month is higher or lower relative to the average energy use of comparable households. 
- Shows a bar chart that compares the user's current monthly usage against the average usage of peer households.
