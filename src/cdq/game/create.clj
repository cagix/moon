(ns cdq.game.create
  (:require [cdq.audio :as audio]
            [cdq.input :as input]
            [cdq.db-impl :as db]
            [cdq.gdx.graphics :as graphics]
            [cdq.gdx.graphics.color :as color]
            [cdq.gdx.ui :as ui]
            [cdq.malli :as m]
            [qrecord.core :as q])
  (:import (com.badlogic.gdx Files)
           (com.badlogic.gdx.graphics Colors
                                      Pixmap)))

(defn- load-cursors [files graphics cursors cursor-path-format]
  (update-vals cursors
               (fn [[file [hotspot-x hotspot-y]]]
                 (let [pixmap (Pixmap. (Files/.internal files (format cursor-path-format file)))
                       cursor (graphics/cursor graphics pixmap hotspot-x hotspot-y)]
                   (.dispose pixmap)
                   cursor))))

(q/defrecord Context [ctx/schema
                      ctx/config
                      ctx/cursors
                      ctx/input
                      ctx/db
                      ctx/audio
                      ctx/stage
                      ctx/mouseover-eid
                      ctx/player-eid
                      ctx/graphics
                      ctx/gdx-graphics
                      ctx/world])

(defn do! [gdx config]
  (doseq [[name color-params] (:colors (::graphics config))]
    (Colors/put name (color/->obj color-params)))
  (ui/load! (::stage config))
  (let [input (:input gdx)
        graphics ((requiring-resolve (:graphics-impl config)) gdx (::graphics config))
        stage (ui/stage (:g/ui-viewport graphics)
                        (:g/batch       graphics))]
    (input/set-processor! input stage)
    (-> (map->Context {:schema (m/schema [:map {:closed true}
                                          [:ctx/schema :some]
                                          [:ctx/config :some]
                                          [:ctx/cursors :some]
                                          [:ctx/input :some]
                                          [:ctx/db :some]
                                          [:ctx/audio :some]
                                          [:ctx/stage :some]
                                          [:ctx/mouseover-eid :any]
                                          [:ctx/player-eid :some]
                                          [:ctx/graphics :some]
                                          [:ctx/gdx-graphics :some]
                                          [:ctx/world :some]])
                       :gdx-graphics (:graphics gdx)
                       :audio (audio/create gdx (::audio config))
                       :config config
                       :cursors (load-cursors
                                 (:files gdx)
                                 (:graphics gdx)
                                 (:cursors (::graphics config))
                                 (:cursor-path-format (::graphics config)))
                       :db (db/create (::db config))
                       :graphics graphics
                       :input input
                       :stage stage})
        ((requiring-resolve (:reset-game-state! config)) (::starting-level config))
        (assoc :ctx/mouseover-eid nil))))
