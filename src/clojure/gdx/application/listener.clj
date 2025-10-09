(ns clojure.gdx.application.listener
  (:import (com.badlogic.gdx ApplicationListener)))

(defn create [{:keys [state
                      create
                      dispose
                      render
                      resize]}]
  (let [state @(requiring-resolve state)]
    (reify ApplicationListener
      (create [_]
        (reset! state (let [[f params] create]
                        ((requiring-resolve f) params))))

      (dispose [_]
        ((requiring-resolve dispose) @state))

      (render [_]
        (swap! state (requiring-resolve render)))

      (resize [_ width height]
        ((requiring-resolve resize) @state width height))

      (pause [_])

      (resume [_]))))
