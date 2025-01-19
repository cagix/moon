(ns cdq.application.create)

(def create-components
  '[[:cdq/entity-states                 cdq.application.create.entity.state]
    [:cdq/effects                       cdq.application.create.effects]
    [:cdq/schemas                       cdq.application.create.schemas]
    [:cdq/db                            cdq.application.create.db]
    [:cdq/assets                        cdq.application.create.assets]
    [:cdq.graphics/batch                cdq.application.create.batch]
    [:cdq.graphics/shape-drawer-texture cdq.application.create.shape-drawer-texture]
    [:cdq.graphics/shape-drawer         cdq.application.create.shape-drawer]
    [:cdq.graphics/cursors              cdq.application.create.cursors]
    [:cdq.graphics/default-font         cdq.application.create.default-font]
    [:cdq.graphics/world-unit-scale     cdq.application.create.world-unit-scale]
    [:cdq.graphics/tiled-map-renderer   cdq.application.create.tiled-map-renderer]
    [:cdq.graphics/ui-viewport          cdq.application.create.ui-viewport]
    [:cdq.graphics/world-viewport       cdq.application.create.world-viewport]
    [:cdq.context/stage                 cdq.application.create.stage]
    [:cdq.context/elapsed-time          cdq.application.create.elapsed-time]
    [:cdq.context/player-message        cdq.application.create.player-message]
    [:cdq.context/level                 cdq.application.create.level]
    [:cdq.context/error                 cdq.application.create.error]
    [:cdq.context/tiled-map             cdq.application.create.tiled-map]
    [:cdq.context/explored-tile-corners cdq.application.create.explored-tile-corners]
    [:cdq.context/grid                  cdq.application.create.grid]
    [:cdq.context/raycaster             cdq.application.create.raycaster]
    [:cdq.context/content-grid          cdq.application.create.content-grid]
    [:cdq.context/entity-ids            cdq.application.create.entity-ids]
    [:cdq.context/factions-iterations   cdq.application.create.factions-iterations]
    [:world/potential-field-cache       cdq.application.create.potential-fields]
    [:cdq.context/player-eid            cdq.application.create.player-eid]])

(defn context []
  (reduce (fn [context [k ns-sym]]
            (require ns-sym)
            (let [f (resolve (symbol (str ns-sym "/create")))]
              (assoc context k (f context))))
          {}
          create-components))

; if you see the game as one with the application
; you can change the application during game moves so they can affect the outside 'world'
; that means you can load assets, set up properties, set cursors, configure the font,
; setup the world unti scale,
; the ui/world viewport
; set up stage actors
; and edit the level (which needs to be somehow one data structure?) or just a bunch of callbacks
; and spawn/delete entities

; The game you are playing god
; and the creatures fight good vs evil
; and you can support them etc

; or set the dock icon ... window title etc .... change the window inside the window ... or outside ?
; dev-mode can see the context if we integrate with libgd x backend and atom state
; as a javafx application ?
; which can see also stacktraces/memory/etc.
; 'dev-menu' for all games made in this style
; with database editor
