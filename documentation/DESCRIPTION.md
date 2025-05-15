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

### 5. Settings Page

**Description:**  

**Requirements:** 

### 6. Home Dashboard

**Description:**  

**Requirements:** 

### 7. User Notifications

**Description:**  

**Requirements:** 

### 8. Energy Monitor Charts

**Description:**  

**Requirements:** 

### 9. Monthly Predictions

**Description:**  

**Requirements:** 

### 10. Energy Analysis

**Description:**  

**Requirements:** 

### 11. User Household Comparison

**Description:**  

**Requirements:** 
