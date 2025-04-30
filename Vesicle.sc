Vesicle {

	var <>path="";
    var <>sounds;
    var <>buffers;

	*new { arg path="";
		^super.newCopyArgs(path).loadBuffers(path).loadSynths();
	}

    loadBuffers {|path|

		{
            this.buffers = Dictionary();
            this.sounds = SoundFile.collect(path);
			this.sounds.do{|snd|

				if(this.buffers[snd.path].isNil, {
					var buf = Buffer.read(Server.default, snd.path);
					Server.default.sync; this.buffers[snd.path.basename] = buf;
				})
			};

			(" ## Completed loading buffers (" + this.buffers.keys.asArray.sort.join(", ") + ")").postln;

		}.fork;
	}

    loadSynths {

        {

            SynthDef(\graingliss, { |buf=0, start=0, amp=0.5, dur=0.3, pan=0, from=0.5,to=4.0|
                var sig, shape, env, pos;
                shape = Env([0, amp, 0], [dur*0.5, dur*0.5], \sine);
                env = EnvGen.ar(shape, doneAction: 2);
                pos = start * BufFrames.ir(buf);
                sig = PlayBuf.ar(1, buf, Line.ar(from,to,dur) * BufRateScale.ir(buf), 1, pos, 0);
                OffsetOut.ar(0, Pan2.ar(sig * env, pan));
            }).add;

            SynthDef(\grainfold, { |buf=0, rate=1, start=0, amp=0.5, dur=0.3, pan=0, flo=0.0,fhi=1.0|
                var sig, shape, env, pos;
                shape = Env([0, amp, 0], [dur*0.5, dur*0.5], \sine);
                env = EnvGen.ar(shape, doneAction: 2);
                pos = start * BufFrames.ir(buf);
                sig = PlayBuf.ar(1, buf, rate * BufRateScale.ir(buf), 1, pos, 0);
                sig = Fold.ar(sig, flo, fhi);
                OffsetOut.ar(0, Pan2.ar(sig * env, pan));
            }).add;

            SynthDef(\grainbp, { |buf=0, rate=1, start=0, amp=0.5, dur=0.3, pan=0, bpf=100, bw=1.0|
                var sig, shape, env, pos;
                shape = Env([0, amp, 0], [dur*0.5, dur*0.5], \sine);
                env = EnvGen.ar(shape, doneAction: 2);
                pos = start * BufFrames.ir(buf);
                sig = PlayBuf.ar(1, buf, rate * BufRateScale.ir(buf), 1, pos, 0);
                sig = BBandPass.ar(sig, bpf, bw);
                OffsetOut.ar(0, Pan2.ar(sig * env, pan));
            }).add;

			{ |i|
				SynthDef(\unigrain ++ (i+1), { |out = 0, buf = 0, start = 0.0, rate = 1.0, gdur = 1.0, bpFreq = 30.0, bpRQ = 0.5, bpBlend = 0.0, smooth = 1.0, skew = 0.5, width = 0.5, index = 1 pan = 0.0, amp = 0.5 |

					var phase = Line.ar(0, 1, gdur, doneAction: 2);

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

					var tukeyGaussWindow = { |phase, skew, width, index|
						var warpedPhase = transferFunc.(phase, skew);
						unitTukeyGauss.(warpedPhase, width, index);
					};

					var sig = PlayBuf.ar(i+1, buf * BufRateScale.kr(buf), rate, 1.0, start * BufFrames.kr(buf), 1.0);
					var env = tukeyGaussWindow.(phase, skew, width, index);

					// amplitude compensation from miSCellaneous_lib example by Daniel Mayer
					// estimation like in Wavesets example by Alberto de Campo
					sig = (BPF.ar(sig, bpFreq, bpRQ, mul: (bpRQ ** -1) * (400 / bpFreq ** 0.5)) * bpBlend + (sig * (1 - bpBlend)));
					sig = LeakDC.ar(sig * 0.5);
					sig = sig * env * amp;

					OffsetOut.ar(out, [{Pan2.ar(sig, pan)}, {Balance2.ar(sig[0], sig[1],  pan)}][i]);

				}).add
			}.dup(2);

            Server.default.sync;

            (" ## Completed loading synths").postln;

        }.fork;

    }
}