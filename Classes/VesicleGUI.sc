
VesicleGUI {
	var <vesicle, <window;
	var <scanTypeMenu, <soundMenu, <durBox;
	var <scalarBoxes, <curveMenu;
	var <sliderPanels; // key -> IdentityDictionary[\slider, \spec, \valLabel]
	var multiSliderContainer;
	var currentPlayer;
	var numSliders;
	var currentSliderColor;
	var isRecording;
	var playBtn;
	var snapshotMenu, snapshotNameField;

	// Style constants
	var bgColor, panelColor, textColor, labelColor, sliderBgColor;
	var sliderColors;
	var fontMono, fontMonoSm, fontMonoBold, fontMonoXs;

	// Tooltips
	classvar paramTooltips;

	*initClass {
		paramTooltips = IdentityDictionary[
			\rate -> "Playback speed multiplier",
			\gdur -> "Duration of each grain in seconds",
			\density -> "Number of grains triggered per second",
			\factor -> "Scanning speed through the sound file",
			\prob -> "Probability (0-1) that a grain is played",
			\amp -> "Amplitude envelope over process duration",
			\pan -> "Stereo position (-1 left, +1 right)",
			\bpFreq -> "Band-pass filter center frequency in Hz",
			\bpBlend -> "Filter mix (0 = dry, 1 = filtered)",
			\bpRQ -> "Filter bandwidth (lower = more resonant)",
			\flo -> "Lower fold distortion boundary",
			\fhi -> "Upper fold distortion boundary",
			\rateFrom -> "Starting playback speed",
			\rateTo -> "Ending playback speed (glissando target)"
		];
	}

	*new { |vesicle, numSliders = 64|
		^super.newCopyArgs(vesicle).init(numSliders);
	}

	init { |sliderCount|
		numSliders = sliderCount;
		isRecording = false;
		sliderPanels = IdentityDictionary.new;
		scalarBoxes = IdentityDictionary.new;

		bgColor = Color(0.94, 0.94, 0.96);
		panelColor = Color.white;
		textColor = Color(0.18, 0.18, 0.18);
		labelColor = Color(0.56, 0.56, 0.58);
		sliderBgColor = Color(0.96, 0.96, 0.98);

		sliderColors = [
			Color(0.725, 0.075, 0.447),  // #b91372
			Color(0.357, 0.753, 0.745),  // #5bc0be
			Color(0.494, 0.498, 0.604),  // #7e7f9a
		];

		fontMono = Font("Monaco", 11);
		fontMonoSm = Font("Monaco", 10);
		fontMonoBold = Font("Monaco", 12, true);
		fontMonoXs = Font("Monaco", 9);

		VesicleSnapshot.load(vesicle.path.dirname +/+ "vesicle_snapshots.scd");
		this.buildGUI;
	}

	multiParamDefs { |type|
		^VesicleProc.multiParamDefs(type);
	}

	buildGUI {
		var sounds = vesicle.buffers.keys.asArray.sort;
		var recBtn;
		currentSliderColor = sliderColors.choose;

		window = Window("Vesicle", Rect(100, 50, 960, 680))
		.background_(bgColor);

		// -- Top bar controls --
		scanTypeMenu = PopUpMenu()
		.items_(["scan", "scanFold", "scanGliss"])
		.font_(fontMono)
		.background_(panelColor)
		.stringColor_(textColor)
		.action_({ this.rebuildMultiSliders });

		soundMenu = PopUpMenu()
		.items_(sounds)
		.font_(fontMono)
		.background_(panelColor)
		.stringColor_(textColor);

		durBox = NumberBox()
		.value_(5)
		.clipLo_(0.1).clipHi_(300)
		.decimals_(1)
		.font_(fontMono)
		.background_(panelColor)
		.normalColor_(textColor);

		// -- Scalar parameter controls --
		[
			[\lo, 0, 0, 1, 3], [\hi, 1, 0, 1, 3],
			[\vary, 0, 0, 1, 4], [\spread, 0.25, 0, 1, 3],
			[\skew, 0.5, 0, 1, 3], [\width, 1, 0, 2, 3],
			[\index, 1, 0, 4, 3]
		].do { |params|
			var key = params[0];
			scalarBoxes[key] = NumberBox()
			.value_(params[1])
			.clipLo_(params[2]).clipHi_(params[3])
			.decimals_(params[4])
			.font_(fontMonoSm)
			.background_(panelColor)
			.normalColor_(textColor);
		};

		curveMenu = PopUpMenu()
		.items_(["sine", "lin", "step", "exp", "welch"])
		.font_(fontMonoSm)
		.background_(panelColor)
		.stringColor_(textColor);

		// -- Record button --
		recBtn = Button()
		.states_([
			["●  Rec", Color(0.7, 0.1, 0.1), Color(0.92, 0.90, 0.90)],
			["●  Stop Rec", Color.white, Color(0.7, 0.1, 0.1)]
		])
		.font_(fontMonoBold)
		.action_({ |btn|
			if (btn.value == 1) {
				this.startRecording;
			} {
				this.stopRecording;
			};
		});

		// -- Container for multislider grid --
		multiSliderContainer = View().background_(bgColor);

		// -- Assemble layout --
		window.layout_(VLayout(
			// Top bar
			HLayout(
				StaticText().string_("process").font_(fontMonoSm).stringColor_(labelColor),
				[scanTypeMenu, s: 2],
				10,
				StaticText().string_("sound").font_(fontMonoSm).stringColor_(labelColor),
				[soundMenu, s: 3],
				10,
				StaticText().string_("dur").font_(fontMonoSm).stringColor_(labelColor),
				durBox,
				nil,
				[playBtn = Button()
					.states_([
						["▶  Play", Color.white, currentSliderColor],
						["■  Stop", Color.white, currentSliderColor.blend(Color.black, 0.3)]
					])
					.font_(fontMonoBold)
					.action_({ |btn|
						if (btn.value == 1) { this.play } { this.stop };
					}), s: 2],
				5,
				recBtn
			).margins_(6),

			// Scalar params row
			View().background_(panelColor).layout_(
				HLayout(
					*[\lo, \hi, \vary, \spread, \skew, \width, \index].collect({ |key|
						VLayout(
							StaticText().string_(key.asString).font_(fontMonoXs).stringColor_(labelColor).align_(\center),
							scalarBoxes[key]
						)
					}) ++ [
						VLayout(
							StaticText().string_("curve").font_(fontMonoXs).stringColor_(labelColor).align_(\center),
							curveMenu
						)
					]
				).margins_(4).spacing_(4)
			).fixedHeight_(42),

			// Snapshot row
			View().background_(panelColor).layout_(
				HLayout(
					StaticText().string_("name").font_(fontMonoXs).stringColor_(labelColor),
					[snapshotNameField = TextField()
						.font_(fontMonoSm)
						.value_("")
						.fixedHeight_(20), s: 2],
					4,
					Button()
					.states_([["Save", textColor, Color(0.88, 0.92, 0.88)]])
					.font_(fontMonoSm)
					.fixedWidth_(50)
					.action_({
						var name = snapshotNameField.value;
						if (name.size > 0) {
							VesicleSnapshot.add(
								VesicleSnapshot.captureFromGUI(this, name)
							);
							this.prRefreshSnapshotMenu;
							snapshotNameField.value_("");
						};
					}),
					10,
					[snapshotMenu = PopUpMenu()
						.items_(VesicleSnapshot.names)
						.font_(fontMonoSm)
						.background_(panelColor)
						.stringColor_(textColor), s: 3],
					4,
					Button()
					.states_([["Load", textColor, Color(0.88, 0.88, 0.92)]])
					.font_(fontMonoSm)
					.fixedWidth_(50)
					.action_({
						if (VesicleSnapshot.snapshots.size > 0) {
							VesicleSnapshot.applyToGUI(this,
								VesicleSnapshot.snapshots[snapshotMenu.value]
							);
							this.prRefreshSnapshotMenu;
						};
					}),
					4,
					Button()
					.states_([["Del", Color(0.6, 0.2, 0.2), Color(0.92, 0.88, 0.88)]])
					.font_(fontMonoSm)
					.fixedWidth_(40)
					.action_({
						if (VesicleSnapshot.snapshots.size > 0) {
							VesicleSnapshot.removeAt(snapshotMenu.value);
							this.prRefreshSnapshotMenu;
						};
					}),
					10,
					Button()
					.states_([["Timeline", Color.white, Color(0.494, 0.498, 0.604)]])
					.font_(fontMonoSm)
					.fixedWidth_(70)
					.action_({ VesicleTimeline(vesicle) })
				).margins_(4).spacing_(2)
			).fixedHeight_(30),

			// Multi-slider area — fills remaining space
			[multiSliderContainer, s: 10]
		).margins_(4).spacing_(3));

		this.rebuildMultiSliders;
		window.front;
	}

	rebuildMultiSliders {
		var type = [\scan, \scanFold, \scanGliss][scanTypeMenu.value];
		var defs = this.multiParamDefs(type);
		var rows = [], currentRow = [];

		currentSliderColor = sliderColors.choose;
		if (playBtn.notNil) {
			playBtn.states_([
				["▶  Play", Color.white, currentSliderColor],
				["■  Stop", Color.white, currentSliderColor.blend(Color.black, 0.3)]
			]);
		};
		sliderPanels.clear;
		multiSliderContainer.removeAll;

		defs.do { |def, i|
			var key = def[0], min = def[1], max = def[2], default = def[3], warp = def[4];
			var spec = ControlSpec(min, max, warp);
			var panel, slider, valLabel;
			var initialNorm = spec.unmap(default);
			var initVals = numSliders.collect({ initialNorm });
			var drunkBtn, whiteBtn, fromBox, toBox;
			var tooltip = paramTooltips[key] ? "";

			slider = MultiSliderView()
			.size_(numSliders)
			.value_(initVals)
			.fillColor_(currentSliderColor.alpha_(0.55))
			.strokeColor_(currentSliderColor)
			.background_(sliderBgColor)
			.drawLines_(true)
			.drawRects_(false)
			.isFilled_(true)
			.elasticMode_(1)
			.gap_(0)
			.toolTip_(tooltip);

			slider.action_({ this.prUpdateLabel(key) });

			valLabel = StaticText()
			.string_(default.round(0.001).asString)
			.font_(fontMonoXs)
			.stringColor_(labelColor)
			.align_(\right)
			.fixedHeight_(14)
			.fixedWidth_(130);

			fromBox = NumberBox()
			.value_(min)
			.clipLo_(min).clipHi_(max)
			.decimals_(3)
			.font_(fontMonoXs)
			.background_(panelColor)
			.normalColor_(textColor)
			.fixedHeight_(18);

			toBox = NumberBox()
			.value_(max)
			.clipLo_(min).clipHi_(max)
			.decimals_(3)
			.font_(fontMonoXs)
			.background_(panelColor)
			.normalColor_(textColor)
			.fixedHeight_(18);

			drunkBtn = Button()
			.states_([["drunk", Color(0.45, 0.35, 0.28), Color(0.92, 0.88, 0.84)]])
			.font_(fontMonoXs)
			.fixedHeight_(18)
			.fixedWidth_(40)
			.action_({
				var lo = spec.unmap(fromBox.value);
				var hi = spec.unmap(toBox.value);
				var step = (hi - lo).abs * 0.12;
				var current = rrand(lo, hi);
				var vals = numSliders.collect({
					current = (current + step.rand2).clip(lo, hi);
					current;
				});
				slider.value_(vals);
				this.prUpdateLabel(key);
			});

			whiteBtn = Button()
			.states_([["white", Color(0.4, 0.4, 0.43), Color(0.90, 0.90, 0.92)]])
			.font_(fontMonoXs)
			.fixedHeight_(18)
			.fixedWidth_(40)
			.action_({
				var lo = spec.unmap(fromBox.value);
				var hi = spec.unmap(toBox.value);
				slider.value_(numSliders.collect({ rrand(lo, hi) }));
				this.prUpdateLabel(key);
			});

			panel = View().background_(panelColor).toolTip_(tooltip);
			panel.layout_(VLayout(
				HLayout(
					StaticText()
					.string_(key.asString)
					.font_(Font("Monaco", 9, true))
					.stringColor_(textColor)
					.fixedHeight_(12)
					.toolTip_(tooltip),
					nil,
					valLabel
				),
				[slider, s: 4],
				HLayout(fromBox, toBox, nil, drunkBtn, 2, whiteBtn)
			).margins_(3).spacing_(1));

			sliderPanels[key] = IdentityDictionary[
				\slider -> slider,
				\spec -> spec,
				\valLabel -> valLabel
			];

			currentRow = currentRow.add(panel);
			if (currentRow.size == 4) {
				rows = rows.add(currentRow);
				currentRow = [];
			};
		};

		if (currentRow.size > 0) {
			while { currentRow.size < 4 } {
				currentRow = currentRow.add(nil);
			};
			rows = rows.add(currentRow);
		};

		multiSliderContainer.layout_(VLayout(
			*rows.collect({ |row|
				[HLayout(*row).spacing_(3), s: 1]
			})
		).margins_(2).spacing_(3));
	}

	prUpdateLabel { |key|
		var data = sliderPanels[key];
		var slider = data[\slider];
		var spec = data[\spec];
		var label = data[\valLabel];
		var vals = slider.value.collect({ |v| spec.map(v).round(0.001) });
		if (vals.size <= 4) {
			label.string_(vals.collect(_.asString).join(", "));
		} {
			label.string_(
				vals[0].asString ++ " .. " ++ vals.last.asString ++
				" (" ++ vals.size ++ ")"
			);
		};
	}

	prGetValue { |key|
		var data = sliderPanels[key];
		var slider = data[\slider];
		var spec = data[\spec];
		var vals = slider.value.collect({ |v| spec.map(v) });
		if (vals.size == 1) { ^vals[0] } { ^vals };
	}

	prGetCurve {
		^["sine", "lin", "step", "exp", "welch"][curveMenu.value].asSymbol;
	}

	prRefreshSnapshotMenu {
		snapshotMenu.items_(VesicleSnapshot.names);
	}

	play {
		var type, sound, dur, lo, hi, vary, spread, skew, width, index, curve;
		type = [\scan, \scanFold, \scanGliss][scanTypeMenu.value];
		sound = soundMenu.items[soundMenu.value];
		dur = durBox.value;
		lo = scalarBoxes[\lo].value;
		hi = scalarBoxes[\hi].value;
		vary = scalarBoxes[\vary].value;
		spread = scalarBoxes[\spread].value;
		skew = scalarBoxes[\skew].value;
		width = scalarBoxes[\width].value;
		index = scalarBoxes[\index].value;
		curve = this.prGetCurve;

		if (currentPlayer.notNil) {
			currentPlayer.stop;
			currentPlayer = nil;
		};

		switch(type,
			\scan, {
				currentPlayer = VesicleProc.scan(
					vesicle, sound, dur,
					rate: this.prGetValue(\rate),
					gdur: this.prGetValue(\gdur),
					density: this.prGetValue(\density),
					factor: this.prGetValue(\factor),
					lo: lo, hi: hi, vary: vary,
					prob: this.prGetValue(\prob),
					spread: spread,
					bpFreq: this.prGetValue(\bpFreq),
					bpBlend: this.prGetValue(\bpBlend),
					bpRQ: this.prGetValue(\bpRQ),
					skew: skew, width: width, index: index,
					curve: curve,
					pan: this.prGetValue(\pan),
					amp: this.prGetValue(\amp)
				);
			},
			\scanFold, {
				currentPlayer = VesicleProc.scanFold(
					vesicle, sound, dur,
					rate: this.prGetValue(\rate),
					gdur: this.prGetValue(\gdur),
					density: this.prGetValue(\density),
					factor: this.prGetValue(\factor),
					lo: lo, hi: hi, vary: vary,
					prob: this.prGetValue(\prob),
					spread: spread,
					flo: this.prGetValue(\flo),
					fhi: this.prGetValue(\fhi),
					skew: skew, width: width, index: index,
					curve: curve,
					pan: this.prGetValue(\pan),
					amp: this.prGetValue(\amp)
				);
			},
			\scanGliss, {
				currentPlayer = VesicleProc.scanGliss(
					vesicle, sound, dur,
					rateFrom: this.prGetValue(\rateFrom),
					rateTo: this.prGetValue(\rateTo),
					gdur: this.prGetValue(\gdur),
					density: this.prGetValue(\density),
					factor: this.prGetValue(\factor),
					lo: lo, hi: hi, vary: vary,
					prob: this.prGetValue(\prob),
					spread: spread,
					skew: skew, width: width, index: index,
					curve: curve,
					pan: this.prGetValue(\pan),
					amp: this.prGetValue(\amp)
				);
			}
		);
	}

	startRecording {
		var dir = vesicle.path.dirname;
		var timestamp = Date.getDate.stamp;
		var path = dir +/+ "vesicle_" ++ timestamp ++ ".aiff";
		Server.default.record(path);
		isRecording = true;
		("Recording to: " ++ path).postln;
	}

	stopRecording {
		Server.default.stopRecording;
		isRecording = false;
		"Recording stopped.".postln;
	}

	stop {
		if (currentPlayer.notNil) {
			currentPlayer.stop;
			currentPlayer = nil;
		};
		if (playBtn.notNil) { playBtn.value_(0) };
	}
}
