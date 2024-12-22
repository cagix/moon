(ns anvil.app
  (:require [clojure.gdx.backends.lwjgl3 :as lwjgl3]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn create  [_])
(defn dispose [_])
(defn render  [_])
(defn resize  [_ w h])

(defn -main []
  (let [{:keys [requires lwjgl3 lifecycle]} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (lwjgl3/start lwjgl3
                  (reify lwjgl3/Application
                    (create [_]
                      (create lifecycle))

                    (dispose [_]
                      (dispose lifecycle))

                    (render [_]
                      (render lifecycle))

                    (resize [_ w h]
                      (resize lifecycle w h))))))
