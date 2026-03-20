# snotes

TODO - migration from old repo in progress

## Project goals

The 1.x release has been in place and stable for ages, but the code is very old and looks very clunky.
It was written against a much older version of swing-extras, and as such, is missing out on a lot of
really cool features, most notably the new application extension mechanism. The goal for 2.0 is to
port over the functionality of 1.x, but in a much cleaner, more modular, and more extensible way,
leveraging the latest and greatest features of swing-extras 2.x (2.6 is the latest release as of this
writing, so that will be the starting point for Snotes 2.0).

Specific 2.0 goals:

- keep the UI as close to 1.x as possible, making changes only to address known pain points (query/template setup, for
  one example)
- add extension hooks throughout the app so that extensions can add new functionality.
- clean up the code! The 1.x code feels like a hacky proof-of-concept, which it kind of was. The 2.0 rewrite should show
  best practices, and ideally should be something that can be used as an example of how to build a modular, extensible
  Swing application using swing-extras.
- unit tests! The old code had almost no tests at all. The new code should have excellent test coverage of the core
  functionality.
- Keep business logic out of UI classes! This is terrible practice and makes unit testing MUCH harder.
- rely as much on swing-extras as possible! There are many opportunities to leverage the functionality provided there.
- 1.x allowed for custom editor/viewer themes (font face/size, foreground/background color, etc.) - that has to be
  ported over to 2.0.

## Old code layout - to be replaced

- ca.corbett.snotes.io: classes for loading/saving model objects, also used for querying/searching
- ca.corbett.snotes.model: model objects (snotes, queries, templates, tags)
- ca.corbett.snotes.ui: all UI code (main app window, dialogs, etc.)
- ca.corbett.snotes.ui.actions: all action classes for menu items, toolbar buttons, etc.

### Problems with the old layout

The "io" package makes no sense. It handles loading, caching, and also searching?
The "io" package should entirely focus on loading/saving model objects to/from disk.

The "model" package is probably fine - these are just POJOs that store simple data.

The "ui" package needs a bit of cleanup, but is essentially okay.

The "ui.actions" package is solid - I like having all actions grouped together with human-readable names. It makes
navigating and understanding (or extending) the UI code later much easier. It also makes it very easy to trace which
actions are being invoked from where, by just searching for references to the action class. And by centralizing
certain functionality into an action (like launching the preferences dialog), we have a single place to make changes
later if needed (for example, reloading the UI after the prefs dialog is okayed).

## Proposed new code layout

- ca.corbett.snotes.io: classes for loading/saving model objects (make these classes more focused!)
- ca.corbett.snotes.model: model objects (snotes, queries, templates, tags, etc. - as before)
- ca.corbett.snotes.ui: all UI code (main app window, dialogs, etc. - as before)
- ca.corbett.snotes.ui.actions: all action classes for menu items, toolbar buttons, etc. (as before)
- ca.corbett.snotes.extensions: extension manager, built-in extensions, and related code (new for 2.0!)

The upgrade to latest swing-extras also introduces the concept of application extensions, which is great.
The application code can have extension hooks that extensions can leverage to provide additional functionality,
or to offer alternatives to application built-in behavior.

### Open questions

1. How do we handle caching? Should the service layer cache model objects in memory for faster access? Or should the io
   layer handle caching?
   a) My inclination is to have the service layer handle caching, as it is the layer that will be interacting with the
   model objects the most.
2. What do the extension points look like? Should the io layer have extension points for custom storage backends?
   a) Given how easy it is to add extension points, I think we could go crazy with it and have extension points
   EVERYWHERE. If they never get used, they cost almost nothing at runtime (one quick check with ExtensionManager to see
   if any extension wants to exercise that extension point).
3. The io "cache" classes currently just store up all model objects in memory to make for extremely rapid searching.
   Should we look at using sqlite for enhanced text-based search options? Perhaps an in-memory sqlite database loaded on
   startup?
   a) I'm inclined to port the old io code over more or less as-is for 2.0, and then look at sqlite integration for 3.0
   or later. The current persistence format for this stuff has been stable since 1.0, and I don't feel the need to
   completely overhaul it right away.
