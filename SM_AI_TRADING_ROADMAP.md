# SM AI TRADING BOT — implementation roadmap

## Existing working baseline
The existing GitHub Pages dashboard remains in `index.html`. It uses Twelve Data 1-minute candles for EUR/USD, GBP/USD, USD/JPY and XAU/USD, with EMA 9/21, RSI 14 and MACD rule-based BUY/SELL/WAIT signals. The API key is entered by the user in the browser session and must not be committed to the repository.

## Target architecture
- Android app: foreground MediaProjection capture with explicit Android system consent.
- Market-data adapter: Twelve Data for Forex symbols; credentials entered locally and never committed.
- Candle vision: on-device chart-region detection and candle/OHLC extraction, calibrated for supported Quotex and chart layouts.
- Fusion engine: compare vision-derived direction with live-data indicators. If sources disagree, the chart is unreadable, timestamps are stale, or confidence is insufficient, return WAIT.
- Signal UI: Bengali BUY / SELL / WAIT, asset, timeframe, candle timestamp, reasons, and alert.
- Evaluation: paper-trade signal log with next-candle outcome tracking; no guaranteed win-rate claims.

## Current Android starter status
The `android/` folder contains an initial Android app scaffold and a MediaProjection foreground service. It requests Android's explicit screen-sharing permission and creates a local frame-capture surface. It does **not yet** recognize candlesticks, call Twelve Data, or generate fused signals. It is source code, not an installable APK.

## Safety and scope
This project is signal-only. It does not log in to Quotex, scrape private account data, click trade buttons, or place orders. Never put API keys in source code or public commits. Test signals on demo/paper data before considering any live decision.
