(ns anvil.app
  (:require [anvil.world :as world]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.context :as ctx]
            [gdl.stage :as stage]
            [gdl.ui :as ui]))

(def ^:private ^:dbg-flag pausing? true)

(defn -main []
  (let [{:keys [requires lwjgl3 lifecycle]} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (lwjgl3/start lwjgl3
                  (reify lwjgl3/Application
                    (create [_ gdx-context]
                      (ui/setup (:ui lifecycle))
                      ; TODO pass vector because order is important
                      (ctx/create gdx-context
                                  {:gdl.context/unit-scale 1
                                   :gdl.context/assets "resources/"
                                   :gdl.context/db (:db lifecycle)
                                   :gdl.context/batch nil
                                   :gdl.context/shape-drawer nil
                                   :gdl.context/default-font (:default-font lifecycle)
                                   :gdl.context/cursors (:cursors lifecycle)
                                   :gdl.context/viewport (:viewport lifecycle)
                                   :gdl.context/tiled-map-renderer nil
                                   :gdl.context/world-unit-scale (:tile-size lifecycle)
                                   :gdl.context/world-viewport (:world-viewport lifecycle)
                                   })
                      (stage/setup)
                      (world/create @ctx/state
                                    (:world lifecycle)))

                    (dispose [_]
                      (ctx/cleanup @ctx/state)
                      (stage/cleanup)
                      (ui/cleanup)
                      (world/dispose))

                    (render [_]
                      (let [c @ctx/state]
                        (clear-screen)
                        (world/render c)
                        (stage/render)
                        (stage/act)
                        (world/tick c pausing?)))

                    (resize [_ w h]
                      (let [{:keys [gdl.context/viewport
                                    gdl.context/world-viewport]} @ctx/state]
                        (viewport/update viewport w h :center-camera? true)
                        (viewport/update world-viewport w h :center-camera? false)))))))
