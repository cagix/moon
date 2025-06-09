# cdq.create.db/transform

## schema/edn->value

    1. cdq.schema.animation
    2. cdq.schema.image
    3. cdq.schema.one-to-many -> [OK]
    4. cdq.schema.one-to-one -> [OK]
    5. cdq.ui.editor.widget.animation -> [FIXED]
    6. cdq.ui.editor.widget.image -> [FIXED]

## cdq.db/build, cdq.db/build-all

    1. cdq.ctx.game/generate-level -> [CREATURES]
    2. cdq.dev/learn-skill!, cdq.dev/create-item! -> [UNUSED CODE]
    3. cdq.schema.one-to-many -> [OK]
    4. cdq.schema.one-to-one -> [OK]
    5. cdq.tx.audiovisual/do! -> [AUDIOVISUAL]
    6. cdq.ui.editor.overview-table -> [FIXED]
    7. cdq.ui.editor.widget.one-to-many -> [FIXED]
    8. cdq.ui.editor.widget.one-to-one -> [FIXED]
    9. cdq.mapgen-test -> [UNUSED CODE]

## :s/animation

* only `:entity/animation`
    -> audiovisual & creature.

## Etc.

* :s/relationships
  entity/inventory
  entity/skills
  :effects/audiovisual
  :effects/projectile
  :effects/spawn
  :effects.target/audiovisual

  => also used @ info - texts ? very confusing ...

  => use datomic ?

  https://docs.datomic.com/reference/entities.html
