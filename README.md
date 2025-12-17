
# Vesicle

Experiments with per-grain processes of microsound

```javascript

/* Install Vesicle */
Quarks.install("https://github.com/Sonology/Vesicle")

// boot the sound server
s.boot

// path to sound material
p = Vesicle.audioFilesPath;

// initialize Vesicle with the path
v = Vesicle(p);

// run a glisson process with a selected sound grain duration and overlap
VesicleProc.scanGliss(v, "voice.wav", dur: 5, rateFrom: 1, rateTo: 2, density: 20)

// run a second process that folds per grain
VesicleProc.scanFold(v, "woodcut.wav", dur: 10, gdur: 0.1, flo: -0.1, fhi: 0.1, rate: 0.8, factor: 1, vary: 0.01, density: 50, spread: 1, amp: 5)

// run a scan process, use variable size arrays for parameter modulation over process
VesicleProc.scan(v, "voice.wav", dur: 12, gdur: [0.5, 2.0, 0.5], rate: [2, 0.5, 2], factor: [1.0, 0.0])
