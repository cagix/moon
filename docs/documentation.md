
* main loop
* context: assets,graphics,stage,db,world,time
    * stage parts: dev-menu, actionbar, inventory, etc.
    * world parts: entities
        -> entity components .... & effects
* extension points: level, entity, effect , info
* side effects, graphic draw functions @ tick/render what can be called

* Observability: show all tiled-map layers, cell infos explored tiles
    show as tree view entity stuff, inventory stuff ?


        => focus on the game 'components', not libgdx/etc. <=
            => desktop app <=
                * db
                * assets
                    -> fns
                * graphics
                    -> fns
                * ui/stage
                    -> fns
                * world
                    -> fns
                        -> entity data/fns/transactions
                * main-loop

=> what is the defcomponent APi for e.g. new entity component ???

    => example of adding map, effect, etc. editing ...


    * entity components
    * effect components
    * modifiers/stats.
    * stage actors & functions

        => could I pass the stage actors & entity / effects in cdq.edn ?
