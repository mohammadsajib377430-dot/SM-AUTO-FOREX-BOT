# SM-AUTO-FOREX-BOT

A mobile-friendly dashboard for analysing one-minute Forex candles and displaying BUY / SELL / WAIT technical signals.

## Included
- `index.html`: browser-based signal dashboard.
- Pairs: EUR/USD, GBP/USD, USD/JPY and XAU/USD.
- Indicators: EMA 9/21, RSI 14 and MACD.
- Market candles are requested from Twelve Data when the user supplies their own API key.

## Run
1. Open the repository's `index.html` file, or enable GitHub Pages in **Settings → Pages** using the `main` branch and `/(root)`.
2. Open the published page.
3. Enter your own Twelve Data API key in the password field and select a pair.
4. Press **Fetch market data & analyse**.

## API key safety
The key is entered by the user in the browser and is not saved by this page. Do not paste your key into GitHub files, screenshots, or public messages. Browser requests send the key to Twelve Data's API endpoint.

## Important
This is a technical-analysis signal tool, not a guaranteed prediction or financial advice. One-minute trading is very risky. Data access, delays and limits depend on the user's Twelve Data plan. The app does not connect to a broker or execute trades. Stop-loss and take-profit values are illustrative estimates, not personalized risk management.
