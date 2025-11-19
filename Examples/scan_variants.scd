p = Platform.userAppSupportDir +/+ "downloaded-quarks/Vesicle/Audio/*";
v = Vesicle(p)

// Basic fold with evolving fold range
(

VesicleProc.scanFold(v, "voice.wav", 8,
	rate: 1,
	gdur: 0.1,
	density: 40,
	factor: 0.5,
	flo: [-0.8, -0.2, -0.8],
	fhi: [0.8, 0.2, 0.8],
	amp: [0, 1, 1, 0]
)

)

// Aggressive fold with high density
(

VesicleProc.scanFold(
	v,
	"woodcut.wav",
	dur: 10,
	rate: [1, 2, 0.5],
	gdur: [0.05, 0.2, 0.05],
	density: [50, 150, 50],
	factor: 1,
	lo: 0.2,
	hi: 0.8,
	flo: [-0.9, -0.1, -0.9],
	fhi: [0.9, 0.1, 0.9],
	amp: [0, 1, 1, 1, 0],
	vary: 0.01,
	spread: 0.5
)

)

// Subtle fold with envelope evolution
(

VesicleProc.scanFold(
	v,
	"voice.wav",
	dur: 15,
	rate: 0.8,
	gdur: [0.2, 0.5, 0.2],
	density: 30,
	factor: 0.25,
	flo: [-0.3, -0.5],
	fhi: [0.3, 0.5],
	skew: [0.2, 0.8],
	width: 0.8,
	amp: [0, 0.8, 0.8, 0],
	curve: \sine
)

)

// Basic upward glissando in each grain
(

VesicleProc.scanGliss(v, "voice.wav", 6,
	rateFrom: 0.5,
	rateTo: 2.0,
	gdur: 0.3,
	density: 20,
	factor: 0.5,
	amp: [0, 1, 1, 0]
)

)

// Bidirectional glissandi with envelope evolution
(
VesicleProc.scanGliss(
	v,
	"woodcut.wav",
	dur: 12,
	rateFrom: [2, 0.5, 2],
	rateTo: [0.5, 2, 0.5],
	gdur: [0.1, 0.5, 0.1],
	density: [40, 80, 40],
	factor: [1, 0.5, 1, 2],
	lo: 0.0,
	hi: 1.0,
	amp: [0, 1, 1, 1, 0],
	vary: 0.05,
	spread: 0.75,
	skew: [0.3, 0.7, 0.5],
	curve: \sine
)
)

// Fast glissandi with high density
(
VesicleProc.scanGliss(
	v,
	"rotterdam.wav",
	dur: 8,
	rateFrom: [1, 3],
	rateTo: [3, 1],
	gdur: [0.05, 0.15],
	density: [100, 200, 100],
	factor: 0.75,
	amp: [0, 0.7, 0.7, 0],
	prob: [1, 0.5, 1],
	spread: 1.0,
	curve: -3
)
)

// Slow evolving glissandi with wide grains
(
VesicleProc.scanGliss(
	v,
	"voice.wav",
	dur: 20,
	rateFrom: [0.25, 1, 0.5],
	rateTo: [1, 0.25, 0.75],
	gdur: [0.5, 1, 0.5],
	density: 15,
	factor: 0.1,
	lo: 0.3,
	hi: 0.7,
	vary: 0.1,
	skew: 0.6,
	width: [1, 0.5, 1],
	index: 2,
	amp: [0, 0.6, 0.6, 0],
	spread: 0.5,
	curve: \welch
)
)
