(ns anvil.app
  (:require [anvil.db :as db]
            [anvil.lifecycle.create :refer [create-world dispose-world]]
            [anvil.lifecycle.render :refer [render-world]]
            [anvil.lifecycle.update :refer [update-world]]
            [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils.screen-utils :as screen-utils]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.assets :as assets]
            [gdl.graphics :as g]
            [gdl.stage :as stage]
            [gdl.ui :as ui]))

(defn- start [{:keys [requires db app-config graphics ui world-id]}]
  (lwjgl3/start app-config
                (reify lwjgl3/Application
                  (create [_]
                    (run! require requires)
                    (db/setup db)
                    (assets/setup)
                    (g/setup graphics)
                    (ui/setup ui)
                    (stage/setup)
                    (create-world (db/build world-id)))

                  (dispose [_]
                    (assets/cleanup)
                    (g/cleanup)
                    (ui/cleanup)
                    (stage/cleanup)
                    (dispose-world))

                  (render [_]
                    (screen-utils/clear color/black)
                    (render-world)
                    (stage/render)
                    (stage/act)
                    (update-world))

                  (resize [_ w h]
                    (g/resize w h)))))

(defn -main []
  (-> "app.edn"
      io/resource
      slurp
      edn/read-string
      start))
