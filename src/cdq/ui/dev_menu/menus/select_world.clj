(ns cdq.ui.dev-menu.menus.select-world
  (:require [cdq.application :as application]))

(defn create []
  {:label "World"
   :items (for [world-fn [['cdq.level.from-tmx/create
                           {:tmx-file "maps/vampire.tmx"
                            :start-position [32 71]}]
                          ['cdq.level.uf-caves/create
                           {:tile-size 48
                            :texture-path "maps/uf_terrain.png"
                            :spawn-rate 0.02
                            :scaling 3
                            :cave-size 200
                            :cave-style :wide}]
                          ['cdq.level.modules/create
                           {:world/map-size 5,
                            :world/max-area-level 3,
                            :world/spawn-rate 0.05}]]]
            {:label (str "Start " (first world-fn))
             :on-click (fn [_actor {:keys [ctx/config]}]
                         (swap! application/state (requiring-resolve (:reset-game-state! config)) world-fn))})})
