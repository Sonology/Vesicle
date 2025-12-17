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
VesicleProc.glisson(v, "voice.wav", 0.1, 1.5)

// run a second process that folds per grain
VesicleProc.fold(v, "voice.wav", 0.1, 2.5)

// run a third process with a bandpass filter per grain
VesicleProc.bp(v, "voice.wav", 0.1, 2.5)

// run a scan process, use variable size arrays for parameter modulation over process
VesicleProc.scan(v, "voice.wav", dur: 12, gdur: [0.5, 2.0, 0.5], rate: [2, 0.5, 2], factor: [1.0, 0.0])
