Vesicle {

	var <>path="";
	var <>sounds;
	var <>buffers;

	*new { arg path="";
		^super.newCopyArgs(path).loadBuffers(path).loadSynths();
	}

	*audioFilesPath {
		var extensionsRoot = PathName(Platform.userExtensionDir).parentPath;
		// checks where Vesicle is (downloaded-quarks or Extensions)
		[
			PathName(extensionsRoot ++ "Extensions").entries,
			PathName(extensionsRoot ++ "downloaded-quarks").entries
		].flat.do({ |item|
			if (item.folderName == "Vesicle", {  ^(item.fullPath ++ "Audio/*") })
		});
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

		// universal grain with variable envelope (mono, stereo)
		{ |i|
			SynthDef(\grainuni ++ [\m, \s][i], { |out = 0, buf = 0, start = 0.0, rate = 1.0, gdur = 1.0, bpFreq = 300.0, bpRQ = 0.5, bpBlend = 0.0, smooth = 1.0, skew = 0.5, width = 0.5, index = 1, pan = 0.0, amp = 0.5 |
				var phase = VesicleGrain.phase(gdur);
				var sig = VesicleGrain.playBuf(i+1, buf, rate, start);
				var env = VesicleGrain.tukeyGauss(phase, skew, width, index);

				// amplitude compensation from miSCellaneous_lib example by Daniel Mayer
				// estimation like in Wavesets example by Alberto de Campo
				sig = (BPF.ar(sig, bpFreq, bpRQ, mul: (bpRQ ** -1) * (400 / bpFreq ** 0.5)) * bpBlend + (sig * (1 - bpBlend)));
				sig = LeakDC.ar(sig * 0.5);
				sig = sig * env * amp;

				OffsetOut.ar(out, [{Pan2.ar(sig, pan)}, {Balance2.ar(sig[0], sig[1], pan)}][i]);
			}).add
		}.dup(2);

		// universal grain with fold distortion
		{ |i|
			SynthDef(\grainunifold ++ [\m, \s][i], { |out = 0, buf = 0, start = 0.0, rate = 1.0, gdur = 1.0,
				flo = -0.5, fhi = 0.5,
				skew = 0.5, width = 0.5, index = 1, pan = 0.0, amp = 0.5 |

				var phase = VesicleGrain.phase(gdur);
				var sig = VesicleGrain.playBuf(i+1, buf, rate, start);
				var env = VesicleGrain.tukeyGauss(phase, skew, width, index);

				sig = Fold.ar(sig, flo, fhi);
				sig = LeakDC.ar(sig * 0.5);
				sig = sig * env * amp;

				OffsetOut.ar(out, [{Pan2.ar(sig, pan)}, {Balance2.ar(sig[0], sig[1], pan)}][i]);
			}).add
		}.dup(2);

		// universal grain with glissando
		{ |i|
			SynthDef(\grainunigliss ++ [\m, \s][i], { |out = 0, buf = 0, start = 0.0,
				rateFrom = 1.0, rateTo = 2.0, gdur = 1.0,
				skew = 0.5, width = 0.5, index = 1, pan = 0.0, amp = 0.5 |

				var phase = VesicleGrain.phase(gdur);
				var playRate = Line.ar(rateFrom, rateTo, gdur);
				var sig = VesicleGrain.playBuf(i+1, buf, playRate, start);
				var env = VesicleGrain.tukeyGauss(phase, skew, width, index);

				sig = LeakDC.ar(sig * 0.5);
				sig = sig * env * amp;

				OffsetOut.ar(out, [{Pan2.ar(sig, pan)}, {Balance2.ar(sig[0], sig[1], pan)}][i]);
			}).add
		}.dup(2);

			// single-cycle wavetable-based phase-distortion grain (mono)
			SynthDef(\grainpd, { |buf, pdbuf, envbuf, rate = 5, gdur = 0.1, index = 0.5, offset = 0.0, distFreq = 1, unipol = 0, pan = 0,  amp = 0.5, out = 0|
				var numFrames = BufFrames.kr(buf);
				var phasor = Phasor.ar(0, BufRateScale.kr(buf) * rate, 0, numFrames);
				var mod = SinOsc.ar(distFreq);
				var dist = PlayBuf.ar(1, pdbuf, rate + (offset * 2pi), loop: 1) * numFrames;
				var sum, sig;
				dist = dist + (LFGauss.ar(BufDur.ir(buf) / (rate + (offset * 2pi)), 0.5, loop: 1) * numFrames) * mod.blend(mod.unipolar(1), unipol);
				dist = dist * index;
				sum = phasor + dist;
				sig = BufRd.ar(1, buf, sum, 1, 4);
				sig = LeakDC.ar(sig * 0.5); // optional DC
				sig = sig * BufRd.ar(1, envbuf, Line.ar(0, BufFrames.ir(envbuf) - 1, gdur, doneAction: 2), 0);
				sig = Pan2.ar(sig, pan, amp);
				OffsetOut.ar(out, sig)
			}).add;

			Server.default.sync;

			(" ## Completed loading synths").postln;

		}.fork;

	}
}