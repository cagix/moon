(ns cdq.world.context
  (:require [cdq.create.elapsed-time :as elapsed-time]
            [cdq.create.error :as error]
            [cdq.create.player-message :as player-message]
            [cdq.create.level :as level]
            cdq.create.grid
            cdq.create.factions-iterations
            cdq.create.explored-tile-corners
            cdq.create.entity-ids
            cdq.create.potential-fields
            cdq.create.player-eid
            cdq.create.content-grid
            cdq.create.raycaster
            [cdq.stage :as stage]
            [clojure.utils :as utils]))

(def actors
  '[(cdq.create.stage.dev-menu/create (cdq.create.stage.dev-menu.config/create))
    (cdq.create.stage.actionbar/create)
    (cdq.create.stage.hp-mana-bar/create)
    (cdq.create.stage.windows/create [(cdq.create.stage.entity-info-window/create)
                                      (cdq.widgets.inventory/create)])
    (cdq.create.stage.player-state/create)
    (cdq.create.stage.player-message/actor)])

(defn- reset-stage! [{:keys [cdq.context/stage] :as context}]
  (com.badlogic.gdx.scenes.scene2d.Stage/.clear stage)
  (run! #(stage/add-actor stage %)
        (map (fn [fn-invoc]
               (utils/req-resolve-call fn-invoc context))
             actors)))

(defn reset [context world-id]
  (reset-stage! context)
  (let [{:keys [tiled-map start-position] :as level} (level/create context world-id)
        grid (cdq.create.grid/create tiled-map)
        context (merge context
                       {:cdq.context/content-grid (cdq.create.content-grid/create tiled-map)
                        :cdq.context/elapsed-time (elapsed-time/create)
                        :cdq.context/entity-ids (cdq.create.entity-ids/create)
                        :cdq.context/player-message (player-message/create)
                        :cdq.context/level level
                        :cdq.context/error (error/create)
                        :cdq.context/explored-tile-corners (cdq.create.explored-tile-corners/create tiled-map)
                        :cdq.context/grid grid
                        :cdq.context/tiled-map tiled-map
                        :cdq.context/raycaster (cdq.create.raycaster/create grid)
                        :cdq.context/factions-iterations (cdq.create.factions-iterations/create)
                        :world/potential-field-cache (cdq.create.potential-fields/create)})]
    (assoc context :cdq.context/player-eid (cdq.create.player-eid/create context))))
