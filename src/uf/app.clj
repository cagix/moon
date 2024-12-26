(ns uf.app ; TODO keep own state
  #_(:require [anvil.widgets.dev-menu :refer [uf-dev-menu-table]]
              [clojure.gdx :refer [white]]
            [clojure.gdx.lwjgl :as lwjgl]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.context :as ctx :refer [draw-tiled-map]]
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
 (camera/position ctx/camera)
 (camera/set-position! ctx/camera [30 70])
 (tiled/tm-height tiled-map)
 )

#_(declare tiled-map)

#_(defn- tile-color-setter [color x y]
  white)

#_(defn -main []
    (let [{:keys [requires lwjgl3 lifecycle]} (-> "uf_app.edn"
                                                  io/resource
                                                  slurp
                                                  edn/read-string)]
      (run! require requires)
      (lwjgl/start lwjgl3
                   (reify lwjgl/Application
                     (create [_]
                       (ctx/setup-db (:db lifecycle))
                       (assets/setup "resources/")
                       (sprite-batch/setup)
                       (viewport-ctx/setup (:viewport lifecycle))
                       (world-viewport/setup (:world-viewport lifecycle))
                       (tiled-map-renderer/setup ctx/world-unit-scale
                                                 ctx/batch)
                       (ui/setup (:ui lifecycle))
                       (stage/setup)
                       (ctx/create)
                       (ctx/add-actor @app/state (uf-dev-menu-table @app/state))
                       (bind-root tiled-map (tiled/load-tmx-map "maps/vampire.tmx"))
                       (camera/set-position! ctx/camera [30 70]))

                     (dispose [_]
                       (assets/cleanup)
                       (sprite-batch/cleanup)
                       (stage/cleanup)
                       (ui/cleanup))

                     (render [_]
                       (let [c @app/state]
                         (clear-screen)
                         (draw-tiled-map c tiled-map tile-color-setter)
                         (stage/render)
                         (stage/act)))

                     (resize [_ w h]
                       (viewport/update ctx/viewport w h :center-camera? true)
                       (viewport/update ctx/world-viewport w h :center-camera? false))))))
