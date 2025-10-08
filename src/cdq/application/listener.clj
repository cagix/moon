(ns cdq.application.listener
  (:require [cdq.game.dispose :as dispose]
            [cdq.game.render :as render]
            [cdq.game.resize :as resize]
            [clojure.config :as config])
  (:import (com.badlogic.gdx ApplicationListener)))

(def state (atom nil))

(defn create []
  (reify ApplicationListener
    (create [_]
      (let [[f config] (config/edn-resource "config.edn")]
        (reset! state ((requiring-resolve f) config))
        (println "after create")
        (println (keys @state))
        ))

    (dispose [_]
      (dispose/do! @state))

    (render [_]
      (swap! state render/do!))

    (resize [_ width height]
      (resize/do! @state width height))

    (pause [_])

    (resume [_])))
