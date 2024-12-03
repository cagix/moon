(ns forge.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.application :as app]
            [forge.system :as system]))

(defn -main []
  (let [{:keys [requires
                config
                components]} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (app/start (reify app/Listener
                 (create  [_]     (run! system/create          components))
                 (dispose [_]     (run! system/dispose         components))
                 (render  [_]     (run! system/render          components))
                 (resize  [_ w h] (run! #(system/resize % w h) components)))
               config)))
