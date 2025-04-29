
/* This is a collection of processes for by-sample granulation, does not have to be a class. */

VesicleProc {

    *glisson {|vesicle, sound, dur=0.02, overlap=2.0|

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
                            \from, i.linlin(0,times,1.0,2.0),
                            \to, i.linlin(0,times,2.0,10.0)
                    ]) }
                );
                
                next.wait;
            };

        }.play;
    }

    *fold {|vesicle, sound, dur=0.02, overlap=2.0|

        var sample = vesicle.buffers[sound];

        Routine {
            var total = sample.numFrames / sample.sampleRate;
            var times = ((total / dur) * overlap).asInteger.postln;

            times.do{|i|
                var next = dur / overlap;
                var start = (i * next) / total;

                Server.default.makeBundle(0.1,
                { Synth(\grainfold, [\buf, sample.bufnum, \dur, dur, \start, 1 - start, \flo, 0.0, \fhi, 2.0]) });
                next.wait;
            };

        }.play;

    }

    *bp {|vesicle, sound, dur=0.02, overlap=2.0|

        var sample = vesicle.buffers[sound];

        Routine {
            var total = sample.numFrames / sample.sampleRate * 2;
            var times = ((total / dur) * overlap).asInteger.postln;

            times.do{|i|
                var next = dur / overlap;
                var start = (i * next) / total;

                Server.default.makeBundle(0.1,
                    { Synth(\grainbp, [\buf, sample.bufnum, \dur, dur, \start, 1.0.rand,
                    \bpf, i.linlin(0,times, 100, 8000),
                    \bw, 5
                ]) });
                next.wait;
            };

        }.play;
    }
}