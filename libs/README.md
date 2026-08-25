# Local development jars

Place third-party mod jars required for compilation/testing in this directory on your local clone.

`*.jar` is ignored by Git and must not be committed.

Initial inspection set:

- Tensura: Reincarnated 2.0.1.2
- matching ManasCore build from the target pack
- Tensura: Better Subordinates 1.2.4
- MCA Reborn 7.7.36-beta.3+1.21.1
- Townstead 0.7.6+1.21.1

Later inspection set as needed:

- Tensura: Skill Obtain 1.1
- Tensura Unique Monsters 1.0.2
- MobChampions 1.0.2.2
- target mob mods when a public API or exact entity behavior must be inspected

The Gradle build currently adds every local `libs/*.jar` to compile-only and local runtime configurations. We will replace individual jars with Maven coordinates where that is reliable and legally cleaner.
