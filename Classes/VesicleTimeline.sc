
VesicleTimeline {
	var <vesicle, <window;
	var <entries;
	var <numLanes;
	var <pixelsPerSecond;
	var <totalDuration;

	// Interaction state
	var selectedEntry;
	var placingSnapshot;
	var dragOffset;
	var isPlaying;
	var playRoutine;
	var playheadTime;
	var playheadUpdater;
	var activePlayers;

	// GUI elements
	var timelineView;
	var paletteMenu;
	var filePath;

	// Style
	var entryColors;
	var fontMono, fontMonoSm, fontMonoXs;

	*new { |vesicle|
		^super.newCopyArgs(vesicle).init;
	}

	init {
		numLanes = 4;
		pixelsPerSecond = 60;
		totalDuration = 60;
		entries = [];
		selectedEntry = nil;
		placingSnapshot = nil;
		dragOffset = 0;
		isPlaying = false;
		playheadTime = 0;
		activePlayers = [];

		entryColors = [
			Color(0.725, 0.075, 0.447),
			Color(0.357, 0.753, 0.745),
			Color(0.494, 0.498, 0.604),
			Color(0.2, 0.6, 0.4),
		];

		fontMono = Font("Monaco", 11);
		fontMonoSm = Font("Monaco", 10);
		fontMonoXs = Font("Monaco", 9);

		filePath = vesicle.path.dirname +/+ "vesicle_timeline.scd";
		this.loadEntries;
		this.buildGUI;
	}

	buildGUI {
		var bgColor = Color(0.94, 0.94, 0.96);
		var panelColor = Color.white;
		var textColor = Color(0.18, 0.18, 0.18);
		var labelColor = Color(0.56, 0.56, 0.58);
		var zoomSlider, durBox;

		window = Window("Vesicle Timeline", Rect(100, 100, 1200, 400))
		.background_(bgColor);

		paletteMenu = PopUpMenu()
		.items_(["-- select snapshot --"] ++ VesicleSnapshot.names)
		.font_(fontMonoSm)
		.background_(panelColor)
		.stringColor_(textColor);

		zoomSlider = Slider()
		.value_(0.3)
		.orientation_(\horizontal)
		.fixedWidth_(120)
		.action_({ |sl|
			pixelsPerSecond = sl.value.linexp(0, 1, 10, 300);
			timelineView.refresh;
		});

		durBox = NumberBox()
		.value_(totalDuration)
		.clipLo_(5).clipHi_(600)
		.decimals_(0)
		.font_(fontMonoSm)
		.background_(panelColor)
		.normalColor_(textColor)
		.fixedWidth_(50)
		.action_({ |box|
			totalDuration = box.value;
			timelineView.refresh;
		});

		timelineView = UserView()
		.background_(Color.white)
		.drawFunc_({ |view| this.prDrawTimeline(view) })
		.mouseDownAction_({ |view, x, y, mod, btn, click|
			this.prMouseDown(view, x, y, click);
		})
		.mouseMoveAction_({ |view, x, y|
			this.prMouseMove(view, x, y);
		})
		.mouseUpAction_({ |view, x, y|
			this.prMouseUp(view, x, y);
		});

		window.layout_(VLayout(
			HLayout(
				StaticText().string_("snapshot").font_(fontMonoXs).stringColor_(labelColor),
				[paletteMenu, s: 3],
				5,
				Button()
				.states_([
					["Place", textColor, Color(0.88, 0.92, 0.88)],
					["Click timeline...", Color.white, Color(0.357, 0.753, 0.745)]
				])
				.font_(fontMonoSm)
				.fixedWidth_(120)
				.action_({ |btn|
					if (btn.value == 1) {
						if (paletteMenu.value > 0) {
							placingSnapshot = paletteMenu.items[paletteMenu.value];
						} {
							btn.value_(0);
						};
					} {
						placingSnapshot = nil;
					};
				}),
				5,
				Button()
				.states_([["Delete", Color(0.6, 0.2, 0.2), Color(0.92, 0.88, 0.88)]])
				.font_(fontMonoSm)
				.fixedWidth_(60)
				.action_({
					if (selectedEntry.notNil) {
						entries.removeAt(selectedEntry);
						selectedEntry = nil;
						this.saveEntries;
						timelineView.refresh;
					};
				}),
				nil,
				StaticText().string_("zoom").font_(fontMonoXs).stringColor_(labelColor),
				zoomSlider,
				5,
				StaticText().string_("dur").font_(fontMonoXs).stringColor_(labelColor),
				durBox,
				10,
				Button()
				.states_([
					["▶  Play", Color.white, Color(0.357, 0.753, 0.745)],
					["■  Stop", Color.white, Color(0.725, 0.075, 0.447)]
				])
				.font_(Font("Monaco", 12, true))
				.fixedWidth_(90)
				.action_({ |btn|
					if (btn.value == 1) { this.play } { this.stop };
				})
			).margins_(6),
			[timelineView, s: 10]
		).margins_(4).spacing_(3));

		window.onClose_({
			this.stop;
		});

		window.front;
	}

	// -- Drawing --

	prDrawTimeline { |view|
		var bounds = view.bounds;
		var laneH = bounds.height / numLanes;
		var maxX = totalDuration * pixelsPerSecond;

		// Lane backgrounds
		numLanes.do { |i|
			var y = i * laneH;
			if (i.even) {
				Pen.fillColor_(Color(0.97, 0.97, 0.99));
			} {
				Pen.fillColor_(Color(0.94, 0.94, 0.96));
			};
			Pen.fillRect(Rect(0, y, bounds.width, laneH));
		};

		// Lane separators
		Pen.strokeColor_(Color(0.85, 0.85, 0.87));
		numLanes.do { |i|
			var y = i * laneH;
			Pen.line(0 @ y, bounds.width @ y);
		};
		Pen.stroke;

		// Time grid
		this.prDrawTimeGrid(bounds, laneH);

		// Entries
		entries.do { |entry, i|
			var x = entry[\startTime] * pixelsPerSecond;
			var y = entry[\lane] * laneH;
			var w = entry[\dur] * pixelsPerSecond;
			var color = entryColors[entry[\lane] % entryColors.size];
			var isSelected = (i == selectedEntry);

			// Shadow
			Pen.fillColor_(Color(0, 0, 0, 0.08));
			Pen.fillRect(Rect(x + 1, y + 3, w, laneH - 6));

			// Entry rect
			if (isSelected) {
				Pen.fillColor_(color.alpha_(0.9));
			} {
				Pen.fillColor_(color.alpha_(0.65));
			};
			Pen.fillRect(Rect(x, y + 2, w, laneH - 4));

			// Selection border
			if (isSelected) {
				Pen.strokeColor_(Color.white);
				Pen.width_(2);
				Pen.strokeRect(Rect(x, y + 2, w, laneH - 4));
				Pen.width_(1);
			};

			// Label
			Pen.stringInRect(
				entry[\snapshot],
				Rect(x + 4, y + 4, w - 8, laneH - 8),
				fontMonoXs,
				Color.white
			);

			// Duration label
			if (w > 50) {
				Pen.stringInRect(
					entry[\dur].round(0.1).asString ++ "s",
					Rect(x + 4, y + laneH - 20, w - 8, 14),
					Font("Monaco", 8),
					Color(1, 1, 1, 0.7)
				);
			};
		};

		// Playhead
		if (isPlaying) {
			var px = playheadTime * pixelsPerSecond;
			Pen.strokeColor_(Color(0.9, 0.15, 0.15));
			Pen.width_(2);
			Pen.line(px @ 0, px @ bounds.height);
			Pen.stroke;
			Pen.width_(1);
		};
	}

	prDrawTimeGrid { |bounds, laneH|
		var step = case
			{ pixelsPerSecond > 100 } { 1 }
			{ pixelsPerSecond > 30 }  { 5 }
			{ true }                  { 10 };
		var t = 0;

		while { t <= totalDuration } {
			var x = t * pixelsPerSecond;
			if (x > bounds.width) { ^this };

			if ((t % (step * 5)) == 0) {
				Pen.strokeColor_(Color(0.75, 0.75, 0.78));
			} {
				Pen.strokeColor_(Color(0.88, 0.88, 0.90));
			};
			Pen.line(x @ 0, x @ bounds.height);
			Pen.stroke;

			// Time label
			if ((t % (step * 5)) == 0) {
				Pen.stringAtPoint(
					t.asString ++ "s",
					(x + 2) @ (bounds.height - 14),
					Font("Monaco", 8),
					Color(0.6, 0.6, 0.62)
				);
			};
			t = t + step;
		};
	}

	// -- Mouse interaction --

	prMouseDown { |view, x, y, clickCount|
		var laneH = view.bounds.height / numLanes;
		var time = x / pixelsPerSecond;
		var lane = (y / laneH).floor.asInteger.clip(0, numLanes - 1);
		var hitIndex = this.prEntryAt(time, lane);

		if (placingSnapshot.notNil) {
			// Place mode: create new entry
			var snap = VesicleSnapshot.at(placingSnapshot);
			if (snap.notNil) {
				entries = entries.add((
					snapshot: placingSnapshot,
					lane: lane,
					startTime: time.max(0).round(0.1),
					dur: snap[\dur]
				));
				this.saveEntries;
				placingSnapshot = nil;
			};
			timelineView.refresh;
			^this;
		};

		if (hitIndex.notNil) {
			selectedEntry = hitIndex;
			dragOffset = time - entries[hitIndex][\startTime];
		} {
			selectedEntry = nil;
		};
		timelineView.refresh;
	}

	prMouseMove { |view, x, y|
		if (selectedEntry.notNil) {
			var laneH = view.bounds.height / numLanes;
			var time = (x / pixelsPerSecond - dragOffset).max(0).round(0.1);
			var lane = (y / laneH).floor.asInteger.clip(0, numLanes - 1);
			entries[selectedEntry][\startTime] = time;
			entries[selectedEntry][\lane] = lane;
			timelineView.refresh;
		};
	}

	prMouseUp { |view, x, y|
		if (selectedEntry.notNil) {
			this.saveEntries;
		};
	}

	prEntryAt { |time, lane|
		entries.do { |entry, i|
			if (entry[\lane] == lane) {
				if ((time >= entry[\startTime]) and: { time <= (entry[\startTime] + entry[\dur]) }) {
					^i;
				};
			};
		};
		^nil;
	}

	// -- Playback --

	play {
		var sorted = entries.copy.sort({ |a, b| a[\startTime] < b[\startTime] });
		var currentTime = 0;

		this.stop;
		isPlaying = true;
		playheadTime = 0;
		activePlayers = [];

		playRoutine = Routine({
			sorted.do { |entry|
				var snap = VesicleSnapshot.at(entry[\snapshot]);
				var waitTime = entry[\startTime] - currentTime;
				if (waitTime > 0) { waitTime.wait };
				currentTime = entry[\startTime];
				if (snap.notNil) {
					activePlayers = activePlayers.add(
						this.prPlaySnapshot(snap)
					);
				};
			};
			// Wait for the last entry to finish
			if (sorted.size > 0) {
				var lastEnd = sorted.collect({ |e| e[\startTime] + e[\dur] }).maxItem;
				var remaining = lastEnd - currentTime;
				if (remaining > 0) { remaining.wait };
			};
			{ this.stop }.defer;
		}).play(TempoClock.default);

		// Playhead animation
		playheadUpdater = SkipJack({
			playheadTime = playheadTime + 0.033;
			timelineView.refresh;
		}, 0.033, { isPlaying.not }, "VesicleTimeline_playhead");
	}

	stop {
		isPlaying = false;
		if (playRoutine.notNil) {
			playRoutine.stop;
			playRoutine = nil;
		};
		activePlayers.do { |player|
			if (player.notNil) { player.stop };
		};
		activePlayers = [];
		if (playheadUpdater.notNil) {
			playheadUpdater.stop;
			playheadUpdater = nil;
		};
		{ timelineView.refresh }.defer;
	}

	prPlaySnapshot { |snapshot|
		var type = snapshot[\processType];
		var defs = VesicleProc.multiParamDefs(type);
		var args = IdentityDictionary.new;
		// Find the matching buffer key (deserialized strings may not be identity-equal)
		var soundKey = vesicle.buffers.keys.detect({ |k| k == snapshot[\sound] });

		if (soundKey.isNil) {
			("VesicleTimeline: sound '" ++ snapshot[\sound] ++ "' not found in buffers").warn;
			^nil;
		};

		// Map normalized slider values through specs
		defs.do { |def|
			var key = def[0], min = def[1], max = def[2], default = def[3], warp = def[4];
			var spec = ControlSpec(min, max, warp);
			var normVals = snapshot[\sliders][key];
			if (normVals.notNil) {
				var mapped = normVals.collect({ |v| spec.map(v) });
				args[key] = if (mapped.size == 1) { mapped[0] } { mapped };
			} {
				args[key] = default;
			};
		};

		^switch(type,
			\scan, {
				VesicleProc.scan(
					vesicle, soundKey, snapshot[\dur],
					rate: args[\rate],
					gdur: args[\gdur],
					density: args[\density],
					factor: args[\factor],
					lo: snapshot[\scalars][\lo],
					hi: snapshot[\scalars][\hi],
					vary: snapshot[\scalars][\vary],
					prob: args[\prob],
					spread: snapshot[\scalars][\spread],
					bpFreq: args[\bpFreq],
					bpBlend: args[\bpBlend],
					bpRQ: args[\bpRQ],
					skew: snapshot[\scalars][\skew],
					width: snapshot[\scalars][\width],
					index: snapshot[\scalars][\index],
					curve: snapshot[\curve],
					pan: args[\pan],
					amp: args[\amp]
				);
			},
			\scanFold, {
				VesicleProc.scanFold(
					vesicle, soundKey, snapshot[\dur],
					rate: args[\rate],
					gdur: args[\gdur],
					density: args[\density],
					factor: args[\factor],
					lo: snapshot[\scalars][\lo],
					hi: snapshot[\scalars][\hi],
					vary: snapshot[\scalars][\vary],
					prob: args[\prob],
					spread: snapshot[\scalars][\spread],
					flo: args[\flo],
					fhi: args[\fhi],
					skew: snapshot[\scalars][\skew],
					width: snapshot[\scalars][\width],
					index: snapshot[\scalars][\index],
					curve: snapshot[\curve],
					pan: args[\pan],
					amp: args[\amp]
				);
			},
			\scanGliss, {
				VesicleProc.scanGliss(
					vesicle, soundKey, snapshot[\dur],
					rateFrom: args[\rateFrom],
					rateTo: args[\rateTo],
					gdur: args[\gdur],
					density: args[\density],
					factor: args[\factor],
					lo: snapshot[\scalars][\lo],
					hi: snapshot[\scalars][\hi],
					vary: snapshot[\scalars][\vary],
					prob: args[\prob],
					spread: snapshot[\scalars][\spread],
					skew: snapshot[\scalars][\skew],
					width: snapshot[\scalars][\width],
					index: snapshot[\scalars][\index],
					curve: snapshot[\curve],
					pan: args[\pan],
					amp: args[\amp]
				);
			}
		);
	}

	// -- Persistence --

	saveEntries {
		var file = File(filePath, "w");
		file.write("[\n");
		entries.do { |entry, i|
			file.write("(snapshot: " ++ entry[\snapshot].asCompileString);
			file.write(", lane: " ++ entry[\lane].asString);
			file.write(", startTime: " ++ entry[\startTime].round(0.1).asString);
			file.write(", dur: " ++ entry[\dur].round(0.1).asString);
			file.write(")");
			if (i < (entries.size - 1)) { file.write(",\n") } { file.write("\n") };
		};
		file.write("]\n");
		file.close;
	}

	loadEntries {
		if (File.exists(filePath)) {
			entries = try { thisProcess.interpreter.executeFile(filePath) } ? [];
		} {
			entries = [];
		};
	}

	refreshPalette {
		paletteMenu.items_(["-- select snapshot --"] ++ VesicleSnapshot.names);
	}
}
