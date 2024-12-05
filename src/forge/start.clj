(ns forge.start
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [forge.app :as app]
            [forge.core :refer [app-create
                                app-dispose
                                app-render
                                app-resize]]))

(defn -main []
  (let [{:keys [requires components] :as config} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (app/start config
               (reify app/Listener
                 (create [_]
                   (run! app-create components))

                 (dispose [_]
                   (run! app-dispose components))

                 (render [_]
                   (run! app-render components))

                 (resize [_ w h]
                   (run! #(app-resize % w h) components))))))
