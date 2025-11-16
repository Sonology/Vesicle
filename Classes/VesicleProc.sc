
/* This is a collection of processes for by-sample granulation, does not have to be a class. */

VesicleProc {

    *glisson {|vesicle, sound, dur=0.02, overlap=2.0, ffrom=1.0, fto=2.0, tfrom=2.0, tto=10|

        var sample = vesicle.buffers[sound];

        Routine {
            var total = sample.numFrames / sample.sampleRate;
            var times = ((total / dur) * overlap).asInteger.postln;

            times.do{|i|
                var next = dur / overlap;
                var start = (i * next) / total;

                Server.default.makeBundle(0.1,
                    {
                        Synth(\graingliss, [
                            \buf, sample.bufnum,
                            \dur, dur,
                            \start, start,
                            \from, i.linlin(0,times,ffrom,fto),
							\to, i.linlin(0,times,tfrom,tto)
                    ]) }
                );

                next.wait;
            };

        }.play;
    }

    *fold {|vesicle, sound, dur=0.02, overlap=2.0, foldfrom=0.0, folto=2.0|

        var sample = vesicle.buffers[sound];

        Routine {
            var total = sample.numFrames / sample.sampleRate;
            var times = ((total / dur) * overlap).asInteger.postln;

            times.do{|i|
                var next = dur / overlap;
                var start = (i * next) / total;

                Server.default.makeBundle(0.1,
                { Synth(\grainfold, [\buf, sample.bufnum, \dur, dur, \start, 1 - start, \flo, foldfrom, \fhi, folto]) });
                next.wait;
            };

        }.play;

    }

    *bp {|vesicle, sound, dur=0.02, overlap=2.0,bpfrom=100,bpto=8000, bw=5, start=1.0|

        var sample = vesicle.buffers[sound];

        Routine {
            var total = sample.numFrames / sample.sampleRate * 2;
            var times = ((total / dur) * overlap).asInteger.postln;

            times.do{|i|
                var next = dur / overlap;
                var start = (i * next) / total;

                Server.default.makeBundle(0.1,
                    { Synth(\grainbp, [\buf, sample.bufnum, \dur, dur, \start, start.rand,
                    \bpf, i.linlin(0,times, bpfrom, bpto),
                    \bw, bw
                ]) });
                next.wait;
            };

        }.play;
    }

	*scan { |
		vesicle, sound, dur = 1,
		rate = 1, gdur = 1, density = 10, factor = 1,
		lo = 0.0, hi = 1.0, vary = 0.0,
		prob = 1, spread = 0.25,
		bpFreq = 300, bpBlend = 0, bpRQ = 0.25,
		skew = 0.5, width = 1, index = 1, curve = 'sine', pan = 0.0, amp = 1
		|
		var sample = vesicle.buffers[sound];
		var sr = Server.default.sampleRate;
		var numFrames = sample.numFrames;
		var sched = SystemClock.sched(dur, { eventStreamPlayer.stop });
		var isArray = { |val| if (val.isArray, { val.size - 1 }, { 1 }) };
		var rateSize, gdurSize, densitySize, factorSize, probSize, ampSize, bpFreqSize, bpBlendSize, bpRQSize, eventStreamPlayer;
		# rateSize, gdurSize, densitySize, factorSize, probSize, ampSize, bpFreqSize, bpBlendSize, bpRQSize = [rate, gdur, density, factor, prob, amp, bpFreq, bpBlend, bpRQ].collect(isArray.value(_));
		eventStreamPlayer = Pbind(
			\instrument, \grainunis,
			\buf, sample,
			\density, Pseg(density, dur / densitySize, curve, inf),
			\dur, (1 / Pkey(\density, inf)),
			\gdur, Pseg(gdur, dur / gdurSize, curve, inf),
			\rate, Pseg(rate, dur / rateSize, curve, inf),
			\start, Pnaryop(\wrap, Pn(Pseries(0.0, ((sr * (Pseg(factor, dur / factorSize, curve, inf))) / Pkey(\density, inf)), numFrames), inf) + (Pwhite(sr.neg, sr, inf) * vary), [lo * numFrames, hi * numFrames]),
			\pan, pan + (Pwhite(-1.0 - pan, 1.0 - pan, inf) * spread),
			\amp, Pseg(amp, dur / ampSize, curve, inf),
			\rest, Pif(Pseg(prob, dur / probSize, curve, inf).coin, \play, \rest),
			\skew, skew, \width, width, \index, index,
			\bpFreq, Pseg(bpFreq, dur / bpFreqSize, curve, inf),
			\bpBlend, Pseg(bpBlend, dur / bpBlendSize, curve, inf),
			\bpRQ, Pseg(bpRQ, dur / bpRQSize, curve, inf)
		).play

		^eventStreamPlayer
	}

}