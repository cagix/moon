(ns anvil.app
  (:require [cdq.context :as context]
            [clojure.gdx.lwjgl :as lwjgl]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def state (atom nil))

(defn -main []
  (let [{:keys [requires lwjgl3 context]} (-> "app.edn" io/resource slurp edn/read-string)]
    (run! require requires)
    (lwjgl/start lwjgl3
                 (reify lwjgl/Application
                   (create [_]
                     (reset! state (context/create context)))

                   (dispose [_]
                     (context/dispose @state))

                   (render [_]
                     (swap! state context/render))

                   (resize [_ width height]
                     (context/resize @state width height))))))
