(ns anvil.app
  (:require [anvil.db :as db]
            [anvil.world :as world]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.assets :as assets]
            [gdl.graphics :as graphics]
            [gdl.stage :as stage]
            [gdl.ui :as ui]))

(defn -main []
  (let [{:keys [requires lwjgl3 lifecycle]} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (lwjgl3/start lwjgl3
                  (reify lwjgl3/Application
                    (create [_]
                      (db/setup (:db lifecycle))
                      (assets/setup)
                      (graphics/setup (:graphics lifecycle))
                      (ui/setup (:ui lifecycle))
                      (stage/setup)
                      (world/create (:world lifecycle)))

                    (dispose [_]
                      (assets/cleanup)
                      (graphics/cleanup)
                      (stage/cleanup)
                      (ui/cleanup)
                      (world/dispose))

                    (render [_]
                      (graphics/clear-screen)
                      (world/render)
                      (stage/render)
                      (stage/act)
                      (world/tick))

                    (resize [_ w h]
                      (graphics/resize w h))))))
