(ns cdq.game.create
  (:require [cdq.audio :as audio]
            [cdq.input :as input]
            [cdq.db-impl :as db]
            [cdq.textures-impl]
            [cdq.gdx.graphics :as graphics]
            [cdq.gdx.graphics.camera :as camera]
            [cdq.gdx.graphics.color :as color]
            [cdq.gdx.graphics.viewport :as viewport]
            [cdq.gdx.graphics.tiled-map-renderer :as tm-renderer]
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
                      ctx/textures
                      ctx/graphics
                      ctx/tiled-map-renderer
                      ctx/gdx-graphics
                      ctx/ui-viewport
                      ctx/world
                      ctx/world-viewport
                      ])

; => this has to be pipelined
; and graphics/world abstractions are questionable
; and maybe ctx/gdx ?!
; use at handle draws ctx and world tick etc
; do select-keys so I know what is only used ?
; but nopt even necessary
(defn do! [gdx config]
  (doseq [[name color-params] (:colors (::graphics config))]
    (Colors/put name (color/->obj color-params)))
  (ui/load! (::stage config))
  (let [input (:input gdx)
        graphics ((requiring-resolve (:graphics-impl config)) gdx (::graphics config))
        ui-viewport (viewport/fit (:width  (:ui-viewport (::graphics config)))
                                  (:height (:ui-viewport (::graphics config)))
                                  (camera/orthographic))
        world-unit-scale (:g/world-unit-scale graphics)
        world-viewport (let [world-width  (* (:width  (:world-viewport (::graphics config))) world-unit-scale)
                             world-height (* (:height (:world-viewport (::graphics config))) world-unit-scale)]
                         (viewport/fit world-width
                                       world-height
                                       (camera/orthographic :y-down? false
                                                            :world-width world-width
                                                            :world-height world-height)))
        stage (ui/stage ui-viewport
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
                                          [:ctx/textures :some]
                                          [:ctx/graphics :some]
                                          [:ctx/gdx-graphics :some]
                                          [:ctx/tiled-map-renderer :some]
                                          [:ctx/ui-viewport :some]
                                          [:ctx/world :some]
                                          [:ctx/world-viewport :some]
                                          ])
                       :gdx-graphics (:graphics gdx)
                       :textures (cdq.textures-impl/create (:files gdx))
                       :audio (audio/create gdx (::audio config))
                       :config config
                       :cursors (load-cursors
                                 (:files gdx)
                                 (:graphics gdx)
                                 (:cursors (::graphics config))
                                 (:cursor-path-format (::graphics config)))
                       :db (db/create (::db config))
                       :graphics graphics
                       :ui-viewport ui-viewport
                       :input input
                       :stage stage
                       :tiled-map-renderer (tm-renderer/create (:g/world-unit-scale graphics)
                                                               (:g/batch graphics))
                       :world-viewport world-viewport})
        ((requiring-resolve (:reset-game-state! config)) (::starting-level config))
        (assoc :ctx/mouseover-eid nil))))
