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

            Server.default.sync; 

            (" ## Completed loading synths").postln;

        }.fork;

    }	
}