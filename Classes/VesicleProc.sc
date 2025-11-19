
/* A collection of processes for by-sample granulation. */

VesicleProc {

	*scan { |
		vesicle, sound, dur = 1,
		rate = 1, gdur = 1, density = 10, factor = 1,
		lo = 0.0, hi = 1.0, vary = 0.0,
		prob = 1, spread = 0.25,
		bpFreq = 300, bpBlend = 0, bpRQ = 0.25,
		skew = 0.5, width = 1, index = 1, curve = 'sine', pan = 0.0, amp = 1
		|
		^this.prScanBase(
			vesicle, sound, dur, \grainunis,
			gdur, density, factor, lo, hi, vary, prob, spread,
			skew, width, index, curve, pan, amp,
			[rate, bpFreq, bpBlend, bpRQ],
			[\rate, \bpFreq, \bpBlend, \bpRQ]
		)
	}

	*scanFold { |
		vesicle, sound, dur = 1,
		rate = 1, gdur = 1, density = 10, factor = 1,
		lo = 0.0, hi = 1.0, vary = 0.0,
		prob = 1, spread = 0.25,
		flo = -0.5, fhi = 0.5,
		skew = 0.5, width = 1, index = 1, curve = 'sine', pan = 0.0, amp = 1
		|
		^this.prScanBase(
			vesicle, sound, dur, \grainunifolds,
			gdur, density, factor, lo, hi, vary, prob, spread,
			skew, width, index, curve, pan, amp,
			[rate, flo, fhi],
			[\rate, \flo, \fhi]
		)
	}

	*scanGliss { |
		vesicle, sound, dur = 1,
		rateFrom = 1, rateTo = 2, gdur = 1, density = 10, factor = 1,
		lo = 0.0, hi = 1.0, vary = 0.0,
		prob = 1, spread = 0.25,
		skew = 0.5, width = 1, index = 1, curve = 'sine', pan = 0.0, amp = 1
		|
		^this.prScanBase(
			vesicle, sound, dur, \grainuniglisss,
			gdur, density, factor, lo, hi, vary, prob, spread,
			skew, width, index, curve, pan, amp,
			[rateFrom, rateTo],
			[\rateFrom, \rateTo]
		)
	}

	// shared scanning logic
	*prScanBase { |
		vesicle, sound, dur, instrument,
		gdur, density, factor, lo, hi, vary, prob, spread,
		skew, width, index, curve, pan, amp,
		extraParams, extraKeys
		|
		var sample = vesicle.buffers[sound];
		var sr = Server.default.sampleRate;
		var numFrames = sample.numFrames;
		var eventStreamPlayer;
		var isArray = { |val| if (val.isArray, { val.size - 1 }, { 1 }) };
		
		// Core parameters that all scan methods use
		var coreParams = [gdur, density, factor, prob, amp];
		var allParams = coreParams ++ extraParams;
		var allSizes = allParams.collect(isArray.value(_));
		
		// Build Pbind event pairs dynamically
		var pbindPairs = [
			\instrument, instrument,
			\buf, sample,
			\density, Pseg(density, dur / allSizes[1], curve, inf),
			\dur, (1 / Pkey(\density, inf)),
			\gdur, Pseg(gdur, dur / allSizes[0], curve, inf),
			\start, Pnaryop(\wrap, 
				Pn(Pseries(0.0, ((sr * (Pseg(factor, dur / allSizes[2], curve, inf))) / Pkey(\density, inf)), numFrames), inf) 
				+ (Pwhite(sr.neg, sr, inf) * vary), 
				[lo * numFrames, hi * numFrames]
			),
			\pan, pan + (Pwhite(-1.0 - pan, 1.0 - pan, inf) * spread),
			\amp, Pseg(amp, dur / allSizes[4], curve, inf),
			\rest, Pif(Pseg(prob, dur / allSizes[3], curve, inf).coin, \play, \rest),
			\skew, skew, \width, width, \index, index
		];
		
		// Add extra parameter keys dynamically
		extraKeys.do { |key, i|
			var paramIndex = 5 + i;  // offset after core params
			var paramValue = extraParams[i];
			pbindPairs = pbindPairs ++ [key, Pseg(paramValue, dur / allSizes[paramIndex], curve, inf)];
		};
		
		eventStreamPlayer = Pbind(*pbindPairs).play;
		SystemClock.sched(dur, { eventStreamPlayer.stop });
		
		^eventStreamPlayer
	}

}