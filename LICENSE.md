# License and Asset Clarity

This repository uses a mixed-license layout so code, story data, and assets are clear before alpha distribution.

## Code and scripts

Unless a file says otherwise, Java source, Gradle files, shell/Python/Node scripts, and documentation helper code in this repository are licensed under the MIT License:

```text
MIT License

Copyright (c) 2026 YJLi-new and contributors

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Bundled story data and authored narrative content

Unless a file says otherwise, JSON/YAML story content under these paths is released for non-commercial sharing and adaptation under **Creative Commons Attribution-NonCommercial-ShareAlike 4.0 International (CC BY-NC-SA 4.0)**:

```text
src/main/resources/data/ebb/
authoring/
docs/story_pack_tutorial.md examples
```

Before a public release, replace or supplement this summary with the full CC BY-NC-SA 4.0 legal text or a platform-supported license identifier.

## Placeholder visual/audio assets

Unless a file says otherwise, placeholder Ebb-owned art/model/animation assets under `src/main/resources/assets/ebb/` are also treated as **CC BY-NC-SA 4.0**. They are temporary alpha assets and should be replaced or reviewed before a polished release.

## Third-party and platform content

This repository does not license Minecraft, Fabric API, Fabric Loader, GeckoLib, Mojang assets, Microsoft services, or third-party dependencies. Those projects remain under their own licenses and distribution rules. Do not upload dependency jars as part of an Ebb release unless the target platform and dependency license explicitly allow it.

## Contribution note

Future contributors should mark files that need different terms. If no explicit marker exists, contributions follow the repository split above: code/scripts under MIT, story/data/assets under CC BY-NC-SA 4.0.
