(ns cdq.start
  (:require [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl3.application :as app]
            [clojure.java.io :as io]
            [cdq.app :as app])
  (:gen-class))

(def state (atom nil))

(defn -main []
  (let [config (-> "app.edn" io/resource slurp edn/read-string)]
    (app/create (reify app/Listener
                  (create [_ context]
                    (reset! state (app/create context (:context config))))

                  (dispose [_]
                    (app/dispose @state))

                  (render [_]
                    (swap! state app/render))

                  (resize [_ width height]
                    (app/resize @state width height)))
                (:app config))))
