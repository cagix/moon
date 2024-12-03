(ns forge.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.application :as app]
            [forge.core :refer :all]))

(defn -main []
  (let [{:keys [requires
                config
                components]} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (app/start (reify app/Listener
                 (create  [_]     (run! app-create          components))
                 (dispose [_]     (run! app-dispose         components))
                 (render  [_]     (run! app-render          components))
                 (resize  [_ w h] (run! #(app-resize % w h) components)))
               config)))
