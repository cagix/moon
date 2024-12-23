(ns uf.app
  (:require [anvil.db :as db]
            [anvil.widgets.dev-menu :refer [uf-dev-menu-table]]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.context :as ctx]
            [gdl.graphics :as graphics]
            [gdl.graphics.camera :as camera]
            [gdl.stage :as stage]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]))

; Cards:
; * uf_heroes_simple - images/creatures.png
; * uf_items   - images/items.png
; * uf_skills  - images/skills.png

; => Mix all together as properties w. image / name / etc.

; library - deck of cards -
; card - one of creatures/items/etc.

(comment
 (camera/position graphics/camera)
 (camera/set-position! graphics/camera [30 70])
 (tiled/tm-height tiled-map)
 )

(declare tiled-map)

(defn- tile-color-setter [color x y]
  graphics/white)

(defn -main []
  (let [{:keys [requires lwjgl3 lifecycle]} (-> "uf_app.edn"
                                                io/resource
                                                slurp
                                                edn/read-string)]
    (run! require requires)
    (lwjgl3/start lwjgl3
                  (reify lwjgl3/Application
                    (create [_]
                      (db/setup (:db lifecycle))
                      (ctx/assets-setup)
                      (graphics/setup (:graphics lifecycle))
                      (graphics/setup-viewport (:viewport lifecycle))
                      (graphics/setup-tiled-map-renderer ctx/world-unit-scale
                                                         ctx/batch)
                      (ui/setup (:ui lifecycle))
                      (stage/setup)
                      (stage/add-actor (uf-dev-menu-table))
                      (bind-root tiled-map (tiled/load-tmx-map "maps/vampire.tmx"))
                      (camera/set-position! graphics/camera [30 70]))

                    (dispose [_]
                      (ctx/assets-cleanup)
                      (graphics/cleanup)
                      (stage/cleanup)
                      (ui/cleanup))

                    (render [_]
                      (clear-screen)
                      (graphics/draw-tiled-map tiled-map
                                               tile-color-setter)
                      (stage/render)
                      (stage/act))

                    (resize [_ w h]
                      (graphics/resize w h))))))
