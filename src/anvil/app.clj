(ns anvil.app
  (:require [anvil.world :as world]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.context :as ctx]
            [gdl.context.assets :as assets]
            [gdl.context.cursors :as cursors]
            [gdl.context.db :as db]
            [gdl.context.default-font :as default-font]
            [gdl.context.sprite-batch :as sprite-batch]
            [gdl.context.viewport :as viewport]
            [gdl.context.world-viewport :as world-viewport]
            [gdl.graphics :as graphics]
            [gdl.stage :as stage]
            [gdl.ui :as ui]))

(def ^:private ^:dbg-flag pausing? true)

(defn -main []
  (let [{:keys [requires lwjgl3 lifecycle]} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (lwjgl3/start lwjgl3
                  (reify lwjgl3/Application
                    (create [_]
                      (db/setup (:db lifecycle))
                      (assets/setup "resources/")
                      (sprite-batch/setup)
                      (graphics/setup-shape-drawer)
                      (default-font/setup (:default-font lifecycle))
                      (cursors/setup (:cursors lifecycle))
                      (viewport/setup (:viewport lifecycle))
                      (world-viewport/setup (:world-viewport lifecycle))
                      (graphics/setup-tiled-map-renderer ctx/world-unit-scale
                                                         ctx/batch)
                      (ui/setup (:ui lifecycle))
                      (stage/setup)
                      (world/create (:world lifecycle)))

                    (dispose [_]
                      (assets/cleanup)
                      (sprite-batch/cleanup)
                      (graphics/dispose-shape-drawer)
                      (default-font/cleanup)
                      (cursors/cleanup)
                      (stage/cleanup)
                      (ui/cleanup)
                      (world/dispose))

                    (render [_]
                      (clear-screen)
                      (world/render)
                      (stage/render)
                      (stage/act)
                      (world/tick pausing?))

                    (resize [_ w h]
                      (ctx/resize-viewport w h)
                      (graphics/resize-world-viewport w h))))))
