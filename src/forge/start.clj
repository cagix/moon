(ns forge.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.application :as app]
            [forge.lifecycle :as lifecycle]))

(defn -main []
  (let [{:keys [requires
                config
                components]} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (app/start (reify app/Listener
                 (create  [_]     (run! lifecycle/create          components))
                 (dispose [_]     (run! lifecycle/dispose         components))
                 (render  [_]     (run! lifecycle/render          components))
                 (resize  [_ w h] (run! #(lifecycle/resize % w h) components)))
               config)))
