
VesicleSnapshot {
	classvar <>snapshots;
	classvar <>filePath;

	*initClass {
		snapshots = [];
	}

	*load { |path|
		filePath = path;
		if (File.exists(path)) {
			snapshots = try { thisProcess.interpreter.executeFile(path) } ? [];
		} {
			snapshots = [];
		};
	}

	*save {
		var file;
		file = File(filePath, "w");
		file.write("[\n");
		snapshots.do { |snap, i|
			file.write(this.prFormatEvent(snap));
			if (i < (snapshots.size - 1)) { file.write(",\n") } { file.write("\n") };
		};
		file.write("]\n");
		file.close;
	}

	*add { |event|
		snapshots = snapshots.add(event);
		this.save;
	}

	*removeAt { |index|
		snapshots.removeAt(index);
		this.save;
	}

	*names {
		^snapshots.collect({ |s| s[\name] });
	}

	*at { |indexOrName|
		if (indexOrName.isKindOf(Integer)) {
			^snapshots[indexOrName];
		} {
			^snapshots.detect({ |s| s[\name] == indexOrName });
		};
	}

	*captureFromGUI { |gui, name|
		var event = (
			name: name,
			processType: [\scan, \scanFold, \scanGliss][gui.scanTypeMenu.value],
			sound: gui.soundMenu.items[gui.soundMenu.value],
			dur: gui.durBox.value,
			scalars: (),
			curve: [\sine, \lin, \step, \exp, \welch][gui.curveMenu.value],
			sliders: ()
		);
		[\lo, \hi, \vary, \spread, \skew, \width, \index].do { |key|
			event[\scalars][key] = gui.scalarBoxes[key].value;
		};
		gui.sliderPanels.keysValuesDo { |key, data|
			event[\sliders][key] = data[\slider].value.copy;
		};
		^event;
	}

	*applyToGUI { |gui, snapshot|
		var typeIndex, soundIndex;

		// Set process type
		typeIndex = [\scan, \scanFold, \scanGliss].indexOf(snapshot[\processType]) ? 0;
		gui.scanTypeMenu.value_(typeIndex);

		// Set sound
		soundIndex = gui.soundMenu.items.indexOf(snapshot[\sound]);
		if (soundIndex.notNil) {
			gui.soundMenu.value_(soundIndex);
		} {
			("VesicleSnapshot: sound '" ++ snapshot[\sound] ++ "' not found").warn;
		};

		// Set duration
		gui.durBox.value_(snapshot[\dur]);

		// Rebuild sliders for the correct process type
		gui.rebuildMultiSliders;

		// Set scalar params
		[\lo, \hi, \vary, \spread, \skew, \width, \index].do { |key|
			if (snapshot[\scalars][key].notNil) {
				gui.scalarBoxes[key].value_(snapshot[\scalars][key]);
			};
		};

		// Set curve
		gui.curveMenu.value_(
			[\sine, \lin, \step, \exp, \welch].indexOf(snapshot[\curve]) ? 0
		);

		// Set multi-slider values
		snapshot[\sliders].keysValuesDo { |key, vals|
			var data = gui.sliderPanels[key];
			if (data.notNil) {
				data[\slider].value_(vals);
				gui.prUpdateLabel(key);
			};
		};
	}

	// -- Serialization helpers --

	*prFormatEvent { |event|
		var parts = [];
		parts = parts.add("name: " ++ event[\name].asCompileString);
		parts = parts.add("processType: " ++ event[\processType].asCompileString);
		parts = parts.add("sound: " ++ event[\sound].asCompileString);
		parts = parts.add("dur: " ++ event[\dur].asString);
		parts = parts.add("curve: " ++ event[\curve].asCompileString);

		// Scalars
		parts = parts.add("scalars: " ++ this.prFormatSubEvent(event[\scalars]));

		// Sliders
		parts = parts.add("sliders: " ++ this.prFormatSliders(event[\sliders]));

		^"(" ++ parts.join(", ") ++ ")";
	}

	*prFormatSubEvent { |event|
		var parts = [];
		event.keysValuesDo { |key, val|
			parts = parts.add(key.asString ++ ": " ++ val.round(0.0001).asString);
		};
		^"(" ++ parts.join(", ") ++ ")";
	}

	*prFormatSliders { |event|
		var parts = [];
		event.keysValuesDo { |key, vals|
			parts = parts.add(key.asString ++ ": " ++ this.prFormatArray(vals));
		};
		^"(\n\t\t" ++ parts.join(",\n\t\t") ++ "\n\t)";
	}

	*prFormatArray { |array|
		^"[" ++ array.collect({ |v| v.round(0.0001).asString }).join(", ") ++ "]";
	}
}
