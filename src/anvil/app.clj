(ns anvil.app
  (:require [cdq.context :as context]
            [clojure.gdx.lwjgl :refer [start Application]]))

(def state (atom nil))

(defn -main []
  (let [{:keys [app-config context]} (read-edn-resource "app.edn")]
    (start app-config
           (reify Application
             (create [_]
               (reset! state (context/create context)))

             (dispose [_]
               (context/dispose @state))

             (render [_]
               (swap! state context/frame))

             (resize [_ width height]
               (context/resize @state width height))))))
