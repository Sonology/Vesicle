v = Vesicle(Platform.userExtensionDir +/+ "Vesicle/Audio/*")

// voice
VesicleProc.scan(v, "voice.wav", 5, factor: 0.25, amp: [0, 1, 1, 0])
VesicleProc.scan(v, "voice.wav", 10, rate: [1, 1, 0.5], gdur: [0.25, 0.5], density: 50, factor: [0, 1, 0.5, 0.01], amp: 0.75, vary: 0.01)
VesicleProc.scan(v, "voice.wav", 30, 1.1, [0.5, 0.25], [10, 100], [0.1, 0.25, 1, 2, 10], 0, 1, 0.005, 1, 0.5, 500, 0.0, 0.25, 0.025, 0, 2, amp: 1)

// woodcut
(
VesicleProc.scan(
	v,
	"woodcut.wav",
	dur: 6,
	gdur: [1, 0.025, 0.2],
	rate: [2, 5, 1, 2],
	factor: [1, 0, 0, 3, 1],
	density: [80, 100, 50],
	lo: 0.1,
	hi: 1.0,
	amp: [1, 1, 1, 0],
	prob: 1,
	curve: -5,
	vary: 0.025,
	spread: 0.25,
	pan: 0.5,
	bpRQ: [0.1, 1, 0.1],
	bpFreq: [100, 5000, 100],
	bpBlend: 1,
	skew: 0.5,
	width: 1,
	index: 1
)
)

// step curve
(
VesicleProc.scan(
	v,
	"woodcut.wav",
	dur: 6,
	gdur: [0.05, 0.125, 0.1],
	rate: [2, 5, 1, 5],
	factor: [1, 0, 0, 3, -1],
	density: [10, 100, 10],
	lo: 0.0,
	hi: 1.0,
	amp: 1,
	prob: 1,
	curve: \step,
	vary: 0.5,
	spread: 0.25,
	pan: 0.5,
	bpRQ: {rrand(0.01, 0.75) }!100,
	bpFreq: {rrand(100, 5000) }!1000,
	bpBlend: { 1.0.rand }!100
)
)

// sine curve
(
VesicleProc.scan(
	v,
	"woodcut.wav",
	dur: b.duration,
	gdur: [0.15, 0.125, 0.5, 0.1],
	rate: 1,
	factor: 0.9,
	density: { rrand(10, 200) }!100,
	lo: 0.0,
	hi: 1.0,
	amp: [0, 1, 1, 1, 1, 0],
	prob: 1,
	curve: \sine,
	vary: 0.025,
	spread: 0.9,
	pan: 0.5,
	bpRQ: {rrand(0.1, 0.5) }!100,
	bpFreq: {rrand(70, 12000) }!100,
	bpBlend: 0.75
)
)