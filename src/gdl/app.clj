(ns gdl.app
  (:require [clojure.application :as application]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.gdx.backends.lwjgl3.application :as lwjgl3]))

(def state (atom nil))

(defn start [config-path {:keys [create
                                 dispose
                                 render
                                 resize]}]
  (let [config (-> config-path
                   io/resource
                   slurp
                   edn/read-string)]
    (lwjgl3/create (reify application/Listener
                     (create [_ context]
                       (reset! state (create context (:context config))))

                     (dispose [_]
                       (dispose @state))

                     (pause [_])

                     (render [_]
                       (swap! state render))

                     (resize [_ width height]
                       (resize @state width height))

                     (resume [_]))
                   (:app config))))
