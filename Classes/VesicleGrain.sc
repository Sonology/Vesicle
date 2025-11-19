VesicleGrain {
	
	*phase { |gdur|
		^Line.ar(0, 1, gdur, doneAction: 2);
	}
	
	*playBuf { |numChannels, buf, rate, start|
		^PlayBuf.ar(numChannels, buf, rate * BufRateScale.kr(buf), 1.0, start, 1.0);
	}
	
	// Tukey-Gauss envelope with phase warping (by dietcv) 
	*tukeyGauss { |phase, skew = 0.5, width = 1.0, index = 1.0|
		var transferFunc = { |phase, skew|
			phase = phase.linlin(0, 1, skew.neg, 1 - skew);
			phase.bilin(0, skew.neg, 1 - skew, 1, 0, 0);
		};
		var unitTukeyGauss = { |phase, width, index|
			var sustain = 1 - width;
			var cosine = cos(phase * 0.5pi / sustain) * index;
			var gaussian = exp(cosine * cosine.neg);
			var hanning = 1 - cos(phase * pi / sustain) / 2;
			Select.ar(phase < sustain, [K2A.ar(1), gaussian * hanning]);
		};
		var warpedPhase = transferFunc.(phase, skew);
		^unitTukeyGauss.(warpedPhase, width, index);
	}
}

