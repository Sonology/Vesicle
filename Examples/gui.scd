// Vesicle GUI — granular scan controller
// Each parameter that supports arrays is controlled via a multislider.
// Snapshots save/restore the full GUI state to disk.
// The timeline sequences snapshots over time.

// 1. Boot server and load Vesicle
s.boot;

(

p = Platform.userAppSupportDir +/+ "downloaded-quarks/Vesicle/Audio/*";
v = Vesicle(p);

)

// 2. Open the GUI
g = VesicleGUI(v);

// The GUI provides:
//  - Process type selector (scan, scanFold, scanGliss)
//  - Sound file selector
//  - Duration control
//  - Scalar parameter row (lo, hi, vary, spread, skew, width, index, curve)
//  - MultiSlider panels for all array-capable parameters
//    with "drunk" and "white" noise generators per slider
//  - Play/Stop toggle button (color follows the sliders)
//  - Record button for capturing audio output
//
// Snapshots:
//  - Type a name and press Save to capture the current state
//  - Select a snapshot from the menu and press Load to restore it
//  - Del removes the selected snapshot
//  - Snapshots are saved to vesicle_snapshots.scd alongside your audio
//  - They persist across sessions
//
// Timeline:
//  - Press the Timeline button to open the sequencer
//  - Select a snapshot from the palette, click Place, then click on the timeline
//  - Drag entries to reposition them in time or across lanes
//  - Click an entry to select it, press Delete to remove
//  - Use zoom and dur controls to adjust the view
//  - Press Play to sequence all entries — processes launch at the correct times
//  - Multiple lanes allow layering simultaneous processes
//  - Timeline entries are saved to vesicle_timeline.scd alongside your audio

// 3. Open the timeline directly (if you already have snapshots)
VesicleTimeline(v);
