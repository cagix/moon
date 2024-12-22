(ns uf.app
  (:require [anvil.db :as db]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.assets :as assets]
            [gdl.graphics :as graphics]
            [gdl.graphics.camera :as camera]
            [gdl.stage :as stage]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]))

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
                      (assets/setup)
                      (graphics/setup (:graphics lifecycle))
                      (ui/setup (:ui lifecycle))
                      (stage/setup)
                      (bind-root tiled-map (tiled/load-tmx-map "maps/vampire.tmx"))
                      (camera/set-position! graphics/camera [30 70]))

                    (dispose [_]
                      (assets/cleanup)
                      (graphics/cleanup)
                      (stage/cleanup)
                      (ui/cleanup))

                    (render [_]
                      (graphics/clear-screen)
                      (graphics/draw-tiled-map tiled-map
                                               tile-color-setter)
                      (stage/render)
                      (stage/act))

                    (resize [_ w h]
                      (graphics/resize w h))))))
