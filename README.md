
# Vesicle

Experiments with per-grain processes of microsound

```javascript

/* Install Vesicle */

Quarks.install("https://github.com/Sonology/Vesicle")

// boot the sound server
s.boot

// path to sound material
p = "/Users/bjarni/samples/*";

// initialize Vesicle with the path
x = Vesicle(p);

// run a glisson process with a selected sound grain duration and overlap
VesicleProc.glisson(x, "caa.wav", 0.04, 1.5)

// run a second process that folds per grain
VesicleProc.fold(x, "caa.wav", 0.1, 2.5)

// run a third process with a bandpass filter per grain
VesicleProc.bp(x, "caa.wav", 0.1, 2.5)